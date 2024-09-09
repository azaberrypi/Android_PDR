package com.example.pdr_aza

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.pdr_aza.ui.theme.Pdr_azaTheme
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import android.content.Intent
import android.os.Build
import android.provider.Settings
import java.text.SimpleDateFormat
import java.util.Date

class MainActivity : ComponentActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var lineAcceleration: Sensor? = null
    private var gyroscope: Sensor? = null

    // ジャイロスコープとリニア加速度センサーの値を保存するリスト
    private val gyroData = mutableListOf<String>()
    private val linAccData = mutableListOf<String>()

    // 計測中かどうかを管理
    private var isCollectingData by mutableStateOf(false)

    // センサーの値
    private var _gyroZ by mutableStateOf(0f)
    private var _gyroTimestamp by mutableStateOf(0L)
    private var _gyroSamplingRate by mutableStateOf(0f)
    private var lastGyroTimestamp: Long = 0L

    private var _linAccX by mutableStateOf(0f)
    private var _linAccY by mutableStateOf(0f)
    private var _linAccZ by mutableStateOf(0f)
    private var _linAccXTimestamp by mutableStateOf(0L)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Android 10 (API 29)以上のデバイスで外部ストレージ管理権限をリクエスト
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                startActivity(intent)
            }
        }

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        lineAcceleration = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)

        setContent {
            Pdr_azaTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(modifier = Modifier
                        .padding(innerPadding)
                        .padding(16.dp)) {
                        SensorDisplay(
                            gyroZ = _gyroZ, gyroTimestamp = _gyroTimestamp, gyroRate = _gyroSamplingRate,
                            linAccX = _linAccX, linAccY = _linAccY, linAccZ = _linAccZ, linAccTimestamp = _linAccXTimestamp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { toggleDataCollection() }) {
                            Text(if (isCollectingData) "Stop" else "Start")
                        }
                        if (!isCollectingData) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { saveCsvFiles() }) {
                                Text("Download CSV")
                            }
                        }
                    }
                }
            }
        }
    }

    private fun toggleDataCollection() {
        if (isCollectingData) {
            // データ収集停止
            sensorManager.unregisterListener(this)
        } else {
            // データリストをクリア（新しいデータ収集を開始）
            gyroData.clear()
            linAccData.clear()

            // データ収集開始
            gyroscope?.let {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
            }
            lineAcceleration?.let {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
            }
        }
        isCollectingData = !isCollectingData
    }

    private fun saveCsvFiles() {
        val externalStorageState = Environment.getExternalStorageState()

        if (externalStorageState == Environment.MEDIA_MOUNTED) {
            try {
                // 現在の日時を取得して "MMDDhhmmss" の形式にフォーマット
                val dateFormat = SimpleDateFormat("MMddHHmmss", java.util.Locale.getDefault())
                val currentTime = dateFormat.format(Date())

                // ダウンロードディレクトリにファイルを保存
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)

                // GyroscopeデータのCSV生成
                val gyroFile = File(downloadsDir, "gyro_data_$currentTime.csv")
                FileOutputStream(gyroFile).use { fos ->
                    fos.write("gyroZ,gyroTimestamp,gyroSamplingRate\n".toByteArray())
                    gyroData.forEach { fos.write("$it\n".toByteArray()) }
                }

                // Linear AccelerationデータのCSV生成
                val linAccFile = File(downloadsDir, "lin_acc_data_$currentTime.csv")
                FileOutputStream(linAccFile).use { fos ->
                    fos.write("linAccX,linAccY,linAccZ,linAccTimestamp\n".toByteArray())
                    linAccData.forEach { fos.write("$it\n".toByteArray()) }
                }

                // 保存成功メッセージ
                Toast.makeText(this, "CSV files saved to Downloads", Toast.LENGTH_LONG).show()

                // 保存後にデータをクリアして、次回の収集準備
                gyroData.clear()
                linAccData.clear()

            } catch (e: IOException) {
                Toast.makeText(this, "Error saving CSV files: ${e.message}", Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(this, "External storage not available", Toast.LENGTH_LONG).show()
        }
    }


    override fun onSensorChanged(event: SensorEvent) {
        if (isCollectingData) {
            when (event.sensor.type) {
                Sensor.TYPE_GYROSCOPE -> {
                    _gyroZ = event.values[2]
                    _gyroTimestamp = event.timestamp

                    if (lastGyroTimestamp != 0L) {
                        val deltaTime = (event.timestamp - lastGyroTimestamp) / 1_000_000_000f
                        _gyroSamplingRate = 1 / deltaTime
                    }
                    lastGyroTimestamp = event.timestamp

                    // Gyroscopeデータの保存
                    gyroData.add("%.6f,%d,%.2f".format(_gyroZ, _gyroTimestamp, _gyroSamplingRate))
                }

                Sensor.TYPE_LINEAR_ACCELERATION -> {
                    _linAccX = event.values[0]
                    _linAccY = event.values[1]
                    _linAccZ = event.values[2]
                    _linAccXTimestamp = event.timestamp

                    // Linear Accelerationデータの保存                    linAccData.add("%.6f,%.6f,%.6f,%d".format(_linAccX, _linAccY, _linAccZ, _linAccXTimestamp))
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // not used
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
    }
}

@Composable
fun SensorDisplay(
    gyroZ: Float, gyroTimestamp: Long, gyroRate: Float,
    linAccX: Float, linAccY: Float, linAccZ: Float, linAccTimestamp: Long
) {
    Text(
        text = """
            TYPE_GYROSCOPE:
            Z = %.6f
            Timestamp: %d
            Sampling Rate: %.2f Hz
            
            TYPE_LINEAR_ACCELERATION:
            X = %.6f, Y = %.6f, Z = %.6f
            Timestamp: %d
        """.trimIndent().format(gyroZ, gyroTimestamp, gyroRate, linAccX, linAccY, linAccZ, linAccTimestamp)
    )
}

@Preview(showBackground = true)
@Composable
fun SensorDisplayPreview() {
    Pdr_azaTheme {
        SensorDisplay(
            gyroZ = 0f, gyroTimestamp = 0L, gyroRate = 0f,
            linAccX = 0f, linAccY = 0f, linAccZ = 0f, linAccTimestamp = 0L
        )
    }
}
