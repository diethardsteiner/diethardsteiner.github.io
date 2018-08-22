define(
  [
    "module"
     , "d3"
   //  , "d3-shape"
//     , "d3-array"
//     , "d3-collection"
//     , "d3-path"
//     , "d3-format"
//     , "d3-time"
//     , "d3-time-format"
//     , "d3-color"
//     , "d3-interpolate"
//     , "d3-scale"
     , "d3-sankey"
    , "pentaho/visual/scene/Base"
    , "css!./css/view-d3"     // <-- Referenced CSS here
  ]
  , function(
      module
      , d3
   //    , shape
//       , d3_array
//       , collection
//       , path
//       , format
//       , time
//       , timeFormat
//       , color
//       , interpolate
//       , scale
       , d3_sankey
      , Scene
    ) {
  "use strict";

  d3 = Object.assign(d3, d3_sankey);

  return [
    "pentaho/visual/base/view",
    "./model",
    function(BaseView, Model) {


      // Create the Bar View subclass
      var SankeyView = BaseView.extend({
        $type: {
          id: module.id,
          props: [
            // Specialize the inherited model property to the Bar model type
            {
              name: "model",
              valueType: Model
            }
          ]
        },
      
        _updateAll: function() {
          // d3.select(this.domContainer).text("Hello World!");

          // This is where we add the main d3js code

          // ------- PART 1 ---------
          // get access to the visualisation model object
          var model = this.model;
          
          var dataTable = model.data;
          
          console.log("----- View: data -----");
          console.log(model.data);
          // The data in the data table needs to be converted into an “array of plain objects” form, 
          // so that then it can be directly consumed by D3; To achieve this,
          // the pentaho.visual.scene.Base helper class is used.
          var scenes = Scene.buildScenesFlat(this).children;

          console.log("---- View: Flattened Data -----");
          console.log(scenes);
          
          // `this.domContainer` gives you access to the div
          // where your visualisation should be rendered  
          var container = d3.select(this.domContainer);

          // get width and height of container that the viz will be rendered in
          // https://stackoverflow.com/questions/21990857/d3-js-how-to-get-the-computed-width-and-height-for-an-arbitrary-element
          const containerWidth = d3.select(this.domContainer).node().getBoundingClientRect().width;
          const containerHeight = d3.select(this.domContainer).node().getBoundingClientRect().height;
          
          // ------- PART 2 ---------
          // the actual d3js implementation
          // based on http://bl.ocks.org/d3noob/c9b90689c1438f57d649

          container.selectAll("*").remove();


          // provide mapping
          var data = scenes;        // <-- ADDED TO WORK WITH VIZ API
          console.log(" --- DATA --- ");
          console.log(data);
          // code from http://bl.ocks.org/d3noob/c9b90689c1438f57d649 -- START
          var units = "Widgets";

          var margin = {top: 10, right: 10, bottom: 10, left: 10},
              width = containerWidth - margin.left - margin.right,
              height = containerHeight - margin.top - margin.bottom;

          var formatNumber = d3.format(",.0f"),    // zero decimal places
              format = function(d) { return formatNumber(d) + " " + units; },
              color = d3.scaleOrdinal(d3.schemeCategory20);

          // append the svg canvas to the page
          // var svg = d3.select("#chart").append("svg") // <-- ADJUSTED TO WORK WITH VIZ API
          var svg = container.append("svg")
              .attr("width", width + margin.left + margin.right)
              .attr("height", height + margin.top + margin.bottom)
            .append("g")
              .attr("transform", 
                    "translate(" + margin.left + "," + margin.top + ")");


          // DS: DISABLED AS WE PROVIDE DATA VIA VIZ API
          // load the data (using the timelyportfolio csv method)
          // d3.csv("sankey.data.csv", function(error, data) {

            //set up graph in same style as original example but empty
            // var graph = {"nodes" : [], "links" : []};

            // OPEN: Possible needs adjustment
            // data.forEach(function (d) {
            //   console.log("logging record ...");
            //   console.log(d.vars.category[0].v);
            //   console.log(d.vars.category[1].v);
            //   console.log(d.vars.measure.v);

            //   var source = d.vars.category[0].v;
            //   var target = d.vars.category[1].v;
            //   var measure = d.vars.measure.v;

            //   graph.nodes.push({ "name": d.vars.category[0].v });
            //   graph.nodes.push({ "name": d.vars.category[1].v });
            //   graph.links.push({ 
            //     "source": d.vars.category[0].v,
            //     "target": d.vars.category[1].v,
            //     "value": d.vars.measure.v
            //   });
            // });

            // console.log("VIEW: Printing transformed data ...");
            // console.log(graph);

            // My adjustment

            // ----- TRANSFORMING THE SOURCE DATA TO THE TARGET FORMAT ----- START

            // GENERATE UNIQUE LIST OF NODES
            // https://stackoverflow.com/questions/1960473/get-all-unique-values-in-a-javascript-array-remove-duplicates
            // onlyUnique checks, if the given value is the first occurring. 
            // If not, it must be a duplicate and will not be copied.
            function onlyUnique(value, index, self) { 
                return self.indexOf(value) === index;
            }

            // The native method filter will loop through the array and leave only those entries 
            // that pass the given callback function onlyUnique.
            const listOfNodesCat0 = data.map(x => x.vars.link[0].v);
            const listOfNodesCat1 = data.map(x => x.vars.link[1].v);
            const listOfNodes = listOfNodesCat0.concat(listOfNodesCat1);
            var uniqueListOfNodes = listOfNodes.filter( onlyUnique );
            // console.log("Unique list of nodes ...");
            // console.log(uniqueListOfNodes);


            // GENERATE REQUIRED ARRAY OF NODES
            // const nodes = uniqueListOfNodes.map(x => {
            //   var rObj = {};
            //   rObj.name = x;
            //   return rObj;
            // });

            const nodes = uniqueListOfNodes;

            // GENERATE REQUIRED ARRAY OF LINKS
            const links = data.map(obj => {
              var rObj = {};
              rObj.source = obj.vars.link[0].v;
              rObj.target = obj.vars.link[1].v;
              rObj.value = obj.vars.measure.v;
              return rObj;
            });

            var graph = {"nodes" : nodes, "links" : links};
            // var graph = {};
            // graph["nodes"] = nodes;
            // graph["links"] = links; 

            // console.log(" --- GRAPH --- ");
            // console.log(JSON.stringify(graph));

            
            // DS: I create the unique nodes further up
            // return only the distinct / unique nodes
            // graph.nodes = d3.keys(
            //   d3.nest()
            //     .key(function (d) { return d.name; })
            //     .map(graph.nodes)
            //   );


            // loop through each **link** replacing the text with its index from **node**
            graph.links.forEach(function (d, i) {
             graph.links[i].source = graph.nodes.indexOf(graph.links[i].source);
             graph.links[i].target = graph.nodes.indexOf(graph.links[i].target);
            });

            // now loop through each node to make nodes an array of objects
            // rather than an array of strings
            graph.nodes.forEach(function (d, i) {
             graph.nodes[i] = { "name": d };
            });

            console.log(" --- GRAPH --- ");
            console.log(JSON.stringify(graph));


            // ----- TRANSFORMING THE SOURCE DATA TO THE TARGET FORMAT ----- END


            // SET THE SANKEY DIAGRAM PROPERTIES
            var sankey = 
              // d3.sankey(): Constructs a new Sankey generator with the default settings
              d3.sankey()
                .nodeWidth(36)
                .nodePadding(40)
                .size([width, height])
                .nodes(graph.nodes)
                .links(graph.links)
                ;//.update(graph);


            /*
            COMPUTE THE NODE AND LINK POSITIONS
            sankey(arguments…): 
            Computes the node and link positions for the given arguments, returning a graph representing the Sankey layout. The returned graph has the following properties:

              graph.nodes - the array of nodes
              graph.links - the array of links
            */
            graph = sankey();
            // graph = sankey.update(graph);
      
            // add in the links
            var link = svg.append("g")
              .selectAll("path")
              .data(graph.links)
              .enter().append("path")
                .attr("class", "link")
                .attr("d", d3.sankeyLinkHorizontal())
                // had to adjust this:
                .attr("stroke-width", function(d) { return d.width; })
                .sort(function(a, b) { return b.with - a.with; });

            // add the link titles
            link.append("title")
                .text(function(d) {
                  return d.source.name + " → " + 
                    d.target.name + "\n" + format(d.value); 
                  }
                );

            // add in the nodes
            var node = svg.append("g").selectAll(".node")
                .data(graph.nodes)
              .enter().append("g")
                .attr("class", "node")
                .attr("transform", function(d) { 
                  return "translate(" + d.x0 + "," + d.y0 + ")"; 
                })
              .call(
                d3.drag()
                //  .origin(function(d) { return d; })
                .on("start", function() { 
                  this.parentNode.appendChild(this); }
                )
                .on("drag", dragmove)
              );

            // add the rectangles for the nodes
            node.append("rect")
                .attr("height", function(d) { return d.y1 - d.y0; })
                // .attr("height", function(d) { return d.dy; })
                .attr("width", sankey.nodeWidth())
                .style("fill", function(d) { 
                  return d.color = color(d.name.replace(/ .*/, "")); 
                })
                .style("stroke", function(d) { 
                  return d3.rgb(d.color).darker(2); 
                })
                .append("title")
                .text(function(d) { 
                  return d.name + "\n" + format(d.value); 
                });

            // add in the title for the nodes
            node.append("text")
                .attr("x", -6)
                .attr("y", function(d) { return (d.y1 - d.y0) / 2; })
                // .attr("y", function(d) { return d.dy / 2; })
                .attr("dy", ".35em")
                .attr("text-anchor", "end")
                .attr("transform", null)
                .text(function(d) { return d.name; })
              // if the x position of the label is less than half the width
              .filter(function(d) { return d.x1 < width / 2; })
                // display the label on the right
                .attr("x", 6 + sankey.nodeWidth())
                .attr("text-anchor", "start");

            // the function for moving the nodes
            function dragmove(d) {
              d3.select(this).attr(
                "transform", 
                "translate(" + d.x + "," + (
                            d.y = Math.max(0, Math.min(height - d.y, d3.event.y))
                        ) + ")"
              );
              sankey.update(graph);
              link.attr("d", d3.sankeyLinkHorizontal());
            }
          // });

          // code from http://bl.ocks.org/d3noob/c9b90689c1438f57d649 -- END

        }
        // ,
        // __getRoleLabel: function(mapping) {

        //   if(!mapping.hasFields) {
        //     return "";
        //   }

        //   var data = this.model.data;

        //   var columnLabels = mapping.fieldIndexes.map(function(fieldIndex) {
        //     return data.getColumnLabel(fieldIndex);
        //   });

        //   return columnLabels.join(", ");
        // }
      });

      return SankeyView;
    }
  ];
});