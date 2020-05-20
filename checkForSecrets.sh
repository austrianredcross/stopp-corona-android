#!/bin/bash

set -e

if test -f "local.properties"; then
  echo "local.properties exist we check it"
else
  echo "no local.properties found - no need to check them"
  exit 0
fi


array=( sdk.dir STAGE_AUTHORIZATION_VALUE STAGE_BASE_URL STAGE_BASE_URL_TAN STAGE_CERTIFICATE_CHAIN STAGE_CERTIFICATE_CHAIN_TAN STAGE_HOSTNAME STAGE_HOSTNAME_TAN STAGE_NEARBY_API_KEY STAGE_P2P_APPLICATION_KEY PROD_AUTHORIZATION_VALUE PROD_BASE_URL PROD_BASE_URL_TAN PROD_CERTIFICATE_CHAIN PROD_CERTIFICATE_CHAIN_TAN PROD_HOSTNAME PROD_HOSTNAME_TAN PROD_NEARBY_API_KEY PROD_P2P_APPLICATION_KEY)
for i in "${array[@]}"
do
  if grep -q $i "local.properties"; then
    echo "forbidden string '$i' was found"
    exit 1
  fi
	echo
done

