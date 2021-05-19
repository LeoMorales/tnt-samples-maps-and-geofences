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

import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel

/*
 * Inicialmente pensada para contener dos piezas de estado importantes:
 *  - Indice de geofence: La geofence que la aplicación piensa que está activa.
 *  - La sugerencia que se muestra para llegar a la geofence.
 * Mientras la sugerencia coincida con la geofence, la Activity a medida que
 * pase por sus diferentes estados no va a actualizar la geofence.
 *
 * Estos estados son almacenados en SavedState, que coincide con el ciclo de vida de Android.
 * Destruir la Activity asociada con el botón de atrás va a borrar el estado y reiniciar la
 * aplicación, mientras que el botón de inicio hará que el estado se guarde, incluso
 * si Android termina la aplicación en segundo plano.
 */
class GeofenceViewModel(state: SavedStateHandle) : ViewModel() {
    private val _geofenceIndice = state.getLiveData(GEOFENCE_INDEX_KEY, -1)
    private val _sugerenciaIndice = state.getLiveData(HINT_INDEX_KEY, 0)

    val geofenceIndice: LiveData<Int>
        get() = _geofenceIndice
    val geofenceSugerenciaRecursoId = Transformations.map(geofenceIndice) {
        val index = geofenceIndice?.value ?: -1
        when {
            index < 0 -> R.string.sugerencia_no_inicializada
            index < GeofencingConstants.NUM_LANDMARKS -> GeofencingConstants.MARCADORES_DATA[geofenceIndice.value!!].indicacion
            else -> R.string.geofence_finalizada
        }
    }

    val geofenceRecursoDeImagenId = Transformations.map(geofenceIndice) {
        val index = geofenceIndice.value ?: -1
        when {
            index < GeofencingConstants.NUM_LANDMARKS -> R.drawable.ic_launcher_background
            else -> R.drawable.ic_launcher_foreground
        }
    }

    fun actualizarSugerencia(indiceActual: Int) {
        _sugerenciaIndice.value = indiceActual+1
    }

    fun geofenceActivada() {
        _geofenceIndice.value = _sugerenciaIndice.value
    }

    fun geofenceEstaActiva() =_geofenceIndice.value == _sugerenciaIndice.value
    fun proximoIndiceGeofence() = _sugerenciaIndice.value ?: 0
}

private const val HINT_INDEX_KEY = "hintIndex"
private const val GEOFENCE_INDEX_KEY = "geofenceIndex"
