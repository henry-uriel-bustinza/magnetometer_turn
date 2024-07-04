package com.example.triangulo_giro

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.triangulo_giro.ui.theme.PolygonOrientationTheme

class MainActivity : ComponentActivity(), SensorEventListener {
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var magnetometer: Sensor? = null

    private val gravity = FloatArray(3)
    private val geomagnetic = FloatArray(3)

    private var azimuth by mutableStateOf(0f)
    private var isFixed by mutableStateOf(false)
    private var fixedAzimuth by mutableStateOf(0f)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        setContent {
            PolygonOrientationTheme {
                PolygonScreen(azimuth, fixedAzimuth, isFixed, { toggleFix() })
            }
        }
    }

    private fun toggleFix() {
        if (isFixed) {
            isFixed = false
        } else {
            fixedAzimuth = azimuth
            isFixed = true
        }
    }

    override fun onResume() {
        super.onResume()
        accelerometer?.also { sensor ->
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI)
        }
        magnetometer?.also { sensor ->
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            gravity[0] = event.values[0]
            gravity[1] = event.values[1]
            gravity[2] = event.values[2]
        } else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
            geomagnetic[0] = event.values[0]
            geomagnetic[1] = event.values[1]
            geomagnetic[2] = event.values[2]
        }

        val rotationMatrix = FloatArray(9)
        val orientation = FloatArray(3)

        if (SensorManager.getRotationMatrix(rotationMatrix, null, gravity, geomagnetic)) {
            SensorManager.getOrientation(rotationMatrix, orientation)
            azimuth = Math.toDegrees(orientation[2].toDouble()).toFloat() // Usar el eje Z para rotación en el eje Y
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        // Not used
    }
}

@Composable
fun PolygonScreen(azimuth: Float, fixedAzimuth: Float, isFixed: Boolean, onFixToggle: () -> Unit) {
    val rotation = if (isFixed) fixedAzimuth else azimuth

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
    ) {
        Canvas(modifier = Modifier.size(300.dp)) {
            drawPolygon(rotation)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onFixToggle) {
            Text(text = if (isFixed) "Desfijar Rotación" else "Fijar Rotación")
        }
    }
}

fun DrawScope.drawPolygon(rotation: Float) {
    val centerX = size.width / 2
    val centerY = size.height / 2
    val radius = Math.min(centerX, centerY) - 20

    val numOfSides = 3
    val angle = 2 * Math.PI / numOfSides

    val path = androidx.compose.ui.graphics.Path().apply {
        moveTo(center(centerX, centerY, radius, 0.0).x, center(centerX, centerY, radius, 0.0).y)
        for (i in 1 until numOfSides) {
            lineTo(center(centerX, centerY, radius, angle * i).x, center(centerX, centerY, radius, angle * i).y)
        }
        close()
    }

    rotate(degrees = rotation) {
        drawPath(path = path, color = Color.Gray)
    }
}

fun center(centerX: Float, centerY: Float, radius: Float, angle: Double) = androidx.compose.ui.geometry.Offset(
    x = centerX + (radius * Math.cos(angle)).toFloat(),
    y = centerY + (radius * Math.sin(angle)).toFloat()
)

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    PolygonOrientationTheme {
        PolygonScreen(0f, 0f, false, {})
    }
}
