package com.jnetaol.securemessenger.pairing

import android.graphics.Bitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.jnetaol.securemessenger.logger.DebugLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.EnumMap

object QRCodeGenerator {
    private const val QR_SIZE = 256

    suspend fun generateQRCode(data: String): Bitmap? = withContext(Dispatchers.Default) {
        try {
            DebugLogger.d("QRCodeGenerator", "generateQRCode", "SM-QR-001", "Generating QR for: ${data.take(20)}...")
            val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java)
            hints[EncodeHintType.ERROR_CORRECTION] = ErrorCorrectionLevel.M
            hints[EncodeHintType.MARGIN] = 2
            hints[EncodeHintType.CHARACTER_SET] = "UTF-8"

            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(data, BarcodeFormat.QR_CODE, QR_SIZE, QR_SIZE, hints)

            val pixels = IntArray(QR_SIZE * QR_SIZE)
            for (y in 0 until QR_SIZE) {
                val rowOffset = y * QR_SIZE
                for (x in 0 until QR_SIZE) {
                    pixels[rowOffset + x] = if (bitMatrix[x, y]) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
                }
            }
            val bitmap = Bitmap.createBitmap(QR_SIZE, QR_SIZE, Bitmap.Config.ARGB_8888)
            bitmap.setPixels(pixels, 0, QR_SIZE, 0, 0, QR_SIZE, QR_SIZE)
            DebugLogger.i("QRCodeGenerator", "generateQRCode", "SM-QR-002", "QR code generated successfully")
            bitmap
        } catch (e: Exception) {
            DebugLogger.e("QRCodeGenerator", "generateQRCode", "SM-QR-ERR-001", "Failed to generate QR code", e)
            null
        }
    }
}
