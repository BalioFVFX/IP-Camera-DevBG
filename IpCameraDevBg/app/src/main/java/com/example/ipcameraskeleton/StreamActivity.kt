package com.example.ipcameraskeleton

import android.annotation.SuppressLint
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.SessionConfiguration
import android.media.ImageReader
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Size
import android.view.SurfaceHolder
import androidx.appcompat.app.AppCompatActivity
import com.example.ipcameraskeleton.databinding.ActivityStreamBinding
import java.util.concurrent.Executors

@SuppressLint("MissingPermission")
class StreamActivity : AppCompatActivity() {

    private val binding: ActivityStreamBinding by lazy {
        ActivityStreamBinding.inflate(layoutInflater)
    }

    private val handlerThread = HandlerThread("")
    private lateinit var handler: Handler
    private lateinit var imageReader: ImageReader
    private lateinit var cameraManager: CameraManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        cameraManager = getSystemService(CameraManager::class.java)


        val cameraId = getBackCameraId()
        val cameraRes = getCameraRes(cameraId)

        handlerThread.start()
        handler = Handler(handlerThread.looper)

        val videoServer = VideoServer()
        videoServer.start()

        imageReader = ImageReader.newInstance(cameraRes.width, cameraRes.height, ImageFormat.JPEG, 3)

        imageReader.setOnImageAvailableListener(object: ImageReader.OnImageAvailableListener {
            override fun onImageAvailable(reader: ImageReader) {
                val image = reader.acquireNextImage()

                val buffer = image.planes[0].buffer
                val bytes = ByteArray(buffer.remaining())

                var i = 0
                while (buffer.hasRemaining()) {
                    bytes[i++] = buffer.get()
                }

                image.close()

                videoServer.send(bytes)
            }
        },handler)

        binding.surfaceView.holder.addCallback(object: SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                holder.setFixedSize(cameraRes.width, cameraRes.height)

                cameraManager.openCamera(cameraId, object: CameraDevice.StateCallback() {
                    override fun onOpened(camera: CameraDevice) {
                        val builder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)

                        builder.addTarget(holder.surface)
                        builder.addTarget(imageReader.surface)
                        builder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_TORCH)
                        builder.set(CaptureRequest.JPEG_QUALITY, 50)

                        val captureRequest = builder.build()

                        val captureCallback = object: CameraCaptureSession.StateCallback() {
                            override fun onConfigured(session: CameraCaptureSession) {
                                session.setRepeatingRequest(
                                    captureRequest,
                                    null,
                                    null)
                            }

                            override fun onConfigureFailed(session: CameraCaptureSession) {

                            }

                        }

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            val sessionConfiguration = SessionConfiguration(
                                SessionConfiguration.SESSION_REGULAR,
                                listOf(OutputConfiguration(holder.surface), OutputConfiguration(imageReader.surface)),
                                Executors.newSingleThreadExecutor(),
                                captureCallback
                            )

                            camera.createCaptureSession(sessionConfiguration)
                        } else {
                            camera.createCaptureSession(
                                listOf(holder.surface, imageReader.surface),
                                captureCallback,
                                null)
                        }
                    }

                    override fun onDisconnected(camera: CameraDevice) {

                    }

                    override fun onError(camera: CameraDevice, error: Int) {

                    }

                }, null)
            }

            override fun surfaceChanged(
                holder: SurfaceHolder,
                format: Int,
                width: Int,
                height: Int
            ) {

            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {

            }

        })
    }


    private fun getBackCameraId() : String {
        val cameraIds = cameraManager.cameraIdList

        for (cameraId in cameraIds) {
            val characteristics = cameraManager.getCameraCharacteristics(cameraId)

            val facing = characteristics.get(CameraCharacteristics.LENS_FACING)

            if (facing == CameraCharacteristics.LENS_FACING_BACK) {
                return cameraId
            }
        }

        throw IllegalStateException("Camera doesn't have back camera")
    }

    private fun getCameraRes(cameraId: String) : Size {
        val streamConfigMap = cameraManager.getCameraCharacteristics(cameraId)
            .get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)

        val sizes = streamConfigMap!!.getOutputSizes(ImageFormat.JPEG)

        for (size in sizes) {
            if (size.width == 1280 && size.height == 720) {
                return size
            }
        }

        throw IllegalStateException("Camera doesn't support 720p")
    }
}