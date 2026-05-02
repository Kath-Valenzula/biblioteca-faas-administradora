#!/usr/bin/env bash
# Demostración del flujo Event Grid — Semana 8
# Muestra: crear préstamo -> publicación de evento -> consumo en function-notificaciones
#
# Uso:
#   chmod +x scripts/demo-eventgrid-flow.sh
#   PRESTAMOS_URL=https://biblio-prestamos-kath2026-v2.azurewebsites.net/api \
#   ./scripts/demo-eventgrid-flow.sh
#
# Si se omite PRESTAMOS_URL, apunta a localhost:7072 (ejecución local con func start).

set -euo pipefail

PRESTAMOS_URL="${PRESTAMOS_URL:-http://localhost:7072/api}"
SEP="========================================"

# ── 1. Verificar que la Function App responde ─────────────────────────────────
echo "$SEP"
echo "1) Health-check de function-prestamos"
echo "$SEP"
curl -sf "$PRESTAMOS_URL/prestamos" | python3 -m json.tool || true
echo ""

# ── 2. Crear préstamo (publica Biblioteca.PrestamoCreado en Event Grid) ───────
echo "$SEP"
echo "2) POST /prestamos  →  crea préstamo + publica evento Event Grid"
echo "   (ajusta usuarioId y libroId según tu BD)"
echo "$SEP"

RESPONSE=$(curl -sf -X POST "$PRESTAMOS_URL/prestamos" \
  -H "Content-Type: application/json" \
  -d '{
    "usuarioId": 1,
    "libroId":   1,
    "fechaDevolucionEsperada": "2026-05-15"
  }')

echo "$RESPONSE" | python3 -m json.tool
PRESTAMO_ID=$(echo "$RESPONSE" | python3 -c "import sys,json; print(json.load(sys.stdin).get('id',''))" 2>/dev/null || true)
echo ""
echo ">>> Préstamo creado con id=$PRESTAMO_ID"
echo ">>> Revisa los Application Insights / Log Stream de biblio-notificaciones-kath2026"
echo ">>> para ver el log: '=== EVENTO EVENT GRID RECIBIDO ==='"
echo ""

# ── 3. Consultar préstamo para confirmar estado ACTIVO ────────────────────────
if [[ -n "$PRESTAMO_ID" ]]; then
  echo "$SEP"
  echo "3) GET /prestamos/$PRESTAMO_ID  →  estado debe ser ACTIVO"
  echo "$SEP"
  curl -sf "$PRESTAMOS_URL/prestamos/$PRESTAMO_ID" | python3 -m json.tool
  echo ""

  # ── 4. Devolver préstamo (publica Biblioteca.PrestamoDevuelto) ────────────
  echo "$SEP"
  echo "4) PUT /prestamos/$PRESTAMO_ID/devolucion  →  devuelve + publica evento"
  echo "$SEP"
  curl -sf -X PUT "$PRESTAMOS_URL/prestamos/$PRESTAMO_ID/devolucion" \
    -H "Content-Type: application/json" \
    -d '{"observacion": "Devuelto en buen estado — demo S8"}' | python3 -m json.tool
  echo ""
  echo ">>> Revisa nuevamente los logs de function-notificaciones:"
  echo "    [NOTIFICACION SIMULADA] ... DEVOLUCION REGISTRADA ..."
fi

echo "$SEP"
echo "Flujo Event Grid completado."
echo "Pasos para el video:"
echo "  1. Mostrar este script ejecutándose"
echo "  2. Abrir Azure Portal → function-notificaciones → Log stream"
echo "  3. Mostrar eventos en Event Grid → Topics → biblioteca-eventos-topic → Metrics"
echo "  4. Mostrar Event Subscription activa en Azure Portal"
echo "$SEP"
