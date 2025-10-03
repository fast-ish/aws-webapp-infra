#!/bin/bash
# Build script that temporarily sets a release version, builds all artifacts, then sets to next version
#
# Usage:
#   ./build-release.sh <release-version> <next-version>
#
# Examples:
#   # Build 1.0.0 release, then go back to snapshot
#   ./build-release.sh 1.0.0 1.0.0-SNAPSHOT
#
#   # Build 1.0.0 release, then move to next minor snapshot
#   ./build-release.sh 1.0.0 1.1.0-SNAPSHOT
#
#   # Build 1.0.0 release, then move to next major snapshot
#   ./build-release.sh 1.0.0 2.0.0-SNAPSHOT
#
#   # Build release candidate, then go back to snapshot
#   ./build-release.sh 2.0.0-rc1 2.0.0-SNAPSHOT
#
# This script will:
#   1. Set all module versions to <release-version>
#   2. Run mvn clean install to build all artifacts
#   3. Set all module versions to <next-version>

set -e

# Check arguments
if [ "$#" -ne 2 ]; then
    echo "❌ Error: Missing arguments"
    echo ""
    echo "Usage: $0 <release-version> <next-version>"
    echo ""
    echo "Examples:"
    echo "  # Build 1.0.0 and revert to snapshot"
    echo "  $0 1.0.0 1.0.0-SNAPSHOT"
    echo ""
    echo "  # Build 1.0.0 and move to next minor"
    echo "  $0 1.0.0 1.1.0-SNAPSHOT"
    echo ""
    echo "  # Build 1.0.0 and move to next major"
    echo "  $0 1.0.0 2.0.0-SNAPSHOT"
    exit 1
fi

RELEASE_VERSION=$1
NEXT_VERSION=$2

echo "📋 Configuration:"
echo "   Release version: $RELEASE_VERSION"
echo "   Next version:    $NEXT_VERSION"
echo ""
echo "🔄 Setting versions to $RELEASE_VERSION..."

# Update root and infra modules
mvn versions:set -DnewVersion=$RELEASE_VERSION -DprocessAllModules
mvn versions:commit

echo "✅ All versions set to $RELEASE_VERSION"
echo ""
echo "🏗️  Building all modules..."

# Build everything
mvn clean install

echo ""
echo "✅ Build complete! Artifacts created with version $RELEASE_VERSION:"
echo "   - fn/layer/auth/target/auth.fn.shared-$RELEASE_VERSION.zip"
echo "   - fn/layer/api/target/api.fn.shared-$RELEASE_VERSION.zip"
echo "   - fn/auth/message/target/auth.fn.message-$RELEASE_VERSION.jar"
echo "   - fn/auth/post-confirmation/target/auth.fn.post-confirmation-$RELEASE_VERSION.jar"
echo "   - fn/api/user/target/api.fn.user-$RELEASE_VERSION.jar"
echo ""
echo "🔄 Setting versions to $NEXT_VERSION..."

# Update to next version
mvn versions:set -DnewVersion=$NEXT_VERSION -DprocessAllModules
mvn versions:commit

echo "✅ Versions updated to $NEXT_VERSION"
echo "🎉 Done!"
