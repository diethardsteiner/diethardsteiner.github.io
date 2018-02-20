---
layout: post
title: "Kubernetes: Manual and Automatic Volume Provisioning"
summary: This article explains how to easily deploy Webspoon with Kubernetes on Google Cloud Platform
date: 2018-02-11
categories: PDI
tags: PDI, Kubernetes, Google Cloud Platform
published: false
---

Pentaho Data Integration: Deploying Webspoon with Kubernetes on Google Cloud Platform (Part 2)

The the second part of our **Deploying Webspoon with Kubernetes on Google Cloud Platform** series we will be taking a look at how to attach a persistent volume to the instance that runs WebSpoon.

I expect that you followed the first article of this series, so I will not create a new project:

```bash
# get current configuration details
gcloud info
```

If you see at the end of the output that the project is still set to `kubwebspoon` then you are all ready to go:

```
...
Current Properties:
  [core]
    project: [kubwebspoon]
    ...
```

If not, set the required configuration details (again this is based on the assumption that you followed the exercise in part 1 of this series):

```bash
# set project id
gcloud config set project kubwebspoon
# set compute zone
# list of available zones
# https://cloud.google.com/compute/docs/regions-zones/#available
gcloud config set compute/zone us-west1-a
```

Next let's create the **cluster**:

```bash
# create kubernetes engine cluster
# running command again after enabling API
gcloud container clusters create webspoon-cluster \
  --cluster-version=1.9.2-gke.1 \
  --machine-type=n1-standard-2 \
  --num-nodes=3
# get authentication credentials to interact with cluster
gcloud container clusters get-credentials webspoon-cluster
```

> **Important**: Pick the correct `machine-type` to support the workload we are expecting!

You can find info on the Google Cloud **machine types** [here](https://cloud.google.com/compute/docs/machine-types) or alternatively you can run this command:

```bash
$ gcloud compute machine-types list
```

If you want to find out more about the cluster create command, run this:

```bash
$ gcloud help container clusters create
```

# Introduction

To understand the various layers better, we'll first take a high level look at how everything hangs together:



| Layers                 | Manual Creation  (non scalable approach)                  | Automatic Creation                             |
| -----------------------| :-------------------------------------------------------: | :--------------------------------------------: |
| Disk request           |                                                           |                                                |
| Storage Class          |                                                           |                                                |
| Persistent Volume      |                                                           |                                                |
| Persistent Volume Claim|                                                           |                                                |
| Controller             | Pod with Volume referencing Persistent Volume Claim [^f1] | StatefulSet with Volume Claim Templates  [^f2] |

 
[^f1]: Using   `volumes.persistentVolumeClaim.ClaimName`
[^f2]: Using `volumeClaimTemplates`

> **Important**: As you can imagine the manual approach is not meant for massivly scaling applications.


Let's understand what each layer does:

- **Storage Class**: Is a high level abstraction of the storage specifications.
- **Persistent Volume** (PV): Allocates a given physical volume to the Kubernetes cluster. Can be linked to a Storage Class.
- **Persistent Volume Claim** (PVC): Create a contract between a pod and a persistent volume
- **Pod**: Runs one or more containers. To scale horizontally usually there are more than one pod. One pod requrest one PVC to access persistent storage.
- **Deployment**:
- **StatefulSet**: 

# Automatic Storage Allocation

We will start with the easiest option: Making use of Kubernetes **automatic storage allocation** features.


## Option 1: Pod with PSV

Layer  | Manual Creation | Automatic Creation
-------|---------------------|-----------------------
Storage Class |  X  |  
Disk   |     |  X
PV  |     |  X
PSV   |   X   |  
Pod |  X |


Create a PVC called `webspoon-persistent-volume-claim-using-default-storage-class.yaml` with following content:

```yaml
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  # the name you assign to the claim
  name: webspoon-persistent-volume-claim
spec:
  accessModes:
    # - ReadWriteOnce
    - ReadOnlyMany
  # volumeMode: Filesystem
  resources:
    requests:
      # amount of storage in gigabytes to request for the cluster
      storage: 2Gi
```

Then run the following command:

```bash
kubectl apply -f webspoon-persistent-volume-claim-using-default-storage-class.yaml
```

Next create the pod definition:

```yaml
kind: Pod
apiVersion: v1
metadata:
  name: mywebspoon
spec:
  volumes:
  - name: 'webspoon-storage'
    persistentVolumeClaim:
      claimName: 'webspoon-persistent-volume-claim'
  containers:
  - name: 'webspoon'
    image: 'hiromuhota/webspoon:0.8.0.13-full'
    env:
    - name: 'JAVA_OPTS'
      value: '-Xms1024m -Xmx1920m'
    ports:
    - containerPort: 8080
      protocol: 'TCP'
    resources:
      requests:
        cpu: '1'
        memory: '2Gi'
    volumeMounts:
      # has to match the volume name defined further up
    - name: 'webspoon-storage'
      # mount path within the container
      mountPath: '/data'
```

Then run the following command:

```bash
kubectl apply -f webspoon-pod.yaml
```

Just to satisfy our curiosity, we retrieve some info on **Storage Classes**, **Persistent Volumes**, **Persistent Volume Claims** and **Pods**:

```bash
$ kubectl get storageclass
NAME                 PROVISIONER            AGE
standard (default)   kubernetes.io/gce-pd   8m
$ kubectl get pv
NAME                                       CAPACITY   ACCESS MODES   RECLAIM POLICY   STATUS    CLAIM                                      STORAGECLASS   REASON    AGE
pvc-852bb2c8-1672-11e8-b97b-42010a8a0015   2Gi        ROX            Delete           Bound     default/webspoon-persistent-volume-claim   standard                 6m
$ kubectl get pvc
NAME                               STATUS    VOLUME                                     CAPACITY   ACCESS MODES   STORAGECLASS   AGE
webspoon-persistent-volume-claim   Bound     pvc-852bb2c8-1672-11e8-b97b-42010a8a0015   2Gi        ROX            standard       6m
$ kubectl get pods
NAME             READY     STATUS              RESTARTS   AGE
mywebspoon       1/1       Running             0          13m
```

> **Note**: It might take some time until the container within the pod is ready. Initially `READY` will be shown as `0/1`, meaning that 0 out of 1 containers are ready. Give it a few minutes until the status changes.

As you can see, the PV got automatically created for us since we use the default `standard` `StorageClassName` provided by the **Kubernetes** cluster. Note that we don't even mention this `StorageClassName` in our PVC definition - it is added by Kubernetes automatically.

Ok, so now we have one pod running with one volume attached!

### Understanding the Relationship between Pods, PVCs and PVs

Next we want to **horizontally scale** the setup!

The approach we take now is not what you normally would do - it's just for learning one of the core concepts: Copy the pod specification that you created previously (call it `webspoon-pod-no2.yaml`) and adjust it slightly as shown below:

```yaml
kind: Pod
apiVersion: v1
metadata:
  name: mywebspoon-no2
spec:
  volumes:
  - name: 'webspoon-storage'
    persistentVolumeClaim:
      claimName: 'webspoon-persistent-volume-claim'
  containers:
  - name: 'webspoon-no2'
    image: 'hiromuhota/webspoon:0.8.0.13-full'
    env:
    - name: 'JAVA_OPTS'
      value: '-Xms1024m -Xmx1920m'
    ports:
    - containerPort: 8080
      protocol: 'TCP'
    resources:
      requests:
        cpu: '1'
        memory: '2Gi'
    volumeMounts:
      # has to match the volume name defined further up
    - name: 'webspoon-storage'
      # mount path within the container
      mountPath: '/data'
```

Create the **pod** now:

```bash
kubectl apply -f webspoon-pod-no2.yaml
```

You will realise that even after several minutes the container in this new pod is not becoming available:

```
$ kubectl get pods
NAME             READY     STATUS              RESTARTS   AGE
mywebspoon       1/1       Running             0          22m
mywebspoon-no2   0/1       ContainerCreating   0          10m
```

It's time now to understand why:

In the **Web Console**, when you inspect the pod's events, you might see a message like this one:

```
FailedMount:
AttachVolume.Attach failed for volume "pvc-852bb2c8-1672-11e8-b97b-42010a8a0015" : googleapi: Error 400: The disk resource 'projects/kubwebspoon/zones/us-west1-a/disks/gke-webspoon-cluster-3-pvc-852bb2c8-1672-11e8-b97b-42010a8a0015' is already being used by 'projects/kubwebspoon/zones/us-west1-a/instances/gke-webspoon-cluster-default-pool-67e6373e-0lqg'
```

**It is important to understand the relationships**:

- A given **pod** has to be linked to a unique **PVC**
- A given **PVC** has to linked to unique **PV**
- A given **PV** has to be linked to a unique **disk** (?? is the last one true ??? surely you can have shared network drives? So the storage type must have something to do with this as well!) [OPEN]


Since we explicitly mention the PVC in the spec of the two pods, we get the error shown above! Had we created two PVCs, we wouln't be in this trouble now.

> **Important**: This shows that the same PVC cannot be reused! This is important to understand. When we create a bit later on a `StatefulSet` - an easier approach to scale stateful apps horizontally - we cannot just reference the volume via claim but have to create a volume claim template, which will make sure that each pod gets a unique PVC and a unique PV!

With this learnings we can proceed and create a `StatefulSet` in a bit to scale horizontally.

At this point you might also wonder what storage type we are actually using? Let's inspect the `StorageClass`:

```
$ kubectl describe storageclass standard
Name:            standard
IsDefaultClass:  Yes
Annotations:     storageclass.beta.kubernetes.io/is-default-class=true
Provisioner:     kubernetes.io/gce-pd
Parameters:      type=pd-standard
ReclaimPolicy:   Delete
Events:          <none>
```

Pay attention to `Provisioner`: It tells us that the standard storage class is set up to use the **Google Cloud Engine Persistent Storage**. What kind of storage is this?

### Scaling horizontally with a StatefulSet

Layer  | Manual Creation | Automatic Creation
-------|---------------------|-----------------------
Storage Class |  X  |  
Disk   |     |  X
PV  |     |  X
PSV   |      | X 
StatefulSet with volumeClaimTemplates |  X |



Sources: 

- [Google Cloud Platform: StatefulSet](https://cloud.google.com/kubernetes-engine/docs/concepts/statefulset)
- [Deploying a Stateful Application](https://cloud.google.com/kubernetes-engine/docs/how-to/stateful-apps)

The `StatefulSet` controller is used to manage **stateful applications**. Unlike with the `Deployment` controller that we used in part 1 of this blog series, the `StatefulSet` controller maintains a **sticky identity** for each pod, so these pods are not interchangeable (although the have the same spec).

We will use the declaritive approach to configure the `StatefulSet`:

You will be happy to hear that this is a very easy approach: using **dynamic provisioning**. In this case, no volumes have to be created upfront by the administrator, the user does neither have to create a **persistent volume** definition nor a **persistent volume claim** definition.

The only definitions we still need is the `service` as well as the `statefulSet` defintion. The one configuration detail that makes this is all possible is `volumeClaimTemplates`, which has to be added to the `statefulSet` definition:

We have to create a **service** (e.g. Load Balancer) to expose the `statefulSet`.

We will also create a **Load Balancer**: Create a file called `webspoon-service.yaml` with following content:

Create a new file called `webspoon-stateful-set-with-volume-claim-templates.yaml` with following content:

```yaml
apiVersion: 'apps/v1beta1'
kind: 'StatefulSet'
metadata:
  name: 'webspoon-stateful-set'
spec:
  selector:
      matchLabels:
        # has to match .spec.template.metadata.labels
        app: 'webspoon-server'
  # reference back to the service name we created earlier on
  serviceName: 'webspoon-service'
  replicas: 2
  template:
    metadata:
      labels:
        # has to match .spec.selector.matchLabels
        app: 'webspoon-server' 
        version: 'v1'
        zone: 'dev'
    spec:
      terminationGracePeriodSeconds: 30
      # volumes:
      # - name: 'webspoon-storage'
      #   persistentVolumeClaim:
      #     claimName: 'webspoon-persistent-volume-claim'
      containers:
      - name: 'webspoon'
        image: 'hiromuhota/webspoon:0.8.0.13-full'
        env:
        - name: 'JAVA_OPTS'
          value: '-Xms1024m -Xmx1920m'
        ports:
        - containerPort: 8080
          protocol: 'TCP'
        resources:
          requests:
            cpu: '1'
            memory: '2Gi'
        volumeMounts:
          # has to match the volume name defined further up
        # - name: 'webspoon-storage'
        - name: 'webspoon-persistent-volume-claim'
          # mount path within the container
          mountPath: '/data'   
  volumeClaimTemplates:
  - metadata:
      name: 'webspoon-persistent-volume-claim'
    spec:
      accessModes: 
        - 'ReadOnlyMany'
      storageClassName: webspoon-storage-class
      resources:
        requests:
          storage: '2Gi'
```

The important bit here is that we use a `volumeClaimTemplates` as opposed to a volume with a claim. The `volumeClaimTemplates` will make sure that **for each pod** a **unique PVC** is automatically generated. And in turn each unique PVC requests a unique PV which in turn requests a unique disk.

> **Note**:  When you use `volumeClaimTemplates` you don't have to use `PersistentVolumeClaim` ... and the `PersitentVolumes` for this matter. Everything gets automatically provisioned.

Run the following command the create the **stateful set**:

```bash
kubectl apply -f webspoon-stateful-set-with-volume-claim-templates.yaml
```

We can retrieve some info to better understand what happened after requesting the `StatefulSet`:

```bash
$ kubectl get pods
NAME                      READY     STATUS    RESTARTS   AGE
webspoon-stateful-set-0   1/1       Running   0          14m
webspoon-stateful-set-1   1/1       Running   0          13m
$ kubectl get pvc
NAME                                                       STATUS    VOLUME                                     CAPACITY   ACCESS MODES   STORAGECLASS             AGE
webspoon-persistent-volume-claim-webspoon-stateful-set-0   Bound     pvc-2620d7e0-1677-11e8-b97b-42010a8a0015   2Gi        ROX            webspoon-storage-class   21m
webspoon-persistent-volume-claim-webspoon-stateful-set-1   Bound     pvc-64cb1dda-1678-11e8-b97b-42010a8a0015   2Gi        ROX            webspoon-storage-class   12m
$ kubectl get pv
NAME                                       CAPACITY   ACCESS MODES   RECLAIM POLICY   STATUS    CLAIM                                                              STORAGECLASS             REASON    AGE
pvc-2620d7e0-1677-11e8-b97b-42010a8a0015   2Gi        ROX            Delete           Bound     default/webspoon-persistent-volume-claim-webspoon-stateful-set-0   webspoon-storage-class             14m
pvc-64cb1dda-1678-11e8-b97b-42010a8a0015   2Gi        ROX            Delete           Bound     default/webspoon-persistent-volume-claim-webspoon-stateful-set-1   webspoon-storage-class             12m
```

You can also retrieve similar info from the **web console**. The status of the **Stateful Set**:

![](/images/webspoon-kubernetes/kubernetes-stateful-set.png)

The status of the **Pods**: Note how the stateful set name is used as the base name of the pod and an ID is added.

![](/images/webspoon-kubernetes/kubernetes-stateful-set-pods.png)

The status of the **PVCs**: Note how the name of the `volumeClaimTemplates` is used as the base name of the PVC and an ID is added as a suffix to make it unique:

![](/images/webspoon-kubernetes/kubernetes-stateful-set-pvcs.png)


> **Note**: I recommend checking the generated definition of the **pods**. You will notice that the `VolumeClaimTemplate` got replaced with a `volumes` section and the `claimName` points to a very specific/unique auto-genreated claim, e.g.:

```yaml
  volumes:
  - name: webspoon-persistent-volume-claim
    persistentVolumeClaim:
      claimName: webspoon-persistent-volume-claim-webspoon-stateful-set-0
```

### Expose 

[OPEN] Why?

```yaml
apiVersion: v1
kind: Service
metadata:
  name: webspoon-service
  labels:
    # must be some of the labels used for 
    # spec.template.metadata.labels in the StatefulSet
    app: webspoon-server
    zone: dev
spec:
  externalTrafficPolicy: Cluster
  ports:
  - nodePort: 30702
    port: 8080
    protocol: TCP
    targetPort: 8080
  selector:
    app: webspoon-server
  sessionAffinity: None
  type: LoadBalancer
```

Apply the config:

```bash
kubectl apply -f webspoon-service.yaml
```


> **Note**: The Kubernetes API is fast evolving and there might be changes in future to it. In case this happens, there is the command `kubectl convert -f webspoon-stateful-set.yaml` to "upgrade" the spec.


[OPEN] Show you can access the Web UI.

# Manual Storage Allocation

> **Important**: This approach is not scalable. We cover this here for background info only.

## Create the volume

[OPEN]: Only got the pod working by skipping this section!

You create a persistent volume like so:

```bash
gcloud compute disks create [DISK_NAME] \
  --image-family [IMAGEFAMILY] \
  --image-project [IMAGEPROJECT] \
  --type [TYPE] \
  --size [SIZE]
```

To find out more about this command, run:

```bash
gcloud help compute disks create
```

There are two disk types available: `pd-ssd` or `pd-standard`. The last one is sufficient for our purpose: storing PDI jobs and transformations. The volume has to be specified as multiples of `GB` or `TB` - it can't be less than 10 GB. Let's run following command now:

```
# create persistent volume
gcloud compute disks create webspoon-volume \
  --image-family centos-7 \
  --image-project=centos-cloud \
  --type pd-standard \
  --size 10GB
```

## Create a Storage Class

Strictly speaking, for the manual approach, you do not have to create a `StorageClass`, however, it will make the process a little bit easier to understand/follow through as it explicitly links the PV to the PVC.

Create a new file called `webspoon-storage-class.yaml` with following content:

```yaml
kind: StorageClass
apiVersion: storage.k8s.io/v1
metadata:
  name: webspoon-storage-class
provisioner: kubernetes.io/gce-pd
parameters:
  type: pd-standard
  zone: us-west1-a
  # zones: us-central1-a, us-central1-b
```

> **Note**: Your **Kubernetes** cluster might already provide a standard storage class. If it suits your purpose, you can use this one and don't have to create an additional one.

## Assign the volume to our cluster  

The next step is the create a `PersistentVolume` resource, which allocates the **volume** to our **cluster**.

Create a new file called `webspoon-persistent-volume.yaml` with following content:

```yaml
apiVersion: v1
kind: PersistentVolume
metadata:
  # name that you assign to the persistent volume
  name: webspoon-persistent-volume
spec:
  # this storage class has to be referenced in the
  # persistent storage claim later on
  # so that pv and pvc are linked together 
  storageClassName: webspoon-storage-class
  # storage capacity in gigabytes
  capacity:
    storage: 10Gi
  accessModes:
    # - ReadWriteOnces
    - ReadOnlyMany
  gcePersistentDisk:
    # name of the persistent disk in the environment
    # has to be the name you specified before when
    # running the gcloud compute disks create command
    pdName: webspoon-volume
    # file system type of the volume
    fsType: ext4
```


[OPEN]: how can the `PersistentVolume` specify a storage class if storage classes are used for dynamic provisioning?


> **Important**: The above definition defines the `StorageClass` named `webspoon-storage-class` for the `PersistentVolume`, which will be used to bind `PersistentVolumeClaim` requests to this `PersistentVolume` later on.

Add volume to **cluster**:

```
kubectl apply -f webspoon-persistent-volume.yaml
```

We can check the status of the volume like so:

```bash
$ kubectl get pv webspoon-persistent-volume
NAME                         CAPACITY   ACCESS MODES   RECLAIM POLICY   STATUS      CLAIM     STORAGECLASS   REASON    AGE
webspoon-persistent-volume   10Gi       ROX            Retain           Available             standard                 14s
```

The volume is marked as `available` and there is currently no claim for it.

You can get pretty much the same info via the **Web UI** when you dive into **Storage** tab wihtin the **Cluster** section:

![](/images/webspoon-kubernetes/webspoon-kubernetes-pv-assigned-to-cluster.png)

> **Note**: Strictly speaking this step is not really required. You could make use of the **dynamic allocation** strategy as well. If there is no explicite volume defined for the cluster when the `PersistentVolumeClaim` is raised (see next step), then Kubernetes will try to find a volume that matches the claim.

## Claim the volume

Now that the volume is available in our cluster, this resource can be allocated via a `PersistentVolumeClaim`.

Create a new file called `webspoon-persistent-volume-claim.yaml` with following content:

```yaml
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  # the name you assign to the claim
  name: webspoon-persistent-volume-claim
spec:
  # refer back to the storage class defined in the persistent volume def
  storageClassName: webspoon-storage-class
  # if you didn't specify a storage class in the persistent volume def
  # use this:
  # storageClassName: ''
  accessModes:
    # - ReadWriteOnce
    - ReadOnlyMany
  # volumeMode: Filesystem
  resources:
    requests:
      # amount of storage in gigabytes to request for the cluster
      storage: 2Gi
```

> **Note**: If you don't specify a `storageClassName` for the PVC, Kubernetes will default it to `standard`, which in turn will trigger an automatic generation of a PV defintion. This one will then trigger the alloaction of the actual volume to the cluster. If you want to prevent this automatic behaviour, set `storageClassName` to blank (as in `storageClassName=''`) if you didn't specify a dedicated storage class in the PV definition or otherwise mentioned the same storage class in the PVC that you specified in the PV.


Apply the config:

```bash
kubectl apply -f webspoon-persistent-volume-claim.yaml
```

Verify that the `PersistentVolume` is bound to the claim:

```bash
$ kubectl get pv webspoon-persistent-volume
NAME                         CAPACITY   ACCESS MODES   RECLAIM POLICY   STATUS    CLAIM                                      STORAGECLASS   REASON    AGE
webspoon-persistent-volume   10Gi       RWO            Retain           Bound     default/webspoon-persistent-volume-claim   standard                 4m
```

**Important**: In the previous output you have the check that `STATUS` is set to `BOUND`! Otherwise a persistent volume got **dynamically provisioned** and bound to the persistent volume claim.

Or you can also find out via the command line which volume got bound to the **persistent volume claim** (pvc):

```bash
# Find out which persistent volume the claim is referencing
$ $ kubectl get pvc webspoon-persistent-volume-claim
NAME                               STATUS    VOLUME                       CAPACITY   ACCESS MODES   STORAGECLASS   AGE
webspoon-persistent-volume-claim   Bound     webspoon-persistent-volume   10Gi       RWO            standard       5m
```

Again, via the **WebUI** you can get similar info via the **Storage** tab wihtin the **Cluster** section:

![](/images/webspoon-kubernetes/webspoon-kubernetes-pv-bound-web-ui.png)

And details on the **claim** can be retrieved via the **Volume** section:

![](/images/webspoon-kubernetes/webspoon-kubernetes-pvc-bound-web-ui.png)

## Pod

Before we conquer the cluster, we first want to make sure that we get one pod running:

Create a file called `webspoon-pod.yaml` with following content:

```yaml
kind: Pod
apiVersion: v1
metadata:
  name: mywebspoon
spec:
  volumes:
  - name: 'webspoon-storage'
    persistentVolumeClaim:
      claimName: 'webspoon-persistent-volume-claim'
  containers:
  - name: 'webspoon'
    image: 'hiromuhota/webspoon:0.8.0.13-full'
    env:
    - name: 'JAVA_OPTS'
      value: '-Xms1024m -Xmx1920m'
    ports:
    - containerPort: 8080
      protocol: 'TCP'
    resources:
      requests:
        cpu: '1'
        memory: '2Gi'
    volumeMounts:
      # has to match the volume name defined further up
    - name: 'webspoon-storage'
      # mount path within the container
      mountPath: '/data'
```

Once the app is running, to log onto the container, you can run:

```bash
kubectl exec -it mywebspoon -c webspoon -- /bin/bash
```

We can check that the data directory exists:

```bash
# check that the data directory exists
root@mywebspoon:/usr/local/tomcat# ls /data
lost+found
# check that the data directory is a mount point
root@mywebspoon:/usr/local/tomcat# mount | grep data
/dev/sdb on /data type ext4 (rw,relatime,data=ordered)
```



## Log on to one Pod and check the mount usage






# Clean-up

```bash
## destroy app
# destroy service
kubectl delete service webspoon-server

kubectl delete pvc webspoon-persistent-volume-claim



# delete volume
gcloud compute disks delete webspoon-volume
# delete cluster
gcloud container clusters delete webspoon-cluster
```

> **Important**: Make sure that you check on the Web console under **Compute Engine > Disks** that there are no volumes left after all these actions. If volumes got dynamically provisioned, it can be that they will not deleted automatically. 

# Sources

- [Configure a Pod to Use a PersistentVolume for Storage](https://kubernetes.io/docs/tasks/configure-pod-container/configure-persistent-volume-storage/)