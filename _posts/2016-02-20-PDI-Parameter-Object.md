---
layout: post
title:  "Pentaho Data Integration: The Parameter Object and replacing Parameter Values with Variable Values"
summary: This article looks at more complex setup where a unique approach has to be chosen to cater for various parameter and variable needs
date: 2016-02-20
categories: PDI
tags: PDI
published: true
---

# Pentaho Data Integration: The Parameter Object

Imagine we want to generate a generic wrapper process for our **Data Integration** processes. The wrapper could be a custom logging processes, which writes records into a table before the main jobs start, if it fails and if it end successfully. We do not want to add this logic directly to all the other data integration processes, but keep the setup modular, so we create a dedicated **wrapper job** for this purpose. We want to be able to pass a parameter to the **wrapper job** to tell it which main job or transformation it should run.

We are facing a bit of a challenge in regards to passing parameters from the wrapper job to the actual process (job or transformation) that we want to execute. As we are planning to run a variety of processes all with their own set of possible parameters it does not make any sense to specify all these parameters in the wrapper job: first of all because there could hundreds of parameters and second of all we probably wouldn't know all of them upfront.

An elegant way would be to pass an object containing our parameters to the wrapper job, then unwrap the object and set the parameters for the process, an approach quit often used with scripting and programming languages. Now while **PDI** does not have a built-in support for **parameter objects**, we can still implement this functionality:

We set out by adding a party parameter called `PARAM_OBJECT` to the wrapper job. The value of this parameter will be a string representation of an **JavaScript** object. Next we create a dedicated transformation which accepts this parameter value as an input, then with the help of the **JavaScript** step we transform this parameter value into a real object using `JSON.parse`. Finally we loop over the properties of this object to create the actual parameters, set the values and define the scope. We will run this transformation at the beginning of our wrapper job. This way the parameter values will be available once the actual process (job or transformation) executes.

## Setting up the basic process

Let's create the **wrapper** job:

1. Fire up **Spoon** (PDI's GUI) and create a new **job** called `jb_wrapper`. 
2. Press `CTRL+J` or `CMD+J` (latter one for *Mac OS X*) and click on the **Parameter** tab. Define the `PARAM_OBJECT` parameter with this value: `{"PARAM_DATE":"2016-02-01","PARAM_CITY":"London"}`. The value is the string representation of a **JSON** object. Add another parameter called `PARAM_JOB_TO_EXECUTE` and set its value to `${Internal.Job.Filename.Directory}/jb_sample.kjb`.
3. Add a **Start**, an **Execute Transformation** and a **Execute Job**  job entry and connect them:

![](/images/pdi_param_obj_1.png)

Create a new **transformation** and name it `tr_obj_string_to_vars`:

1. Add a **Get Variables** step. In the step settings define a new field of type String called `param_object_string` which maps to `${PARAM_OBJECT}`: The parameter we defined in the parent job.
2. Add a **Modified Java Script Value** and link it up with the previous step. Add following script:

```javascript
param_obj = JSON.parse(param_object_string);

for(property in param_obj){
	//Alert('Property: ' + property + ', Value: ' + param_obj[property]);
	setVariable(property, param_obj[property], 'r');
}
```

Here we transform the string value into a proper **JavaScript** object. Then we loop over the properties of this object and create the **PDI** **variables** set to the respective value. So once this is executed, e.g. `PARAM_CITY` will be available as a proper **PDI** variable and will have the value `London` assigned to it. We also make sure that the **scope** of the variable is set to `root` (`r`).

> **Note**: For testing purposes I also made use of the `Alert()` function, which will display the parameter name and value. This section is commented out. 

![](/images/pdi_param_obj_2.png)

**Save** this transformation and link the **Execute Transformation** job entry in `jb_wrapper` to it.

Next we want to **test** this setup, so we will create a **dummy job** and **transformation** which will represent the processes we want to run later on:

**Dummy Job:**

1. Create a new job called `jb_sample`. Link the **Execute Job** job entry in `jb_wrapper` to this job.
2. Add **Start**, **Write to Log** and **Execute Transformation** job entries to `jb_sample` and link them up.
3. Configure the **Write to Log** job entry to output this:

```
========= Job Parameters set to following values ==========

city: ${PARAM_CITY}
date: ${PARAM_DATE}
```

![](/images/pdi_param_obj_3.png)

**Dummy Transformation**

1. Create a new transformation called `tr_sample`. Link the **Execute Job** job entry in `jb_sample` to this transformation.
2. Add **Set Variables** and **Write to Log** steps to `tr_sample` and link them up.
3. Configure **Set Variables** as follows:

		Name | Variable | Type
		-----|----------|--------
		city | ${PARAM_CITY} | String
		date | ${PARAM_DATE} | String

4. Configure **Write to Log** to output the values of both fields.

![](/images/pdi_param_obj_4.png)

**Save** all the files and **execute** `jb_wrapper`. **Watch the log** output to see how the variables are passed down from the wrapper to the dummy job and finally to the dummy transformation.

## Substituting Parameters with Variables

There is one problem though with our setup: We do not always want to run the main job (in our case `jb_sample`) with the wrapper. Especially for testing, we might want to run `jb_sample` on its own. This means that we will have to define any required **parameters** directly in the job settings.

As soon as we define `PARAM_CITY` and `PARAM_DATE` in our `jb_sample` job, the chain of passing down parameters breaks. **Pentaho Data Integration** does not automatically **substitute** a **parameter** value with a **variable** value if you pass it down from a job in example. **Roland Bouman** wrote an excellent article on how to solve this challenge many years ago on his [blog post](http://rpbouman.blogspot.co.uk/2010/12/substituting-variables-in-kettle.html). We will use the approach he outlined [here](http://wiki.pentaho.com/display/EAI/Substituting+variable+references+in+Job+Parameter+values).

Let's tackle this challenge: As you might know, the PDI job (`.kjb`) and transformation (`.ktr`) files are just **XML** files. We will source the parameter names directly from **XML file** of the job we want to execute from the wrapper. Once we have the parameter names, we will create variables of the same name and assign the respective values of the variables to them. We will try to keep everything dynamic, hence we will follow mostly Roland's approach here:

1. Define the following parameters in `jb_sample`'s job settings: `PARAM_CITY` and `PARAM_DATE`. Do not set any value for them. 
2. Create a new **transformation** called `tr_substitute_params_with_vars`.
3. Add a **Get XML Data** step. As the source file define `...`. In the **Content** tab set the **Loop XPath** expression to `/job/parameters/parameter`. In the **Fields** tab define the field `parameter_name`, set Xpath to `name`, element to `Node`, result type to `Value of` and type to `String`.
4. Add a **Modified JavaScript Value** step and define following script:

```javascript
//Alert(parameter_name + ": " + getVariable(parameter_name, "")); 
setVariable(parameter_name, getVariable(parameter_name, ""), "r");
```

![](/images/pdi_param_obj_6.png)

Now let's turn our attention again to `jb_sample`: Just after the **Start** job entry add a new **Execute Transformation** job entry and link it to the `tr_substitute_params_with_vars` transformation:

![](/images/pdi_param_obj_5.png)

Now it's time to execute `jb_wrapper` and understand if the new setup paid off!

You will realise that this solution is still not working (Roland's original setup was different to ours). The problem is that you cannot pass down a variable from a job to a child job or transformation if the parameter defined in the **child job** has the same name (so also my previous blog post [ Pentaho Kettle Parameters and Variables: Tips and Tricks ](http://diethardsteiner.blogspot.co.uk/2013/07/pentaho-kettle-parameters-and-variables.html)). So there is an easy workaround to this: We just change the name of the **variables** we set in the **JavaScript** step in `tr_obj_string_to_vars` like this:

```javascript
param_obj = JSON.parse(param_object_string);

for(property in param_obj){
	//Alert('Property: ' + property + ', Value: ' + param_obj[property]);
	// It seems like you cannot have parameters in a sub job
	// of the same name, hence we added a suffix to the parameter name:
	setVariable(property + '_OUTER', param_obj[property], 'r');
}
```

Note that the change is very subtle, we just suffix the variable name with `_OUTER`. This in turn means that we have to adjust the **JavaScript** in `tr_substitute_params_with_vars`:

```javascript
//Alert(parameter_name + ": " + getVariable(parameter_name + "_OUTER", "")); 
setVariable(parameter_name, getVariable(parameter_name + "_OUTER", ""), "r");
```

Execute `jb_wrapper` again now and you should see that the values are passed down correctly.

**Important**: While this is working, it is unnecessarily complicated: We actually do not need `tr_substitute_params_with_vars` at all! All we have to do is to supply a mapping in `jb_sample` which basically sets the variable values set in `tr_obj_string_to_vars` as the default value of the parameters defined in the current job: 


Parameter | Default Value | Description
----------|---------------|------------
`PARAM_CITY` | `${PARAM_CITY_OUTER}` | 
`PARAM_DATE` | `${PARAM_DATE_OUTER}` | 

This is a much simpler to setup. However, using `tr_substitute_params_with_vars` saves us though from specifying this mapping. So decide for yourself
what works better for you.

We finally got our setup working - what an interesting ride this has been! You can **download** the **PDI jobs and transformation** from [here](https://github.com/diethardsteiner/diethardsteiner.github.io/tree/master/sample-files/pdi/pdi-param-obj).


While this is a very interesting example, a more practical and easier approach is to just read a properties file from the sub-job instead (and pass the filepath as a parameter down from the parent job to the sub-job).