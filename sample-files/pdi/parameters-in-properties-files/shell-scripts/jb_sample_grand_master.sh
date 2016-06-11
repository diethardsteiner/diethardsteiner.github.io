#!/bin/bash
source ./set-project-variables.sh

cd $PDI_HOME
sh ./kitchen.sh -file=$PROJECT_HOME/di/jb_sample_grand_master.kjb -Level=Basic > /tmp/jb_sample_grand_master.err.log