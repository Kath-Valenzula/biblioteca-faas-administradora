# Demostracion del flujo Event Grid y eliminacion de usuario — S8/S9
# BFF: Azure Container Apps
# Muestra: prestamo creado -> evento -> notificacion
#          devolucion    -> evento -> notificacion
#          usuario eliminado con prestamos -> cascade delete -> evento UsuarioEliminado
#
# Uso:
#   .\scripts\demo-eventgrid-flow.ps1
# Para apuntar a otro BFF:
#   $env:BFF_URL = "https://mi-bff.azurecontainerapps.io"; .\scripts\demo-eventgrid-flow.ps1

$BFF = if ($env:BFF_URL) { $env:BFF_URL.TrimEnd('/') } `
       else { "https://bff-biblioteca-kath2026.orangemushroom-45a0eb3b.eastus2.azurecontainerapps.io" }

$SEP = "=" * 60
$FechaHoy = (Get-Date).ToString("yyyy-MM-dd")
$FechaDev  = (Get-Date).AddDays(30).ToString("yyyy-MM-dd")

function Invoke-Api {
    param([string]$Uri, [string]$Method = "GET", [object]$Body = $null)
    $params = @{ Uri = $Uri; Method = $Method; ContentType = "application/json" }
    if ($Body) { $params.Body = ($Body | ConvertTo-Json -Depth 5) }
    try { Invoke-RestMethod @params } catch { $_.Exception.Response }
}

Write-Host $SEP
Write-Host "BFF = $BFF"
Write-Host $SEP

# 1. Health check
Write-Host ""
Write-Host "1) Health check BFF"
Write-Host $SEP
Invoke-Api "$BFF/actuator/health" | ConvertTo-Json
Write-Host ""

# 2. Listar prestamos actuales
Write-Host $SEP
Write-Host "2) GET /api/prestamos"
Write-Host $SEP
Invoke-Api "$BFF/api/prestamos" | ConvertTo-Json -Depth 5
Write-Host ""

# 3. Crear prestamo -> publica Biblioteca.PrestamoCreado
Write-Host $SEP
Write-Host "3) POST /api/prestamos  ->  Biblioteca.PrestamoCreado"
Write-Host $SEP
$bodyPrestamo = @{
    usuarioId              = 1
    libroId                = 1
    fechaPrestamo          = $FechaHoy
    fechaDevolucionEstimada = $FechaDev
    observacion            = "Demo S9 — flujo Event Grid PowerShell"
}
$respCreate = Invoke-Api "$BFF/api/prestamos" "POST" $bodyPrestamo
$respCreate | ConvertTo-Json -Depth 5

$prestamoId = if ($respCreate.data.id) { $respCreate.data.id } elseif ($respCreate.id) { $respCreate.id } else { $null }
Write-Host ""
Write-Host ">>> prestamoId: $prestamoId"
Write-Host ">>> Verificar Log stream de biblio-notificaciones-kath2026:"
Write-Host "    [NOTIFICACION SIMULADA] ... PRESTAMO REGISTRADO ..."
Write-Host ""

# 4. Consultar prestamo creado
if ($prestamoId) {
    Write-Host $SEP
    Write-Host "4) GET /api/prestamos/$prestamoId  ->  estado ACTIVO"
    Write-Host $SEP
    Invoke-Api "$BFF/api/prestamos/$prestamoId" | ConvertTo-Json -Depth 5
    Write-Host ""

    # 5. Devolver prestamo -> publica Biblioteca.PrestamoDevuelto
    Write-Host $SEP
    Write-Host "5) POST /api/prestamos/$prestamoId/devolucion  ->  Biblioteca.PrestamoDevuelto"
    Write-Host $SEP
    $bodyDev = @{
        fechaDevolucionReal = $FechaHoy
        observacion         = "Devuelto en buen estado — demo S9"
    }
    $respDev = Invoke-Api "$BFF/api/prestamos/$prestamoId/devolucion" "POST" $bodyDev
    $respDev | ConvertTo-Json -Depth 5
    Write-Host ""
    Write-Host ">>> Verificar Log stream:"
    Write-Host "    [NOTIFICACION SIMULADA] ... DEVOLUCION REGISTRADA ..."
    Write-Host ""
} else {
    Write-Host ">>> No se extrajo prestamoId. Revisa la respuesta del paso 3."
}

# 6. Demo S9: crear usuario de prueba, crearle un prestamo, eliminarlo con cascade
Write-Host $SEP
Write-Host "6) DEMO S9 — Eliminar usuario con prestamos asociados (cascade delete)"
Write-Host $SEP

$bodyUsuario = @{
    nombre   = "Usuario Demo S9"
    correo   = "demo-s9-$(Get-Random)@test.com"
    telefono = "999000001"
}
$respUsuario = Invoke-Api "$BFF/api/usuarios" "POST" $bodyUsuario
$respUsuario | ConvertTo-Json -Depth 5
$usuarioId = if ($respUsuario.data.id) { $respUsuario.data.id } elseif ($respUsuario.id) { $respUsuario.id } else { $null }
Write-Host ">>> usuarioId creado: $usuarioId"
Write-Host ""

if ($usuarioId) {
    # Crear un prestamo para ese usuario
    $bodyP2 = @{
        usuarioId              = $usuarioId
        libroId                = 2
        fechaPrestamo          = $FechaHoy
        fechaDevolucionEstimada = $FechaDev
        observacion            = "Prestamo para demo cascade delete S9"
    }
    $respP2 = Invoke-Api "$BFF/api/prestamos" "POST" $bodyP2
    $p2Id = if ($respP2.data.id) { $respP2.data.id } elseif ($respP2.id) { $respP2.id } else { $null }
    Write-Host ">>> prestamoId creado para usuario $usuarioId: $p2Id"
    Write-Host ""

    # Eliminar el usuario (debe cascade-deletear el prestamo y publicar Biblioteca.UsuarioEliminado)
    Write-Host "DELETE /api/usuarios/$usuarioId  ->  cascade + Biblioteca.UsuarioEliminado"
    $respDel = Invoke-Api "$BFF/api/usuarios/$usuarioId" "DELETE"
    $respDel | ConvertTo-Json -Depth 5
    Write-Host ""
    Write-Host ">>> Verificar Log stream de biblio-notificaciones-kath2026:"
    Write-Host "    [NOTIFICACION SIMULADA] Asunto: USUARIO ELIMINADO | prestamosEliminados=1"
    Write-Host ">>> El libro $p2Id debe haber vuelto a DISPONIBLE automaticamente."
}

Write-Host ""
Write-Host $SEP
Write-Host "Flujo S8/S9 completado."
Write-Host ""
Write-Host "Checklist para el video:"
Write-Host "  [ ] BFF en Azure respondiendo (health 200)"
Write-Host "  [ ] POST /prestamos -> HTTP 201 + Biblioteca.PrestamoCreado en Log stream"
Write-Host "  [ ] POST devolucion -> HTTP 200 + Biblioteca.PrestamoDevuelto en Log stream"
Write-Host "  [ ] DELETE /usuarios/{id} -> HTTP 200 + cascade + Biblioteca.UsuarioEliminado en Log stream"
Write-Host "  [ ] Azure Portal -> Event Grid Topics -> Metrics"
Write-Host "  [ ] Azure Portal -> Event Subscriptions -> Succeeded"
Write-Host $SEP
