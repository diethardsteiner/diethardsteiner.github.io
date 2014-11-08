## Mondrian: The meaning of column, columnName and captionColumn Level Attributes

As quick intro let's quickly have a look at the essential attributes of a **Mondrian Level** element.

Level attribute description (partial listing, full listing available [here](http://mondrian.pentaho.com/documentation/xml_schema.php#Level)):
    
*General*
  - **name**
  - **table** (optional)
  - **visible** (optional)

*Member Specific*
  - **column**: key column
  - **type**: type of the key column
  - **nameColumn**: internally used name, e.g. for MDX query
  - **captionColumn** (optional): member value shown to end user
  - **ordinalColumn** (optional): the column which specifies the order of the members
  - **uniqueMembers** 
  
*Level Specific*
  - **caption** (optional): A string being displayed instead of the level's name. Can be localized from Properties file using #{propertyname}.
  - **description** (optional): Description of this level. Can be localized from Properties file using #{propertyname}.
  - **levelType**: Whether this is a regular or a time-related level

Imagine we just started writing the date dimension definition and it looks like this so far:

```
<Dimension name="Date" type="TimeDimension" visible="true" highCardinality="true">
    <Hierarchy name="Monthly Calendar" caption="Monthly Calendar" visible="true" hasAll="true" allMemberName="Total" allMemberCaption="Total" primaryKey="date_tk">
        <Table name="dim_date" schema="common"/>
        <Level name="Year" column="year4" type="Integer" uniqueMembers="false" levelType="TimeYears" hideMemberIf="Never" visible="true" />
        
    </Hierarchy>	      
</Dimension>
```

Now we want to add the quarter.

Suppose you have these three columns in your date table which holds quarter info:

- **year_quarter_int** (Integer): 201401
- **year_quarter** (String): 2014-Q1
- **quarter_number** (Integer): 1 

Let's define the **Quarter Level**:

```
<Level 
    name="Quarter" 
    column="year_quarter_int"
    type="Integer"  
    captionColumn="year_quarter"
    ordinalColumn="year_quarter_int" 
    levelType="TimeQuarters"
	... >
</Level>
```

This means that in a client like **Saiku** or **Pentaho Analyzer** the user will see in example *2014-Q1*, but if we had to manually write our MDX query, we would have to write it this way (using the key `column` value):

```
{[Start Date.Monthly Calendar].[2014].[201402]}
```

As we are lazy - or let's better call it efficient - we do not want to repeat the year again for the quarter level member. To achieve just this, we add the `nameColumn` attribute to our level:

```
<Level 
    name="Quarter" 
    column="year_quarter_int"
    type="Integer" 
    nameColumn="quarter_number" 
    captionColumn="year_quarter"
    ordinalColumn="year_quarter_int" 
    levelType="TimeQuarters"
	... >
</Level>
```

In our MDX query we can use now:

```
{[Start Date.Monthly Calendar].[2014].[2]}
```

Just to summarize:

If you **do NOT** define a `captionColumn` or `nameColumn` then the end user will see the value of the key `column` in their analysis. The data analyst writing the MDX query will as well use the value of the key `column`.

If you don't define a `captionColumn` but **NOT** `nameColumn` then the end user will see the value of the `captionColumn` in their analysis. The data analyst writing the MDX query will however use the value of the key `column`.

If you define a `captionColumn` and a `nameColumn` then the end user will see the value of the `captionColumn` in their analysis. The data analyst writing the MDX query will however use the value of the `nameColumn`. So in this last case non of the users works directly with the key `column` values.