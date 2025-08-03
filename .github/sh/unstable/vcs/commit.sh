#!/usr/local/bin/bash

VARIANT='unstable'
TARGET_BRANCH="${VARIANT}"

ISSUER='lib/build/yml/metadata.yml'
if [[ ! -f "$ISSUER" ]]; then echo "No file \"$ISSUER\"!"; exit 1
elif [[ ! -s "$ISSUER" ]]; then echo "File \"$ISSUER\" is empty!"; exit 1; fi
VERSION="$(yq -erM .version "$ISSUER")" || exit 1

for it in VERSION; do
 if test -z "${!it}"; then echo "Argument \"${it}\" is empty!"; exit 1; fi; done

git add . \
 && git commit -m "${TARGET_BRANCH} <- ${VERSION}" \
 && git tag "${VERSION}"

if test $? -ne 0; then
 echo 'Commit error!'; exit 1; fi
