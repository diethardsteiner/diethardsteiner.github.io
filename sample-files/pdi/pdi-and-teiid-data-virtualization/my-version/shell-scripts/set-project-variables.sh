#!/bin/bash
echo "SETTING ENVIRONMENT VARIABLES ..."
echo "---------------------------------"

# Define variables

# ENVIRONMENT
# should be set in .bashrc
export PROJECT_ENV=dev
echo "- PROJECT_ENV: " $PROJECT_ENV

# run the following on the command line and only then start kettle spoon

# get path of current dir based on http://www.ostricher.com/2014/10/the-right-way-to-get-the-directory-of-a-bash-script/
get_script_dir () {
     SOURCE="${BASH_SOURCE[0]}"
     # While $SOURCE is a symlink, resolve it
     while [ -h "$SOURCE" ]; do
          DIR="$( cd -P "$( dirname "$SOURCE" )" && pwd )"
          SOURCE="$( readlink "$SOURCE" )"
          # If $SOURCE was a relative symlink (so no "/" as prefix, need to resolve it relative to the symlink base directory
          [[ $SOURCE != /* ]] && SOURCE="$DIR/$SOURCE"
     done
     DIR="$( cd -P "$( dirname "$SOURCE" )" && pwd )"
     echo "$DIR"
}

export PROJECT_SHELL_SCRIPTS_FOLDER="$(get_script_dir)"

# KETTLE HOME LOCATION
export KETTLE_HOME=$PROJECT_SHELL_SCRIPTS_FOLDER/../config/$PROJECT_ENV
echo "- KETTLE_HOME: " $KETTLE_HOME

# JNDI FILE LOCATION
# KETTLE_JNDI_ROOT is only availble in the spoon shell scripts as of version 5. For an earlier version, add the following to the OPT section in the shell script: -DKETTLE_JNDI_ROOT=$KETTLE_JNDI_ROOT
#export KETTLE_JNDI_ROOT=$PROJECT_SHELL_SCRIPTS_FOLDER/../config/$PROJECT_ENV/simple-jndi
#echo "- KETTLE_JNDI_ROOT: " $KETTLE_JNDI_ROOT

# METASTORE LOCATION
# Add the following to the OPT section in spoon.sh: -DPENTAHO_METASTORE_FOLDER=$PENTAHO_METASTORE_FOLDER
export PENTAHO_METASTORE_FOLDER=$PROJECT_SHELL_SCRIPTS_FOLDER/../config/$PROJECT_ENV
echo "- PENTAHO_METASTORE_FOLDER: " $PENTAHO_METASTORE_FOLDER
