#!/bin/bash

adp_log() {
  local msg="$(echo "$@" | sed 's|"|\\"|g' | tr -d '\n')"
  printf '{"version":"0.3.0", "timestamp":"%s", "severity":"debug", "service_id":"%s", "message":"%s"}\n' \
    "$(date --iso-8601=seconds)" "$SERVICE_ID" "$msg"
}

## Add multiple certs if they exist in file bundle.
if [  -f  $CACERT_PATH/$CACERT_NAME ]; then
  mkdir /tmp/individualCerts && cd $_
  FILE_COUNT=$(csplit -f individual- $CACERT_PATH/$CACERT_NAME '/-----BEGIN CERTIFICATE-----/' '{*}' --elide-empty-files | wc -l)
  echo "Number of certs in cacert bundles is ${FILE_COUNT}"
  for i in $(ls); do
    echo "Adding ${i} to java keystore"
    OUTPUT="$(keytool -storepass 'changeit' -noprompt -trustcacerts -importcert -file ${i} -alias ${i} -keystore $DEFAULT_JAVA_CERTS 2>&1)"
    adp_log "keytool: $OUTPUT"
  done
  cd ../
  rm -rf /tmp/individualCerts
fi

java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -Dspring.profiles.active=$SPRING_PROFILE -jar /eric-esoa-api-gateway.jar
