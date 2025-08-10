package com.daveswaves.qrapp

import android.view.View
import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.google.common.util.concurrent.ListenableFuture
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import androidx.camera.view.PreviewView

class MainActivity : AppCompatActivity() {

    private lateinit var cameraExecutor: ExecutorService
    private lateinit var scanButton: Button
    private lateinit var makeButton: Button
    // private lateinit var qrPreview: ImageView
    private lateinit var ssidText: TextView
    private lateinit var passwordText: TextView
    private lateinit var encryptionText: TextView
    private lateinit var connectButton: Button
    private lateinit var previewView: PreviewView

    private var scannedSSID: String? = null
    private var scannedPassword: String? = null
    private var scannedEncryption: String? = null

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                startCamera()
            } else {
                Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        scanButton = findViewById(R.id.scanButton)
        makeButton = findViewById(R.id.makeButton)
        previewView = findViewById(R.id.previewView)
        ssidText = findViewById(R.id.ssidText)
        passwordText = findViewById(R.id.passwordText)
        encryptionText = findViewById(R.id.encryptionText)
        connectButton = findViewById(R.id.connectButton)

        scanButton.setOnClickListener {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        // connectButton.setOnClickListener {
        //     if (scannedSSID != null && scannedPassword != null) {
        //         connectToWifi(scannedSSID!!, scannedPassword!!, scannedEncryption)
        //     } else {
        //         Toast.makeText(this, "No Wi-Fi details found", Toast.LENGTH_SHORT).show()
        //     }
        // }

        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = androidx.camera.core.Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

            val imageAnalysis = ImageAnalysis.Builder()
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor) { imageProxy ->
                        processImageProxy(imageProxy)
                    }
                }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalysis
                )
            } catch (exc: Exception) {
                exc.printStackTrace()
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun processImageProxy(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            val scanner = BarcodeScanning.getClient()

            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    for (barcode in barcodes) {
                        when (barcode.valueType) {
                            Barcode.TYPE_WIFI -> {
                                scannedSSID = barcode.wifi?.ssid
                                scannedPassword = barcode.wifi?.password
                                scannedEncryption = when (barcode.wifi?.encryptionType) {
                                    Barcode.WiFi.TYPE_WPA -> "WPA"
                                    Barcode.WiFi.TYPE_WEP -> "WEP"
                                    else -> "Open"
                                }
                                showWifiDetails()
                            }

                            Barcode.TYPE_URL -> {
                                val url = barcode.url?.url
                                showUrlDetails(url)
                            }
                        }
                    }
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        }
    }

    private fun showWifiDetails() {
        runOnUiThread {
            // Reset first
            passwordText.visibility = View.VISIBLE
            encryptionText.visibility = View.VISIBLE
            
            ssidText.text = "SSID: $scannedSSID"
            passwordText.text = "Password: $scannedPassword"
            encryptionText.text = "Encryption: $scannedEncryption"

            ssidText.visibility = View.VISIBLE
            passwordText.visibility = View.VISIBLE
            encryptionText.visibility = View.VISIBLE

            connectButton.text = "Tap to connect"
            connectButton.visibility = if (!scannedSSID.isNullOrBlank()
                && !scannedPassword.isNullOrBlank()
                && !scannedEncryption.isNullOrBlank()
            ) View.VISIBLE else View.GONE

            connectButton.setOnClickListener {
                connectToWifi(scannedSSID!!, scannedPassword!!, scannedEncryption)
                resetUI()
            }
        }
    }

    private fun showUrlDetails(url: String?) {
        runOnUiThread {
            passwordText.visibility = View.GONE
            encryptionText.visibility = View.GONE
            
            ssidText.text = "URL: $url"

            connectButton.text = "Open Link"
            connectButton.visibility = if (!url.isNullOrBlank()) View.VISIBLE else View.GONE

            connectButton.setOnClickListener {
                val browserIntent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url))
                startActivity(browserIntent)
                resetUI()
            }
        }
    }

    private fun connectToWifi(ssid: String, password: String, _encryption: String?) {
        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // On Android 10+, we must use network suggestion or request
            Toast.makeText(this, "Redirecting to Wi-Fi settings...", Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
        } else {
            val wifiConfig = WifiConfiguration().apply {
                SSID = "\"$ssid\""
                preSharedKey = "\"$password\""
            }
            val netId = wifiManager.addNetwork(wifiConfig)
            wifiManager.disconnect()
            wifiManager.enableNetwork(netId, true)
            wifiManager.reconnect()
        }
    }

    private fun resetUI() {
        scannedSSID = null
        scannedPassword = null
        scannedEncryption = null

        ssidText.text = ""
        passwordText.text = ""
        encryptionText.text = ""

        connectButton.visibility = View.GONE

        // Ensure these are visible again for Wi-Fi scans
        passwordText.visibility = View.VISIBLE
        encryptionText.visibility = View.VISIBLE
    }


    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}