/*
 * Copyright (C) 2019 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package unpsjb.ing.tnt.mapsapp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat

/*
 * Necesitamos crear un NotificationChannel asociado a nuestro CHANNEL_ID antes de poder
 * enviar cualquier notificacion.
 */
fun crearCanalDeNotificaciones(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        // nuevo NotificationChannel (instancia)
        val canalDeNotification = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.canal_de_notificacion_nombre),
            NotificationManager.IMPORTANCE_HIGH
        )
            .apply {
                setShowBadge(false)
            }
        // configurar el canal:
        canalDeNotification.enableLights(true)
        canalDeNotification.lightColor = Color.RED
        canalDeNotification.enableVibration(true)
        canalDeNotification.description = context.getString(R.string.canal_de_notificacion_descripcion)

        // obtener el gestor de notificaciones
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        // solicitar al gestor la creación de un nuevo canal de notificación en el sistema
        notificationManager.createNotificationChannel(canalDeNotification)
    }
}

/*
 * Una función de extensión de Kotlin para NotificationCompat de AndroidX
 * que envía nuestra notificación de ingreso de Geofence.
 * Envía una notificación personalizada basada en la cadena de nombre asociada
 * con LANDMARK_DATA desde GeofencingConstants en el archivo GeofenceUtils.
 */
fun NotificationManager.dispararNotificacionEntradaAlGeofence(context: Context, indexEncontrado: Int) {
    val contentIntent = Intent(context, MapsActivity::class.java)
    contentIntent.putExtra(GeofencingConstants.EXTRA_GEOFENCE_INDICE, indexEncontrado)
    val pendingIntentContenido = PendingIntent.getActivity(
        context,
        NOTIFICATION_ID,
        contentIntent,
        PendingIntent.FLAG_UPDATE_CURRENT
    )
    val imagenMapa = BitmapFactory.decodeResource(
        context.resources,
        R.drawable.tantos_5
    )
    val estiloImagenGrande = NotificationCompat.BigPictureStyle()
        .bigPicture(imagenMapa)
        .bigLargeIcon(null)

    // Usamos el ID de recurso del nombre de LANDMARK_DATA junto con content_text para crear
    // un mensaje personalizado cuando se activa un Geofence.
    val builder = NotificationCompat.Builder(context, CHANNEL_ID)
        .setContentTitle(context.getString(R.string.app_name))
        .setContentText(context.getString(R.string.notificacion_texto_contenido,
            context.getString(GeofencingConstants.MARCADORES_DATA[indexEncontrado].nombre)))
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setContentIntent(pendingIntentContenido)
        .setSmallIcon(R.drawable.tantos_5)
        .setStyle(estiloImagenGrande)
        .setLargeIcon(imagenMapa)

    notify(NOTIFICATION_ID, builder.build())
}

private const val NOTIFICATION_ID = 33
private const val CHANNEL_ID = "GeofenceChannel"