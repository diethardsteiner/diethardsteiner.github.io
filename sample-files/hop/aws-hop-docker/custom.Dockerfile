FROM apache/incubator-hop:0.70-SNAPSHOT
ENV GIT_REPO_URI=
# example value: https://github.com/diethardsteiner/apache-hop-minimal-project.git
ENV GIT_REPO_NAME=
# example value: apache-hop-minimal-project
USER root
RUN apk update \
  && apk add --no-cache git  
# copy custom entrypoint extension shell script
COPY --chown=hop:hop ./resources/clone-git-repo.sh /home/hop/clone-git-repo.sh
USER hop