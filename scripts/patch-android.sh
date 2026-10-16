#!/usr/bin/env bash
set -e

MANIFEST="android/app/src/main/AndroidManifest.xml"

if [ ! -f "$MANIFEST" ]; then
  echo "AndroidManifest.xml not found"
  exit 1
fi

# Add Termux RUN_COMMAND permission if missing
if ! grep -q "com.termux.permission.RUN_COMMAND" "$MANIFEST"; then
  sed -i 's#<uses-permission android:name="android.permission.INTERNET" />#<uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="com.termux.permission.RUN_COMMAND" />#' "$MANIFEST"
fi

# Add package visibility for Termux on Android 11+
if ! grep -q "com.termux</package>" "$MANIFEST"; then
  python3 - <<'PY'
import re

path = "android/app/src/main/AndroidManifest.xml"
text = open(path).read()

queries = '''    <queries>
        <package android:name="com.termux" />
    </queries>
'''

if "<queries>" not in text:
    text = text.replace("<application", queries + "    <application", 1)

open(path, "w").write(text)
PY
fi

echo "Android manifest patched successfully."
