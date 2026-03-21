#!/bin/bash

# Build and sign Homer Player 2 openSourceRelease and googleRelease variants
# This script:
# 1. Validates the keystore path environment variable
# 2. Verifies the keystore file exists
# 3. Prompts for keystore and key passwords (masked input)
# 4. Cleans the build
# 5. Builds and signs the openSourceRelease variant
# 6. Builds and signs the googleRelease variant

set -e

# Configuration
KEYSTORE_ENV_VAR="HOMERPLAYER2_KEYSTORE_PATH"
KEY_ALIAS="homerplayer2"
BUILD_VARIANT="openSourceRelease"
APP_BUILD_GRADLE="app/build.gradle"
METADATA_DIR="metadata/en-US/changelogs"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to print colored output
print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_info() {
    echo -e "${YELLOW}[INFO]${NC} $1"
}

# Extract versionCode and versionName from app/build.gradle
extract_version_info() {
    local version_code=$(grep -oP "versionCode\s+\K\d+" "$APP_BUILD_GRADLE")
    local version_name=$(grep -oP "versionName\s+\"?\K[^\"]*" "$APP_BUILD_GRADLE")
    echo "$version_code:$version_name"
}

# Check if current commit has a version tag matching A.B.C format
check_version_tag() {
    local tag
    tag=$(git describe --tags --exact-match 2>/dev/null) || {
        print_error "Current commit does not have a git tag"
        echo "Please create a tag for this release in format A.B.C (e.g., 1.5.5)"
        echo "  git tag <version>"
        return 1
    }

    # Validate tag format: A.B.C where A, B, C are numbers
    if ! [[ $tag =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
        print_error "Git tag '$tag' does not match version format A.B.C"
        echo "Expected format: numbers separated by dots (e.g., 1.5.5)"
        return 1
    fi

    echo "$tag"
}

# Check if changelog file exists for current version
check_changelog_exists() {
    local version_code=$1
    local changelog_file="$METADATA_DIR/$version_code.txt"

    if [ ! -f "$changelog_file" ]; then
        print_error "Changelog file not found: $changelog_file"
        echo "Please create a changelog file for versionCode $version_code"
        return 1
    fi
}

# Check if git tag matches versionName
check_tag_matches_version() {
    local tag=$1
    local version_name=$2

    if [ "$tag" != "$version_name" ]; then
        print_error "Git tag '$tag' does not match versionName '$version_name'"
        echo "Please ensure git tag and versionName are synchronized"
        return 1
    fi
}

# Check if keystore path is set
if [ -z "${!KEYSTORE_ENV_VAR}" ]; then
    print_error "Environment variable $KEYSTORE_ENV_VAR is not set"
    echo "Please set it before running this script:"
    echo "  export $KEYSTORE_ENV_VAR=/path/to/your/keystore.jks"
    exit 1
fi

KEYSTORE_PATH="${!KEYSTORE_ENV_VAR}"

# Verify keystore file exists
if [ ! -f "$KEYSTORE_PATH" ]; then
    print_error "Keystore file not found at: $KEYSTORE_PATH"
    exit 1
fi

print_info "Keystore path: $KEYSTORE_PATH"
print_info "Key alias: $KEY_ALIAS"

# Check version information
echo ""
print_info "Checking version information..."

# Extract version info from build.gradle
VERSION_INFO=$(extract_version_info)
VERSION_CODE="${VERSION_INFO%:*}"
VERSION_NAME="${VERSION_INFO#*:}"

print_info "versionCode: $VERSION_CODE"
print_info "versionName: $VERSION_NAME"

# Check if current commit has version tag
if ! check_version_tag > .build_version_tag_output 2>&1; then
    cat .build_version_tag_output >&2
    rm -f .build_version_tag_output
    exit 1
fi
GIT_TAG=$(cat .build_version_tag_output)
rm -f .build_version_tag_output
print_info "Git tag: $GIT_TAG"

# Check if git tag matches versionName
check_tag_matches_version "$GIT_TAG" "$VERSION_NAME"
if [ $? -ne 0 ]; then
    exit 1
fi

# Check if changelog file exists
check_changelog_exists "$VERSION_CODE"
if [ $? -ne 0 ]; then
    exit 1
fi
print_info "Changelog file found: $METADATA_DIR/$VERSION_CODE.txt"

echo ""

# Prompt for keystore password
echo ""
read -sp "Enter keystore password: " KEYSTORE_PASSWORD
echo ""

# Validate keystore password is not empty
if [ -z "$KEYSTORE_PASSWORD" ]; then
    print_error "Keystore password cannot be empty"
    exit 1
fi

# Prompt for key password
read -sp "Enter key password (press Enter to use keystore password): " KEY_PASSWORD
echo ""

# Default key password to keystore password if empty
if [ -z "$KEY_PASSWORD" ]; then
    KEY_PASSWORD="$KEYSTORE_PASSWORD"
    print_info "Key password defaulted to keystore password"
fi

print_info "Building and signing release variants..."
echo ""

# Clean and build openSourceRelease
print_info "Building openSourceRelease variant..."
./gradlew clean assembleOpenSourceRelease \
    -PkeystorePath="$KEYSTORE_PATH" \
    -PkeystorePassword="$KEYSTORE_PASSWORD" \
    -PkeyPassword="$KEY_PASSWORD" \
    -PkeyAlias="$KEY_ALIAS"

OPENSOURCE_RESULT=$?

if [ $OPENSOURCE_RESULT -ne 0 ]; then
    print_error "openSourceRelease build failed"
    exit 1
fi

print_success "openSourceRelease build completed!"
echo ""

# Build googleRelease as a separate gradle command to avoid interference
print_info "Building googleRelease variant..."
./gradlew assembleGoogleRelease \
    -PkeystorePath="$KEYSTORE_PATH" \
    -PkeystorePassword="$KEYSTORE_PASSWORD" \
    -PkeyPassword="$KEY_PASSWORD" \
    -PkeyAlias="$KEY_ALIAS"

GOOGLE_RESULT=$?

if [ $GOOGLE_RESULT -ne 0 ]; then
    print_error "googleRelease build failed"
    exit 1
fi

print_success "googleRelease build completed!"
echo ""

# Clear passwords from memory (not foolproof, but better than nothing)
KEYSTORE_PASSWORD=""
KEY_PASSWORD=""

print_success "All builds completed successfully!"
echo ""
echo "Signed APK locations:"
echo "  OpenSource:"
echo "    app/build/outputs/apk/openSource/release/app-openSource-release.apk"
echo "  Google:"
echo "    app/build/outputs/apk/google/release/app-google-release.apk"
