#!/bin/bash

# simulating properly set up env
export PDI_ROOT_DIR=/Applications/Development/pdi-ce-6.0.1
export PROJECT_ROOT_DIR=/Users/diethardsteiner/Dropbox/Pentaho/Examples/PDI/logging-on-hadoop

cd $PDI_ROOT_DIR

sh ./kitchen.sh -file=$PROJECT_ROOT_DIR/di/jb_master_wrapper.kjb \
-param:VAR_PROPERTIES_FILE=$PROJECT_ROOT_DIR/config/project.properties \
-param:VAR_DI_PROCESS_NAME='sample job' \
-param:VAR_DI_PROCESS_FILE_PATH=$PROJECT_ROOT_DIR/di/jb_sample.kjb \
-param:VAR_PARAMETER_OBJECT='{"VAR_DATE":"2016-02-01","VAR_CITY":"London"}' \
> /tmp/jb_sample.err.log