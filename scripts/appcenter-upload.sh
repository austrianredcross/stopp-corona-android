#!/bin/sh

# API Token generated in https://appcenter.ms/settings/apitokens
APPCENTER_API_TOKEN=$1
# User that has access to the published app
APPCENTER_OWNER_NAME=$2
# App name from the AppCenter console
APPCENTER_APP_NAME=$3
# Path to the APK that will be uploaded
APK_PATH=$4
# Path to the changelog file
CHANGELOG=$5
# Distribution group in which the app will be published
DISTRIBUTION_GROUP=$6

apt-get update
apt-get --assume-yes install jq
# Call the upload resources endpoint in order to obtain the `upload_url` and `upload_id` which will be used in the nexts steps
UPLOAD_RESOURCE_ENDPOINT="https://api.appcenter.ms/v0.1/apps/$APPCENTER_OWNER_NAME/$APPCENTER_APP_NAME/release_uploads"
RESPONSE=`curl -X POST --header "Content-Type: application/json" --header "Accept: application/json" --header "X-API-Token: $APPCENTER_API_TOKEN" $UPLOAD_RESOURCE_ENDPOINT`
UPLOAD_URL=`echo $RESPONSE | jq -r '.upload_url'`
UPLOAD_ID=`echo $RESPONSE | jq -r '.upload_id'`

# Upload the APK
curl -F "ipa=@$APK_PATH" $UPLOAD_URL

# Update upload resource's status to committed and get the `release_url`
COMMIT_RESPONSE=`curl -X PATCH --header "Content-Type: application/json" --header "Accept: application/json" --header "X-API-Token: $APPCENTER_API_TOKEN" -d '{ "status": "committed"  }' https://api.appcenter.ms/v0.1/apps/$APPCENTER_OWNER_NAME/$APPCENTER_APP_NAME/release_uploads/$UPLOAD_ID`
RELEASE_URL=`echo $COMMIT_RESPONSE | jq -r '.release_url'`

# Distribute the uploaded release to a distribution group
curl -X PATCH --header "Content-Type: application/json" --header "Accept: application/json" --header "X-API-Token: $APPCENTER_API_TOKEN" -d "{ \"destination_name\": \"$DISTRIBUTION_GROUP\", \"release_notes\": \"$CHANGELOG\" }" "https://api.appcenter.ms/$RELEASE_URL"
