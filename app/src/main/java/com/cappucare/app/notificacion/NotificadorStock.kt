package com.cappucare.app.notificacion

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.cappucare.app.R
import com.cappucare.app.model.Producto
import com.cappucare.app.viewmodel.CappucareViewModel

/**
 * RNF-07 (opcional) · Recurso nativo: notificación local cuando un producto queda bajo su
 * nivel mínimo (RF-09).
 *
 * - Usa NotificationCompat y un canal propio ("stock_minimo"), obligatorio desde Android 8.
 * - En Android 13 o superior requiere el permiso POST_NOTIFICATIONS (declarado en el
 *   AndroidManifest y pedido en ReposicionScreen al tocar "Activar").
 * - Si no hay permiso, simplemente no se muestra: la lista de alertas sigue funcionando.
 */
object NotificadorStock {

    private const val ID_CANAL = "stock_minimo"

    fun tienePermiso(context: Context): Boolean {
        val permisoOk = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        return permisoOk && NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    private fun crearCanal(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canal = NotificationChannel(
                ID_CANAL,
                "Alertas de stock mínimo",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Avisa cuando un producto queda bajo su nivel mínimo de existencias." }
            // Crear un canal que ya existe no hace nada, por eso se puede llamar siempre.
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(canal)
        }
    }

    @SuppressLint("MissingPermission") // el permiso se revisa en tienePermiso() justo antes
    fun mostrar(context: Context, producto: Producto, falta: Int) {
        if (!tienePermiso(context)) return
        crearCanal(context)

        // Al tocar la notificación se vuelve a la app (a la misma sesión si sigue abierta).
        val pendingIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.let { abrirApp ->
            PendingIntent.getActivity(
                context, 0, abrirApp, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        val notificacion = NotificationCompat.Builder(context, ID_CANAL)
            .setSmallIcon(R.drawable.ic_alerta_stock)
            .setContentTitle("Reposición necesaria: ${producto.nombre}")
            .setContentText("Quedan ${producto.cantidadDisponible} de ${producto.nivelMinimo} (mínimo). Faltan $falta.")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "${producto.codigo} · ${producto.ubicacion}\n" +
                        "Quedan ${producto.cantidadDisponible} de ${producto.nivelMinimo} (mínimo). " +
                        "Faltan $falta. Revisa Alertas de reposición para registrar un ingreso."
                )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        // Un id por producto: si vuelve a cruzar el mínimo, reemplaza su aviso anterior.
        NotificationManagerCompat.from(context).notify(producto.id.hashCode(), notificacion)
    }
}

/**
 * Escucha los avisos del ViewModel y los convierte en notificaciones. Se llama una sola vez en
 * AppNavigation, así funciona sin importar en qué pantalla esté el usuario.
 */
@Composable
fun EscuchaAvisosStock(viewModel: CappucareViewModel) {
    val context = LocalContext.current.applicationContext
    LaunchedEffect(viewModel) {
        viewModel.avisoStockMinimo.collect { producto ->
            NotificadorStock.mostrar(context, producto, viewModel.faltaParaMinimo(producto))
        }
    }
}
