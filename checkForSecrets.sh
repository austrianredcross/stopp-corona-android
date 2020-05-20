#!/bin/bash

array=( STAGE_AUTHORIZATION_VALUE STAGE_BASE_URL STAGE_BASE_URL_TAN )
for i in "${array[@]}"
do
  if grep -q $i "local.properties"; then
    echo "forbidden string '$i' was found"
    exit 1
  fi
	echo
done