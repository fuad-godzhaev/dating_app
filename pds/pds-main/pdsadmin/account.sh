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

# curl a URL and fail if the request fails.
function curl_cmd_get {
  curl --fail --silent --show-error "$@"
}

# curl a URL and fail if the request fails.
function curl_cmd_post {
  curl --fail --silent --show-error --request POST --header "Content-Type: application/json" "$@"
}

# curl a URL but do not fail if the request fails.
function curl_cmd_post_nofail {
  curl --silent --show-error --request POST --header "Content-Type: application/json" "$@"
}

# The subcommand to run.
SUBCOMMAND="${1:-}"

#
# account list
#
if [[ "${SUBCOMMAND}" == "list" ]]; then
  echo "Listing all accounts..."
  echo ""

  # Get list of DIDs
  DIDS_JSON="$(curl_cmd_get "${BASE_URL}/xrpc/com.atproto.sync.listRepos?limit=100")"

  # Build output array
  OUTPUT='[{"handle":"Handle","email":"Email","did":"DID"}'

  # Process each DID
  while IFS= read -r did; do
    if [[ -n "${did}" ]]; then
      # Strip any whitespace or carriage returns from DID
      did="${did%$'\r'}"
      did="${did## }"
      did="${did%% }"

      ITEM="$(curl --fail --silent --show-error \
        --user "admin:${PDS_ADMIN_PASSWORD}" \
        "${BASE_URL}/xrpc/com.atproto.admin.getAccountInfo?did=${did}"
      )"
      OUTPUT="${OUTPUT},${ITEM}"
    fi
  done < <(echo "${DIDS_JSON}" | jq --raw-output '.repos[].did')

  OUTPUT="${OUTPUT}]"
  echo "${OUTPUT}" | jq --raw-output '.[] | [.handle, .email, .did] | @tsv' | column --table

#
# account create
#
elif [[ "${SUBCOMMAND}" == "create" ]]; then
  EMAIL="${2:-}"
  HANDLE="${3:-}"

  if [[ "${EMAIL}" == "" ]]; then
    read -p "Enter an email address (e.g. alice@${PDS_HOSTNAME}): " EMAIL
  fi
  if [[ "${HANDLE}" == "" ]]; then
    read -p "Enter a handle (e.g. alice.${PDS_HOSTNAME}): " HANDLE
  fi

  if [[ "${EMAIL}" == "" || "${HANDLE}" == "" ]]; then
    echo "ERROR: missing EMAIL and/or HANDLE parameters." >/dev/stderr
    echo "Usage: $0 ${SUBCOMMAND} <EMAIL> <HANDLE>" >/dev/stderr
    exit 1
  fi

  PASSWORD="$(openssl rand -base64 30 | tr -d "=+/" | cut -c1-24)"
  INVITE_CODE="$(curl_cmd_post \
    --user "admin:${PDS_ADMIN_PASSWORD}" \
    --data '{"useCount": 1}' \
    "${BASE_URL}/xrpc/com.atproto.server.createInviteCode" | jq --raw-output '.code'
  )"
  RESULT="$(curl_cmd_post_nofail \
    --data "{\"email\":\"${EMAIL}\", \"handle\":\"${HANDLE}\", \"password\":\"${PASSWORD}\", \"inviteCode\":\"${INVITE_CODE}\"}" \
    "${BASE_URL}/xrpc/com.atproto.server.createAccount"
  )"

  DID="$(echo $RESULT | jq --raw-output '.did')"
  if [[ "${DID}" != did:* ]]; then
    ERR="$(echo ${RESULT} | jq --raw-output '.message')"
    echo "ERROR: ${ERR}" >/dev/stderr
    echo "Usage: $0 ${SUBCOMMAND} <EMAIL> <HANDLE>" >/dev/stderr
    exit 1
  fi

  echo
  echo "Account created successfully!"
  echo "-----------------------------"
  echo "Handle   : ${HANDLE}"
  echo "DID      : ${DID}"
  echo "Password : ${PASSWORD}"
  echo "-----------------------------"
  echo "Save this password, it will not be displayed again."
  echo

#
# account delete
#
elif [[ "${SUBCOMMAND}" == "delete" ]]; then
  DID="${2:-}"

  if [[ "${DID}" == "" ]]; then
    echo "ERROR: missing DID parameter." >/dev/stderr
    echo "Usage: $0 ${SUBCOMMAND} <DID>" >/dev/stderr
    exit 1
  fi

  if [[ "${DID}" != did:* ]]; then
    echo "ERROR: DID parameter must start with \"did:\"." >/dev/stderr
    echo "Usage: $0 ${SUBCOMMAND} <DID>" >/dev/stderr
    exit 1
  fi

  echo "This action is permanent."
  read -r -p "Are you sure you'd like to delete ${DID}? [y/N] " response
  if [[ ! "${response}" =~ ^([yY][eE][sS]|[yY])$ ]]; then
    exit 0
  fi

  curl_cmd_post \
    --user "admin:${PDS_ADMIN_PASSWORD}" \
    --data "{\"did\": \"${DID}\"}" \
    "${BASE_URL}/xrpc/com.atproto.admin.deleteAccount" >/dev/null

  echo "${DID} deleted"

#
# account takedown
#
elif [[ "${SUBCOMMAND}" == "takedown" ]]; then
  DID="${2:-}"
  TAKEDOWN_REF="$(date +%s)"

  if [[ "${DID}" == "" ]]; then
    echo "ERROR: missing DID parameter." >/dev/stderr
    echo "Usage: $0 ${SUBCOMMAND} <DID>" >/dev/stderr
    exit 1
  fi

  if [[ "${DID}" != did:* ]]; then
    echo "ERROR: DID parameter must start with \"did:\"." >/dev/stderr
    echo "Usage: $0 ${SUBCOMMAND} <DID>" >/dev/stderr
    exit 1
  fi

  PAYLOAD="$(cat <<EOF
    {
      "subject": {
        "\$type": "com.atproto.admin.defs#repoRef",
        "did": "${DID}"
      },
      "takedown": {
        "applied": true,
        "ref": "${TAKEDOWN_REF}"
      }
    }
EOF
)"

  curl_cmd_post \
    --user "admin:${PDS_ADMIN_PASSWORD}" \
    --data "${PAYLOAD}" \
    "${BASE_URL}/xrpc/com.atproto.admin.updateSubjectStatus" >/dev/null

  echo "${DID} taken down"

#
# account untakedown
#
elif [[ "${SUBCOMMAND}" == "untakedown" ]]; then
  DID="${2:-}"

  if [[ "${DID}" == "" ]]; then
    echo "ERROR: missing DID parameter." >/dev/stderr
    echo "Usage: $0 ${SUBCOMMAND} <DID>" >/dev/stderr
    exit 1
  fi

  if [[ "${DID}" != did:* ]]; then
    echo "ERROR: DID parameter must start with \"did:\"." >/dev/stderr
    echo "Usage: $0 ${SUBCOMMAND} <DID>" >/dev/stderr
    exit 1
  fi

  PAYLOAD=$(cat <<EOF
  {
    "subject": {
      "\$type": "com.atproto.admin.defs#repoRef",
      "did": "${DID}"
    },
    "takedown": {
      "applied": false
    }
  }
EOF
)

  curl_cmd_post \
    --user "admin:${PDS_ADMIN_PASSWORD}" \
    --data "${PAYLOAD}" \
    "${BASE_URL}/xrpc/com.atproto.admin.updateSubjectStatus" >/dev/null

  echo "${DID} untaken down"
#
# account reset-password
#
elif [[ "${SUBCOMMAND}" == "reset-password" ]]; then
  DID="${2:-}"
  PASSWORD="$(openssl rand -base64 30 | tr -d "=+/" | cut -c1-24)"

  if [[ "${DID}" == "" ]]; then
    echo "ERROR: missing DID parameter." >/dev/stderr
    echo "Usage: $0 ${SUBCOMMAND} <DID>" >/dev/stderr
    exit 1
  fi

  if [[ "${DID}" != did:* ]]; then
    echo "ERROR: DID parameter must start with \"did:\"." >/dev/stderr
    echo "Usage: $0 ${SUBCOMMAND} <DID>" >/dev/stderr
    exit 1
  fi

  curl_cmd_post \
    --user "admin:${PDS_ADMIN_PASSWORD}" \
    --data "{ \"did\": \"${DID}\", \"password\": \"${PASSWORD}\" }" \
    "${BASE_URL}/xrpc/com.atproto.admin.updateAccountPassword" >/dev/null

  echo
  echo "Password reset for ${DID}"
  echo "New password: ${PASSWORD}"
  echo

else
  echo "Unknown subcommand: ${SUBCOMMAND}" >/dev/stderr
  exit 1
fi
