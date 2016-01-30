#!/bin/sh

#cd $PDI_DIR

cd /Applications/Development/pdi-ce-6.0

sh ./kitchen.sh -file=/Users/diethardsteiner/Dropbox/Pentaho/Examples/PDI/database-versioning-tool/jb_dvm_master.kjb \
-param:VAR_DB_DVM_JDBC_DRIVER_CLASS_NAME=org.postgresql.Driver \
-param:VAR_DB_DVM_JDBC_URL=jdbc:postgresql://localhost:5432/test \
-param:VAR_DB_DVM_PW=postgres \
-param:VAR_DB_DVM_USER=postgres \
-param:VAR_DDL_FILES_DIR=/Users/diethardsteiner/Dropbox/Pentaho/Examples/PDI/database-versioning-tool/ddl \
-param:VAR_PROCESSED_DDL_FILES_LOG=/Users/diethardsteiner/Dropbox/Pentaho/Examples/PDI/database-versioning-tool/processed/processed.csv \
> /tmp/jb_dvm_master.err.log
