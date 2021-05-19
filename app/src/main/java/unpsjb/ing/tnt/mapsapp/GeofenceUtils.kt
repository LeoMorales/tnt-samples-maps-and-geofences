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

import android.content.Context
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.maps.model.LatLng
import java.util.concurrent.TimeUnit

/**
 * Retorna la cadena de error para un error en codigo de geofencing.
 */
fun mensajeError(context: Context, errorCodigo: Int): String {
    val resources = context.resources
    return when (errorCodigo) {
        GeofenceStatusCodes.GEOFENCE_NOT_AVAILABLE -> resources.getString(
            R.string.geofence_no_disponible
        )
        GeofenceStatusCodes.GEOFENCE_TOO_MANY_GEOFENCES -> resources.getString(
            R.string.geofence_demasiados_geofences
        )
        GeofenceStatusCodes.GEOFENCE_TOO_MANY_PENDING_INTENTS -> resources.getString(
            R.string.geofence_demasiados_intents_pendientes
        )
        else -> resources.getString(R.string.error_geofence_desconocido)
    }
}

/**
 * Almacena la información de latitud y longitud junto con una sugerencia para ayudar al usuario a
 * llegar a la ubicacion
 */
data class LandmarkDataObject(val id: String, val indicacion: Int, val nombre: Int, val latLong: LatLng)

internal object GeofencingConstants {

    /**
     * Se utiliza para darle un tiempo de expiración a cada geofence.
     * Pasado este tiempo. el servicio de Ubicación deja de rastrear
     * la geofence. Para este ejemplo, geofences expiran después de una hora.
     */
    val GEOFENCE_EXPIRACION_EN_MILLISEGUNDOS: Long = TimeUnit.HOURS.toMillis(1)

    val MARCADORES_DATA = arrayOf(

        LandmarkDataObject(
            "torneo_truco",
            R.string.torneo_truco_descripcion_breve,
            R.string.torneo_ubicacion,
            LatLng(-43.327159, -65.050439)),

        LandmarkDataObject(
            "aquavida",
            R.string.aquavida_descripcion_breve,
            R.string.aquavida_ubicacion,
            LatLng(-43.321762, -65.047201)),

        LandmarkDataObject(
            "unpsjb_trelew",
            R.string.unpsjb_descripcion_breve,
            R.string.unpsjb_ubicacion,
            LatLng(-43.249545, -65.307981)),

        LandmarkDataObject(
            "unpsjb_madryn_real",
            R.string.unpsjb_descripcion_breve,
            R.string.unpsjb_ubicacion,
            LatLng(-42.785603, -65.005689)),

        LandmarkDataObject(
            "paseo_las_toninas",
            R.string.paseo_las_toninas_descripcion_breve,
            R.string.paseo_las_toninas_ubicacion,
            LatLng(-43.311837, -65.038742))


        )

    val NUM_LANDMARKS = MARCADORES_DATA.size
    const val GEOFENCE_RADIO_EN_METROS = 500f
    const val EXTRA_GEOFENCE_INDICE = "GEOFENCE_INDEX"
}
