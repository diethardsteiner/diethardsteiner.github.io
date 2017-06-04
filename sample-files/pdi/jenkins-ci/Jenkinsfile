#!/usr/bin/env groovy
pipeline {

  agent none

  stages {
    stage('Build') {
      agent any
      }
      steps {
        echo 'Building..'
      }
    }
    stage('Test') {
      agent {
        docker {
          image 'maven:3-alpine'
          label 'my-defined-label'
          args  '-v /tmp:/tmp'
        }
      }
      steps {
        echo 'Testing..'
      }
    }
    stage('Deploy') {
      agent any
      // Accessing the currentBuild.result variable allows the Pipeline to determine 
      // if there were any test failures. In which case, the value would be UNSTABLE.
      when {
        expression {
          currentBuild.result == null || currentBuild.result == 'SUCCESS' 
        }
      }
      steps {
          sh 'make publish'
      }
    }
  }
}

