#!/bin/bash
cd /home/hop
aws s3 cp ${S3_URI} .
unzip ${HOP_PROJECT_ZIP_FILE_NAME}