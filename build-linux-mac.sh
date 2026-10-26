#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
VERSION=8.10.2
DIR=.build-tools
if [ ! -x "$DIR/gradle-$VERSION/bin/gradle" ]; then
  mkdir -p "$DIR"
  curl -fL "https://services.gradle.org/distributions/gradle-$VERSION-bin.zip" -o "$DIR/gradle.zip"
  (cd "$DIR" && unzip -q gradle.zip && rm gradle.zip)
fi
"$DIR/gradle-$VERSION/bin/gradle" assembleDebug
printf 'APK: %s/app/build/outputs/apk/debug/app-debug.apk\n' "$PWD"
