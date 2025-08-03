#!/usr/local/bin/bash

VARIANT='unstable'

gradle 'checkLicense'

if test $? -ne 0; then
 echo 'Check error!'; exit 1; fi
