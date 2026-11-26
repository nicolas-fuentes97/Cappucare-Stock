package com.cappucare.app.ui.components

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

/**
 * RNF-06 · Recurso nativo: CÁMARA para leer el código QR o de barras de un producto.
 *
 * - El permiso CAMERA se pide recién al tocar este botón, no al abrir la app.
 * - Si el usuario lo rechaza, se llama a [onPermisoDenegado] y la app sigue funcionando con la
 *   búsqueda escrita.
 * - La lectura la hace la librería ZXing (journeyapps) en su propia pantalla de cámara; aquí solo
 *   recibimos el texto leído en [onCodigoLeido]. Solo se usan códigos preparados para fines
 *   académicos (docs/qr).
 */
@Composable
fun BotonEscanearCodigo(
    onCodigoLeido: (String) -> Unit,
    onPermisoDenegado: () -> Unit
) {
    val context = LocalContext.current

    val opciones = remember {
        ScanOptions()
            .setDesiredBarcodeFormats(ScanOptions.QR_CODE, ScanOptions.CODE_128, ScanOptions.EAN_13)
            .setPrompt("Apunta al código QR o de barras del producto")
            .setBeepEnabled(false)
            .setOrientationLocked(false)
    }

    // 2) Resultado de la cámara: texto leído (null si el usuario volvió sin escanear).
    val lanzadorEscaneo = rememberLauncherForActivityResult(ScanContract()) { resultado ->
        resultado.contents?.let { onCodigoLeido(it.trim()) }
    }

    // 1) Permiso en tiempo de ejecución.
    val lanzadorPermiso = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { concedido ->
        if (concedido) lanzadorEscaneo.launch(opciones) else onPermisoDenegado()
    }

    IconButton(onClick = {
        val yaTienePermiso = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        if (yaTienePermiso) lanzadorEscaneo.launch(opciones) else lanzadorPermiso.launch(Manifest.permission.CAMERA)
    }) {
        Icon(Icons.Filled.QrCodeScanner, contentDescription = "Escanear código con la cámara")
    }
}
