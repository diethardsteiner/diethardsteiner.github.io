---
typora-root-url: ..
typora-copy-images-to: ../images
layout: post
title: "Converting PDI Repositories to PDI Standalone Files"
summary: This article explains how to convert PDI repos to PDI standalone files
date: 2020-03-02
categories: [PDI]
tags: 
published: true
---


This article explains how to convert PDI repositories (PDI managed files) to PDI standalone files (PDI un-managed files). This might not be a complete set of instructions but most common conversion steps should be covered.

> **Note**: The term **repository** does not refer to a Git repo here but a PDI repo.


Historically PDI has been able to store jobs and transformations in 4 different ways, so of which are deprecated now. 

PDI File **storage types**:


| Type	| PDI Managed?	| Supported?	| Comment
|--------	|:---:	|:---:	|---
| Standalone Files	| No	| Yes 	| 
| File-based Repository	| Yes	| No	| Legacy
| DB-based Repository	| Yes	| No	| Legacy
| Jackrabbit Repository	| Yes	| Yes	| aka EE repo, Pentaho Server Repo

It is **best practice** to store all PDI jobs and transformations as **PDI Standalone Files** in a **GIT repository**. Then you can either deploy your pipeline as PDI Standalone Files (to run via the PDI Client tool on a remote server) or upload it to a Pentaho Server (where they will be ultimately stored in a Jackrabbit repository).

# Convert PDI EE Repo or PDI FileBasedRepo to Files

This guide explains how to convert a **Jackrabbit repo** or a **PDI file-based repo** to standard independent OS level files (aka **PDI Standalone Files**).

## Converting the repo

Depending on whether your **jobs** and **transformations** are stored in a **Jackrabbit repo** or in a file-based repo, the following steps differ:

### Export Jackrabbit Repo

If your **jobs** and **transformations** are stored on the Pentaho Server (perviously also know as EE repo) apply the following approach:

> It is not recommended to export the jobs and transformations via Spoon Use (Tools > Repository > Export Repository) since all the jobs and transformations are saved to one and only one XML file.

#### Via Pentaho User Console (Web-Interface)

You can just export **jobs** and **transformations** via the **Pentaho User Console** (Pentaho Server web interface): The web-interface offers a **Browse** perspective. Right click on the relevant folder there and choose **Download** from the **File Actions**. All your jobs and transformations will be provided as a ZIP file.


#### Via Command Line

Alternatively, files can be exported via a command line utility:

Log on to the node where your **Pentaho Server** is running on and run a similar command to the one shown below to export the jobs and transformations. The most important setting is the repository path to the folder that you want to export (which is defined via the --path flag):

```bash
./import-export.sh \
 --export \
 --url=http://localhost:8080/pentaho \
 --username=admin \
 --password=password \
 --path=/public/my-pdi-project-a \
 --file-path=/tmp/pentaho-server-export.zip \
 --charset=UTF-8 \
 --withManifest=true \
 --logfile=/tmp/pentaho-server-export.log
```

The export tool will automatically write the latest connection details from the global config into the jobs and transformations on export (so this is different from a file-based PDI repo, where we have to adjust them manually by opening each job and transformation in Spoon before exporting).

All the files will be written into a zip file.

### PDI File-based Repo

There is no export process required for the file-based repo since all the artefacts (jobs, transformations etc) are already stored as standard OS level files, however, we have to make changes to the content of the files so that they can be used independently (outside a PDI file-based repo).

In a PDI file-based repo, some **configuration details** like DB connection credentials get stored within a transformation and job as well as in "global" files (e.g. there is a `*.kdb` file for each database connection). These "global" files will always reflect the latest configuration (and are also used when the job/transformation gets executed via the PDI repository), however, these latest config details are only reflected in the jobs and transformations if they got opened in Spoon and then subsequently saved after the connection details were adjusted in the "global" file. In a nutshell: They could be out of sync. Since we want to move to a files only setup, we have to make sure that the connection details stored in the jobs and transformations reflect the latest position. Hence, it is strongly recommended that you open all the jobs and transformations in Spoon and save them again in case Spoon marks the filename in the tab as bold (indicating a change - so the config details were out-of-sync).


## Job and Transformation File Adjustments

### Replace legacy PDI Variables

Use a good text editor (like Sublime Text) to replace any occurrence of the variables mentioned below:

Replace these variables:

```xml
Internal.Job.Repository.Directory
Internal.Transformation.Repository.Directory
```

with:

```xml
Internal.Entry.Current.Directory
```

In Sublime Text use **Find > Find in Files** to perform this operation in batch. Sublime will open all the files that it changed. Save all of them at once via **Files > Save All** and then close all of them via **File > Close All Files**.

`Internal.Entry.Current.Directory` was introduced in PDI version 7 to resolve the problem around having specific variables per PDI storage type (managed and unmanaged PDI files). By using `Internal.Entry.Current.Directory`` jobs and transformations work in any of these PDI storage types.

### Remove Repository Directory Path

Each job or transformation has the **directory path** to where this very job or transformation is located within the repository stored within the XML file.

**Transformation**:

Example of how the XML looks: We want to replace this ...

From:


```xml
<trans_type>Normal</trans_type>
<trans_status>0</trans_status>
<directory>/public/common</directory>
```

To:

```xml
<trans_type>Normal</trans_type>
<trans_status>0</trans_status>
<directory>/</directory>
```

**Job**:

From:


```xml
<job_status>0</job_status>
<directory>/public/common</directory>
```

To:

```xml
<job_status>0</job_status>
<directory>/</directory>
```

Of course we don't want to do this manually, hence we can use a powerful text editor to do the bulk of the work.

In Sublime, go to **Find > Find in Files**. Enable **Regular Expression** in the search bar.

First let's replace the references for the **transformations**:

| Option	| Value
|----	|-----
| Find:	| `<trans_type>Normal<\/trans_type>\s+<trans_status>0<\/trans_status>\s+<directory>(.+)<\/directory>`
| Where:	| Path to root of project git repo
| Replace:	| `<trans_type>Normal</trans_type><trans_status>0</trans_status><directory></directory>`

Then click the **Replace** button.

Next let's replace the references for the **jobs**:

| Option	| Value
| ----------	| ---------
| Find: 	| `<job_status>0<\/job_status>\s+<directory>(.+)<\/directory>`
| Where: 	| Path to root of project git repo
| Replace: 	| `<job_status>0</job_status><directory></directory>`


### Replace repository file references with file system references

#### References in Job and Transformation job entries

References to jobs and transformations have to be adjusted.

**Example**: Switching from repo type definition ...

```xml
<specification_method>rep_name</specification_method>
<trans_object_id />
<filename />
<transname>file_list_and_validation</transname>
<directory>${Internal.Entry.Current.Directory}</directory>
```

... to file system type definition for a transformation:

```xml
<specification_method>filename</specification_method>
<trans_object_id />
<filename>${Internal.Entry.Current.Directory}/file_list_and_validation.ktr</filename>
<transname></transname>
```

Of course we don't want to do this manually, hence we can use a powerful text editor to do the bulk of the work.

In Sublime, go to **Find > Find in Files**. Enable **Regular Expression** in the search bar.

First let's replace the references for the **transformations**:

| Option	| Value
| ----------	| ---------
| Find: 	| `<specification_method>rep_name<\/specification_method>[\s]+<trans_object_id\s?\/>[\s]+<filename\s?\/>[\s]+<transname>(.+)<\/transname>[\s]+<directory>(.+)</directory>`
| Where: 	| Path to root of project git repo
| Replace: 	| `<specification_method>filename</specification_method><trans_object_id/><filename>$2/$1.ktr</filename><transname/>`

Then click the **Replace** button.

Next let's replace the references for the **jobs**:

| Option	| Value
| ----------	| ---------
| Find: 	| `<specification_method>rep_name<\/specification_method>[\s]+<job_object_id\s?\/>[\s]+<filename\s?\/>[\s]+<jobname>(.+)<\/jobname>[\s]+<directory>(.+)</directory>`
| Where: 	| Path to root of project git repo
| Replace: 	| `<specification_method>filename</specification_method><job_object_id/><filename>$2/$1.kjb</filename><jobname/>`

Then click the **Replace** button.


#### Job and Transformation Executor and Simple Mapping (sub-transformation)

For referenced transformations the structure looks like this:

```xml
<specification_method>rep_name</specification_method>
<trans_object_id/>
<trans_name>tr_find_files_matching_regex</trans_name>
<filename/>
<directory_path>/home/pentaho/project-a/common/hadoop_xls_loader</directory_path>
```

In **Sublime Text**, go to **Find > Find in Files**. Enable **Regular Expression** in the search bar.

First let's replace the references for the **transformations**:

| Option	| Value
| ----------	| ---------
| Find:	|  `<specification_method>rep_name<\/specification_method>[\s]+<trans_object_id\s?\/>[\s]+<trans_name>(.+)<\/trans_name>[\s]+<filename\s?\/>[\s]+<directory_path>(.+)</directory_path>`
| Where: 	| Path to root of project git repo
| Replace:	|  `<specification_method>filename</specification_method><trans_object_id/><filename>$2/$1.ktr</filename><trans_name/><directory_path/>`

Then click the **Replace** button.

For **jobs** the structure looks like this:


```xml
<specification_method>rep_name</specification_method>
<job_object_id/>
<job_name>jb_metadata_inject_excel_in_hadoop_out</job_name>
<filename/>
<directory_path>/home/pentaho/project-a/common/hadoop_xls_loader</directory_path>
```

Next let's replace the references for the **jobs**:

| Option	| Value
| ----------	| ---------
| Find: 	| `<specification_method>rep_name<\/specification_method>[\s]+<job_object_id\s?\/>[\s]+<job_name>(.+)<\/job_name>[\s]+<filename\s?\/>[\s]+<directory_path>(.+)</directory_path>`
| Where: 	| Path to root of project git repo
| Replace:	|  `<specification_method>filename</specification_method><job_object_id/><filename>$2/$1.kjb</filename><job_name/><directory_path/>`

Then click the **Replace** button.


#### Pentaho MapReduce Step

Change **Reducer** entry from (Example):

```xml
<map_trans_repo_dir>${VAR_PMR_WRAPPER_MAP_TR_DIR}</map_trans_repo_dir>
<map_trans_repo_file>${VAR_PMR_WRAPPER_MAP_TR_FILE}</map_trans_repo_file>
<map_trans_repo_reference/>
<map_trans></map_trans>
```

to:

```xml
<map_trans_repo_dir></map_trans_repo_dir>
<map_trans_repo_file></map_trans_repo_file>
<map_trans_repo_reference/>
<map_trans>${VAR_PMR_WRAPPER_MAP_TR_DIR}/${VAR_PMR_WRAPPER_MAP_TR_FILE}.ktr</map_trans>
```

Change **mapper** entry from (example):

```xml
<reduce_trans_repo_dir>${VAR_PMR_WRAPPER_REDUCE_TR_DIR}</reduce_trans_repo_dir>
<reduce_trans_repo_file>${VAR_PMR_WRAPPER_REDUCE_TR_FILE}</reduce_trans_repo_file>
<reduce_trans_repo_reference/>
<reduce_trans></reduce_trans>
```

to:

```xml
<reduce_trans_repo_dir></reduce_trans_repo_dir>
<reduce_trans_repo_file></reduce_trans_repo_file>
<reduce_trans_repo_reference/>
<reduce_trans>${VAR_PMR_WRAPPER_REDUCE_TR_DIR}/${VAR_PMR_WRAPPER_REDUCE_TR_FILE}.ktr</reduce_trans>
```

**Pentaho MapReduce** steps were hardly ever used, hence please adjusted manually when you encounter them (just open the kjb or ktr file in Sublime and switch on XML syntax highlighting).

To help the process a bit, just run a search in the git repo root for files that are using a **Pentaho MapReduce** step, e.g.:


```
grep -r "<map_trans_repo_dir>"
```

Open these files then and manually adjust them.

### Adjust absolute paths

Your approach here might vary a bit. This step has to be dealt with on a project by project level.

Say your job resided in the directory shown below when it was part of the EE/Jackrabbit Repo:

```
/home/pentaho/project-a/common/di_process_execution_log
```

However, since we are using the file system now and store our code in a dedicated directory, e.g.:

```
/path/to/root/dir/projecta-code/etl
```

... we also need the absolute path to this directory:

```
/path/to/root/dir/projecta-code/etl/home/pentaho/project-a/common/di_process_execution_log
```

We don't want to hard-code this. Ideally `Internal.Entry.Current.Directory` should have been used, however, that would mean we'd have to adjust the jobs and transformations manually. It's easier if we represent:

```
/path/to/root/dir/projecta-code
```


with a dedicated variable e.g. the whole path could be made up of this:


```
${PARAM_PROJECT_CODE_PDI_DIR}/home/pentaho/project-a/common/di_process_execution_log
```

Again here you have to decide by yourself what is the most sensible solution. I will just mention here the adjustments I made via Sublime Text based on the above example:

In **Sublime Text**, go to **Find > Find in Files**. Enable **Regular Expression** in the search bar.

First let's replace the references for the **transformations**:

> Make sure you do not replace paths that use `Internal.Entry.Current.Directory`!

| Option	| Value
| ----------	| ---------
| Find: 	| `<filename>(/home/.+)<\/filename>`
| Where: 	| Path to root of project git repo
| Replace: 	| `<filename>\$\{PARAM_PROJECT_CODE_PDI_DIR\}/$1<\/filename>`

**Important**: Historically within PDI there have been some inconsistencies as to how the path is stored. In same cases the forward slashes are escaped for the whole or part of the path, e.g.:

```xml
<filename>&#x2f;home&#x2f;pentaho&#x2f;project-a&#x2f;common&#x2f;set_variables/tr_set_properties_if_not_already_set.ktr</filename><transname/>
```

So we'd also have to run another replace like this (based on the example above):

| Option	| Value
| ----------	| ---------
| Find	| `<filename>(&#x2f;home&#x2f;.+)<\/filename>`
| Where	| Path to root of project git repo
| Replace	| `<filename>\$\{PARAM_PROJECT_CODE_PDI_DIR\}/$1<\/filename>`

#### Parameter Values

Next we also have to check if repository paths are also passed as values for **Parameters**, e.g.:


```xml
<parameter>
  <name>PARAM_UNIT_OF_WORK_DIRECTORY</name>
  <stream_name/>
  <value>/home/pentaho/project-a/action_history</value>
</parameter>
```

In this case apply a replace pattern similar to this one:

| Option	| Value
| ----------	| ---------
| Find: 	| `<value>(/home/pentaho/.+)<\/value>`
| Where: 	| Path to root of project git repo
| Replace: 	| `<value>\$\{PARAM_PROJECT_CODE_PDI_DIR\}/$1<\/value>`

#### Pentaho MapReduce steps

Apply a replace pattern similar to this one (adjust for your project):

| Option	| Value
| ----------	| ---------
| Find: 	| `<map_trans>(/home/pentaho/.+)<\/map_trans>`
| Where: 	| Path to root of project git repo
| Replace: 	| `<map_trans>\$\{PARAM_PROJECT_CODE_PDI_DIR\}/$1<\/map_trans>`

and:

| Option	| Value
| ----------	| ---------
| Find: 	| `<reduce_trans>(/home/pentaho/.+)<\/reduce_trans>`
| Where: 	| Path to root of project git repo
| Replace: 	| `<reduce_trans>\$\{PARAM_PROJECT_CODE_PDI_DIR\}/$1<\/reduce_trans>`


## Remove entry from repositories.xml

This step is not mandatory but it should be applied for best practise:

Remove the reference to the **Jackrabbit repo** (Example):


```xml
<repository>
   <id>PentahoEnterpriseRepository</id>
   <name>pentaho-di</name>
   <description></description>
   <repository_location_url>http&#x3a;&#x2f;&#x2f;xx.xxx.xxx.xx&#x3a;9080&#x2f;pentaho-di</repository_location_url>
   <version_comment_mandatory>N</version_comment_mandatory>
</repository>
```

or File based repo (Example):

```xml
<repository>
    <id>KettleFileRepository</id>
    <name>pentaho-di</name>
    <description> </description>
    <base_directory>/home/user-a/project-a/repo</base_directory>
    <read_only>N</read_only>
    <hides_hidden_files>N</hides_hidden_files>
</repository>
```

## Adjust kitchen command

Previously PDI was instructed to connect to a PDI repo either via using these **environment variables**:


```
KETTLE_REPOSITORY
KETTLE_USER
KETTLE_PASSWORD
```

or by adding flags to the kitchen command:

```
-rep=<value> -user=<value> -password=<value> -job=<value>
```

Remove these flags or the environment variables (whatever applies to your setup).

Also remove the `-dir` and `-job` flags and add the `-file` flag specifying the path to the job that has to be executed.

> **Important**: Job name has to have the file extension of `.kjb`!

```
-file=/path/to/job.kjb
```

## Integration/Regression Test

It is strongly recommended that after the conversion the code goes through the standard testing procedures, especially a full integration test to make sure that everything is working as expected.


# Convert PDI Jackrabbit Repo to PDI File-Based Repo

This approach is only mentioned for completeness sake. Since PDI file based repos are not supported any more, it is not recommended you go down this route.

This is a very simple and straight forward conversion:

> **Important**: Export one project at a time and not everything at once!

Export the repo:

1. Connect with Spoon to the EE repo on the Pentaho Server.

2. Click on the **Explore Repository** icon.

3. In the **Repository Explorer** window, right click on the project folder on the left hand side and choose **Export**.

4. Save the backup XML file to a convenient location.

Creating a file-based repo and importing the backup XML file:

1. Disconnect from the current repo (via repo switcher in top right hand corner).

2. Next click on the **Connect** button in the top right hand corner and then on **Repository Manager**. Click **Add** next. At the bottom of the screen you'll see a small link called **Other repositories** - click on it. On the next screen choose **File Repository**. Finally click on **Get Started**. On the next screen provide the display name and the folder you want the PDI file-based repo to be saved to. Then click on **Finish** followed by **Connect Now**.

3. Our file-based repo is set up now. Next we want to import the jobs and transformations (and any other related artefacts). Click on **Tools > Repository > Import Repository**. Pick the backup XML file you exported previously and provide the required info to finish the import process. Check the import log that everything was imported successfully. Finally open the **Repository Explorer** to visually double check that everything looks fine (the import went fine).

The export tool will automatically write the latest connection details from the global config into the jobs and transformations on export.

Next steps:

1. Adjust the `repository.xml` so that the entry for this project reflects its new file-based repo setup. It should look something like this:

  ```xml
  <repository>
  <id>KettleFileRepository</id>
  <name>pentaho-di</name>
  <description> </description>
  <base_directory>/home/user-a/project-a/repo</base_directory>
  <read_only>N</read_only>
  <hides_hidden_files>N</hides_hidden_files>
  </repository>
  ```

2. There might be minor adjustments required to the way you start the job via kitchen. Please double check.
3. It is recommended that you make DB connections available on the project level. After import from the backup file connection are stored within each file. In case you create a new job or transformation, it would be nice if you could refer to the same connections. This can be achieved by sharing connections: Open an existing job or transformation that has the DB connection defined. From the left hand side **View** side panel expand the **Database Connections** and right click on each of them and choose **Share**. This will create a shared.xml file in the `.kettle` folder (`KETTLE_HOME`).
