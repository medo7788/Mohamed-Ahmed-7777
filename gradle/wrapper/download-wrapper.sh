#!/bin/bash

# Script to download official Gradle 9.3.1 wrapper JAR
# This ensures the wrapper is the official version from Gradle distributions

set -e

GRADLE_VERSION="9.3.1"
WRAPPER_JAR_URL="https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-wrapper.jar"
WRAPPER_JAR_PATH="gradle/wrapper/gradle-wrapper.jar"
EXPECTED_CHECKSUM="b3a875ddc1f044746e1b1a55f645584505f4a10438c1afea9f15e92a7c42ec13"

echo "🔄 Downloading official Gradle ${GRADLE_VERSION} wrapper JAR..."
echo "📦 Source: ${WRAPPER_JAR_URL}"

# Download the wrapper JAR
curl -fsSL "${WRAPPER_JAR_URL}" -o "${WRAPPER_JAR_PATH}"

echo "✅ Downloaded to: ${WRAPPER_JAR_PATH}"

# Verify checksum (if on macOS/Linux with shasum/sha256sum)
if command -v sha256sum &> /dev/null; then
    ACTUAL_CHECKSUM=$(sha256sum "${WRAPPER_JAR_PATH}" | awk '{print $1}')
    echo "🔐 Verifying checksum..."
    if [ "${ACTUAL_CHECKSUM}" = "${EXPECTED_CHECKSUM}" ]; then
        echo "✅ Checksum verified: ${ACTUAL_CHECKSUM}"
    else
        echo "❌ Checksum mismatch!"
        echo "   Expected: ${EXPECTED_CHECKSUM}"
        echo "   Actual:   ${ACTUAL_CHECKSUM}"
        exit 1
    fi
elif command -v shasum &> /dev/null; then
    ACTUAL_CHECKSUM=$(shasum -a 256 "${WRAPPER_JAR_PATH}" | awk '{print $1}')
    echo "🔐 Verifying checksum..."
    if [ "${ACTUAL_CHECKSUM}" = "${EXPECTED_CHECKSUM}" ]; then
        echo "✅ Checksum verified: ${ACTUAL_CHECKSUM}"
    else
        echo "❌ Checksum mismatch!"
        echo "   Expected: ${EXPECTED_CHECKSUM}"
        echo "   Actual:   ${ACTUAL_CHECKSUM}"
        exit 1
    fi
else
    echo "⚠️  sha256sum/shasum not available, skipping verification"
fi

echo ""
echo "✅ Official Gradle Wrapper updated successfully!"
echo ""
echo "📝 Next steps:"
echo "   git add gradle/wrapper/gradle-wrapper.jar"
echo "   git commit -m 'Update Gradle Wrapper JAR to official 9.3.1'"
echo "   git push"
