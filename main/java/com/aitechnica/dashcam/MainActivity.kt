package com.aitechnica.dashcamapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.Surface
import android.view.TextureView
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.aitechnica.dashcamapp.R

class MainActivity : ComponentActivity() {

    private val TAG = "Main Activity"

    private lateinit var cameraManager: CameraManager
    private lateinit var textureView: TextureView
    private lateinit var handler: Handler
    private lateinit var handlerThread: HandlerThread
    private var cameraDevice: CameraDevice? = null
    private var cameraCaptureSession: CameraCaptureSession? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)

        textureView = findViewById(R.id.textureView)
        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        handlerThread = HandlerThread("videoThread")
        handlerThread.start()
        handler = Handler(handlerThread.looper)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            openCamera()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_REQUEST_CODE)
        }

        textureView.surfaceTextureListener = object: TextureView.SurfaceTextureListener{
            override fun onSurfaceTextureAvailable(
                surface: SurfaceTexture,
                width: Int,
                height: Int
            ) {
                if (cameraDevice != null) {
                    startCameraPreview()
                } else {
                    openCamera()
                }
            }

            override fun onSurfaceTextureSizeChanged(
                surface: SurfaceTexture,
                width: Int,
                height: Int
            ) {
                TODO("Not yet implemented")
            }

            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                closeCamera()
                return true
            }

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
                Log.d(TAG, "onSurfaceTextureUpdated: Not yet implemented")
            }

        }
    }

    override fun onStop() {
        super.onStop()
        closeCamera()
        handlerThread.quitSafely()
    }

    override fun onDestroy() {
        super.onDestroy()
        closeCamera()
        handlerThread.quitSafely()
    }

    override fun onPause() {
        super.onPause()
        closeCamera()
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            openCamera()
        } else {
            Log.e(TAG, "Camera permission denied")
        }
    }

    @SuppressLint("MissingPermission")
    private fun openCamera() {
        try {
            val cameraIdList = cameraManager.cameraIdList
            if (cameraIdList.isEmpty()) {
                Log.e(TAG, "No camera available")
                return
            }

            var backCameraId: String? = null
            for (cameraId in cameraIdList) {
                val cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId)
                val lensFacing = cameraCharacteristics.get(CameraCharacteristics.LENS_FACING)
                if (lensFacing == CameraCharacteristics.LENS_FACING_BACK) {
                    backCameraId = cameraId
                    Log.d(TAG, "Back camera found: $cameraId")
                    break
                }
            }

            if (backCameraId == null) {
                Log.e(TAG, "Back camera not found!")
                return
            }

            val stateCallback = object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    this@MainActivity.cameraDevice = camera
                    startCameraPreview()
                }

                override fun onDisconnected(camera: CameraDevice) {
                    cameraDevice?.close()
                    cameraDevice = null
                    Log.d(TAG, "Camera disconnected: ${camera.id}")
                }

                override fun onError(camera: CameraDevice, error: Int) {
                    cameraDevice?.close()
                    cameraDevice = null
                    Log.e(TAG, "Failed to open camera: $error")
                }
            }


            cameraManager.openCamera(backCameraId, stateCallback, handler)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to open camera: ${e.message}")
        }
    }

    private fun closeCamera() {
        cameraCaptureSession?.close()
        cameraCaptureSession = null
        cameraDevice?.close()
        cameraDevice = null
    }

    private fun startCameraPreview() {
        try {
            val surfaceTexture = textureView.surfaceTexture
            if (surfaceTexture != null) {
                surfaceTexture.setDefaultBufferSize(textureView.width, textureView.height)
                val surface = Surface(surfaceTexture)

                cameraDevice?.let { device ->
                    val captureRequestBuilder = device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                    captureRequestBuilder.addTarget(surface)

                    device.createCaptureSession(listOf(surface), object: CameraCaptureSession.StateCallback(){
                        override fun onConfigured(session: CameraCaptureSession) {
                            cameraCaptureSession = session
                            val captureRequest = captureRequestBuilder.build()
                            cameraCaptureSession?.setRepeatingRequest(captureRequest, null, handler)
                        }

                        override fun onConfigureFailed(session: CameraCaptureSession) {
                            Log.e(TAG, "Failed to configure camera capture session")
                        }

                    }, handler)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to setup camera preview: ${e.message}")
        }
    }

    companion object {
        private const val CAMERA_PERMISSION_REQUEST_CODE = 100
    }
}
