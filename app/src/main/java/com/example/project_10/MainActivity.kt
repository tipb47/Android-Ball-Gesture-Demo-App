package com.example.project_10

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Geocoder
import android.location.LocationListener
import android.location.LocationManager
import android.location.LocationRequest
import android.os.Bundle
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.project_10.ui.theme.Project_10Theme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.absoluteValue

class MainActivity : ComponentActivity() {
    private val permissionRequestCode = 123 //random

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            Project_10Theme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    //check if location perms exist
                    val hasLocationPermission = ContextCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED

                    if (!hasLocationPermission) {
                        //request permission for location
                        ActivityCompat.requestPermissions(
                            this@MainActivity,
                            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                            permissionRequestCode
                        )
                    } else {
                        //permission already granted
                        MainMenu()
                    }
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == permissionRequestCode) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //permission granted
                setContent {
                    Project_10Theme {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.background
                        ) {
                            MainMenu()
                        }
                    }
                }
            }
        }
    }
}


@SuppressLint("MissingPermission")
@Composable
fun MainMenu() {
    val context = LocalContext.current
    val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    val temperatureSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE)

    //state variables to hold values
    var temperature by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("Your City") }
    var state by remember { mutableStateOf("Your State") }
    var airPressure by remember { mutableStateOf("Your AirPressure") }

    //listen for temp changes
    val temperatureListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            event?.let {
                val newTemperature = "${it.values[0]}"
                CoroutineScope(Dispatchers.Main).launch {
                    temperature = newTemperature
                }
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }


    //listen for location changes
    val locationListener = LocationListener { location -> //update city, state variables
        val geocoder = Geocoder(context, Locale.getDefault())
        val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
        if (addresses != null) {
            if (addresses.isNotEmpty()) {
                city = addresses[0].locality ?: "Unknown"
                state = addresses[0].adminArea ?: "Unknown"
            } else {
                city = "Unknown"
                state = "Unknown"
            }
        }
    }

    //listen for air pressure changes
    val pressureListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            event?.let {
                //update airPressure
                airPressure = "${it.values[0]}"
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }


    DisposableEffect(sensorManager, locationManager) {
        // register listeners for temperature and air pressure
        temperatureSensor?.also { sensor ->
            sensorManager.registerListener(
                temperatureListener,
                sensor,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        }
        val pressureSensor = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)
        pressureSensor?.also { sensor ->
            sensorManager.registerListener(
                pressureListener,
                sensor,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        }

        val permissionCheck = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                0L,
                0f,
                locationListener
            )
        }


        // cleanup function for the DisposableEffect
        onDispose {
            temperatureSensor?.let {
                sensorManager.unregisterListener(temperatureListener)
            }
            pressureSensor?.let {
                sensorManager.unregisterListener(pressureListener)
            }
            locationManager.removeUpdates(locationListener)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Text(
            text = "Sensors Playground",
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            textAlign = TextAlign.Center
        )

        //logged sensors text
        Text(
            text = "Location:",
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
            textAlign = TextAlign.Left
        )

        Text(
            text = "City: $city",
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
            textAlign = TextAlign.Left
        )

        Text(
            text = "State: $state",
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
            textAlign = TextAlign.Left
        )

        Text(
            text = "Temperature: $temperature",
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
            textAlign = TextAlign.Left
        )

        Text(
            text = "Air Pressure: $airPressure",
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
            textAlign = TextAlign.Left
        )

        //box so text can be overlayed over button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(50.dp)
        ) {
            Button(
                onClick = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        detectDragGestures { _, dragAmount ->
                            if (dragAmount.y.absoluteValue > 5f) {
                                //navigate on fling
                                val intent = Intent(context, GestureActivity::class.java)
                                context.startActivity(intent)
                            }
                        }
                    },
            ) {
            }

            Text(
                text = "Gestures Playground",
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(8.dp),
                color = Color.White
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainMenuPreview() {
    Project_10Theme {
        MainMenu()
    }
}