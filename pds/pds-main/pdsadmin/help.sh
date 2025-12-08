#!/usr/bin/env bash
set -o errexit
set -o nounset
set -o pipefail

# This script is used to display help information for the pdsadmin-local command.
cat <<HELP
PDS Admin Tool - Docker Edition
================================

Usage: bash pdsadmin-local.sh <command> [args]

Available Commands:

account
  list
    List all accounts
    e.g. bash pdsadmin-local.sh account list

  create <EMAIL> <HANDLE>
    Create a new account (auto-generates invite code and password)
    e.g. bash pdsadmin-local.sh account create alice@example.com alice.test

  delete <DID>
    Delete an account specified by DID
    e.g. bash pdsadmin-local.sh account delete did:plc:xyz123abc456

  takedown <DID>
    Takedown an account specified by DID
    e.g. bash pdsadmin-local.sh account takedown did:plc:xyz123abc456

  untakedown <DID>
    Remove a takedown from an account specified by DID
    e.g. bash pdsadmin-local.sh account untakedown did:plc:xyz123abc456

  reset-password <DID>
    Reset password for an account specified by DID
    e.g. bash pdsadmin-local.sh account reset-password did:plc:xyz123abc456

create-invite-code [USE_COUNT]
  Create a new invite code with specified number of uses (default: 1)
  e.g. bash pdsadmin-local.sh create-invite-code 5

help
  Display this help information

Configuration:
  The tool reads from .env in the same directory:
    - PDS_HOSTNAME (default: localhost)
    - PDS_PORT (default: 3002)
    - PDS_ADMIN_PASSWORD (required)
    - PDS_DEV_MODE (true for HTTP, false for HTTPS)

Requirements:
  - PDS container must be running
  - .env file must exist with PDS_ADMIN_PASSWORD set
  - jq (for formatted JSON output)

HELP
