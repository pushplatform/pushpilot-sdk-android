#!/usr/bin/env bash
set -euo pipefail

root=$(cd "$(dirname "$0")/.." && pwd)
staging="$root/build/maven-repository"
coordinate="com/pushplatform/sdk-android/1.2.0"
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
    echo 'Version 1.2.0 already exists. Published versions must never be overwritten.' >&2
    exit 1
fi
# --prepared reuses the publication already prepared in this checkout.
if [[ ${1:-} != --prepared ]]; then
    "$root/gradlew" -p "$root" :sdk:publishReleasePublicationToReleaseStagingRepository --no-daemon --console=plain
fi
for extension in aar pom module sources.jar javadoc.jar; do
    case "$extension" in
        *.jar) artifact="sdk-android-1.2.0-$extension" ;;
        *) artifact="sdk-android-1.2.0.$extension" ;;
    esac
    if [[ ! -s "$staging/$coordinate/$artifact" ]]; then
        printf 'Missing prepared artifact: %s\n' "$artifact" >&2
        exit 1
    fi
done
# Copy only the new immutable coordinate; never replace older release files.
mkdir -p "$repository/$(dirname "$coordinate")"
cp -R "$staging/$coordinate" "$repository/$coordinate"
python3 - "$repository/$(dirname "$coordinate")" <<'PYMETA'
from pathlib import Path
import datetime,hashlib,sys,xml.etree.ElementTree as ET
folder=Path(sys.argv[1]);versions=sorted((p.name for p in folder.iterdir() if p.is_dir()),key=lambda v:tuple(map(int,v.split('.'))))
root=ET.Element('metadata');ET.SubElement(root,'groupId').text='com.pushplatform';ET.SubElement(root,'artifactId').text='sdk-android'
node=ET.SubElement(root,'versioning');ET.SubElement(node,'latest').text=versions[-1];ET.SubElement(node,'release').text=versions[-1]
entries=ET.SubElement(node,'versions')
for version in versions:ET.SubElement(entries,'version').text=version
ET.SubElement(node,'lastUpdated').text=datetime.datetime.now(datetime.timezone.utc).strftime('%Y%m%d%H%M%S')
metadata=folder/'maven-metadata.xml';ET.indent(root);metadata.write_bytes(ET.tostring(root,encoding='utf-8',xml_declaration=True)+b'\n')
for algorithm in ['md5','sha1','sha256','sha512']:(folder/('maven-metadata.xml.'+algorithm)).write_text(hashlib.new(algorithm,metadata.read_bytes()).hexdigest())
PYMETA
cp "$root/LICENSE" "$repository/LICENSE"
cat > "$repository/README.md" <<EOF
# PushPilot Android public Maven repository

Repository URL: https://raw.githubusercontent.com/pushplatform/pushpilot-sdk-android/maven/

Dependency: com.pushplatform:sdk-android:1.2.0

Source: https://github.com/pushplatform/pushpilot-sdk-android/commit/$source_commit

Release instructions: https://github.com/pushplatform/pushpilot-sdk-android/blob/feat/unified-applications/README.md

Artifacts under versioned paths are immutable. No authentication is needed to install.
EOF
git -C "$repository" add .
git -C "$repository" commit -m "release: publish Android SDK 1.2.0 from $source_commit"
git -C "$repository" push origin maven
printf 'Published public Maven repository from %s\n' "$repository"
