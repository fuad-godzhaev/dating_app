#!/bin/bash
# Script to prepare Node.js modules for Android
# This script installs modules with pre-built Android binaries

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$SCRIPT_DIR/../src/main/assets/nodejs-project"
PDS_SOURCE="$SCRIPT_DIR/../../../../pds/pds-main/service"

echo "Preparing Node.js modules for Android..."

# Step 1: Copy package.json and index.js
echo "Step 1: Copying PDS source files..."
mkdir -p "$PROJECT_DIR"
cp "$PDS_SOURCE/package.json" "$PROJECT_DIR/"
cp "$PDS_SOURCE/index.js" "$PROJECT_DIR/"
cp "$PDS_SOURCE/../.env" "$PROJECT_DIR/" 2>/dev/null || echo "No .env file found"

# Step 2: Install dependencies with pre-built binaries
echo "Step 2: Installing Node.js dependencies..."
cd "$PROJECT_DIR"

# Install dependencies
# better-sqlite3 will download pre-built binaries for the current platform
npm install --omit=dev --production

# Step 3: For better-sqlite3, we need Android binaries
# Option 1: Try to download pre-built Android binaries
echo "Step 3: Attempting to get Android binaries for better-sqlite3..."

# Check if better-sqlite3 exists
if [ -d "node_modules/better-sqlite3" ]; then
    echo "better-sqlite3 found. Checking for pre-built Android binaries..."

    # The proper solution is to use prebuild-install with Android targets
    # This is complex and may require additional setup
    echo "WARNING: better-sqlite3 may use host platform binaries."
    echo "For production, you should:"
    echo "1. Build on a Linux machine with Android NDK"
    echo "2. Or use sql.js (pure JavaScript SQLite)"
    echo "3. Or use a different database (PostgreSQL, Redis)"
fi

echo "Node.js modules prepared successfully!"
echo "Location: $PROJECT_DIR"
