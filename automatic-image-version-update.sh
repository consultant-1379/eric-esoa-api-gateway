####################################################################################
# Script to automatically change SO-BASE-IMAGE and KEYCLOAK_CLIENT_VERSION new     #
# version in Dockerfile and eric-product-info.yaml                                 #
####################################################################################
#!/bin/bash
so_base_version_placeholder="SO_BASE_VERSION"
keycloak_client_version_placeholder="KEYCLOAK_CLIENT_VERSION"
so_base_postgres_placeholder="SO_BASE_POSTGRES"

sed_check(){
  if [ $? -ne 0 ]; then
      echo "Unable to update the image version"
      exit 1
  fi
}

echo "Querying for the latest base image version..."
so_base_image_version=$(curl -u amadm100:AKCp5bBhBeBH1StEyF5jb1ZCrhWWJ97jkUCGFpcZvbnAqVSVAzH5RkzKCJi7dVdYJxjNDoCq9 \
-X POST https://arm.epk.ericsson.se/artifactory/api/search/aql \
-H "content-type: text/plain" \
-d 'items.find({ "repo": {"$eq":"docker-v2-global-local"}, "path": {"$match" : "proj-orchestration-so/so-base-openjdk17/1.*"}}).sort({"$desc": ["created"]}).limit(1)' \
2>/dev/null | grep path | sed -e 's_.*\/\(.*\)".*_\1_')

echo "Updating Dockerfile and eric-product-info.yaml with new so-base-image version-$so_base_image_version"
grep -q '\b'"$so_base_version_placeholder"'\b' Dockerfile
if [ $? -eq 0 ]; then
  sed -i 's/'"$so_base_version_placeholder"'/'"$so_base_image_version"'/' Dockerfile
  sed_check
else
  echo "The placeholder value could not be found in Dockerfile"; exit 1
fi

grep -q '\b'"$so_base_version_placeholder"'\b' ./charts/eric-esoa-api-gateway/eric-product-info.yaml
if [ $? -eq 0 ]; then
  sed -i 's/'"$so_base_version_placeholder"'/'"$so_base_image_version"'/' ./charts/eric-esoa-api-gateway/eric-product-info.yaml
  sed_check
else
  echo "The placeholder value could not be found in eric-product-info.yaml"; exit 1
fi


#echo "Querying for the latest keycloak client image version..."
#keycloak_client_image_version=$(curl -u amadm100:AKCp5bBhBeBH1StEyF5jb1ZCrhWWJ97jkUCGFpcZvbnAqVSVAzH5RkzKCJi7dVdYJxjNDoCq9 \
#-X POST https://arm.epk.ericsson.se/artifactory/api/search/aql \
#-H "content-type: text/plain" \
#-d 'items.find({ "repo": {"$eq":"docker-v2-global-local"}, "path": {"$match" : "proj-orchestration-so/keycloak-client/1.*"}}).sort({"$desc": ["created"]}).limit(1)' \
#2>/dev/null | grep path | sed -e 's_.*\/\(.*\)".*_\1_')

#echo "Updating eric-product-info.yaml with new keycloak client image version-$keycloak_client_image_version"
#grep -q '\b'"$keycloak_client_version_placeholder"'\b' ./charts/eric-esoa-api-gateway/eric-product-info.yaml
#if [ $? -eq 0 ]; then
#  sed -i 's/'"$keycloak_client_version_placeholder"'/'"$keycloak_client_image_version"'/' ./charts/eric-esoa-api-gateway/eric-product-info.yaml
#  sed_check
#else
#  echo "The placeholder value could not be found in eric-product-info.yaml"; exit 1
#fi

echo "Querying for the latest so postgres13 image version..."
so_base_postgres_version=$(curl -u amadm100:AKCp5bBhBeBH1StEyF5jb1ZCrhWWJ97jkUCGFpcZvbnAqVSVAzH5RkzKCJi7dVdYJxjNDoCq9 \
-X POST https://arm.epk.ericsson.se/artifactory/api/search/aql \
-H "content-type: text/plain" \
-d 'items.find({ "repo": {"$eq":"docker-v2-global-local"}, "path": {"$match" : "proj-orchestration-so/so-base-postgres13/1.*"}}).sort({"$desc": ["created"]}).limit(1)' \
2>/dev/null | grep path | sed -e 's_.*\/\(.*\)".*_\1_')

echo "Updating eric-product-info.yaml with new so base postgres image version-"$so_base_postgres_version
sed -i 's/'$so_base_postgres_placeholder'/'$so_base_postgres_version'/' ./charts/eric-esoa-api-gateway/eric-product-info.yaml

echo "END of shell"
exit
