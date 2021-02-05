#!/bin/zsh

DOCKER_IMG_CHECK=$(docker images | grep ds/custom-hop)

if [ ! -z "${DOCKER_IMG_CHECK}" ]; then
  echo "removing existing ds/custom-hop image"
  docker rmi ds/custom-hop:latest
fi

docker build . -f custom.Dockerfile -t ds/custom-hop:latest

echo " ==== TESTING ====="

if [ ! -d "/tmp/apache-hop-minimal-project" ]; then
  echo "... cloning git repo into /tmp folder"
  cd /tmp
  git clone https://github.com/diethardsteiner/apache-hop-minimal-project.git
fi

HOP_DOCKER_IMAGE=ds/custom-hop:latest
PATH_TO_LOCAL_PROJECT_DIR=/tmp/apache-hop-minimal-project

docker run -it --rm \
  --env HOP_LOG_LEVEL=Basic \
  --env HOP_FILE_PATH='${PROJECT_HOME}/main.hwf' \
  --env HOP_PROJECT_DIRECTORY=/files \
  --env HOP_PROJECT_NAME=apache-hop-minimum-project \
  --env HOP_ENVIRONMENT_NAME=dev \
  --env HOP_ENVIRONMENT_CONFIG_FILE_NAME_PATHS=/files/dev-config.json \
  --env HOP_RUN_CONFIG=local \
  -v ${PATH_TO_LOCAL_PROJECT_DIR}:/files \
  --name my-simple-hop-container \
  ${HOP_DOCKER_IMAGE}