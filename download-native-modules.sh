#!/bin/bash
# Download and extract prebuilt PDS native modules from GitHub Actions

set -e

echo "========================================="
echo "PDS Native Modules Downloader"
echo "========================================="
echo ""

# Configuration
REPO_OWNER="fuad-godzhaev"  # Change this to your GitHub username
REPO_NAME="dating_app"  # Change this to your repo name
ARTIFACT_NAME="pds-native-modules-android"
EXTRACT_PATH="ui/core/pds/src/main/assets"

# Check if gh CLI is installed
if ! command -v gh &> /dev/null; then
    echo "❌ GitHub CLI (gh) is not installed"
    echo ""
    echo "Install it from: https://cli.github.com/"
    echo "Or use Homebrew: brew install gh"
    echo "Or on Windows: winget install --id GitHub.cli"
    exit 1
fi

echo "✓ GitHub CLI found"

# Check if authenticated
if ! gh auth status &> /dev/null; then
    echo "❌ Not authenticated with GitHub"
    echo ""
    echo "Run: gh auth login"
    exit 1
fi

echo "✓ Authenticated with GitHub"
echo ""

# Get the latest successful workflow run
echo "Finding latest successful workflow run..."
RUN_ID=$(gh run list \
    --repo "$REPO_OWNER/$REPO_NAME" \
    --workflow "build-pds-native-modules.yml" \
    --status success \
    --limit 1 \
    --json databaseId \
    --jq '.[0].databaseId')

if [ -z "$RUN_ID" ]; then
    echo "❌ No successful workflow runs found"
    echo ""
    echo "Please run the workflow first:"
    echo "1. Go to: https://github.com/$REPO_OWNER/$REPO_NAME/actions"
    echo "2. Click 'Build PDS Native Modules for Android'"
    echo "3. Click 'Run workflow'"
    exit 1
fi

echo "✓ Found workflow run: $RUN_ID"
echo ""

# Download the artifact
echo "Downloading artifact '$ARTIFACT_NAME'..."
mkdir -p tmp
gh run download "$RUN_ID" \
    --repo "$REPO_OWNER/$REPO_NAME" \
    --name "$ARTIFACT_NAME" \
    --dir tmp

echo "✓ Artifact downloaded"
echo ""

# Extract the archive
echo "Extracting native modules..."
mkdir -p "$EXTRACT_PATH"
cd tmp
tar -xzf nodejs-project-android.tar.gz
cd ..

# Move to destination
echo "Moving to $EXTRACT_PATH..."
rm -rf "$EXTRACT_PATH/nodejs-project"
mv tmp/nodejs-project "$EXTRACT_PATH/"

# Cleanup
rm -rf tmp

echo ""
echo "========================================="
echo "✓ Native modules installed successfully!"
echo "========================================="
echo ""
echo "Location: $EXTRACT_PATH/nodejs-project"
echo ""
echo "Next steps:"
echo "1. Build your Android app: ./gradlew assembleDebug"
echo "2. Test on device or emulator"
echo ""
echo "The native modules are now ready for Android ARM64, ARM, and x64!"
