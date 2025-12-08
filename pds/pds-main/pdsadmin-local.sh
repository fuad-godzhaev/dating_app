#!/usr/bin/env bash
set -o errexit
set -o nounset
set -o pipefail

# Local PDS Admin Tool for Docker Setup
# This script works with PDS running in Docker containers

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
COMMANDS_DIR="${SCRIPT_DIR}/pdsadmin"

# Command to run
COMMAND="${1:-help}"
shift || true

# Check if command script exists
COMMAND_SCRIPT="${COMMANDS_DIR}/${COMMAND}.sh"

if [[ ! -f "${COMMAND_SCRIPT}" ]]; then
  echo "ERROR: Command '${COMMAND}' not found"
  echo ""
  echo "Available commands:"
  for script in "${COMMANDS_DIR}"/*.sh; do
    if [[ -f "${script}" ]]; then
      basename "${script}" .sh
    fi
  done
  exit 1
fi

# Execute the command script
bash "${COMMAND_SCRIPT}" "$@"
