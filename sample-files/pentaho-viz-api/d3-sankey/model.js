define([
  "module"
], function(module) {
  "use strict";

  return ["pentaho/visual/base/model", function(BaseModel) {
    // Create the Bar Model subclass
    // We extend the **Base Model** with our specific implementation 
    var SankeyModel = BaseModel.extend({
      $type: {
        id: module.id,

        // CSS class
        styleClass: "pentaho-visual-samples-sankey-d3",

        // The label may show up in menus
        label: "D3 Sankey Chart",

        // The default view to use to render this visualization is
        // a sibling module named `view-d3.js`
        defaultView: "./view-d3",   // <== DEFINE DEFAULT VIEW

        // Properties
        props: [
          // General properties
          // {
          //   name: "barSize",
          //   valueType: "number",
          //   defaultValue: 30,
          //   isRequired: true
          // },

          // Visual role properties
          {
            name: "link", // VISUAL_ROLE: you can name it anything you like
            base: "pentaho/visual/role/property",
            modes: [
              // {dataType: "list"}
              /* defaults to:
              {dataType: "string"}
              which accepts a single value only
              to accept multiple values use: */
              {dataType: ["string"]}
            ]
            // ordinal: 6
          },
          // {
          //   name: "category",
          //   base: "pentaho/visual/role/property",
          //   fields: {isRequired: true}
          // },
          {
            name: "measure", // VISUAL_ROLE: you can name it anything you like
            base: "pentaho/visual/role/property",
            modes: [{dataType: "number"}],
            fields: {isRequired: true}
          },

          // Palette property
          {
            name: "palette",
            base: "pentaho/visual/color/paletteProperty",
            levels: "nominal",
            isRequired: true
          }
        ]
      }
    });

    console.log(" -----  Generated Model ----- ");
    console.log(SankeyModel);

    return SankeyModel;
  }];
});