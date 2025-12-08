#!/usr/bin/env bash
set -o errexit
set -o nounset
set -o pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && cd .. && pwd)"
ENV_FILE="${SCRIPT_DIR}/.env"

# Load environment variables
if [[ ! -f "${ENV_FILE}" ]]; then
  echo "ERROR: .env file not found at ${ENV_FILE}"
  exit 1
fi

# Source the .env file
set -a
source "${ENV_FILE}"
set +a

# Set defaults and strip carriage returns (Windows compatibility)
PDS_HOSTNAME="${PDS_HOSTNAME:-localhost}"
PDS_HOSTNAME="${PDS_HOSTNAME%$'\r'}"
PDS_PORT="${PDS_PORT:-3002}"
PDS_PORT="${PDS_PORT%$'\r'}"
PDS_ADMIN_PASSWORD="${PDS_ADMIN_PASSWORD:-}"
PDS_ADMIN_PASSWORD="${PDS_ADMIN_PASSWORD%$'\r'}"
PDS_DEV_MODE="${PDS_DEV_MODE:-false}"
PDS_DEV_MODE="${PDS_DEV_MODE%$'\r'}"

# Build base URL (use http for local Docker setup)
if [[ "${PDS_DEV_MODE}" == "true" ]]; then
  BASE_URL="http://${PDS_HOSTNAME}:${PDS_PORT}"
else
  BASE_URL="https://${PDS_HOSTNAME}"
fi

USE_COUNT="${1:-1}"

echo "Creating invite code with ${USE_COUNT} use(s)..."

curl \
  --fail \
  --silent \
  --show-error \
  --request POST \
  --user "admin:${PDS_ADMIN_PASSWORD}" \
  --header "Content-Type: application/json" \
  --data "{\"useCount\": ${USE_COUNT}}" \
  "${BASE_URL}/xrpc/com.atproto.server.createInviteCode" | jq --raw-output '.code'
