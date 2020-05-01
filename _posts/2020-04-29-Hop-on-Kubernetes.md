---
typora-root-url: ..
typora-copy-images-to: ../images/project-hop
layout: post
title: "Project Hop: Hop on Kubernetes"
summary: A brief overview on how to deploy Hop with Kubernetes
date: 2020-04-29
categories: [Hop]
tags: 
published: true
---


This is a follow-up article to [Hop on Docker](/hop/2020/04/27/Hop-on-Docker.html). In this article we will explore how to create a simple **Kubernetes Job** for short-lived data processes and a **Kubernetes Deployment** to run an always-on server that can execute data processes on request. Our tool of choice for data processing in this case is [Hop](https://www.project-hop.org).


# Short-lived processes: Kubernetes Job

Sources:
 
- "Running pods that perform a single completable task", Kubernetes in Action.
- "Using a Git repository as a starting point for a volume", Kubernetes in Action.
- [Jobs - Run to Completion](https://kubernetes.io/docs/concepts/workloads/controllers/jobs-run-to-completion/)

Our main aim here is to only consume resources while the process is running, hence we are opting for a **Kubernetes Job**, which will start the container on an available node and remove it once the data processing is finished.

I assume your are a bit familiar with **Kubernetes**, so I will only provide a brief overview of the job definition:

```yaml
apiVersion: batch/v1
kind: Job
metadata:
  name: hop-job
spec:
  template:
    metadata:
      labels:
        app: hop-job
    spec:
      restartPolicy: OnFailure
      initContainers:
      - name: clone-git-repo
        image: alpine/git
        volumeMounts:
        - name: git-repo-volume
          mountPath: /tmp/git-repo
          readOnly: false
        command: ['sh', '-c', 
          'cd /tmp/git-repo; git clone https://github.com/diethardsteiner/project-hop-in-the-cloud.git; mv project-hop-in-the-cloud/project-a/.hop .; chmod -R 777 .hop']
      containers:
      - name: hop
        image: diethardsteiner/project-hop:0.20-20200429.230019-56
        volumeMounts:
        - name: git-repo-volume
          mountPath: /home/hop
          readOnly: false
        env:
          - name: HOP_LOG_LEVEL
            value: "Basic"
          - name: HOP_FILE_PATH
            value: "/home/hop/project-hop-in-the-cloud/project-a/pipelines-and-workflows/main.hwf"
          - name: HOP_RUN_CONFIG
            value: "classic"
          - name: HOP_RUN_PARAMETERS
            value: "PARAM_LOG_MESSAGE=Hello,PARAM_WAIT_FOR_X_MINUTES=2"
      volumes:
        - name: git-repo-volume
          emptyDir: {}
```


There isn't anything ground-breaking going on really: 

- We use an `initContainer` to clone a **git repo** to a volume that we later on mount to the main container hosting **Hop**. The **git repo** contains our project specific **Hop configuration** as well as the **Hop workflows and pipelines**.
- For the main container we source the Docker image we created in the previous article and we define a few **environment variables** that will be used to run the **Hop** data process.

The beauty about **Kubernetes** is that you can run it on various **cloud** offerings - in other words you are not locked into a specific cloud ecosystem. In my case I'll run it on **GCP**. Let's first create our **Kubernetes cluster**:

```bash
# create project
gcloud projects create k8s-project-hop
# OR if project already exist, set project id
gcloud config set project k8s-project-hop
# set compute zone
# list of available zones
# https://cloud.google.com/compute/docs/regions-zones/#available
gcloud config set compute/zone us-west1-a
# create kubernetes engine cluster
# running command again after enabling API
gcloud container clusters create project-hop-cluster \
  --machine-type=n1-standard-2 \
  --num-nodes=1
# get authentication credentials to interact with cluster
gcloud container clusters get-credentials project-hop-cluster

gcloud container clusters list
kubectl get nodes
```
 

Now that the cluster is in place, we can run our **Kubernetes job**:


```
kubectl apply -f hob-job.yaml
```


Let's get some info about the **job** and the **pod**:


```
kubectl get jobs
kubectl get po
kubectl describe job hop-job
```

Your job might finish rather quickly. Pods aren't deleted when the job finishes. Completed jobs aren't shown when running `kubectl get po`, but this can be changed by adding the `-a` flag:

```
kubectl get po -a
```

Pods aren't deleted when the job finishes so that you can still examine the lgos:

```
% kubectl get po                              (master)project-hop-in-the-cloud
NAME                  READY   STATUS      RESTARTS   AGE
hop-batch-job-7lvqx   0/1     Completed   0          118s
% kubectl logs hop-batch-job-7lvqx            (master)project-hop-in-the-cloud
Error found during execution!
picocli.CommandLine$ExecutionException: There was an error during execution of file '/home/hop/pipelines-and-workflows/main.hwf'
	at org.apache.hop.cli.HopRun.run(HopRun.java:121)
	at org.apache.hop.cli.HopRun.main(HopRun.java:642)
Caused by: picocli.CommandLine$ExecutionException: There was a problem during the initialization of the Hop environment
	at org.apache.hop.cli.HopRun.initialize(HopRun.java:150)
	at org.apache.hop.cli.HopRun.run(HopRun.java:109)
	... 1 more
Caused by: java.io.FileNotFoundException: /home/hop/.hop/hop.properties (No such file or directory)
	at java.io.FileInputStream.open0(Native Method)
	at java.io.FileInputStream.open(FileInputStream.java:195)
	at java.io.FileInputStream.<init>(FileInputStream.java:138)
	at java.io.FileInputStream.<init>(FileInputStream.java:93)
	at org.apache.hop.cli.HopRun.buildVariableSpace(HopRun.java:159)
	at org.apache.hop.cli.HopRun.initialize(HopRun.java:142)
	... 2 more
```


## Clean up

Delete the job:

```
kubectl delete job <JOBNAME> 
# or 
kubectl delete -f ./hob-job.yaml
```

When you delete the job all related pods it created are deleted too.

We keep the cluster running for the next exercise.


# Long-lived process: Kubernetes Deployment


For running the `hop-server`, we are using a **Kubernetes deployment**:

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: hop-deployment
  labels:
    app: hop
spec:
  replicas: 1
  selector:
    matchLabels:
      app: hop
  template:
    metadata:
      labels:
        app: hop
    spec:
      initContainers:
      - name: clone-git-repo
        image: alpine/git
        volumeMounts:
        - name: git-repo-volume
          mountPath: /tmp/git-repo
          readOnly: false
        command: ['sh', '-c', 
          'cd /tmp/git-repo; git clone https://github.com/diethardsteiner/project-hop-in-the-cloud.git; mv project-hop-in-the-cloud/project-a/.hop .; chmod -R 777 .hop']
      containers:
      - name: hop-server
        image: diethardsteiner/project-hop:0.20-20200429.230019-56
        volumeMounts:
        - name: git-repo-volume
          mountPath: /home/hop
          readOnly: false
        env:
          - name: HOP_LOG_LEVEL
            value: "Basic"
        resources:
          requests:
            memory: "4Gi"
            cpu: "1"
      volumes:
        - name: git-repo-volume
          emptyDir: {}
```


As it turns out, the K8s deployment isn't so much different from the K8s job, so I won't repeat the explanation here. The only main difference here is that we now request a specific amount of memory and CPU to be available.


To determine how much memory our container needs, let's have a look at home much memory our app, in this case `hop-run` requires:

```
HOP_OPTIONS="-Xmx2048m"
```


Now that we learnt that it needs 2GB, we should set the memory for our container a bit higher, e.g. 4GB.

To start our **deployment** run:

```
kubectl apply -f hop-deployment.yaml
```


And then we can get some info to understand what's going on:


```
% kubectl get deployments
NAME         READY   UP-TO-DATE   AVAILABLE   AGE
hop-server   0/1     1            0           1m01s
% kubectl get pod
NAME                          READY   STATUS    RESTARTS   AGE
hop-server-7f65f9bdd4-rjfg8   0/1     Pending   0          9m52s

# debugging in case of pod not starting up
kubectl describe pod hop-server-7f65f9bdd4-rjfg8
kubectl get events

# get the logs
% kubectl logs hop-server-7f65f9bdd4-rjfg8
# container name only required if we are running more than one container on the given pod
% kubectl logs hop-server-7f65f9bdd4-rjfg8 -c hop-server
```


If you are really super curious, you can log onto the the running container:


```
kubectl exec -it hop-server-7f65f9bdd4-rjfg8 -c hop-server -- /bin/bash
```

# Conclusion

And that's it really. With a small amount of effort we've create **short-lived** and **long-lived** hop deployments that are easily repeatable (and easy to further automate). Certainly this is only a starting point, but I hope that this article provided you at least a good idea on how this setup works and sparked your motivation to explore this interesting topic further.