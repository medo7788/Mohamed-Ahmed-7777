#!/usr/bin/env python3
"""
Script to download the official Gradle wrapper JAR for version 9.3.1
and prepare it for commit to the repository.

Usage: python3 update-gradle-wrapper.py
"""

import urllib.request
import base64
import json
import sys

GRADLE_VERSION = "9.3.1"
DOWNLOAD_URL = f"https://services.gradle.org/distributions/gradle-{GRADLE_VERSION}-wrapper.jar"
JAR_PATH = "gradle/wrapper/gradle-wrapper.jar"

def download_wrapper():
    """Download the official Gradle wrapper JAR"""
    print(f"Downloading gradle-wrapper.jar for Gradle {GRADLE_VERSION}...")
    try:
        with urllib.request.urlopen(DOWNLOAD_URL) as response:
            jar_content = response.read()
        print(f"✓ Downloaded {len(jar_content)} bytes")
        return jar_content
    except Exception as e:
        print(f"✗ Failed to download: {e}")
        sys.exit(1)

def save_jar(content):
    """Save the JAR file locally"""
    try:
        with open(JAR_PATH, 'wb') as f:
            f.write(content)
        print(f"✓ Saved to {JAR_PATH}")
    except Exception as e:
        print(f"✗ Failed to save: {e}")
        sys.exit(1)

def main():
    print(f"Updating Gradle Wrapper to version {GRADLE_VERSION}")
    print("-" * 50)
    
    # Download
    jar_content = download_wrapper()
    
    # Save locally
    save_jar(jar_content)
    
    print("-" * 50)
    print("✓ Done! Now run:")
    print("  git add gradle/wrapper/gradle-wrapper.jar")
    print("  git commit -m 'Update Gradle Wrapper to 9.3.1 (official)'")
    print("  git push")

if __name__ == "__main__":
    main()
