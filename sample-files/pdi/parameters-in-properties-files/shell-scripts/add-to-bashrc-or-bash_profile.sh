
# ADD THIS TO BASHRC OR BASH_PROFILE

# PDI GLOBAL VARS
# ----------------

# ENVIRONMENT
export PROJECT_ENV=dev
echo "- PROJECT_ENV: " $PROJECT_ENV

# KETTLE HOME LOCATION
export KETTLE_HOME=$PROJECT_HOME/config/$PROJECT_ENV
echo "- KETTLE_HOME: " $KETTLE_HOME

# JNDI FILE LOCATION
# KETTLE_JNDI_ROOT is only availble in the spoon shell scripts as of version 5. For an earlier version, add the following to the OPT section in spoon.sh: -DKETTLE_JNDI_ROOT=$KETTLE_JNDI_ROOT
export KETTLE_JNDI_ROOT=$PROJECT_SHELL_SCRIPTS_FOLDER/../config/$PROJECT_ENV/simple-jndi
echo "- KETTLE_JNDI_ROOT: " $KETTLE_JNDI_ROOT

# METASTORE LOCATION
# Add the following to the OPT section in spoon.sh: -DPENTAHO_METASTORE_FOLDER=$PENTAHO_METASTORE_FOLDER
export PENTAHO_METASTORE_FOLDER=$PROJECT_HOME/config/$PROJECT_ENV
echo "- PENTAHO_METASTORE_FOLDER: " $PENTAHO_METASTORE_FOLDER

# PDI DIR
export PDI_HOME=/Applications/Development/pdi-ce-6.1
echo "- PDI_HOME: " $PDI_HOME