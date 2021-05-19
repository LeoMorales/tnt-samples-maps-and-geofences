package unpsjb.ing.tnt.mapsapp

import android.Manifest
import android.annotation.TargetApi
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.databinding.library.BuildConfig
import androidx.lifecycle.SavedStateViewModelFactory
import androidx.lifecycle.ViewModelProviders
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.snackbar.Snackbar
import unpsjb.ing.tnt.mapsapp.databinding.ActivityMapsBinding
import java.util.*

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var map: GoogleMap
    private val CODIGO_SOLICITUD_PERMISO_UBICACION = 1
    private lateinit var binding: ActivityMapsBinding

    // TODO [Geofences] #1.2: Comprobar si el dispositivo está corriendo Android Q (API 29) o posterior.
    private val ejecutando_Q_o_superior = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q
    private lateinit var clienteGeofencing: GeofencingClient
    private lateinit var viewModel: GeofenceViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_maps)
        viewModel = ViewModelProviders.of(
            this,
            SavedStateViewModelFactory(
                this.application,
                this)
            ).get(GeofenceViewModel::class.java)
        binding.lifecycleOwner = this

        // Obtener un SupportMapFragment y ser notificado cuando esté listo para ser utilizado.
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // TODO [Geofences] #1.8: Obtenemos el cliente geofences
        clienteGeofencing = LocationServices.getGeofencingClient(this)

        // Crear el canal para las notificaciones
        crearCanalDeNotificaciones(this )
    }


    override fun onStart() {
        super.onStart()
        comprobarPermisosEIniciarGeofencing()
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        // TODO #1.1: Cambiar el marcador
        // Add a marker
        val playaUnionLatLong = LatLng(-43.327159, -65.050439)

        // Create a val for how zoomed in you want to be on the map. Use zoom level 15f.
        val zoomLevel = 15f
        //map.moveCamera(CameraUpdateFactory.newLatLng(playaUnion))
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(playaUnionLatLong, zoomLevel))
    }


    // TODO #2.1: Menú -> Creamos el menu:
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.opciones_mapa, menu)
        return true
    }

    // TODO #2.2: Menú -> Click en las opciones del menú:
    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        // Cambiar el tipo del mapa segun la elección del usuario
        R.id.mapa_normal -> {
            map.mapType = GoogleMap.MAP_TYPE_NORMAL
            true
        }
        R.id.mapa_hibrido -> {
            map.mapType = GoogleMap.MAP_TYPE_HYBRID
            true
        }
        R.id.mapa_satelite -> {
            map.mapType = GoogleMap.MAP_TYPE_SATELLITE
            true
        }
        R.id.mapa_terreno -> {
            map.mapType = GoogleMap.MAP_TYPE_TERRAIN
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    /*****************************************************************************************
     *****************************************************************************************
     *** GEOFENCES  **************************************************************************
     *****************************************************************************************
     *****************************************************************************************/
    // TODO [Geofences] #1.3: Crear el método para comprobar permisos
    @TargetApi(29)
    private fun permisosDeUbicacionEnPrimerYSegundoPlanoAprobados(): Boolean {
        // Primero se chequea si el permiso ACCESS_FINE_LOCATION fue aceptado
        val ubicacionEnPrimerPlanoAprobada = (
                PackageManager.PERMISSION_GRANTED ==
                        ActivityCompat.checkSelfPermission(this,
                            Manifest.permission.ACCESS_FINE_LOCATION))
        // Si el dispositivo ejecuta Android Q (API 29) o superior, verificar que se haya otorgado el
        // permiso ACCESS_BACKGROUND_LOCATION. Devolver true si el dispositivo ejecuta una versión
        // inferior a Q, donde no se necesita un permiso para acceder a la ubicación en segundo plano.
        val permisoEnBackgroundAprobado =
            if (ejecutando_Q_o_superior) {
                PackageManager.PERMISSION_GRANTED ==
                        ActivityCompat.checkSelfPermission(
                            this, Manifest.permission.ACCESS_BACKGROUND_LOCATION
                        )
            } else {
                true
            }

        // Devuelve verdadero si se han otorgado los permisos, de lo contrario devuielve falso.
        return ubicacionEnPrimerPlanoAprobada && permisoEnBackgroundAprobado
    }

    // TODO [Geofences] #1.4: Crear método para solicitar permisos de ubicación al usuario
    @TargetApi(29 )
    private fun solicitarPermisosDeUbicacionForegroundYBackground() {
        // Si los permisos ya se han otorgado, no se necesita volver a solicitarlos, por lo podemos salir del método.
        if (permisosDeUbicacionEnPrimerYSegundoPlanoAprobados()) {
            return
        }
        // _listaDePermisos_ contiene los permisos que se solicitarán.
        // Inicialmente contiene ACCESS_FINE_LOCATION ya que es necesario para todos los niveles de API.
        var listaDePermisos = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)

        // Decidir todos los permisos se deben solicitar:
        val codigoResultado = when {
            ejecutando_Q_o_superior -> {
                listaDePermisos += Manifest.permission.ACCESS_BACKGROUND_LOCATION
                CODIGO_RESULTADO_PARA_SOLICITUD_PERMISOS_FOREGROUND_Y_BACKGROUND
            }
            else -> CODIGO_RESULTADO_PARA_SOLICITUD_SOLO_PERMISO_FOREGROUND
        }
        Log.d(
            TAG,
            if (codigoResultado == CODIGO_RESULTADO_PARA_SOLICITUD_SOLO_PERMISO_FOREGROUND)
                "Solicitar solo permiso de ubicación foreground"
            else
                "Solicitar permisos de ubicación foreground y background"
        )

        // Finalmente, solicitamos permisos pasando la actividad actual, el arreglo de permisos y el código de resultado.
        ActivityCompat.requestPermissions(
            this@MapsActivity,
            listaDePermisos,
            codigoResultado
        )
    }

    private fun seOtorgoPermisoDeUbicacion() : Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    private fun activarUbicacion() {
        if (seOtorgoPermisoDeUbicacion()) {
            map.isMyLocationEnabled = true
        }
        else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf<String>(Manifest.permission.ACCESS_FINE_LOCATION),
                CODIGO_SOLICITUD_PERMISO_UBICACION
            )
        }
    }

    // TODO #6.5: Manejar la solicitud de permisos
    // TODO [Geofences] #1.5: Manejar la solicitud de permisos
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == CODIGO_SOLICITUD_PERMISO_UBICACION) {
            if (grantResults.contains(PackageManager.PERMISSION_GRANTED)) {
                activarUbicacion()
            }
        }
        Log.d(TAG, "-> onRequestPermissionResult")

        if (
            grantResults.isEmpty() ||
            grantResults[PERMISO_UBICACION_INDICE] == PackageManager.PERMISSION_DENIED ||
            (requestCode == CODIGO_RESULTADO_PARA_SOLICITUD_PERMISOS_FOREGROUND_Y_BACKGROUND &&
                    grantResults[PERMISO_UBICACION_BACKGROUND_INDICE] ==
                    PackageManager.PERMISSION_DENIED))
        {
            // Permiso denegado, explicar que la app necesita la ubicación:
            Snackbar.make(
                binding.mapsActivityMain,
                R.string.permiso_denegado_explicacion,
                Snackbar.LENGTH_INDEFINITE
            )
                .setAction(R.string.settings) {
                    startActivity(Intent().apply {
                        action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    })
                }.show()
        } else {
            verificarConfigDeUbicacionDelDispositivoEIniciarGeofences()
        }
    }

    /**
     * Comenzar el chequeo de permisos y el proceso de Geofence
     * solo si la Geofence asociada con el punto actual
     * no está activo.
     */
    private fun comprobarPermisosEIniciarGeofencing() {

        if (viewModel.geofenceEstaActiva()) return
        if (permisosDeUbicacionEnPrimerYSegundoPlanoAprobados()) {
            verificarConfigDeUbicacionDelDispositivoEIniciarGeofences()
        } else {
            solicitarPermisosDeUbicacionForegroundYBackground()
        }
    }


    // TODO [Geofences] #1.6: Chequear la ubicación del usuario
    /*
     *  Utiliza el Cliente de Ubicacion (Location Client) para verificar el estado actual de
     *  configuración de ubicacion y permitir al usuario prender la ubicación con nuestra aplicación.
     */
    private fun verificarConfigDeUbicacionDelDispositivoEIniciarGeofences(resolver:Boolean = true) {
        // Primero crear una solicitud (LocationRequest) y agregarla a una nueva petición
        // en el constructor de peticiones (LocationSettingsRequest Builder)
        val solicitudDeUbicacion = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_LOW_POWER
        }
        val builder = LocationSettingsRequest.Builder().addLocationRequest(solicitudDeUbicacion)

        // Luego utilizar LocationServices para obtener el cliente de configuración (SettingsClient).
        val clienteDeConfiguracion = LocationServices.getSettingsClient(this)
        // Crear una tarea-de-respuesta-de-configuracion-de-ubicacion
        // destinada a comprobar la configuración de la ubicación
        val tareaRespuestaDeConfiguracionDeUbicacion =
            clienteDeConfiguracion.checkLocationSettings(builder.build())

        // Agregamos un escuchador onFailureListener a la tarea para el caso en que la configuracion de la ubicación no sea satisfecha:
        tareaRespuestaDeConfiguracionDeUbicacion.addOnFailureListener { exception ->
            // Comprobar si la excepción es del tipo ResolvableApiException y, en caso afirmativo,
            // intentamos llamar al método startResolutionForResult() para solicitar al usuario
            // que active la ubicación del dispositivo.
            if (exception is ResolvableApiException && resolver){
                // La configuración de ubicación no está satisfecha.
                // Esto se puede solucionar mostrando al usuario un cuadro de diálogo.
                try {
                    // Mostrar el dialogo llamando a startResolutionForResult(),
                    // comprobar luego el resultado en el método onActivityResult().
                    exception.startResolutionForResult(this@MapsActivity,
                        SOLICITUD_ENCENDIDO_UBICACION_DEL_DISPOSITIVO)
                } catch (sendEx: IntentSender.SendIntentException) {
                    // caso de error, mostrar un mensaje de log
                    Log.d(TAG, "Error al obtener la resolucion de la ubicacion: " + sendEx.message)
                }
            } else {
                // si la excepción no es del tipo ResolvableApiException, mostrar una snackbar que
                // alerte al usuario se necesita habilitada para aprovechar una mejor funcionalidad
                Snackbar.make(
                    binding.mapsActivityMain,
                    R.string.error_ubicacion_requerida, Snackbar.LENGTH_INDEFINITE
                ).setAction(android.R.string.ok) {
                    verificarConfigDeUbicacionDelDispositivoEIniciarGeofences()
                }.show()
            }
        }

        // si locationSettingsResponseTask se completa, se comprueba de
        // que haya sido exitosa y se agregan los geofences
        tareaRespuestaDeConfiguracionDeUbicacion.addOnCompleteListener {
            if ( it.isSuccessful ) {
                agregarGeofences()
            }
        }
    }

    /* Cuando se obtiene el resultado de la solicitud de activación de ubicación del dispositivo al
     * usuario, llamamos a verificarConfigDeUbicacionDelDispositivoEIniciarGeofences
     * nuevamente para asegurarnos de que esté realmente activado, pero no resolvemos
     * la verificación para evitar que el usuario vea un bucle sin fin.
     **/
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SOLICITUD_ENCENDIDO_UBICACION_DEL_DISPOSITIVO) {
            verificarConfigDeUbicacionDelDispositivoEIniciarGeofences(false)
        }
    }

    // TODO [Geofences] #1.7: Crear un Intent pendiente
    // Un PendingIntent para el Broadcast Receiver que maneja las transiciones de geofences.
    private val geofencePendingIntent: PendingIntent by lazy { // acerca de lazy properties -> el valor se obtiene solo en el primer acceso
        val intent = Intent(this, GeofenceBroadcastReceiver::class.java)
        intent.action = ACCION_EVENTO_GEOFENCE
        // Usamos FLAG_UPDATE_CURRENT para obtener el mismo pending intent cuando se invoquen addGeofences() y removeGeofences().
        PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    /* Cuando el usuario hace clic en la notificación, se llamará a este método, informándonos que
     * se ha activado el geofence y que es hora de pasar al siguiente.
     * */
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        val extras = intent?.extras
        if(extras != null){
            if(extras.containsKey(GeofencingConstants.EXTRA_GEOFENCE_INDICE)){
                viewModel.actualizarSugerencia(extras.getInt(GeofencingConstants.EXTRA_GEOFENCE_INDICE))
                comprobarPermisosEIniciarGeofencing()
            }
        }
    }

    // TODO [Geofences] #1.9: Agregar Geofences!
    /*
     * Agrega una geofence y si es necesario remueve cualquier Geofence existente. Este método debe
     * ser invocado despues de que el usuario otorgue permisos de ubicacion.
     * En caso de que no tengamos mas geofences para agregar, removemos la que nos queda y le
     * indicamos al viewModel que la sugerencia final esta "activa."
     */
    private fun agregarGeofences() {
        // Comprobamos si ya tenemos geofences activos, deberíamos agregar de a uno...
        if (viewModel.geofenceEstaActiva()) return

        // Obtener el indice del geofence desde el viewMOdel.
        val indiceGeofenceActual = viewModel.proximoIndiceGeofence()
        // si nos excedimos en la cantidad de geofences -> remover y desactivar
        if(indiceGeofenceActual >= GeofencingConstants.NUM_LANDMARKS) {
            removeGeofences()
            viewModel.geofenceActivada()
            return
        }

        // una vez que tenemos el indice del geofence y sabemos que es válido,
        // obtener los datos del geofence: id, lat y longitud
        val datosNuevaGeofence = GeofencingConstants.MARCADORES_DATA[indiceGeofenceActual]

        // Crear el object Geofence
        val geofence = Geofence.Builder()
            // Setear un request ID, cadena para identificar la geofence.
            .setRequestId(datosNuevaGeofence.id)
            // Setear la region circular region de la geofence.
            .setCircularRegion(
                datosNuevaGeofence.latLong.latitude,
                datosNuevaGeofence.latLong.longitude,
                GeofencingConstants.GEOFENCE_RADIO_EN_METROS
            )
            // Setear la duración de expiration de geofence. Esta geofence es removida
            // automaticamente luego de este período de tiempo.
            .setExpirationDuration(GeofencingConstants.GEOFENCE_EXPIRACION_EN_MILLISEGUNDOS)
            // Setear el tipo de transición de interes. Las alertas se generan solo para
            // estas transiciones. En este ejemplo se restrean transiciones de entrada
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
            .build()

        // Crear el geofence request
        val geofencingRequest = GeofencingRequest.Builder()
            // La bandera INITIAL_TRIGGER_ENTER indica que el servicio de geofencing debe
            // disparar una notificacion GEOFENCE_TRANSITION_ENTER cuando el geofence es
            // agregado y si el dispositivo ya se encuentra dentro de la geofence
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            // Agregar las geofences que van a ser monitoreadas por el servicio de geofencing.
            .addGeofence(geofence)
            .build()

        // Primero, remover cualquier geofence existente que utilice nuestro pending intent
        clienteGeofencing.removeGeofences(geofencePendingIntent)?.run {
            // Independientemente de si se pudo efectuar el remover, agregar la geofence
            addOnCompleteListener {
                if (permisosDeUbicacionEnPrimerYSegundoPlanoAprobados()) {
                    // Agregar la nueva solicitud de geofence con la nueva geofence.
                    clienteGeofencing.addGeofences(geofencingRequest, geofencePendingIntent)?.run {
                        addOnSuccessListener {
                            // Geofence agregada.
                            Toast.makeText(
                                this@MapsActivity, R.string.geofences_agregada,
                                Toast.LENGTH_SHORT
                            )
                                .show()
                            Log.e("Geofence agregada:", geofence.requestId)
                            // Indicarle al viewmodel que se activo la geofence.
                            viewModel.geofenceActivada()
                        }
                        addOnFailureListener {
                            // Fallo al agregar geofences.
                            Toast.makeText(
                                this@MapsActivity, R.string.geofences_no_agregadas,
                                Toast.LENGTH_SHORT
                            ).show()
                            if ((it.message != null)) {
                                Log.w(TAG, it.message)
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Remover geofences. Este método debe ser invocado despues de que el usuario otorgue permisos de
     * ubicacion.
     */
    private fun removeGeofences() {
        if (!permisosDeUbicacionEnPrimerYSegundoPlanoAprobados()) {
            return
        }
        clienteGeofencing.removeGeofences(geofencePendingIntent)?.run {
            addOnSuccessListener {
                // Geofences removida
                Log.d(TAG, getString(R.string.geofences_removida))
                Toast.makeText(applicationContext, R.string.geofences_removida, Toast.LENGTH_SHORT)
                    .show()
            }
            addOnFailureListener {
                // Fallo al remover geofences
                Log.d(TAG, getString(R.string.geofences_no_removidas))
            }
        }
    }

    /**
     * This will also destroy any saved state in the associated ViewModel, so we remove the
     * geofences here.
     */
    override fun onDestroy() {
        super.onDestroy()
        removeGeofences()
    }


    companion object {
        internal const val ACCION_EVENTO_GEOFENCE =
            "MapsActivity.torneotruco.action.ACTION_GEOFENCE_EVENT"
    }
}

private const val CODIGO_RESULTADO_PARA_SOLICITUD_PERMISOS_FOREGROUND_Y_BACKGROUND = 33
private const val CODIGO_RESULTADO_PARA_SOLICITUD_SOLO_PERMISO_FOREGROUND = 34
private const val SOLICITUD_ENCENDIDO_UBICACION_DEL_DISPOSITIVO = 29
private const val TAG = "MapsActivity"
private const val PERMISO_UBICACION_INDICE = 0
private const val PERMISO_UBICACION_BACKGROUND_INDICE = 1
