// cd ~/android_apps/qr_app_files/QrApp/
package com.daveswaves.qrapp

import android.graphics.Bitmap
import android.graphics.Color

import android.view.View
import android.view.Gravity
import android.view.inputmethod.InputMethodManager

import android.Manifest

import android.content.Context
import android.content.Intent

import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager

import android.os.Build
import android.os.Bundle

import android.provider.Settings

import android.widget.PopupMenu
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.EditText
import android.widget.Toast

import androidx.activity.result.contract.ActivityResultContracts
//-----------------------
import androidx.appcompat.app.AppCompatActivity
//-----------------------
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
//-----------------------
import androidx.core.content.ContextCompat

import com.google.common.util.concurrent.ListenableFuture
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    // variables that will be initialized later
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var scanButton: Button
    private lateinit var makeButton: Button
    private lateinit var textSsid: TextView
    private lateinit var textPassword: TextView
    private lateinit var textEncryption: TextView
    //-----------------------
    private lateinit var inputA: EditText
    private lateinit var inputB: EditText
    private lateinit var inputC: EditText
    //-----------------------
    private lateinit var connectButton: Button
    private lateinit var generateQrButton: Button
    private lateinit var previewView: PreviewView
    private lateinit var generatedQrImage: ImageView
    //-----------------------
    private var scannedSSID: String? = null
    private var scannedPassword: String? = null
    private var scannedEncryption: String? = null
    //-----------------------
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
        //-----------------------
        previewView = findViewById(R.id.previewView)
        //-----------------------
        textSsid = findViewById(R.id.textSsid)
        textPassword = findViewById(R.id.textPassword)
        textEncryption = findViewById(R.id.textEncryption)
        //-----------------------
        inputA = findViewById(R.id.inputA)
        inputB = findViewById(R.id.inputB)
        inputC = findViewById(R.id.inputC)
        //-----------------------
        connectButton = findViewById(R.id.connectButton)
        generateQrButton = findViewById(R.id.generateQrButton)
        //-----------------------
        generatedQrImage = findViewById(R.id.generatedQrImage)

        scanButton.setOnClickListener {
            visible(View.GONE, inputA, inputB, inputC, connectButton, textSsid, textPassword, textEncryption, generateQrButton, generatedQrImage)
            visible(View.VISIBLE, previewView)
            
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
        makeButton.setOnClickListener { view ->
            visible(View.GONE, previewView, generatedQrImage, textSsid, textPassword, textEncryption, connectButton)
            
            showMakeMenu(view)
        }

        generateQrButton.setOnClickListener {
            val textC = inputC.text.toString()
            // Toast.makeText(this, "textC: $textC", Toast.LENGTH_SHORT).show()

            val hintText = inputC.hint?.toString()
            when (hintText) {
                "Enter Text" -> {
                    generateQrCode(textC)
                }
                "Enter URL" -> {
                    generateQrCode("URL:$textC")
                }
                "Enter Encryption (WPA/WPA2)" -> {
                    val textA = inputA.text.toString()
                    val textB = inputB.text.toString()
                    
                    generateQrCode("WIFI:T:$textA;S:$textB;P:$textC;;")
                }
            }

            visible(View.GONE, generateQrButton, inputA, inputB, inputC)

            hideKeyboard(currentFocus ?: it)
        }

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

                            Barcode.TYPE_TEXT -> {
                                val text = barcode.displayValue
                                showTxtDetails(text)
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
            visible(View.GONE, previewView, inputA, inputB, inputC)
            visible(View.VISIBLE, textSsid, textPassword, textEncryption, connectButton)
            
            textSsid.text = "SSID: $scannedSSID"
            textPassword.text = "Password: $scannedPassword"
            textEncryption.text = "Encryption: $scannedEncryption"

            connectButton.text = "Tap to connect"

            connectButton.setOnClickListener {
                connectToWifi(scannedSSID!!, scannedPassword!!, scannedEncryption)
                resetUI()
            }
        }
    }

    private fun showUrlDetails(url: String?) {
        runOnUiThread {
            visible(View.GONE, previewView, textPassword, textEncryption)
            visible(View.VISIBLE, connectButton, textSsid)
            
            textSsid.text = "URL: $url"

            connectButton.text = "Open Link"

            connectButton.setOnClickListener {
                Toast.makeText(this, "URL scanned: $url", Toast.LENGTH_LONG).show()
                val browserIntent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url))
                startActivity(browserIntent)
                resetUI()
            }
        }
    }

    private fun showTxtDetails(text: String?) {
        runOnUiThread {
            visible(View.GONE, previewView, textPassword, textEncryption)
            visible(View.VISIBLE, connectButton, textSsid)

            textSsid.text = "$text"

            connectButton.text = "OK"

            connectButton.setOnClickListener {
                Toast.makeText(this, "Text scanned: $text", Toast.LENGTH_LONG).show()
                resetUI()
            }
        }
    }

    // Create dropdown menu to select QR type
    private fun showMakeMenu(anchor: View) {
        val popup = PopupMenu(this, anchor)

        // Add menu options manually
        popup.menu.add("WiFi QR")
        popup.menu.add("WiFi QR (Home)")
        popup.menu.add("WiFi QR (Wilmer)")
        popup.menu.add("URL QR")
        popup.menu.add("URL QR (The Guardian)")
        popup.menu.add("Text QR")
        popup.menu.add("Text QR (Hello)")

        popup.setOnMenuItemClickListener { item ->
            when (item.title.toString()) {
                "WiFi QR" -> {
                    showInputFields("wifi")
                }
                "WiFi QR (Home)" -> {
                    showInputFields("wifi", "Home")
                }
                "WiFi QR (Wilmer)" -> {
                    showInputFields("wifi", "Wilmer")
                }
                "URL QR" -> {
                    showInputFields("url")
                }
                "URL QR (The Guardian)" -> {
                    showInputFields("url", "guardian")
                }
                "Text QR" -> {
                    showInputFields("text")
                }
                "Text QR (Hello)" -> {
                    showInputFields("text", "hello")
                }
            }
            true
        }

        // Align PopupMenu menu below Make
        try {
            val method = popup.javaClass.getDeclaredMethod("setGravity", Int::class.javaPrimitiveType)
            method.isAccessible = true
            method.invoke(popup, Gravity.BOTTOM)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        popup.show()
    }

    // Display WiFi text fields
    private fun showInputFields(type: String, opt: String? = null) {
        runOnUiThread {
            visible(View.GONE, textSsid, textPassword, textEncryption, connectButton, inputA, inputB)
            visible(View.VISIBLE, inputC, generateQrButton)

            // Show input EditTexts
            if ("wifi" == type) {
                visible(View.VISIBLE, inputA, inputB)
            }

            // Clear old text
            inputA.text.clear()
            inputB.text.clear()
            inputC.text.clear()

            if ("wifi" == type) {
                // Prepopulate
                if (opt == "Home") {
                    inputA.setText("BTWholeHome-2MH")
                    inputB.setText("9GdNR9iMwrFx")
                }
                else if (opt == "Wilmer") {
                    inputA.setText("Wilmer WiFi")
                    inputB.setText("Hol3Farm!")
                }
                else {
                    focusEditText(inputA)
                }

                inputC.setHint("Enter Encryption (WPA/WPA2)")
                inputC.setText("WPA")
            }
            else if ("text" == type) {
                if (opt == "hello") {
                    inputC.setText("Hello")
                }
                else {
                    focusEditText(inputC)
                }
                inputC.setHint("Enter Text")
            }
            else if ("url" == type) {
                if (opt == "guardian") {
                    inputC.setText("https://www.theguardian.com/uk")
                }
                else {
                    focusEditText(inputC)
                }
                inputC.setHint("Enter URL")
            }
        }
    }

    private fun generateQrCode(data: String) {
        val writer = com.google.zxing.qrcode.QRCodeWriter()
        val bitMatrix = writer.encode(data, com.google.zxing.BarcodeFormat.QR_CODE, 800, 800)

        val width = bitMatrix.width
        val height = bitMatrix.height
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)

        for (x in 0 until width) {
            for (y in 0 until height) {
                bmp.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
            }
        }

        runOnUiThread {
            visible(View.GONE, previewView)
            visible(View.VISIBLE, generatedQrImage)
            
            generatedQrImage.setImageBitmap(bmp)
        }
    }

    // Automatically gain focus and display soft keyboard
    private fun focusEditText(editText: EditText) {
        editText.requestFocus()

        editText.post {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
            imm.showSoftInput(editText, InputMethodManager.SHOW_FORCED)
        }
    }

    // Automatically hide soft keyboard
    private fun hideKeyboard(view: View) {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
        view.clearFocus()
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

        textSsid.text = ""
        textPassword.text = ""
        textEncryption.text = ""

        visible(View.GONE, connectButton)
        visible(View.VISIBLE, textPassword, textEncryption)
    }

    private fun visible(visibility: Int, vararg views: View) {
        views.forEach { it.visibility = visibility }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}