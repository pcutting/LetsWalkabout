Sandbox project for modeling concepts.

UX purchased: https://ui8.net/royalz-store/products/deleted-611ca51f04f024004251da7b
Reference to Figma: https://www.figma.com/file/5QQ87ZKRHDXVEixcPwp2Ju/Lets-Walkabout---(Dark-Version)?node-id=0%3A1


Description: Using mapbox, create an application for tracking a walk.

Requirements

1) The user will tap to start a run in the app. Then they will run any distance. When they are done 
   with the run they will open the app and tap stop run. You will show them a map with a line 
   showing the route they just ran.
2) You must also show the distance they ran.
3) Have an option to show distance in miles or kilometers
4) Have a share button that takes a screenshot of your route via SMS.
5) Post screenshots of your projects here in the discussions Note: you will need to research how to
   track the users location using Mapbox
6 [Extra credit]) Instead of sending an SMS image of your route, you will send an app url. If 
   another user taps that link, it opens that same app on their phone, then loads the route into 
   their map

* Example Design You can purchase the full UI pack here: https://ui8.net/products/keira-ios-ui-kit


References: 
Style Data Listeners for geoJson: https://github.com/mapbox/mapbox-maps-android/pull/718/files


In main Activity I was able to with the following code to draw a predefined path.  But I wasn't able
to dynamically update it:

```
//        mapView.getMapboxMap().getStyle { style ->
//
//            val testPath = arrayListOf<Point>(
//                Point.fromLngLat(23.3129909, 42.66594740),
//                Point.fromLngLat(23.3129909, 42.6659474),
//                Point.fromLngLat(23.312228430009565, 42.66589287000068),
//                Point.fromLngLat(23.3117390595374, 42.665942498188734),
//                Point.fromLngLat(23.1953, 42.6377,),
//                Point.fromLngLat(23.310123353011488, 42.66610732903864),
//                Point.fromLngLat(23.309381942956698, 42.66618467417269),
//                Point.fromLngLat(23.308231339373087, 42.66630470705846),
//                Point.fromLngLat(23.30728735837411, 42.66640318473142),
//                Point.fromLngLat(23.30696417501965, 42.66644107083942),
//                Point.fromLngLat(23.306600593736853, 42.66648369271197),
//                Point.fromLngLat(23.4453, 42.7830),
//                Point.fromLngLat(23.306625838599505, 42.66651449489985),
//                Point.fromLngLat(23.30665860999862, 42.666524314999585),
//                Point.fromLngLat(23.306691497203147, 42.66653416980095),
//                Point.fromLngLat(23.3067223, 42.6665434)
//            )

//            val lineString = LineString.fromLngLats(testPath)
//
//            val feature = Feature.fromGeometry(lineString)
//            val geoJsonSource = GeoJsonSource.Builder("geoJson_source").feature(feature).build()
//
//            style.addSource(geoJsonSource)
//            style.addLayer(LineLayer("geoJson_source", "geoJson_source"))
//        }
```