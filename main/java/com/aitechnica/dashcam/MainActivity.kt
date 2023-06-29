package com.aitechnica.dashcam

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.view.TextureView
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.view.Surface

class MainActivity : ComponentActivity() {

    lateinit var cameraManager: CameraManager
    lateinit var textureView: TextureView
    lateinit var cameraCaptureSession: CameraCaptureSession
    lateinit var cameraDevice: CameraDevice
    lateinit var captureRequest: CaptureRequest
    lateinit var handler: Handler
    lateinit var handlerThread: HandlerThread
    lateinit var captureRequestBuilder: CaptureRequest.Builder

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        checkPermissions()

        textureView = findViewById(R.id.textureview)
        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        handlerThread = HandlerThread("videoThread")
        handlerThread.start()
        handler = Handler(handlerThread.looper)

        textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                surface?.let { texture ->
                    openCamera()
                }
            }

            override fun onSurfaceTextureSizeChanged(
                surface: SurfaceTexture,
                width: Int,
                height: Int
            ) {
                // Handle texture size changed event if needed
            }

            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                // Clean up resources when surface texture is destroyed if needed
                return true
            }

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
                // Handle surface texture updated event if needed
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun openCamera() {
        cameraManager.openCamera(cameraManager.cameraIdList[0], object : CameraDevice.StateCallback() {
            override fun onOpened(cameraDevice: CameraDevice) {
                this@MainActivity.cameraDevice = cameraDevice
                captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                val surfaceTexture = textureView.surfaceTexture
                if (surfaceTexture != null) {
                    surfaceTexture.setDefaultBufferSize(textureView.width, textureView.height)
                    val surface = Surface(surfaceTexture)
                    captureRequestBuilder.addTarget(surface)

                    cameraDevice.createCaptureSession(
                        listOf(surface),
                        object : CameraCaptureSession.StateCallback() {
                            override fun onConfigured(session: CameraCaptureSession) {
                                cameraCaptureSession = session
                                captureRequest = captureRequestBuilder.build()
                                cameraCaptureSession.setRepeatingRequest(captureRequest, null, handler)
                            }

                            override fun onConfigureFailed(session: CameraCaptureSession) {
                                // Handle configuration failure if needed
                            }
                        },
                        handler
                    )
                }
            }

            override fun onDisconnected(cameraDevice: CameraDevice) {
                cameraDevice.close()
            }

            override fun onError(cameraDevice: CameraDevice, error: Int) {
                cameraDevice.close()
            }
        }, handler)
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


}

