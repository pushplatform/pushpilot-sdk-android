#!/usr/bin/env bash
set -euo pipefail

root=$(cd "$(dirname "$0")/.." && pwd)
staging="$root/build/maven-repository"
coordinate="com/pushplatform/sdk-android/1.0.0"
remote="https://github.com/pushplatform/pushpilot-sdk-android.git"
source_commit=$(git -C "$root" rev-parse HEAD)
if [[ -n $(git -C "$root" status --porcelain --untracked-files=no) ]]; then
    echo 'Commit source and publication changes before publishing.' >&2
    exit 1
fi

repository=$(mktemp -d "${TMPDIR:-/tmp}/pushpilot-android-maven.XXXXXX")
if [[ -n $(git ls-remote --heads "$remote" maven) ]]; then
    git clone --single-branch --branch maven "$remote" "$repository"
else
    git -C "$repository" init -b maven
    git -C "$repository" remote add origin "$remote"
fi
if [[ -e "$repository/$coordinate" ]]; then
    echo 'Version 1.0.0 already exists. Published versions must never be overwritten.' >&2
    exit 1
fi
# --prepared reuses the publication already prepared in this checkout.
if [[ ${1:-} != --prepared ]]; then
    "$root/gradlew" -p "$root" :sdk:publishReleasePublicationToReleaseStagingRepository --no-daemon --console=plain
fi
for extension in aar pom module sources.jar javadoc.jar; do
    case "$extension" in
        *.jar) artifact="sdk-android-1.0.0-$extension" ;;
        *) artifact="sdk-android-1.0.0.$extension" ;;
    esac
    if [[ ! -s "$staging/$coordinate/$artifact" ]]; then
        printf 'Missing prepared artifact: %s\n' "$artifact" >&2
        exit 1
    fi
done
cp -R "$staging/." "$repository/"
cp "$root/LICENSE" "$repository/LICENSE"
cat > "$repository/README.md" <<EOF
# PushPilot Android public Maven repository

Repository URL: https://raw.githubusercontent.com/pushplatform/pushpilot-sdk-android/maven/

Dependency: com.pushplatform:sdk-android:1.0.0

Source: https://github.com/pushplatform/pushpilot-sdk-android/commit/$source_commit

Release instructions: https://github.com/pushplatform/pushpilot-sdk-android/blob/release/android-1.0.0/docs/RELEASING.md

Artifacts under versioned paths are immutable. No authentication is needed to install.
EOF
git -C "$repository" add .
git -C "$repository" commit -m "release: publish Android SDK 1.0.0 from $source_commit"
git -C "$repository" push origin maven
printf 'Published public Maven repository from %s\n' "$repository"
