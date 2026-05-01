# Demostracion del flujo Event Grid y eliminacion de usuario - S8/S9
# BFF: Azure Container Apps
# Muestra:
#   1. Prestamo creado -> evento Biblioteca.PrestamoCreado -> notificacion
#   2. Devolucion -> evento Biblioteca.PrestamoDevuelto -> notificacion
#   3. Usuario eliminado con prestamos -> cascade delete -> evento Biblioteca.UsuarioEliminado
#
# Uso:
#   .\scripts\demo-eventgrid-flow.ps1
#
# Para apuntar a otro BFF:
#   $env:BFF_URL = "https://mi-bff.azurecontainerapps.io"; .\scripts\demo-eventgrid-flow.ps1

$ErrorActionPreference = "Stop"

$BFF = if ($env:BFF_URL) {
    $env:BFF_URL.TrimEnd('/')
} else {
    "https://bff-biblioteca-kath2026.orangemushroom-45a0eb3b.eastus2.azurecontainerapps.io"
}

$SEP = "=" * 72
$FechaHoy = (Get-Date).ToString("yyyy-MM-dd")
$FechaDev = (Get-Date).AddDays(30).ToString("yyyy-MM-dd")

function Convert-JsonSafe {
    param([string]$Content)

    if ([string]::IsNullOrWhiteSpace($Content)) {
        return $null
    }

    try {
        return $Content | ConvertFrom-Json
    } catch {
        return $null
    }
}

function Read-ErrorBody {
    param($ErrorRecord)

    if ($ErrorRecord.ErrorDetails -and $ErrorRecord.ErrorDetails.Message) {
        return $ErrorRecord.ErrorDetails.Message
    }

    if ($ErrorRecord.Exception.Response) {
        try {
            $stream = $ErrorRecord.Exception.Response.GetResponseStream()
            if ($stream) {
                $reader = New-Object System.IO.StreamReader($stream)
                return $reader.ReadToEnd()
            }
        } catch {
            return ""
        }
    }

    return ""
}

function Invoke-Api {
    param(
        [Parameter(Mandatory = $true)][string]$Method,
        [Parameter(Mandatory = $true)][string]$Path,
        [object]$Body = $null,
        [int[]]$ExpectedStatus = @(200)
    )

    $uri = if ($Path.StartsWith("http")) { $Path } else { "$BFF$Path" }
    $params = @{
        Method = $Method
        Uri = $uri
        Headers = @{ "Accept" = "application/json" }
        ErrorAction = "Stop"
    }

    if ($PSVersionTable.PSVersion.Major -lt 6) {
        $params.UseBasicParsing = $true
    }

    if ($null -ne $Body) {
        $jsonBody = $Body | ConvertTo-Json -Depth 10 -Compress
        $params.ContentType = "application/json; charset=utf-8"
        $params.Body = $jsonBody
        Write-Host "Body: $jsonBody"
    }

    Write-Host "$Method $uri"

    try {
        $response = Invoke-WebRequest @params
        $statusCode = [int]$response.StatusCode
        Write-Host "HTTP $statusCode"

        if ($ExpectedStatus -notcontains $statusCode) {
            throw "HTTP inesperado. Esperado: $($ExpectedStatus -join ', '), recibido: $statusCode"
        }

        $parsed = Convert-JsonSafe $response.Content
        if ($null -ne $parsed) {
            $parsed | ConvertTo-Json -Depth 8
        } elseif ($response.Content) {
            Write-Host $response.Content
        }

        return [pscustomobject]@{
            StatusCode = $statusCode
            Json = $parsed
            Content = $response.Content
        }
    } catch {
        $status = "N/A"
        if ($_.Exception.Response -and $_.Exception.Response.StatusCode) {
            $status = [int]$_.Exception.Response.StatusCode
        }

        Write-Host "HTTP $status"
        $errorBody = Read-ErrorBody $_
        if ($errorBody) {
            Write-Host "Respuesta del servidor:"
            Write-Host $errorBody
        }

        throw
    }
}

function Get-DataArray {
    param([object]$ApiResult)

    if ($null -eq $ApiResult.Json) {
        return @()
    }

    if ($ApiResult.Json.data -is [System.Array]) {
        return @($ApiResult.Json.data)
    }

    if ($null -ne $ApiResult.Json.data) {
        return @($ApiResult.Json.data)
    }

    return @()
}

function Get-AvailableBook {
    param([int[]]$ExcludeIds = @())

    $librosResult = Invoke-Api -Method "GET" -Path "/api/libros" -ExpectedStatus @(200)
    $libros = Get-DataArray $librosResult

    $available = @($libros | Where-Object {
        ($_.disponible -eq $true -or $_.estado -eq "DISPONIBLE") -and
        ($ExcludeIds -notcontains [int]$_.id)
    })

    if ($available.Count -eq 0) {
        throw "No hay libros DISPONIBLE para continuar la demo. Devuelve o libera algun libro antes de grabar."
    }

    return $available[0]
}

function Get-FirstUser {
    $usuariosResult = Invoke-Api -Method "GET" -Path "/api/usuarios" -ExpectedStatus @(200)
    $usuarios = Get-DataArray $usuariosResult

    if ($usuarios.Count -eq 0) {
        throw "No hay usuarios disponibles para crear prestamos."
    }

    return $usuarios[0]
}

Write-Host $SEP
Write-Host "BFF = $BFF"
Write-Host $SEP

Write-Host ""
Write-Host "1) Health check del BFF"
Write-Host $SEP
Invoke-Api -Method "GET" -Path "/actuator/health" -ExpectedStatus @(200) | Out-Null

Write-Host ""
Write-Host "2) Seleccionando usuario y libro disponible para prestamo"
Write-Host $SEP
$usuarioBase = Get-FirstUser
$libroPrestamo = Get-AvailableBook
Write-Host "Usuario seleccionado: id=$($usuarioBase.id), nombre=$($usuarioBase.nombre)"
Write-Host "Libro seleccionado: id=$($libroPrestamo.id), titulo=$($libroPrestamo.titulo)"

Write-Host ""
Write-Host "3) POST /api/prestamos - publica Biblioteca.PrestamoCreado"
Write-Host $SEP
$bodyPrestamo = @{
    usuarioId = [int]$usuarioBase.id
    libroId = [int]$libroPrestamo.id
    fechaPrestamo = $FechaHoy
    fechaDevolucionEstimada = $FechaDev
    observacion = "Demo S9 - flujo Event Grid PowerShell"
}
$respCreate = Invoke-Api -Method "POST" -Path "/api/prestamos" -Body $bodyPrestamo -ExpectedStatus @(201)
$prestamoId = $respCreate.Json.data.id

if (-not $prestamoId) {
    throw "No se pudo extraer prestamoId desde la respuesta de creacion."
}

Write-Host "Prestamo creado con ID: $prestamoId"
Write-Host "Verificar Log Stream: [NOTIFICACION SIMULADA] ... PRESTAMO REGISTRADO ..."

Write-Host ""
Write-Host "4) GET /api/prestamos/$prestamoId - confirma estado ACTIVO"
Write-Host $SEP
Invoke-Api -Method "GET" -Path "/api/prestamos/$prestamoId" -ExpectedStatus @(200) | Out-Null

Write-Host ""
Write-Host "5) POST /api/prestamos/$prestamoId/devolucion - publica Biblioteca.PrestamoDevuelto"
Write-Host $SEP
$bodyDev = @{
    fechaDevolucionReal = $FechaHoy
    observacion = "Devuelto en buen estado - demo S9"
}
Invoke-Api -Method "POST" -Path "/api/prestamos/$prestamoId/devolucion" -Body $bodyDev -ExpectedStatus @(200) | Out-Null
Write-Host "Verificar Log Stream: [NOTIFICACION SIMULADA] ... DEVOLUCION REGISTRADA ..."

Write-Host ""
Write-Host "6) DEMO S9 - eliminar usuario con prestamos asociados"
Write-Host $SEP

$stamp = Get-Date -Format "yyyyMMddHHmmssfff"
$bodyUsuario = @{
    nombre = "Usuario Demo S9"
    correo = "demo-s9-$stamp@test.com"
    telefono = "999000001"
}
$respUsuario = Invoke-Api -Method "POST" -Path "/api/usuarios" -Body $bodyUsuario -ExpectedStatus @(201)
$usuarioId = $respUsuario.Json.data.id

if (-not $usuarioId) {
    throw "No se pudo extraer usuarioId desde la respuesta de creacion."
}

Write-Host "Usuario creado con ID: $usuarioId"

$libroCascade = Get-AvailableBook
Write-Host "Libro para cascade: id=$($libroCascade.id), titulo=$($libroCascade.titulo)"

$bodyPrestamoCascade = @{
    usuarioId = [int]$usuarioId
    libroId = [int]$libroCascade.id
    fechaPrestamo = $FechaHoy
    fechaDevolucionEstimada = $FechaDev
    observacion = "Prestamo para demo cascade delete S9"
}
$respPrestamoCascade = Invoke-Api -Method "POST" -Path "/api/prestamos" -Body $bodyPrestamoCascade -ExpectedStatus @(201)
$prestamoCascadeId = $respPrestamoCascade.Json.data.id
Write-Host "Prestamo cascade creado con ID: $prestamoCascadeId"

Write-Host ""
Write-Host "DELETE /api/usuarios/$usuarioId - cascade delete + Biblioteca.UsuarioEliminado"
$respDelete = Invoke-Api -Method "DELETE" -Path "/api/usuarios/$usuarioId" -ExpectedStatus @(200)
$prestamosEliminados = $respDelete.Json.data.prestamosEliminados
Write-Host "prestamosEliminados: $prestamosEliminados"

if ([int]$prestamosEliminados -lt 1) {
    throw "La demo S9 esperaba prestamosEliminados >= 1."
}

Write-Host ""
Write-Host $SEP
Write-Host "Flujo S8/S9 completado."
Write-Host ""
Write-Host "Checklist para el video:"
Write-Host "  [ ] BFF en Azure respondiendo: HTTP 200"
Write-Host "  [ ] POST /prestamos: HTTP 201 + Biblioteca.PrestamoCreado en Log Stream"
Write-Host "  [ ] POST devolucion: HTTP 200 + Biblioteca.PrestamoDevuelto en Log Stream"
Write-Host "  [ ] DELETE /usuarios/{id}: HTTP 200 + prestamosEliminados >= 1"
Write-Host "  [ ] Log Stream: [NOTIFICACION SIMULADA] Asunto: USUARIO ELIMINADO"
Write-Host "  [ ] Event Grid Subscription: Succeeded con 3 filtros"
Write-Host $SEP
