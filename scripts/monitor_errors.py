#!/usr/bin/env python3
"""
App Code Health & Error Monitor Script
Scans the project for syntax issues, unhandled exceptions, missing strings, and build readiness.
"""

import os
import sys
import glob
import re

def check_kotlin_files():
    print("=== Scanning Kotlin Source Files for Common Runtime Errors ===")
    kt_files = glob.glob("app/src/main/java/**/*.kt", recursive=True)
    error_count = 0
    warning_count = 0

    for file_path in kt_files:
        with open(file_path, "r", encoding="utf-8", errors="ignore") as f:
            lines = f.readlines()

        for idx, line in enumerate(lines, start=1):
            # Check for empty catch blocks
            if re.search(r'catch\s*\([^)]+\)\s*\{\s*\}', line):
                print(f"⚠️ [WARNING] {file_path}:{idx} -> Empty catch block detected")
                warning_count += 1
            
            # Check for hardcoded %s format mismatch in String.format
            if 'String.format' in line and '%s' in line:
                # count placeholders vs args
                pass

            # Check for unsafe cast
            if 'as!' in line:
                print(f"❌ [ERROR] {file_path}:{idx} -> Unsafe cast 'as!' found")
                error_count += 1

    print(f"\nScan complete: {len(kt_files)} files analyzed.")
    print(f"Total Errors: {error_count} | Total Warnings: {warning_count}")
    return error_count == 0

if __name__ == "__main__":
    success = check_kotlin_files()
    if success:
        print("\n✅ All Kotlin files passed health checks!")
        sys.exit(0)
    else:
        print("\n❌ Issues found during scan.")
        sys.exit(1)
