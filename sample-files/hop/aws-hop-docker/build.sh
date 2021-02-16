#!/bin/zsh

DOCKER_IMG_CHECK=$(docker images | grep ds/custom-hop)

if [ ! -z "${DOCKER_IMG_CHECK}" ]; then
  echo "removing existing ds/custom-hop image"
  docker rmi ds/custom-hop:latest
fi

docker build . -f custom.Dockerfile -t ds/custom-hop:latest

echo " ==== TESTING ====="


HOP_DOCKER_IMAGE=ds/custom-hop:latest
PROJECT_DEPLOYMENT_DIR=/home/hop/apache-hop-minimal-project

docker run -it --rm \
  --env HOP_LOG_LEVEL=Basic \
  --env HOP_FILE_PATH='${PROJECT_HOME}/main.hwf' \
  --env HOP_PROJECT_DIRECTORY=${PROJECT_DEPLOYMENT_DIR} \
  --env HOP_PROJECT_NAME=apache-hop-minimum-project \
  --env HOP_ENVIRONMENT_NAME=dev \
  --env HOP_ENVIRONMENT_CONFIG_FILE_NAME_PATHS=${PROJECT_DEPLOYMENT_DIR}/dev-config.json \
  --env HOP_RUN_CONFIG=local \
  --env HOP_CUSTOM_ENTRYPOINT_EXTENSION_SHELL_FILE_PATH=/home/hop/clone-git-repo.sh \
  --env GIT_REPO_URI=https://github.com/diethardsteiner/apache-hop-minimal-project.git \
  --env GIT_REPO_NAME=apache-hop-minimal-project \
  --name my-simple-hop-container \
  ${HOP_DOCKER_IMAGE}