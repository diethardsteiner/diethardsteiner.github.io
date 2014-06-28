---
layout: post
title:  "Pentaho Dashboards (CDE): Bootstrap styled custom selects"
date:   2014-06-28
categories: Dashboards
tags: Pentaho CDF CDE Dashboards
published: true
---

This is a follow-up article to the concepts previously discussed in [Pentaho Dashboards (CDE): Create your custom table](http://diethardsteiner.github.io//dashboards/2014/05/21/Pentaho-CDE-Custom-Table.html).

Sometimes the form controls / selects have to provide some additional functionality than the out-of-the-box form selector components provide.

If you need more flexibility with form controls, then this simple approach might be useful to you:
1. Create a datasource
2. Create a **Query Component**
3. Use the **Post Execution** function in the **Advanced Properties** section of the **Query Component** to dynamically add the necessary form controls.

On a side note, it would be nice if the standard CDE form controls had a CSS Class, so that you could apply the the bootstrap styles directly. 

In any case, this article is not only about applying bootstrap styles, but also about adding some additional functionality to the form controls.

The approach outlined above is fairly simple, apart from maybe the last one, which I will discuss in detail here:

In the **Layout Structure** create a structure similar to this one: Column > Row > HTML. Key part is that you use HTML at the end. This will allow you to make use of more than one boostrap component.

For the **HTML** node click on the ellipsis button next to the *HTML* property and insert following HTML snippet (example shown below). Provide an `id` for the container that you want to place your form element in (in example *html_db_connection_picker*). Note that we are already using the **Bootstrap** style *panel panel-default* for the outer-most div element:

```html
<div class="panel panel-default">
  <div class="panel-heading">
    <h3 class="panel-title">My Form Title</h3>
  </div>
  <div class="panel-body">
    <div id="html_db_connection_picker"></div>
  </div>
</div>
```

In the **Datasource Panel** add a datasource of your choice and provide the mandatory properties.

In the **Component Panel** insert a **Query Component**, link it to the datasource you just created, and provide a name for the **Result Var** (i.e. *result_fetch_db_connections*).

Click on the ellipsis button next to the **Post Execution** property and insert a code similar to this one:

```javascript
function(){
    //document.getElementById('html_db_connection_picker').innerHTML = JSON.stringify(result_fetch_db_connections);
    var myData = result_fetch_db_connections;
    var myContainer = document.getElementById('html_db_connection_picker');
    var mySelect = document.createElement('select');
    var myOption = document.createElement('option');
    myContainer.appendChild(mySelect.cloneNode()).className = "form-control";
    for(var i = 0; i < myData.length; i++) {
        var myValue = document.createTextNode(myData[i][0]);
        myContainer.lastChild.appendChild(myOption.cloneNode()).appendChild(myValue);
        }
}
```

This is the very basic and simple function and a good starting point to add additional functionality.

Let's do a preview:

![](/images/pentaho-cde-custom-form-components-1.png)

Next step is to make this a bit more generic. Cut this code and go to the **Layout Structure** panel and add a **Resource** of the type *JavaScript* *snippet*. Ideally later on you can move this to an external JavaScript file. In the **Properties** panel proved a name and click on the ellipsis button next to the **Resource Code** property. Paste the code. With a few slight modifications it should work:

```javascript
function cdeBootstrapSelect(myContainerId, myData){
    var myContainer = document.getElementById(myContainerId);
    var mySelect = document.createElement('select');
    var myOption = document.createElement('option');
    mySelect.className = "form-control";
    for(var i = 0; i < myData.length; i++) {
        var myValue = document.createTextNode(myData[i][0]);
        mySelect.appendChild(myOption.cloneNode()).appendChild(myValue);
        }
    myContainer.appendChild(mySelect);
}
```

Go back to the **Component** panel and change the **Post Execution** function of our **Query Component** to the following:

```javascript
function(){
  cdeBootstrapSelect('html_db_connection_picker',result_fetch_db_connections);
}
```

> **Note**: The second argument is without quotation marks as we are passing the actual result set along. Our code is reusable now - we can build several other select components of the same type without duplicating the code. It's still quite basic code, but we can extend the functionality in future.

Now the question is of course, how can my custom form components interact with other components? The code below shows a *generic* implementation of the above code with the addition of an **event listener**. The code below supports cascading selects: Imagine you have several selects that deplend on each other.

- Only the relevant selects are shown (and not all selects).
- If the user decides to change a value further up the select tree, irrelevant child selects will be automatically removed.

```javascript
function bissolCreateSelect(myCdeContainerId, myDashboardObjectId, myLabelText, myData, cdeParam){
    var myContainer = document.getElementById(myCdeContainerId);
    var myLabel = document.createElement('label');
    var mySelect = document.createElement('select');
    var myOption = document.createElement('option');
    var myLabelTextNode = document.createTextNode(myLabelText);
    myLabel.setAttribute('for', myDashboardObjectId);
    mySelect.className = "form-control";
    mySelect.id = myDashboardObjectId;
    // avoid having a default value preselected
    var myDefaultOption = '<option disabled selected>Please select an option...</option>';
    console.log(myDashboardObjectId + " with following values: " + JSON.stringify(myData));

    // 1. Check if data is available
    if(myData.length > 0){  

        mySelect.innerHTML = myDefaultOption;

        for(var i = 0; i < myData.length; i++) {
            var myValue = document.createTextNode(myData[i][0]);
            mySelect.appendChild(myOption.cloneNode()).appendChild(myValue);
        }

        // 2. Check if select exists - !! checks for a truthy value 
        // if it exists ...
        if(!!document.getElementById(myDashboardObjectId)){
            myContainer.replaceChild(mySelect,document.getElementById(myDashboardObjectId));
            bissolEstablishSelectListener(myDashboardObjectId, cdeParam);
            Dashboards.fireChange(cdeParam,null);
        }
        // if it doesnt exist ...
        else {
            myContainer.appendChild(myLabel.cloneNode()).appendChild(myLabelTextNode);
            myContainer.appendChild(mySelect);
            bissolEstablishSelectListener(myDashboardObjectId, cdeParam);
        }
    } 
    // if no data is available remove any existing selects
    else {
        //myContainer.removeChild(document.getElementById(myDashboardObjectId));
        myContainer.innerHTML = '';
        Dashboards.fireChange(cdeParam,null);
    }   
}

function bissolPropagateSelectSelection(evt){
    Dashboards.fireChange(evt.target.cdeParam, evt.target.options[evt.target.selectedIndex].text);
    console.log('Setting parameter ' + evt.target.cdeParam + ' to: ' + evt.target.options[evt.target.selectedIndex].text);
}

function bissolEstablishSelectListener(myDashboardObjectId, cdeParam){
    var select = document.getElementById(myDashboardObjectId);
    // add arguments to pass on to event
    //select.selectedText = select.options[select.selectedIndex].text; // dont set it here as the changes wont be picked up - set in function which the event listener calls
    select.cdeParam = cdeParam;
    select.addEventListener('change', bissolPropagateSelectSelection, false);
    console.log('Adding event listener for id: ' + myDashboardObjectId);
}
```

This is all written in plain JavaScript - to make things a bit easier you could just make use of JQuery as well.

This function can be referenced from the **Post Execution** property of the **Query Component** in the following fashion in example:

```javascript
function(){
    bissolCreateSelect('html_db_connection_picker', 'dbConnectionPicker', 'Choose Connection:', result_fetch_db_connections, 'param_db_connection');
}
```

An important concept to understand is that however complicated your form may be, you should always link one selector to on CDE parameter (which can be created via javascript function `Dashboards.setParameter()` or the CDE GUI). Just make sure that you update the value of this parameter once the value in the selector changes. You can use the `Dashboards.fireChange(parameter, value)` function to do just this. Remember, that the values are then always stored in these CDE parameters, so there is no need to create an extra array or similar to hold the selected values. You can easily retrieve the value of a given parameter by using the JavaScript function `Dashboards.getParameterValue(parameter)`. I have mentioned a few CDF JavaScript functions so far: I guess you are wondering by now, what other ones are available! How do you get hold of them? Probably one of the easiest ways to get a list of all functions etc. is to preview your Dashboard, fire up your browser's developer tools and type `Dashboards.` into the JavaScript console and wait until auto-completion kicks in:

![](/images/pentaho-cde-custom-form-components-2.png)

In case you are more curious, you can search for the *Dashboard.js* file in your `biserver/pentaho-solutions/system` folder. In older versions you could find it in the `pentaho-cdf/js` folder.

> **Note**: If you have a set of cascading selects, you do not have to pass all parameters for each step in your JavaScript. In example the picture below depicts a scenario where the user can choose first the database connection, then the database schema and finally the table. In each step, there is one more parameter required to query the options. In the JavaScript, at each step only one parameter is passed: So there is no need to pass both the selected database name and schema name to the table select (which it depends on), but only the schema name (via the `Dashboards.fireChange()` function), because the other parameter values are already stored. So in your CDE **Query Component** for the table selector you would define the *schema name* parameter as **Listener** and specify the *database name* and *schema name* parameters in the **Parameters** property.

![](/images/pentaho-cde-custom-form-components-3.png)

Let's now take a look at checkboxes. We will continue with the example mentoined above. Imagine that after selecting the table, we want the user to select some columns of the table. A bootstrap style checkbox is made up of [following syntax](http://getbootstrap.com/css/#forms):

```html
  <div class="checkbox">
    <label>
      <input name="fruit" type="checkbox" value="orange">orange
    </label>
  </div>
```

Our JavaScript version (part of the resource file) could look something like this then:

```javascript
function bissolCreateCheckboxSet(myCdeContainerId, myDashboardObjectId, myLabelText, myData, cdeParam){
    var myContainer = document.getElementById(myCdeContainerId);
    var myLabel = document.createElement('label');

    // create main container which holds all the checkboxes
    var myCheckboxSetContainer = document.createElement('div');
    myCheckboxSetContainer.id = myDashboardObjectId;    
    var myMainLabelTextNode = document.createTextNode(myLabelText);
    myCheckboxSetContainer.appendChild(myLabel.cloneNode()).appendChild(myMainLabelTextNode);

    // create structure for one checkbox
    var myCheckboxContainer = document.createElement('div');
    myCheckboxContainer.className = 'checkbox';
    var myCheckbox = document.createElement('input');   
    myCheckbox.setAttribute('type','checkbox');

    // 1. Check if data is available
    if(myData.length > 0){  


        for(var i = 0; i < myData.length; i++) 
        {
            var myCheckboxLabel = document.createTextNode(myData[i][0]);
            
            var myC = myCheckbox.cloneNode();
            myC.setAttribute('name', cdeParam);
            myC.setAttribute('value', myData[i][0]);
            
            var myL = myLabel.cloneNode();
            myL.appendChild(myCheckboxLabel);
            myL.appendChild(myC);
            
            myCheckboxSetContainer.appendChild(myCheckboxContainer.cloneNode()).appendChild(myL);
        }

        // 2. Check if checkbox exists - !! checks for a truthy value 
        // if it exists ...
        if(!!document.getElementById(myDashboardObjectId)){
            myContainer.replaceChild(myCheckboxSetContainer,document.getElementById(myDashboardObjectId));
            bissolEstablishCheckboxListener(myDashboardObjectId, cdeParam);
            //Dashboards.fireChange(cdeParam,null);
            Dashboards.fireChange(cdeParam,'');
        }
        // if it doesnt exist ...
        else {
            myContainer.appendChild(myCheckboxSetContainer);
            bissolEstablishCheckboxListener(myDashboardObjectId, cdeParam);
        }
    } 
    // if no data is available remove any existing selects
    else {
        //myContainer.removeChild(document.getElementById(myDashboardObjectId));
        myContainer.innerHTML = '';
        //Dashboards.fireChange(cdeParam,null);
        Dashboards.fireChange(cdeParam,'');
    }
} 

// - when a checkbox is ticked, add value to cde param array
// - when a checkbox is unticked, remove value from cde param array

function bissolEstablishCheckboxListener(myDashboardObjectId, cdeParam){
    var myDashboardObject = document.getElementById(myDashboardObjectId);
    var checkboxes = myDashboardObject.getElementsByTagName('input');
    // establish listener for each checkbox
    for(var i = 0; i < checkboxes.length; i++){
        var checkbox = checkboxes[i];
        checkbox.cdeParam = cdeParam;
        checkbox.addEventListener('change', bissolPropagateCheckboxSelection, false);
        console.log('Adding event listener for id: ' + myDashboardObjectId +', checkbox: ' + i );        
    }
}

function bissolPropagateCheckboxSelection(evt){
    console.log('The target value is: ' + evt.target.value);   
    var myCheckedValues = [];    
    if(Dashboards.getParameterValue(evt.target.cdeParam) === ''){
        //myCheckedValues = [];
    } else {
        myCheckedValues = Dashboards.getParameterValue(evt.target.cdeParam);    
    }

    console.log('My previously checked values are: ' + myCheckedValues);

    if(evt.target.checked){
        myCheckedValues.push(evt.target.value);
        Dashboards.fireChange(evt.target.cdeParam,myCheckedValues);
        console.log('Setting parameter ' + evt.target.cdeParam + ' to: ' + myCheckedValues);
    } else {
        // find element in array
        var index = myCheckedValues.indexOf(evt.target.value);
        // remove element from array
        if (index > -1) {
            myCheckedValues.splice(index, 1);
        }
        // fire change
        Dashboards.fireChange(evt.target.cdeParam,myCheckedValues);
        console.log('Setting parameter ' + evt.target.cdeParam + ' to: ' + myCheckedValues);
    }
}
```

And you can call this function via the **Post Execution** property of the **Query Component** like this:

```javascript
function(){
    bissolCreateCheckboxSet('html_db_column_picker','dbColumnPicker', 'Choose Columns:', result_fetch_db_columns, 'param_db_column');
}
```

This is how our selects look so far:

![](/images/pentaho-cde-custom-form-components-4.png)

The idea is that as soon as a user ticks a checkbox, the subsequent form item or button gets displayed.

The last thing to do is to write the `JavaScript` which displays a **submit** button once a checkbox is ticked. Let's create the button structure:

```
function bissolCreateButton(myCdeContainerId, myLabelText, cdeParamIncoming){
    //console.log('param_db_column values is: ' + Dashboards.getParameterValue(cdeParamIncoming));
    var myContainer = document.getElementById(myCdeContainerId);

    var myButton = document.createElement('button');
    var myButtonText = document.createTextNode(myLabelText);
    myButton.className = 'btn btn-primary bissolConfigSubmit';
    myButton.setAttribute('type','submit');
    myButton.appendChild(myButtonText);

    var myExistingButtons = myContainer.getElementsByTagName('button');

    if(!!Dashboards.getParameterValue(cdeParamIncoming) && Dashboards.getParameterValue(cdeParamIncoming).length > 0){
        // check if select already exists otherwise create   
        if(myExistingButtons == 'undefined' || myExistingButtons.length === 0){
            myContainer.appendChild(myButton);
        }
    } else {
        console.log('Removing button ...');
        // remove button
        myContainer.innerHTML = '';
    }

}
```

So the idea is that we have to listen to a specific paramter - and only when this parameter has a value, we want to crate this button. If the parameter has no value, the button must be removed. How do we achieve this?

An easy way to implement this is to use the **Freeform Component**. Just set its **Listener** to the specific parameter which should trigger the action and the **Custom Script** to the following:

```
function(){
    bissolCreateButton('html_submit','Submit','param_db_column');
}
```

Now our dynamic form should be working!

![](/images/pentaho-cde-custom-form-components-5.png)

I hope this tutorial gave you a good idea on how flexible **Pentaho Community Dashboards** really are. And the really exciting point is that you can all implement this using standard HTML technologies!