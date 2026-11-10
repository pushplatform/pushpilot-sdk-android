#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."
export GPG_TTY="$(tty)"

read -r -p "Central token username: " CENTRAL_USER
read -r -s -p "Central token password: " CENTRAL_PASSWORD
printf '\n'

SDK_VERSION="$(tr -d '[:space:]' < VERSION)"
ARTIFACT_PATH="ru/pushplatform/sdk-android/$SDK_VERSION"
BUNDLE="sdk-android-$SDK_VERSION-central.zip"

./gradlew :sdk:publishReleasePublicationToReleaseStagingRepository \
  -Psigning.gnupg.keyName=A71B2CC8B220144377C9EB2685084BEFE83D9955

# Удаляем предыдущий ZIP, чтобы в него не попали старые файлы.
rm -f "build/$BUNDLE"
(
  cd build/maven-repository
  zip -qr "../$BUNDLE" "$ARTIFACT_PATH"
)

AUTH="$(printf '%s:%s' "$CENTRAL_USER" "$CENTRAL_PASSWORD" |
  base64 | tr -d '\r\n')"

DEPLOYMENT_ID="$(curl --fail-with-body --silent --show-error \
  --header "Authorization: Bearer $AUTH" \
  --form "bundle=@build/$BUNDLE" \
  "https://central.sonatype.com/api/v1/publisher/upload?publishingType=AUTOMATIC")"

rm -f "build/$BUNDLE"

printf 'Отправлено. Deployment ID: %s\n' "$DEPLOYMENT_ID"
printf 'Проверка и публикация продолжаются в Central Portal.\n'