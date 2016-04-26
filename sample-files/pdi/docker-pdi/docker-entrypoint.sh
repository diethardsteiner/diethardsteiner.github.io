#!/bin/bash
# based on https://github.com/aloysius-lim/docker-pentaho-di/blob/master/docker/Dockerfile
set -e

if [ "$1" = 'carte.sh' ]; then
  # checking if env vars get passed down
  # echo "$KETTLE_HOME/carte.config.xml"
  # echo "$PENTAHO_HOME"
  if [ ! -f "$KETTLE_HOME/carte.config.xml" ]; then
    # Set variables to defaults if they are not already set
    : ${CARTE_NAME:=carte-server}
    : ${CARTE_NETWORK_INTERFACE:=eth0}
    : ${CARTE_PORT:=8181}
    : ${CARTE_USER:=cluster}
    : ${CARTE_PASSWORD:=cluster}
    : ${CARTE_IS_MASTER:=Y}

    : ${CARTE_INCLUDE_MASTERS:=N}

    : ${CARTE_REPORT_TO_MASTERS:=Y}
    : ${CARTE_MASTER_NAME:=carte-master}
    : ${CARTE_MASTER_HOSTNAME:=localhost}
    : ${CARTE_MASTER_PORT:=8181}
    : ${CARTE_MASTER_USER:=cluster}
    : ${CARTE_MASTER_PASSWORD:=cluster}
    : ${CARTE_MASTER_IS_MASTER:=Y}

    # Copy the right template and replace the variables in it
    if [ "$CARTE_INCLUDE_MASTERS" = "Y" ]; then
      cp $PENTAHO_HOME/templates/carte-slave.config.xml "$KETTLE_HOME/carte.config.xml"
      sed -i "s/CARTE_REPORT_TO_MASTERS/$CARTE_REPORT_TO_MASTERS/" "$KETTLE_HOME/carte.config.xml"
      sed -i "s/CARTE_MASTER_NAME/$CARTE_MASTER_NAME/" "$KETTLE_HOME/carte.config.xml"
      sed -i "s/CARTE_MASTER_HOSTNAME/$CARTE_MASTER_HOSTNAME/" "$KETTLE_HOME/carte.config.xml"
      sed -i "s/CARTE_MASTER_PORT/$CARTE_MASTER_PORT/" "$KETTLE_HOME/carte.config.xml"
      sed -i "s/CARTE_MASTER_USER/$CARTE_MASTER_USER/" "$KETTLE_HOME/carte.config.xml"
      sed -i "s/CARTE_MASTER_PASSWORD/$CARTE_MASTER_PASSWORD/" "$KETTLE_HOME/carte.config.xml"
      sed -i "s/CARTE_MASTER_IS_MASTER/$CARTE_MASTER_IS_MASTER/" "$KETTLE_HOME/carte.config.xml"
    else
      cp $PENTAHO_HOME/templates/carte-master.config.xml "$KETTLE_HOME/carte.config.xml"
    fi
    sed -i "s/CARTE_NAME/$CARTE_NAME/" "$KETTLE_HOME/carte.config.xml"
    sed -i "s/CARTE_NETWORK_INTERFACE/$CARTE_NETWORK_INTERFACE/" "$KETTLE_HOME/carte.config.xml"
    sed -i "s/CARTE_PORT/$CARTE_PORT/" "$KETTLE_HOME/carte.config.xml"
    sed -i "s/CARTE_USER/$CARTE_USER/" "$KETTLE_HOME/carte.config.xml"
    sed -i "s/CARTE_PASSWORD/$CARTE_PASSWORD/" "$KETTLE_HOME/carte.config.xml"
    sed -i "s/CARTE_IS_MASTER/$CARTE_IS_MASTER/" "$KETTLE_HOME/carte.config.xml"
  fi
fi

# Run any custom scripts
if [ -d $PENTAHO_HOME/docker-entrypoint.d ]; then
  for f in $PENTAHO_HOME/docker-entrypoint.d/*.sh; do
    [ -f "$f" ] && . "$f"
  done
fi

exec "$@"