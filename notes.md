
# Notes

### Example of ChatGPT having no concept that there might be a simpler way: 

```kotlin
// Too much unnecessary code
generateQrButton.setOnClickListener {
    val hintText = inputC.hint?.toString()

    val content = when (hintText) {
        "Enter Text" -> {
            val text = inputC.text.toString()
            if (text.isEmpty()) return@setOnClickListener
            QrContent.Text(text)
        }
        "Enter URL" -> {
            val url = inputC.text.toString()
            if (url.isEmpty()) return@setOnClickListener
            QrContent.Url(url)
        }
        else -> {
            val ssid = inputA.text.toString()
            val password = inputB.text.toString()
            val encryption = inputC.text.toString()
            if (ssid.isEmpty() || password.isEmpty()) return@setOnClickListener
            QrContent.Wifi(ssid, password, encryption)
        }
    }

    generateQrCode(content)
}


sealed class QrContent {
    data class Wifi(val ssid: String, val password: String, val encryption: String) : QrContent()
    data class Url(val url: String) : QrContent()
    data class Text(val text: String) : QrContent()
}

private fun generateQrCode(content: QrContent) {
    val text = when (content) {
        is QrContent.Wifi -> "WIFI:T:${content.encryption};S:${content.ssid};P:${content.password};;"
        is QrContent.Url -> "URL:${content.url}"
        is QrContent.Text -> content.text
    }

    val writer = com.google.zxing.qrcode.QRCodeWriter()
    val bitMatrix = writer.encode(text, com.google.zxing.BarcodeFormat.QR_CODE, 800, 800)

    ...
}
```


```kotlin
// My simplified version
generateQrButton.setOnClickListener {
    val textC = inputC.text.toString()

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
}


private fun generateQrCode(data: String) {
    val writer = com.google.zxing.qrcode.QRCodeWriter()
    val bitMatrix = writer.encode(data, com.google.zxing.BarcodeFormat.QR_CODE, 800, 800)
    ...
}
```



## Common Imports:
```
   android.widget.
      Button
      EditText
      ImageView
      TextView
      Toast
      PopupMenu

   android.view.
      Gravity
      View

   android.graphics.
      Bitmap
      Color

   android.net.wifi.
      WifiConfiguration
      WifiManager
```
