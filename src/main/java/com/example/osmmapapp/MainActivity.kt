package com.example.osmmapapp

    import android.Manifest
    import android.content.pm.PackageManager
    import android.os.Bundle
    import android.os.Environment
    import android.util.Log
    import android.widget.Button
    import android.widget.Toast
    import androidx.activity.result.contract.ActivityResultContracts
    import androidx.appcompat.app.AppCompatActivity
    import androidx.core.app.ActivityCompat
    import androidx.core.content.ContextCompat
    import com.example.osmmapapp.databinding.ActivityMainBinding
    import org.osmdroid.config.Configuration
    import org.osmdroid.tileprovider.tilesource.TileSourceFactory
    import org.osmdroid.util.GeoPoint
    import org.osmdroid.views.overlay.FolderOverlay
    import org.osmdroid.views.overlay.Marker
    import org.osmdroid.views.overlay.Overlay
    import java.io.File
    import java.io.FileInputStream
    import java.io.InputStream
    import org.mapsforge.kml.kml.Kml
    import org.mapsforge.kml.kml.KmlFactory
    import org.mapsforge.kml.kml.KmlParser
    import org.mapsforge.kml.kml.Placemark
    import org.mapsforge.kml.kml.Point
    import org.mapsforge.kml.kml.LineString
    import org.mapsforge.kml.kml.Polygon
    import org.mapsforge.kml.kml.Geometry
    import org.osmdroid.views.overlay.Polyline
    import org.osmdroid.views.overlay.Polygon as OSMPolygon
    import org.osmdroid.views.overlay.OverlayItem
    import org.osmdroid.views.overlay.ItemizedOverlay
    import android.graphics.drawable.Drawable
    import android.content.Context
    import com.github.geoxol.shp.ShapefileReader
    import com.github.geoxol.shp.Shapefile
    import com.github.geoxol.shp.Shape
    import com.github.geoxol.shp.Point as SHPPoint
    import com.github.geoxol.shp.Polyline as SHPPolyline
    import com.github.geoxol.shp.Polygon as SHPPolygon
    import org.osmdroid.views.overlay.OverlayItem
    import org.osmdroid.views.overlay.ItemizedOverlay
    import android.graphics.drawable.Drawable
    import android.content.Context
    import org.osmdroid.views.overlay.ScaleBarOverlay
    import org.osmdroid.views.CustomZoomButtonsController
    import org.osmdroid.views.MapView
    import org.osmdroid.views.overlay.MinimapOverlay
    import org.osmdroid.views.overlay.compass.CompassOverlay
    import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider
    import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
    import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
    import java.util.ArrayList

    class MainActivity : AppCompatActivity() {

        private lateinit var binding: ActivityMainBinding
        private val REQUEST_PERMISSIONS_REQUEST_CODE = 1
        private val filePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                val filePath = FileUtils.getPathFromUri(this, it)
                filePath?.let {
                    loadFile(File(filePath))
                } ?: run {
                    Toast.makeText(this, "Failed to get file path", Toast.LENGTH_SHORT).show()
                }
            }
        }

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            binding = ActivityMainBinding.inflate(layoutInflater)
            setContentView(binding.root)

            // Configure OSMdroid
            Configuration.getInstance().load(this, getPreferences(MODE_PRIVATE))
            binding.mapView.setTileSource(TileSourceFactory.MAPNIK)
            binding.mapView.setMultiTouchControls(true)
            binding.mapView.controller.setZoom(14.0)
            binding.mapView.controller.setCenter(GeoPoint(37.7749, -122.4194)) // San Francisco

            // Add scale bar
            val scaleBarOverlay = ScaleBarOverlay(binding.mapView)
            scaleBarOverlay.setAlignBottom(true)
            scaleBarOverlay.setAlignRight(true)
            binding.mapView.overlays.add(scaleBarOverlay)

            // Add minimap
            val minimapOverlay = MinimapOverlay(this, binding.mapView.tileRequestCompleteHandler)
            minimapOverlay.setZoomDifference(5)
            binding.mapView.overlays.add(minimapOverlay)

            // Add compass
            val compassOverlay = CompassOverlay(this, InternalCompassOrientationProvider(this), binding.mapView)
            compassOverlay.enableCompass()
            binding.mapView.overlays.add(compassOverlay)

            // Add my location
            val myLocationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(this), binding.mapView)
            myLocationOverlay.enableMyLocation()
            binding.mapView.overlays.add(myLocationOverlay)

            // Hide zoom controls
            binding.mapView.zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)

            // Request permissions
            requestPermissionsIfNecessary(arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ))

            // Load file button
            binding.loadFileButton.setOnClickListener {
                openFilePicker()
            }
        }

        private fun openFilePicker() {
            filePickerLauncher.launch("*/*") // Allow all file types
        }

        private fun loadFile(file: File) {
            val fileExtension = file.extension.lowercase()
            when (fileExtension) {
                "shp" -> loadShp(file)
                "kml", "kmz" -> loadKml(file)
                else -> Toast.makeText(this, "Unsupported file type", Toast.LENGTH_SHORT).show()
            }
        }

        private fun loadShp(file: File) {
            try {
                val shapefileReader = ShapefileReader(file)
                val shapefile = shapefileReader.read()

                shapefile?.let {
                    runOnUiThread {
                        processShpShapes(it)
                    }
                } ?: run {
                    runOnUiThread {
                        Toast.makeText(this, "Failed to read SHP file", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("SHP", "Error loading SHP: ${e.message}", e)
                runOnUiThread {
                    Toast.makeText(this, "Error loading SHP file: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        private fun processShpShapes(shapefile: Shapefile) {
            val overlays = mutableListOf<Overlay>()
            for (shape in shapefile.shapes) {
                when (shape) {
                    is SHPPoint -> {
                        val marker = Marker(binding.mapView)
                        marker.position = GeoPoint(shape.y, shape.x)
                        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        overlays.add(marker)
                    }
                    is SHPPolyline -> {
                        val polyline = Polyline()
                        polyline.color = resources.getColor(android.R.color.holo_blue_dark)
                        polyline.width = 5f
                        for (point in shape.points) {
                            polyline.addPoint(GeoPoint(point.y, point.x))
                        }
                        overlays.add(polyline)
                    }
                    is SHPPolygon -> {
                        val polygon = OSMPolygon()
                        polygon.fillColor = resources.getColor(android.R.color.holo_green_light)
                        polygon.strokeColor = resources.getColor(android.R.color.holo_green_dark)
                        polygon.strokeWidth = 2f
                        for (point in shape.points) {
                            polygon.addPoint(GeoPoint(point.y, point.x))
                        }
                        overlays.add(polygon)
                    }
                }
            }
            runOnUiThread {
                binding.mapView.overlays.addAll(overlays)
                binding.mapView.invalidate() // Refresh the map
            }
        }

        private fun loadKml(file: File) {
            try {
                val inputStream: InputStream = FileInputStream(file)
                val kmlParser = KmlParser(KmlFactory.createKml(), inputStream)
                val kml: Kml = kmlParser.parse()

                kml?.let {
                    runOnUiThread {
                        processKmlFeatures(it)
                    }
                } ?: run {
                    runOnUiThread {
                        Toast.makeText(this, "Failed to parse KML/KMZ file", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("KML", "Error loading KML: ${e.message}", e)
                runOnUiThread {
                    Toast.makeText(this, "Error loading KML/KMZ file: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        private fun processKmlFeatures(kml: Kml) {
            val overlays = mutableListOf<Overlay>()
            kml.feature?.let { feature ->
                when (feature) {
                    is Placemark -> {
                        feature.geometry?.let { geometry ->
                            when (geometry) {
                                is Point -> {
                                    val marker = Marker(binding.mapView)
                                    marker.position = GeoPoint(geometry.latitude, geometry.longitude)
                                    marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                    overlays.add(marker)
                                }
                                is LineString -> {
                                    val polyline = Polyline()
                                    polyline.color = resources.getColor(android.R.color.holo_red_dark)
                                    polyline.width = 5f
                                    geometry.coordinates.forEach { coordinate ->
                                        polyline.addPoint(GeoPoint(coordinate.latitude, coordinate.longitude))
                                    }
                                    overlays.add(polyline)
                                }
                                is Polygon -> {
                                    val polygon = OSMPolygon()
                                    polygon.fillColor = resources.getColor(android.R.color.holo_orange_light)
                                    polygon.strokeColor = resources.getColor(android.R.color.holo_orange_dark)
                                    polygon.strokeWidth = 2f
                                    geometry.outerBoundaryIs?.linearRing?.coordinates?.forEach { coordinate ->
                                        polygon.addPoint(GeoPoint(coordinate.latitude, coordinate.longitude))
                                    }
                                    overlays.add(polygon)
                                }
                            }
                        }
                    }
                }
            }

            runOnUiThread {
                binding.mapView.overlays.addAll(overlays)
                binding.mapView.invalidate() // Refresh the map
            }
        }

        private fun requestPermissionsIfNecessary(permissions: Array<String>) {
            val permissionsToRequest = ArrayList<String>()
            permissions.forEach { permission ->
                if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                    permissionsToRequest.add(permission)
                }
            }
            if (permissionsToRequest.isNotEmpty()) {
                ActivityCompat.requestPermissions(this, permissionsToRequest.toTypedArray(), REQUEST_PERMISSIONS_REQUEST_CODE)
            }
        }

        override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
            if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
                val allGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }
                if (!allGranted) {
                    Toast.makeText(this, "Permissions not granted", Toast.LENGTH_SHORT).show()
                }
            }
        }

        override fun onResume() {
            super.onResume()
            //this will refresh the osmdroid configuration on resuming.
            //if you make changes to the configuration, use
            //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            //Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));
            binding.mapView.onResume() //needed for compass, my location overlays, v6.0.0 and later
        }

        override fun onPause() {
            super.onPause()
            //this will refresh the osmdroid configuration on resuming.
            //if you make changes to the configuration, use
            //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            //Configuration.getInstance().save(this, prefs);
            binding.mapView.onPause()  //needed for compass, my location overlays, v6.0.0 and later
        }
    }
