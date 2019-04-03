---
layout: post
title: GeoServer and Pentaho C-Tools
summary: 
date: 2015-07-09
categories: CTools GIS Spatial-analysis
tags: QGIS PostGIS GeoServer
published: false
---

# Using GeoServer With Pentaho C-Tools Dashboards

After some searching I found [this forum post](http://forums.pentaho.com/archive/index.php/t-151084.html) ... I just copied it here for convenience:

Finally I can use GeoServer as MapEngine.

Here's what I do:

Edit `system/pentaho-cdf-dd/resource/resources/custom/components/NewMapComponent.js` and then find the code like this :

```javascript
var layer = new OpenLayers.Layer.TMS( "OpenStreetMap","http://tile.openstreetmap.org/",
{
type: 'png', getURL: osm_getTileURL,
transparent: 'true',
displayOutsideMaxExtent: true}
);
```

Change to :

```javascript
var layer = new OpenLayers.Layer.WMS.Untiled("GeoServer WMS", "http://<your GeoServer Host>/geoserver/<your layer>/wms", 
{layers: '<your layer>'}
);
```

Ok, no back to my writing: The only concern I have about this approach is that data security is not handled directly by Pentaho.

# Other Interesting Project

- [GeoGig](http://geogig.org): git like tool for geospatial data management.
- [PGRestAPI (a.k.a. Chubbs Spatial Server)](https://github.com/spatialdev/PGRestAPI): Node.js REST API for PostgreSQL Spatial Tables
 



Video tutorial series by Benicio Junior (in Portuguese): 

1. [Tutorial GeoServer - PostGIS](https://www.youtube.com/watch?v=1CXhCs6GU8M)
2. [Tutorial GeoServer - SQL](https://www.youtube.com/watch?v=RH0qBGLqXdU)
3. [Tutorial GeoServer - Grupo de Camadas](https://www.youtube.com/watch?v=YO2Y--rC7MY)
4. [Tutorial GeoServer - Estilos](https://www.youtube.com/watch?v=kQXW4dSA9lQ)

[OPEN] still to read:
http://blog.georepublic.info/2012/leaflet-example-with-wfs-t/
the last part is interesting:
http://wiki.ieee-earth.org/index.php?title=Documents/GEOSS_Tutorials/GEOSS_Provider_Tutorials/Web_Feature_Service_Tutorial_for_GEOSS_Providers/Section_4:_Provisioning%2F%2FUsing_the_service_or_application/Section_4.3:_Detail_Steps_for_Use_Cases/Section_4.3.2_Offering_data_with_geoserver