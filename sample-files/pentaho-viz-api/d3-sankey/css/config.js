define(["module"], function(module) {
  // Replace /config by /model.
  // e.g. "pentaho-visual-samples-bar-d3/model".
  var vizId = module.id.replace(/(\w+)$/, "model");

  return {
    rules: [
      // Sample rule
      {
        priority: -1,
        select: {
          type: vizId,
          application: "pentaho-analyzer"
        },
        apply: {
          application: {
            keepLevelOnDrilldown: false
          }
        }
        // apply: {
        //   props: {
        //     barSize: {defaultValue: 50}
        //   }
        // }
      }
    ]
  };
});
