package com.aitechnica.dashcam

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DashcamApp()
        }
        checkPermissions()
    }

    private fun checkPermissions() {
        val cameraPermission = Manifest.permission.CAMERA
        val storagePermission = Manifest.permission.WRITE_EXTERNAL_STORAGE

        val hasCameraPermission = ContextCompat.checkSelfPermission(this, cameraPermission)
        val hasStoragePermission = ContextCompat.checkSelfPermission(this, storagePermission)

        val permissions = mutableListOf<String>()
        if (hasCameraPermission != PackageManager.PERMISSION_GRANTED) {
            permissions.add(cameraPermission)
        }
        if (hasStoragePermission != PackageManager.PERMISSION_GRANTED) {
            permissions.add(storagePermission)
        }

        if (permissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissions.toTypedArray(), 101)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        grantResults.forEach {
            if (it != PackageManager.PERMISSION_GRANTED) {
                checkPermissions()
            }
        }
    }

    @Composable
    private fun Greeting(message: String) {
        Text(text = message)
    }

    @Composable
    fun DashcamApp() {
        Surface(modifier = Modifier.fillMaxSize()) {
            Greeting(message = "Hello, Dashcam")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    MainActivity().DashcamApp()
}
