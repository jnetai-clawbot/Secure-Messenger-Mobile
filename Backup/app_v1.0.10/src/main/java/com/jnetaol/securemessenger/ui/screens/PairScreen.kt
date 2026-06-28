package com.jnetaol.securemessenger.ui.screens

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer
import com.jnetaol.securemessenger.MainViewModel
import com.jnetaol.securemessenger.pairing.QRScannerActivity
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PairScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val pinCode = viewModel.identityManager.pinCode
    val qrBitmap by viewModel.qrBitmap.collectAsState()
    var pinInput by remember { mutableStateOf("") }
    var showRegenerateDialog by remember { mutableStateOf(false) }
    var showQrOptions by remember { mutableStateOf(false) }
    var showPinOptions by remember { mutableStateOf(false) }

    val scannerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val qrResult = result.data?.getStringExtra("QR_RESULT") ?: return@rememberLauncherForActivityResult
        if (qrResult.startsWith("SM|")) {
            val parts = qrResult.split("|")
            if (parts.size >= 3) {
                viewModel.pairWithQr(qrResult, parts[2])
                onBack()
            }
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return@rememberLauncherForActivityResult
            val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            if (bitmap == null) return@rememberLauncherForActivityResult
            scanQRFromBitmap(context, bitmap) { result ->
                if (result != null && result.startsWith("SM|")) {
                    val parts = result.split("|")
                    if (parts.size >= 3) {
                        viewModel.pairWithQr(result, parts[2])
                        onBack()
                    }
                } else {
                    viewModel.showToast("No valid QR code found in image")
                }
            }
        } catch (_: Exception) {
            viewModel.showToast("Error reading image")
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            scannerLauncher.launch(Intent(context, QRScannerActivity::class.java))
        }
    }

    if (showRegenerateDialog) {
        AlertDialog(
            onDismissRequest = { showRegenerateDialog = false },
            title = { Text("Regenerate Identity") },
            text = { Text("Are you sure you want to regenerate your identity? This will create a new QR code and PIN.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.regenerateIdentity()
                    showRegenerateDialog = false
                }) { Text("Confirm") }
            },
            dismissButton = {
                TextButton(onClick = { showRegenerateDialog = false }) { Text("Cancel") }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurface
        )
    }

    if (showQrOptions) {
        AlertDialog(
            onDismissRequest = { showQrOptions = false },
            title = { Text("QR Code") },
            text = {
                Column {
                    TextButton(onClick = { showQrOptions = false; shareQRCode(context, qrBitmap, pinCode) }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Share, null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Share QR Code")
                    }
                    TextButton(onClick = { showQrOptions = false; saveQRCode(context, qrBitmap, pinCode) }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Download, null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Save as Image")
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showQrOptions = false }) { Text("Close") } },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurface
        )
    }

    if (showPinOptions) {
        AlertDialog(
            onDismissRequest = { showPinOptions = false },
            title = { Text("PIN Code") },
            text = {
                Column {
                    TextButton(onClick = { showQrOptions = false; sharePin(context, pinCode) }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Share, null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Share PIN")
                    }
                    TextButton(onClick = { showQrOptions = false; copyToClipboard(context, pinCode) }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Copy to Clipboard")
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showPinOptions = false }) { Text("Close") } },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurface
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pair Device", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Your Identity", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier.size(220.dp).background(Color.White, RoundedCornerShape(12.dp)).clickable { showQrOptions = true }.padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val bmp = qrBitmap
                        if (bmp != null) {
                            Image(bmp.asImageBitmap(), "QR Code", Modifier.fillMaxSize())
                        } else {
                            CircularProgressIndicator(Modifier.size(32.dp), color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(pinCode, fontSize = 36.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, letterSpacing = 8.sp, modifier = Modifier.clickable { showPinOptions = true })
                    Text("Tap QR or PIN for options", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }
            }

            Spacer(Modifier.height(16.dp))

            OutlinedButton(onClick = { showRegenerateDialog = true }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Icon(Icons.Default.Refresh, null)
                Spacer(Modifier.width(8.dp))
                Text("Regenerate Identity")
            }

            Spacer(Modifier.height(32.dp))
            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            Spacer(Modifier.height(24.dp))

            Text("Connect to Another Device", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(16.dp))

            Button(onClick = {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    scannerLauncher.launch(Intent(context, QRScannerActivity::class.java))
                } else {
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                }
            }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Icon(Icons.Default.QrCodeScanner, null)
                Spacer(Modifier.width(8.dp))
                Text("Scan QR Code")
            }

            Spacer(Modifier.height(8.dp))

            OutlinedButton(onClick = { imagePickerLauncher.launch("image/*") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Icon(Icons.Default.Image, null)
                Spacer(Modifier.width(8.dp))
                Text("Select QR Image")
            }

            Spacer(Modifier.height(12.dp))
            Text("— OR —", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = pinInput,
                onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) pinInput = it },
                label = { Text("Enter 6-digit PIN") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(Modifier.height(12.dp))

            Button(onClick = {
                if (pinInput.length == 6) {
                    viewModel.pairWithPin(pinInput)
                    pinInput = ""
                    onBack()
                }
            }, modifier = Modifier.fillMaxWidth(), enabled = pinInput.length == 6, shape = RoundedCornerShape(12.dp)) {
                Icon(Icons.Default.Link, null)
                Spacer(Modifier.width(8.dp))
                Text("Connect with PIN")
            }

            Spacer(Modifier.height(80.dp))
        }
    }
}

private fun shareQRCode(context: Context, bitmap: Bitmap?, pin: String) {
    if (bitmap == null) return
    try {
        val cacheDir = File(context.cacheDir, "shared_images").also { it.mkdirs() }
        val file = File(cacheDir, "secure_messenger_qr.png")
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, "Connect with me on Secure Messenger! PIN: $pin")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share QR Code"))
    } catch (_: Exception) {}
}

private fun saveQRCode(context: Context, bitmap: Bitmap?, pin: String) {
    if (bitmap == null) return
    try {
        val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).also { it.mkdirs() }
        val file = File(dir, "SecureMessenger_QR_$pin.png")
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        context.sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE).apply { data = android.net.Uri.fromFile(file) })
        Toast.makeText(context, "Saved to Pictures", Toast.LENGTH_SHORT).show()
    } catch (_: Exception) {}
}

private fun sharePin(context: Context, pin: String) {
    context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, "Connect with me on Secure Messenger! My PIN: $pin")
    }, "Share PIN"))
}

private fun copyToClipboard(context: Context, pin: String) {
    (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
        .setPrimaryClip(ClipData.newPlainText("Secure Messenger PIN", pin))
    Toast.makeText(context, "PIN copied to clipboard", Toast.LENGTH_SHORT).show()
}

private fun scanQRFromBitmap(context: Context, bitmap: Bitmap, callback: (String?) -> Unit) {
    Thread {
        try {
            val pixels = IntArray(bitmap.width * bitmap.height)
            bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
            val source = RGBLuminanceSource(bitmap.width, bitmap.height, pixels)
            val result = MultiFormatReader().decode(com.google.zxing.BinaryBitmap(HybridBinarizer(source)))
            android.os.Handler(context.mainLooper).post { callback(result.text) }
        } catch (_: Exception) {
            android.os.Handler(context.mainLooper).post { callback(null) }
        }
    }.start()
}
