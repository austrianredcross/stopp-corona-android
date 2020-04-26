#!/bin/bash

babelish version
babelish csv2android
wait
mv ./app/src/main/res/values-en/strings.xml ./app/src/main/res/values/strings.xml
rmdir ./app/src/main/res/values-en/
rm ./translations.csv
echo Done.
