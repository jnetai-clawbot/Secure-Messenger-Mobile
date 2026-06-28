package com.jnetaol.securemessenger.pairing

import android.graphics.Bitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.jnetaol.securemessenger.logger.DebugLogger
import java.util.EnumMap

object QRCodeGenerator {
    private const val QR_SIZE = 512

    fun generateQRCode(data: String): Bitmap? {
        return try {
            DebugLogger.d("QRCodeGenerator", "generateQRCode", "SM-QR-001", "Generating QR for: ${data.take(20)}...")
            val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java)
            hints[EncodeHintType.ERROR_CORRECTION] = ErrorCorrectionLevel.M
            hints[EncodeHintType.MARGIN] = 2
            hints[EncodeHintType.CHARACTER_SET] = "UTF-8"

            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(data, BarcodeFormat.QR_CODE, QR_SIZE, QR_SIZE, hints)

            val bitmap = Bitmap.createBitmap(QR_SIZE, QR_SIZE, Bitmap.Config.ARGB_8888)
            for (x in 0 until QR_SIZE) {
                for (y in 0 until QR_SIZE) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) 0xFF000000.toInt() else 0xFFFFFFFF.toInt())
                }
            }
            DebugLogger.i("QRCodeGenerator", "generateQRCode", "SM-QR-002", "QR code generated successfully")
            bitmap
        } catch (e: Exception) {
            DebugLogger.e("QRCodeGenerator", "generateQRCode", "SM-QR-ERR-001", "Failed to generate QR code", e)
            null
        }
    }
}
