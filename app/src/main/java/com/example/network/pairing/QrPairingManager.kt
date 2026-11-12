package com.example.network.pairing

import android.graphics.Bitmap
import android.graphics.Color
import com.example.data.model.QrPairingPayload
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeWriter
import org.json.JSONObject

object QrPairingManager {

    /**
     * Serializes pairing info into a compact JSON string.
     */
    fun createPayloadString(payload: QrPairingPayload): String {
        val json = JSONObject()
        json.put("v", payload.v)
        json.put("id", payload.deviceId)
        json.put("name", payload.deviceName)
        json.put("ip", payload.ip)
        json.put("port", payload.port)
        json.put("token", payload.token)
        json.put("exp", payload.expiresAt)
        json.put("net", payload.transport)
        return json.toString()
    }

    /**
     * Parses and validates a QR string. Returns null if invalid or expired.
     */
    fun parsePayload(qrText: String): Pair<QrPairingPayload?, String?> {
        return try {
            val json = JSONObject(qrText)
            val expiresAt = json.optLong("exp", 0L)
            val now = System.currentTimeMillis()

            if (expiresAt > 0 && now > expiresAt) {
                return Pair(null, "QR Code has expired. Please refresh the Camera QR.")
            }

            val payload = QrPairingPayload(
                v = json.optInt("v", 1),
                deviceId = json.optString("id", "Unknown"),
                deviceName = json.optString("name", "TwinGuard Cam"),
                ip = json.getString("ip"),
                port = json.optInt("port", 8080),
                token = json.optString("token", ""),
                expiresAt = expiresAt,
                transport = json.optString("net", "LOCAL_WIFI")
            )
            Pair(payload, null)
        } catch (e: Exception) {
            Pair(null, "Invalid QR code format: ${e.localizedMessage}")
        }
    }

    /**
     * Generates a high-contrast QR Code Bitmap.
     */
    fun generateQrBitmap(content: String, sizePx: Int = 512): Bitmap? {
        return try {
            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

            val darkColor = 0xFF0B132B.toInt()
            val lightColor = 0xFFF8FAFC.toInt()

            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) darkColor else lightColor)
                }
            }
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Decodes QR code from a Bitmap (e.g. from camera scanner).
     */
    fun decodeBitmap(bitmap: Bitmap): String? {
        return try {
            val width = bitmap.width
            val height = bitmap.height
            val pixels = IntArray(width * height)
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

            val source = RGBLuminanceSource(width, height, pixels)
            val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
            val reader = MultiFormatReader()
            val result = reader.decode(binaryBitmap)
            result.text
        } catch (e: Exception) {
            null
        }
    }
}
