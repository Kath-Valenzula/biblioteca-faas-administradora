#!/usr/bin/env bash
# Demostración del flujo Event Grid — Semana 8
# Muestra: crear préstamo → publicación de evento → consumo en function-notificaciones
#
# Uso (cloud completo a través del BFF):
#   chmod +x scripts/demo-eventgrid-flow.sh
#   ./scripts/demo-eventgrid-flow.sh
#
# Para apuntar directo a la function (sin BFF):
#   BASE_URL=https://biblio-prestamos-kath2026-v2.azurewebsites.net/api \
#   ./scripts/demo-eventgrid-flow.sh

set -uo pipefail   # sin -e para no abortar si un endpoint retorna error HTTP

BASE_URL="${BASE_URL:-https://bff-biblioteca-kath2026.orangemushroom-45a0eb3b.eastus2.azurecontainerapps.io/api}"
SEP="========================================"

# Formateador JSON portable: usa jq si está, si no python3, si no cat
pretty() { command -v jq &>/dev/null && jq . || python3 -m json.tool 2>/dev/null || cat; }

# Fecha de hoy + 30 días como fechaDevolucionEstimada
FECHA_DEV=$(date -d "+30 days" +%Y-%m-%d 2>/dev/null || date -v+30d +%Y-%m-%d 2>/dev/null || echo "2026-05-29")
FECHA_HOY=$(date +%Y-%m-%d)

echo "$SEP"
echo "BASE_URL = $BASE_URL"
echo "$SEP"

# ── 1. Health-check del BFF ───────────────────────────────────────────────────
echo ""
echo "1) Health-check del BFF"
echo "$SEP"
HEALTH_URL="${BASE_URL%/api}/actuator/health"
curl -s "$HEALTH_URL" | pretty || echo "(sin respuesta — verifica que el BFF esté corriendo)"
echo ""

# ── 2. Listar préstamos existentes ────────────────────────────────────────────
echo "$SEP"
echo "2) GET /api/prestamos  →  listado actual"
echo "$SEP"
curl -s "$BASE_URL/prestamos" | pretty
echo ""

# ── 3. Crear préstamo  →  dispara Biblioteca.PrestamoCreado en Event Grid ─────
echo "$SEP"
echo "3) POST /api/prestamos  →  crea préstamo + publica Biblioteca.PrestamoCreado"
echo "$SEP"

RAW_CREATE=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/prestamos" \
  -H "Content-Type: application/json" \
  -d "{
    \"usuarioId\": 1,
    \"libroId\": 1,
    \"fechaPrestamo\": \"$FECHA_HOY\",
    \"fechaDevolucionEstimada\": \"$FECHA_DEV\",
    \"observacion\": \"Demo S8 — flujo Event Grid\"
  }")

HTTP_CODE=$(echo "$RAW_CREATE" | tail -1)
BODY=$(echo "$RAW_CREATE" | head -n -1)
echo "HTTP $HTTP_CODE"
echo "$BODY" | pretty

# Extraer id: jq primero, luego grep como fallback
if command -v jq &>/dev/null; then
  PRESTAMO_ID=$(echo "$BODY" | jq -r '(.data.id // .id // "") | tostring' 2>/dev/null || true)
else
  PRESTAMO_ID=$(echo "$BODY" | grep -o '"id":[0-9]*' | head -1 | grep -o '[0-9]*' || true)
fi

echo ""
echo ">>> prestamoId extraído: ${PRESTAMO_ID:-<no extraído — revisa respuesta>}"
echo ">>> Verifica en Azure Portal → biblio-notificaciones-kath2026 → Log stream:"
echo "    '=== EVENTO EVENT GRID RECIBIDO ==='"
echo "    '[NOTIFICACION SIMULADA] ... PRESTAMO REGISTRADO ...'"
echo ""

# ── 4. Confirmar estado ACTIVO ────────────────────────────────────────────────
if [[ -n "${PRESTAMO_ID}" ]]; then
  echo "$SEP"
  echo "4) GET /api/prestamos/$PRESTAMO_ID  →  estado debe ser ACTIVO"
  echo "$SEP"
  curl -s "$BASE_URL/prestamos/$PRESTAMO_ID" | pretty
  echo ""

  # ── 5. Devolver préstamo  →  dispara Biblioteca.PrestamoDevuelto ─────────
  echo "$SEP"
  echo "5) POST /api/prestamos/$PRESTAMO_ID/devolucion  →  devuelve + publica Biblioteca.PrestamoDevuelto"
  echo "$SEP"

  RAW_DEV=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/prestamos/$PRESTAMO_ID/devolucion" \
    -H "Content-Type: application/json" \
    -d "{
      \"fechaDevolucionReal\": \"$FECHA_HOY\",
      \"observacion\": \"Devuelto en buen estado — demo S8\"
    }")

  HTTP_CODE_DEV=$(echo "$RAW_DEV" | tail -1)
  BODY_DEV=$(echo "$RAW_DEV" | head -n -1)
  echo "HTTP $HTTP_CODE_DEV"
  echo "$BODY_DEV" | pretty
  echo ""
  echo ">>> Verifica en Log stream de biblio-notificaciones-kath2026:"
  echo "    '[NOTIFICACION SIMULADA] ... DEVOLUCION REGISTRADA ...'"
else
  echo ">>> No se pudo extraer prestamoId — omitiendo pasos 4 y 5."
  echo "    Revisa la respuesta del paso 3 y ajusta usuarioId/libroId si es necesario."
fi

echo ""
echo "$SEP"
echo "Flujo Event Grid completado."
echo ""
echo "Checklist para el video:"
echo "  [✓] BFF en Azure respondiendo"
echo "  [✓] POST /prestamos → HTTP 201"
echo "  [✓] Log stream function-notificaciones: EVENTO EVENT GRID RECIBIDO"
echo "  [✓] Log stream function-notificaciones: NOTIFICACION SIMULADA / PRESTAMO REGISTRADO"
echo "  [✓] POST devolucion → HTTP 200"
echo "  [✓] Log stream function-notificaciones: NOTIFICACION SIMULADA / DEVOLUCION REGISTRADA"
echo "  [✓] Azure Portal → Event Grid Topics → Metrics (publicaciones)"
echo "  [✓] Azure Portal → Event Subscriptions → biblioteca-prestamos-notificaciones-sub"
echo "$SEP"
