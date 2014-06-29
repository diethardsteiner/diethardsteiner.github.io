---
layout: post
title:  "Pentaho Dashboards CDE: Create your custom Bootstrap table"
date:   2014-05-21
categories: Dashboards
tags: Pentaho CDF CDE Dashboards Bootstrap
published: true
---

You want to implement something in your dashboard that is not covered by the out-of-the-box dashboard components? Luckily, with Pentaho CDE the world is open: CDE makes use of the standard web technologies (CSS, JavaScript, HTML), so theoretically you can implement whatever is in the realm of these technologies. Obviously you will need some basic knowledge of these technologies (setup is not as easy any more as filling out some config dialogs), but the possibilities are endless.
In this post I’ll briefly talk you through how to source some data and then to create a custom table with it (which you can easily do with one of the CDE components as well, but that’s not the point here … imagine what else you could do):

1. In CDE, register a **Datasource**. In example create a *sql over sqlJndi* datasource, provide a Name i.e. *qry_generic_select*, choose *SampleData* for JNDI and specify following query: 

		SELECT customername, customernumber, phone FROM customers
![](/images/pentaho-cde-custom-table-1.png)
2. In the component section, add a **Query Component**. This component is most commonly used for displaying simple results, like one number in a dashboard (i.e. max temperature). Here we will use this component to retrieve a bigger result set.
3. Click on **Advanced Properties**.
4. For the *Datasource property* specify the datasource you created in step 1 (i.e. qry_generic_select)
5. Provide a name for the **Result Var**. This is the variable, which will hold the output data of your datasource.
6. Write a *Post Execution* function, in example: 

		function() {
		     document.getElementById('test').innerHTML = JSON.stringify(select_result);
		} 

7. We will only use this function for now to test if the query is working. Later on we will change it.
8. The setup so far should look like this: 

    ![](/images/pentaho-cde-custom-table-2.png)

9. In the **Layout Panel** create a basic structure which should at least have one column. Name the column test as we referenced it already in our JavaScript function. 

    ![](/images/pentaho-cde-custom-table-3.png)

10. Preview your dashboard (partial screenshot): 

    ![](/images/pentaho-cde-custom-table-13.png)

11. Let’s change the Post Execution function to return only the first record:

		function() {
		     document.getElementById('test').innerHTML = JSON.stringify(select_result[0]);
		}
And the preview looks like this: ![](/images/pentaho-cde-custom-table-4.png)
12. Let’s change the *Post Execution* function to return only the first entry from the first record:

		function() {
		     document.getElementById('test').innerHTML = JSON.stringify(select_result[0][0]);
		}
And the preview looks like this: 
![](/images/pentaho-cde-custom-table-5.png)
13. Let’s extend our *Post Execution* function to create a basic table:

		function() {
		  var myContainer = document.getElementById('test');
		  var myTable = document.createElement('table');
		  var myTr = document.createElement('tr');
		  var myTd = document.createElement('td');
		myContainer.appendChild(myTable).appendChild(myTr).appendChild(myTd).innerHTML = select_result[0][0];
		}
Do a preview and make use of your browser’s developer tools to see the generated HTML:
![](/images/pentaho-cde-custom-table-6.png)
14. Ok, now that this is working, let’s add some very basic design. Click on **Settings** in the main CDE menu:
![](/images/pentaho-cde-custom-table-7.png)
15. Choose *bootstrap* from the **Dashboard Type** pull down menu:

    ![](/images/pentaho-cde-custom-table-8.png)
    
    Click **Save**.
16. Back to the *Post Execution* function of the **Query Component**: Now we want to make this a bit more dynamic: For every data row must be enclosed by `<td>` and within each data row each data value must be enclosed by `<td>`. We also have to add the `<tbody>` element to make a proper table. And we will apply the [Bootstrap Striped Table](http://getbootstrap.com/css/#tables) design:

    ```
    // Simple function preparing the table body 
     
    function() {
        var myContainer = document.getElementById('test');
        var myTable = document.createElement('table');
        var myTBody = document.createElement('tbody');
        var myTr = document.createElement('tr');
        var myTd = document.createElement('td');
        
        //myTable.id = 'table1';
        myTable.className = 'table table-striped';
        
        myContainer.appendChild(myTable).appendChild(myTBody);
            
        for(var i = 0; i < select_result.length; i++) {
            myContainer.lastChild.lastChild.appendChild(myTr.cloneNode());
            for(var j = 0; j < select_result[i].length; j++) {
                myText = document.createTextNode(select_result[i][j]);
                myContainer.lastChild.lastChild.lastChild.appendChild(myTd.cloneNode()).appendChild(myText);
            }
        }
    }
	```
	
    You can find a text version of this JavaScript code a bit further down as well in case you want to copy it.
17. Do a preview now and you will see that we have a basic table now:![](/images/pentaho-cde-custom-table-9.png)

    **Note**: In case you are creating this dashboard as part of a Sparkl plugin and you are having troubles seeing the bootstrap styles applied (and are sure that the problem is not within your code), try to preview the dashboard from within your Sparkl project endpoint listing (which seems to work better for some unknown reason): ![](/images/pentaho-cde-custom-table-10.png)

18. One important thing missing is the header. Let’s source this info now. The Query Component provides following useful functions, which you can access within **Post Execution** function:

    ```
	this.metadata
	this.queryInfo
	this.resultset
	```
	
    To get an idea of what is exactly available with in the metadata object, you can use in example this function:

    ```
	document.getElementById('test').innerHTML = JSON.stringify(this.metadata);
	```
	
    Which reveals the following:
    
    ![](/images/pentaho-cde-custom-table-11.png)
        
19. This is the function preparing the full table (header and body):

        function() {
            var myContainer = document.getElementById('test');
            var myTable = document.createElement('table');
            var myTHead = document.createElement('thead');
            var myTh = document.createElement('th');
            var myTBody = document.createElement('tbody');
            var myTr = document.createElement('tr');
            var myTd = document.createElement('td');
            
            //myTable.id = 'table1';
            myTable.className = 'table table-striped';
            
            //document.getElementById('test').innerHTML = JSON.stringify(this.metadata);
            myMetadata = this.metadata;
               
            myContainer.appendChild(myTable).appendChild(myTHead).appendChild(myTr);
            
            for(var s = 0; s < myMetadata.length; s++){
                myHeaderText = document.createTextNode(myMetadata[s]['colName']);
                myContainer.lastChild.lastChild.lastChild.appendChild(myTh.cloneNode()).appendChild(myHeaderText);
            }
             
            myContainer.lastChild.appendChild(myTBody);
                
            for(var i = 0; i < select_result.length; i++) {
                myContainer.lastChild.lastChild.appendChild(myTr.cloneNode());
                for(var j = 0; j < select_result[i].length; j++) {
                    myText = document.createTextNode(select_result[i][j]);
                    myContainer.lastChild.lastChild.lastChild.appendChild(myTd.cloneNode()).appendChild(myText);
                }
            }

        }
   
	
		
20. And the preview looks like this:
 
    ![](/images/pentaho-cde-custom-table-12.png)

21. We can simplify the script by making use of **JQuery** like show below. At the same time we also put this code into a **Resource** file, which you can reference (or create) in **Layout Panel > Layout Structure > Resource**:

        function buildTable(data,metadata) {
            
            // get data
            var myMetadata = metadata;
            var myData = data;
            
            // prepare table basic structure
            $('#tableeditor').append('<table class="table table-striped"><thead><tr></tr></thead><tbody></tbody></table>');

            // add table header cells
            $.each(myMetadata, function( i, val ){
                    $('#tableeditor > table > thead > tr').append('<th>' + val.colName + '</th>');
            });

            // add table body
            $.each(myData, function( i, val ){

                // add row
                $('#tableeditor > table > tbody').append('<tr></tr>');
                
                $.each(myData[i], function( j, value ){
                
                    // add cells within row       
                    $('#tableeditor > table > tbody > tr:last').append('<td>' + value + '</td>');       
                
                });
            });

        }
    
22. You can call this function from within your **Query Component** by specifying the **Advanced Properties > Post Execution** property like this:

        
        function(){
            buildTable(select_result,this.metadata);
        }
        

Voilá, our custom boostrap table is finished. This is not to say that you have to create a table this way in CDE: This was just an exercise to demonstrate a bit of the huge amount of flexibility that CDE offers. Take this as a starting point for something even better.
