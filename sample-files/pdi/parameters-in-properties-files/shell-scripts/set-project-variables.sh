#!/bin/bash
echo "SETTING ENVIRONMENT VARIABLES ..."
echo "---------------------------------"

# Define variables

# PROJECT NAME
export PROJECT_NAME=sample-project
echo "- PROJECT_NAME: " $PROJECT_NAME

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
export PROJECT_HOME=$PROJECT_SHELL_SCRIPTS_FOLDER/..