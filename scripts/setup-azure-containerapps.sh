#!/usr/bin/env bash
# Provisiona Azure Container Apps para bff-springboot y servicio-libros.
# Idempotente: verifica antes de crear cada recurso.
#
# Prerequisitos:
#   - az CLI instalado y con sesión activa (az login)
#   - Docker instalado y corriendo
#   - Archivo .env presente con las variables requeridas
#   - Extension containerapp: az extension add --name containerapp
#
# Uso:
#   chmod +x scripts/setup-azure-appservice.sh
#   ./scripts/setup-azure-appservice.sh

set -euo pipefail

# ── Cargar .env ───────────────────────────────────────────────────────────────
if [[ ! -f .env ]]; then
  echo "ERROR: archivo .env no encontrado. Copia .env.example y rellena los valores." >&2
  exit 1
fi
set -a; source .env; set +a

# ── Validar variables requeridas (falla rápido sin valores por defecto) ───────
: "${ORACLE_JDBC_URL:?ERROR: ORACLE_JDBC_URL no definido en .env}"
: "${ORACLE_APP_USER:?ERROR: ORACLE_APP_USER no definido en .env}"
: "${ORACLE_APP_PASSWORD:?ERROR: ORACLE_APP_PASSWORD no definido en .env}"
: "${SERVICEBUS_CONNECTION_STRING:?ERROR: SERVICEBUS_CONNECTION_STRING no definido en .env}"

# ── Configuración ─────────────────────────────────────────────────────────────
RESOURCE_GROUP="rg-biblioteca-kath2026"
LOCATION="eastus2"
ACR_NAME="acrbibliokath2026"
ACR_SERVER="${ACR_NAME}.azurecr.io"
ACA_ENV="env-biblioteca-kath2026"
BFF_APP_NAME="bff-biblioteca-kath2026"
LIBROS_APP_NAME="servicio-libros-kath2026"

SB_QUEUE="${SERVICEBUS_QUEUE_NAME:-prestamo-notificaciones}"
USUARIOS_URL="https://biblio-usuarios-kath2026-v2.azurewebsites.net/api"
PRESTAMOS_URL="https://biblio-prestamos-kath2026-v2.azurewebsites.net/api"
LIBROS_URL="https://${LIBROS_APP_NAME}.<placeholder>.${LOCATION}.azurecontainerapps.io/api"

SEP="========================================"

# ── 0. Asegurar extensión containerapp ───────────────────────────────────────
echo "$SEP"
echo "0) Extensión Azure Container Apps"
az extension add --name containerapp --upgrade --output none 2>/dev/null || true
echo "   OK"

# ── 1. Resource Group ─────────────────────────────────────────────────────────
echo "$SEP"
echo "1) Resource Group: $RESOURCE_GROUP"
az group create --name "$RESOURCE_GROUP" --location "$LOCATION" --output none
echo "   OK"

# ── 2. Azure Container Registry ───────────────────────────────────────────────
echo "$SEP"
echo "2) Azure Container Registry: $ACR_NAME"
if ! az acr show --name "$ACR_NAME" --resource-group "$RESOURCE_GROUP" &>/dev/null; then
  az acr create \
    --resource-group "$RESOURCE_GROUP" \
    --name "$ACR_NAME" \
    --sku Basic \
    --admin-enabled true \
    --output none
  echo "   Creado."
else
  echo "   Ya existe."
fi

# Login al ACR usando credenciales de az CLI (sin exponer password en shell)
az acr login --name "$ACR_NAME"

# ── 3. Container Apps Environment ─────────────────────────────────────────────
echo "$SEP"
echo "3) Container Apps Environment: $ACA_ENV"
if ! az containerapp env show --name "$ACA_ENV" --resource-group "$RESOURCE_GROUP" &>/dev/null; then
  az containerapp env create \
    --name "$ACA_ENV" \
    --resource-group "$RESOURCE_GROUP" \
    --location "$LOCATION" \
    --output none
  echo "   Creado."
else
  echo "   Ya existe."
fi

# ── 4. Build y push de imágenes al ACR ───────────────────────────────────────
echo "$SEP"
echo "4) Build y push de imágenes"

echo "   Building bff-springboot..."
docker build -t "${ACR_SERVER}/bff-springboot:latest" ./bff-springboot
docker push "${ACR_SERVER}/bff-springboot:latest"

echo "   Building servicio-libros..."
docker build -t "${ACR_SERVER}/servicio-libros:latest" ./servicio-libros
docker push "${ACR_SERVER}/servicio-libros:latest"

# Credenciales ACR para que Container Apps pueda hacer pull
ACR_USERNAME=$(az acr credential show --name "$ACR_NAME" --query username -o tsv)
ACR_PASSWORD=$(az acr credential show --name "$ACR_NAME" --query "passwords[0].value" -o tsv)

# ── 5. Container App: servicio-libros (primero, porque BFF lo referencia) ─────
echo "$SEP"
echo "5) Container App servicio-libros: $LIBROS_APP_NAME"
if ! az containerapp show --name "$LIBROS_APP_NAME" --resource-group "$RESOURCE_GROUP" &>/dev/null; then
  az containerapp create \
    --name "$LIBROS_APP_NAME" \
    --resource-group "$RESOURCE_GROUP" \
    --environment "$ACA_ENV" \
    --image "${ACR_SERVER}/servicio-libros:latest" \
    --registry-server "$ACR_SERVER" \
    --registry-username "$ACR_USERNAME" \
    --registry-password "$ACR_PASSWORD" \
    --target-port 8080 \
    --ingress external \
    --min-replicas 1 \
    --max-replicas 1 \
    --cpu 0.5 \
    --memory 1Gi \
    --env-vars \
      LIBROS_PORT=8080 \
      "ORACLE_JDBC_URL=$ORACLE_JDBC_URL" \
      "ORACLE_APP_USER=$ORACLE_APP_USER" \
      "ORACLE_APP_PASSWORD=secretref:oracle-password" \
    --secrets "oracle-password=$ORACLE_APP_PASSWORD" \
    --output none
  echo "   Creado."
else
  az containerapp update \
    --name "$LIBROS_APP_NAME" \
    --resource-group "$RESOURCE_GROUP" \
    --image "${ACR_SERVER}/servicio-libros:latest" \
    --output none
  echo "   Actualizado."
fi

LIBROS_FQDN=$(az containerapp show --name "$LIBROS_APP_NAME" --resource-group "$RESOURCE_GROUP" --query "properties.configuration.ingress.fqdn" -o tsv)
LIBROS_URL="https://${LIBROS_FQDN}/api"
echo "   URL: $LIBROS_URL"

# ── 6. Container App: bff-springboot ──────────────────────────────────────────
echo "$SEP"
echo "6) Container App BFF: $BFF_APP_NAME"
if ! az containerapp show --name "$BFF_APP_NAME" --resource-group "$RESOURCE_GROUP" &>/dev/null; then
  az containerapp create \
    --name "$BFF_APP_NAME" \
    --resource-group "$RESOURCE_GROUP" \
    --environment "$ACA_ENV" \
    --image "${ACR_SERVER}/bff-springboot:latest" \
    --registry-server "$ACR_SERVER" \
    --registry-username "$ACR_USERNAME" \
    --registry-password "$ACR_PASSWORD" \
    --target-port 8080 \
    --ingress external \
    --min-replicas 1 \
    --max-replicas 1 \
    --cpu 0.5 \
    --memory 1Gi \
    --env-vars \
      BFF_PORT=8080 \
      "USUARIOS_FUNCTION_BASE_URL=$USUARIOS_URL" \
      "PRESTAMOS_FUNCTION_BASE_URL=$PRESTAMOS_URL" \
      "LIBROS_SERVICE_BASE_URL=$LIBROS_URL" \
      "SERVICEBUS_CONNECTION_STRING=secretref:sb-conn" \
      "SERVICEBUS_QUEUE_NAME=$SB_QUEUE" \
      LOG_LEVEL_ROOT=INFO \
      LOG_LEVEL_APP=INFO \
    --secrets "sb-conn=$SERVICEBUS_CONNECTION_STRING" \
    --output none
  echo "   Creado."
else
  az containerapp update \
    --name "$BFF_APP_NAME" \
    --resource-group "$RESOURCE_GROUP" \
    --image "${ACR_SERVER}/bff-springboot:latest" \
    --set-env-vars \
      BFF_PORT=8080 \
      "USUARIOS_FUNCTION_BASE_URL=$USUARIOS_URL" \
      "PRESTAMOS_FUNCTION_BASE_URL=$PRESTAMOS_URL" \
      "LIBROS_SERVICE_BASE_URL=$LIBROS_URL" \
      "LIBROS_SERVICE_BASE_URL=$LIBROS_URL" \
      "SERVICEBUS_CONNECTION_STRING=secretref:sb-conn" \
      "SERVICEBUS_QUEUE_NAME=$SB_QUEUE" \
      LOG_LEVEL_ROOT=INFO \
      LOG_LEVEL_APP=INFO \
    --output none
  echo "   Actualizado."
fi

BFF_FQDN=$(az containerapp show --name "$BFF_APP_NAME" --resource-group "$RESOURCE_GROUP" --query "properties.configuration.ingress.fqdn" -o tsv)

# ── 7. Resumen ────────────────────────────────────────────────────────────────
echo "$SEP"
echo "DESPLIEGUE COMPLETADO"
echo ""
echo "  BFF:             https://${BFF_FQDN}"
echo "  servicio-libros: https://${LIBROS_FQDN}"
echo ""
echo "Verificación (esperar ~60 s para arranque de contenedores):"
echo "  curl https://${BFF_FQDN}/actuator/health"
echo "  curl https://${BFF_FQDN}/api/usuarios"
echo "  curl https://${BFF_FQDN}/api/prestamos"
echo "  curl https://${BFF_FQDN}/api/libros"
echo ""
echo "Demo Event Grid:"
echo "  BFF_URL=https://${BFF_FQDN}/api bash scripts/demo-eventgrid-flow.sh"
echo ""
echo "Para CI/CD (GitHub Actions), crea estos Secrets en el repo:"
echo "  AZURE_CREDENTIALS  → az ad sp create-for-rbac --name sp-biblioteca --sdk-auth"
echo "  ACR_NAME           → $ACR_NAME"
echo "  ACR_USERNAME       → $ACR_USERNAME"
echo "  ACR_PASSWORD       → (az acr credential show --name $ACR_NAME)"
echo ""
echo "Para wallet del servicio-libros en GitHub Actions:"
echo "  tar -czf - -C servicio-libros/src/main/resources wallet | base64 > /tmp/wallet.b64"
echo "  cat /tmp/wallet.b64   # pegar como secret ORACLE_WALLET_BASE64"
echo "$SEP"
