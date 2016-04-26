# Mainly based on:
# https://github.com/aloysius-lim/docker-pentaho-di/blob/master/docker/Dockerfile
# https://github.com/wmarinho/docker-pentaho/blob/master/data-integration/Dockerfile
# Docker supports OpenJDK only
# For versions and tags see https://github.com/docker-library/docs/blob/master/java/tag-details.md
FROM java:8-jre

MAINTAINER Diethard Steiner

# Set required environment vars
ENV PDI_RELEASE=6.0 \
    PDI_VERSION=6.0.1.0-386 \
    CARTE_PORT=8181 \
    PENTAHO_JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64 \
    PENTAHO_HOME=/home/pentaho

# Create user
RUN mkdir ${PENTAHO_HOME} && \
    groupadd -r pentaho && \
    useradd -s /bin/bash -d ${PENTAHO_HOME} -r -g pentaho pentaho && \
    chown pentaho:pentaho ${PENTAHO_HOME}

# Add files
RUN mkdir $PENTAHO_HOME/docker-entrypoint.d $PENTAHO_HOME/templates $PENTAHO_HOME/scripts

COPY carte-*.config.xml $PENTAHO_HOME/templates/

COPY docker-entrypoint.sh $PENTAHO_HOME/scripts/

RUN chown -R pentaho:pentaho $PENTAHO_HOME 
# chmod +x $PENTAHO_HOME/scripts/docker-entrypoint.sh && \


# Switch to the pentaho user
USER pentaho


# Download PDI
RUN /usr/bin/wget \
    --progress=dot:giga \
    http://downloads.sourceforge.net/project/pentaho/Data%20Integration/${PDI_RELEASE}/pdi-ce-${PDI_VERSION}.zip \
    -O /tmp/pdi-ce-${PDI_VERSION}.zip && \
    /usr/bin/unzip -q /tmp/pdi-ce-${PDI_VERSION}.zip -d  $PENTAHO_HOME && \
    rm /tmp/pdi-ce-${PDI_VERSION}.zip

    # alternatively: curl -L -o /tmp/pdi-ce-${PDI_VERSION}.zip \
    # http://downloads.sourceforge.net/project/pentaho/Data%20Integration/${PDI_RELEASE}/pdi-ce-${PDI_VERSION}.zip && \
    # unzip file into the pentaho home directory, target indicated by the -d flag \
    # by default the extracted file will end up in a folder called data-integration \


# We can only add KETTLE_HOME to the PATH variable now
# as the path gets eveluated - so it must already exist
ENV KETTLE_HOME=$PENTAHO_HOME/data-integration \
    PATH=$KETTLE_HOME:$PATH


# Expose Carte Server
EXPOSE ${CARTE_PORT}

# As we cannot use env variable with the entrypoint and cmd instructions
# we set the working directory here to a convenient location
# We set it to KETTLE_HOME so we can start carte easily
WORKDIR $KETTLE_HOME


ENTRYPOINT ["../scripts/docker-entrypoint.sh"]

# Run Carte - these parameters are passed to the entrypoint
CMD ["carte.sh", "carte.config.xml"]