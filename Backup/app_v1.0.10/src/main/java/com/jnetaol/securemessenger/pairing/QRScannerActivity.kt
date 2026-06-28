package com.jnetaol.securemessenger.pairing

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Size
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.jnetaol.securemessenger.logger.DebugLogger
import java.util.concurrent.Executors

class QRScannerActivity : ComponentActivity() {

    private lateinit var previewView: PreviewView
    private var scanned = false
    private val cameraExecutor = Executors.newSingleThreadExecutor()
    private lateinit var requestPermissionLauncher: androidx.activity.result.ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DebugLogger.d("QRScannerActivity", "onCreate", "SM-QRS-001")

        requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) {
                startCamera()
            } else {
                DebugLogger.w("QRScannerActivity", "permission", "SM-QRS-WARN-001", "Camera permission denied")
                Toast.makeText(this, "Camera permission required for QR scanning", Toast.LENGTH_LONG).show()
                finishWithResult(null)
            }
        }

        previewView = PreviewView(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
        setContentView(previewView)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder()
                    .setTargetResolution(Size(1280, 720))
                    .build()
                preview.setSurfaceProvider(previewView.surfaceProvider)

                val imageAnalysis = ImageAnalysis.Builder()
                    .setTargetResolution(Size(1280, 720))
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                    if (!scanned) {
                        scanQRCode(imageProxy)
                    } else {
                        imageProxy.close()
                    }
                }

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageAnalysis
                )
                DebugLogger.i("QRScannerActivity", "startCamera", "SM-QRS-002", "Camera started")
            } catch (e: Exception) {
                DebugLogger.e("QRScannerActivity", "startCamera", "SM-QRS-ERR-001", "Camera start failed", e)
                Toast.makeText(this, "Failed to start camera: ${e.message}", Toast.LENGTH_LONG).show()
                finishWithResult(null)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun scanQRCode(imageProxy: ImageProxy) {
        try {
            val mediaImage = imageProxy.image
            if (mediaImage != null) {
                val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                val scanner = BarcodeScanning.getClient()

                scanner.process(image)
                    .addOnSuccessListener { barcodes ->
                        for (barcode in barcodes) {
                            if (barcode.valueType == Barcode.TYPE_TEXT && !scanned) {
                                val rawValue = barcode.rawValue
                                if (rawValue != null && rawValue.startsWith("SM|")) {
                                    scanned = true
                                    DebugLogger.i("QRScannerActivity", "scanQRCode", "SM-QRS-003", "QR scanned: $rawValue")
                                    runOnUiThread {
                                        Toast.makeText(this, "QR Code detected!", Toast.LENGTH_SHORT).show()
                                    }
                                    finishWithResult(rawValue)
                                    return@addOnSuccessListener
                                }
                            }
                        }
                    }
                    .addOnFailureListener { e ->
                        DebugLogger.w("QRScannerActivity", "scanQRCode", "SM-QRS-WARN-002", "Scan error: ${e.message}")
                    }
                    .addOnCompleteListener {
                        imageProxy.close()
                    }
            } else {
                imageProxy.close()
            }
        } catch (e: Exception) {
            DebugLogger.e("QRScannerActivity", "scanQRCode", "SM-QRS-ERR-002", "Scan processing failed", e)
            imageProxy.close()
        }
    }

    private fun finishWithResult(result: String?) {
        val intent = android.content.Intent().apply {
            putExtra("QR_RESULT", result ?: "")
        }
        setResult(if (result != null) RESULT_OK else RESULT_CANCELED, intent)
        finish()
    }

    override fun onDestroy() {
        DebugLogger.d("QRScannerActivity", "onDestroy", "SM-QRS-004")
        cameraExecutor.shutdown()
        super.onDestroy()
    }
}
