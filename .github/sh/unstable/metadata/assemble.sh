#!/usr/local/bin/bash

VARIANT='unstable'

gradle "lib:assemble${VARIANT^}Metadata"

if test $? -ne 0; then
 echo "Assemble \"$VARIANT\" error!"; exit 1; fi

ISSUER='lib/build/yml/metadata.yml'
if [[ ! -f "${ISSUER}" ]]; then echo "No file \"${ISSUER}\"!"; exit 1
elif [[ ! -s "${ISSUER}" ]]; then echo "File \"${ISSUER}\" is empty!"; exit 1; fi
