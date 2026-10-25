package com.solventa4bits.movil

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.solventa4bits.movil.cotizacion.datos.AdaptadorCotizaciones
import com.solventa4bits.movil.cotizacion.datos.AdaptadorCreditos
import com.solventa4bits.movil.cotizacion.datos.crearCotizacionesApi
import com.solventa4bits.movil.cotizacion.datos.crearCreditosApi
import com.solventa4bits.movil.cotizacion.dominio.Cotizacion
import com.solventa4bits.movil.cotizacion.dominio.ServicioDeCotizaciones
import com.solventa4bits.movil.cotizacion.dominio.ServicioDeCreditos
import com.solventa4bits.movil.cotizacion.ui.Declinacion
import com.solventa4bits.movil.cotizacion.ui.NuevaCotizacionViewModel
import com.solventa4bits.movil.cotizacion.ui.PantallaCoberturas
import com.solventa4bits.movil.cotizacion.ui.PantallaLoQuePagas
import com.solventa4bits.movil.cotizacion.ui.PantallaNuevaCotizacion
import com.solventa4bits.movil.identidad.datos.AdaptadorEvidencias
import com.solventa4bits.movil.identidad.datos.PreparadorDeFotos
import com.solventa4bits.movil.identidad.datos.crearEvidenciasApi
import com.solventa4bits.movil.identidad.dominio.ServicioDeEvidencias
import com.solventa4bits.movil.identidad.dominio.CaraDeCedula
import com.solventa4bits.movil.identidad.ui.CedulaViewModel
import com.solventa4bits.movil.identidad.ui.PantallaSelfie
import com.solventa4bits.movil.identidad.ui.QueFoto
import com.solventa4bits.movil.identidad.ui.SelfieViewModel
import com.solventa4bits.movil.identidad.ui.Camara
import com.solventa4bits.movil.identidad.ui.camaraReal
import com.solventa4bits.movil.identidad.ui.PantallaCedula
import com.solventa4bits.movil.inicio.ui.InicioViewModel
import com.solventa4bits.movil.inicio.ui.PantallaInicio
import com.solventa4bits.movil.sesion.datos.AdaptadorSesion
import com.solventa4bits.movil.sesion.datos.AlmacenDeSesionSeguro
import com.solventa4bits.movil.sesion.datos.BovedaKeystore
import com.solventa4bits.movil.sesion.datos.ConSesion
import com.solventa4bits.movil.sesion.datos.crearSesionApi
import com.solventa4bits.movil.sesion.dominio.AlmacenDeSesion
import com.solventa4bits.movil.sesion.dominio.ServicioDeSesion
import com.solventa4bits.movil.sesion.ui.LoginViewModel
import com.solventa4bits.movil.sesion.ui.PantallaLogin
import com.solventa4bits.movil.traza.Traza
import com.solventa4bits.movil.ui.tema.TemaSolventa
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Aqui se arman las piezas de la app y se conectan entre si.
        val traza = Traza { origen, mensaje, causa -> Log.e(origen, mensaje, causa) }
        val almacen = AlmacenDeSesionSeguro(BovedaKeystore(applicationContext, traza), traza)
        val sesion = AdaptadorSesion(crearSesionApi(BuildConfig.URL_BASE_BFF), traza)
        val conSesion = ConSesion(almacen, sesion)
        val creditos = AdaptadorCreditos(crearCreditosApi(BuildConfig.URL_BASE_BFF), conSesion, traza)
        val cotizaciones = AdaptadorCotizaciones(crearCotizacionesApi(BuildConfig.URL_BASE_BFF), conSesion, traza)
        val evidencias = AdaptadorEvidencias(crearEvidenciasApi(BuildConfig.URL_BASE_BFF), conSesion, traza)

        setContent {
            TemaSolventa { AppSolventa(sesion, almacen, creditos, cotizaciones, evidencias) }
        }
    }
}

/**
 * Decide que pantalla se ve: sin sesion, el inicio de sesion; con sesion, el
 * inicio o la nueva cotizacion.
 */
@Composable
fun AppSolventa(
    sesion: ServicioDeSesion,
    almacen: AlmacenDeSesion,
    creditos: ServicioDeCreditos,
    cotizaciones: ServicioDeCotizaciones,
    evidencias: ServicioDeEvidencias,
    camara: Camara = camaraReal,
) {
    val login = viewModel { LoginViewModel(sesion, almacen) }
    val cotizacion = viewModel { NuevaCotizacionViewModel(creditos, cotizaciones) }
    val inicio = viewModel { InicioViewModel(cotizaciones) }
    // Preparar la foto pesa: va en segundo plano para no congelar la pantalla.
    val preparar: suspend (ByteArray) -> ByteArray? = { foto ->
        withContext(Dispatchers.Default) { PreparadorDeFotos.preparar(foto) }
    }
    val cedula = viewModel { CedulaViewModel(evidencias, preparar) }
    val selfie = viewModel { SelfieViewModel(evidencias, preparar) }
    var cotizando by rememberSaveable { mutableStateOf(false) }
    // Acepto las condiciones y esta cargando la cedula (BITS-275).
    var enCedula by rememberSaveable { mutableStateOf(false) }
    // La cotizacion se retomo desde el Home: "Atras" vuelve al Home.
    var retomada by rememberSaveable { mutableStateOf(false) }
    val cerrarSesion = {
        cotizando = false
        enCedula = false
        login.cerrarSesion()
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { relleno ->
        val conRelleno = Modifier.padding(relleno)
        when {
            !login.estado.sesionIniciada -> PantallaLogin(
                estado = login.estado,
                alCambiarCorreo = login::cambiarCorreo,
                alCambiarContrasena = login::cambiarContrasena,
                alIniciarSesion = login::iniciarSesion,
                modifier = conRelleno,
            )

            cotizando && enCedula -> TuCedula(
                cedula = cedula,
                selfie = selfie,
                camara = camara,
                alVolver = { enCedula = false },
                alCerrarSesion = cerrarSesion,
                modifier = conRelleno,
            )

            cotizando && cotizacion.estado.cotizacion != null && !cotizacion.estado.coberturasElegidas -> {
                val alInicio = {
                    cotizacion.volverAlCredito()
                    cotizando = false
                }
                val atras = if (retomada) alInicio else cotizacion::volverAlCredito
                BackHandler(onBack = atras)
                PantallaCoberturas(
                    cotizacion = checkNotNull(cotizacion.estado.cotizacion),
                    conTuCasa = cotizacion.estado.conTuCasa,
                    alCambiarTuCasa = cotizacion::cambiarTuCasa,
                    alContinuar = cotizacion::continuarALoQuePagas,
                    alVolver = atras,
                    alCerrarSesion = cerrarSesion,
                    modifier = conRelleno,
                )
            }

            cotizando && cotizacion.estado.cotizacion != null -> LoQuePagas(
                cotizacion = cotizacion,
                alInicio = { cotizando = false },
                // AC-1: aceptar lleva a cargar la cedula.
                alAceptar = {
                    cedula.abrir()
                    enCedula = true
                },
                alCerrarSesion = cerrarSesion,
                modifier = conRelleno,
            )

            cotizando -> {
                // Al girar el telefono esto se vuelve a ejecutar: no debe
                // borrar lo que el cliente ya escribio. Solo consulta si la
                // pantalla se restauro sin haber consultado nunca.
                LaunchedEffect(Unit) { if (!cotizacion.empezo) cotizacion.empezar() }
                // La sesion ya no se pudo refrescar: toca iniciar sesion de nuevo.
                LaunchedEffect(cotizacion.estado.sesionVencida) {
                    if (cotizacion.estado.sesionVencida) cerrarSesion()
                }
                BackHandler { cotizando = false }
                PantallaNuevaCotizacion(
                    estado = cotizacion.estado,
                    acciones = cotizacion,
                    alVolver = { cotizando = false },
                    alCerrarSesion = cerrarSesion,
                    modifier = conRelleno,
                )
            }

            else -> Inicio(
                inicio = inicio,
                // Cada vez que se entra desde el inicio, la cotizacion empieza de cero.
                alCotizar = {
                    cotizacion.empezar()
                    retomada = false
                    cotizando = true
                },
                alVerCotizacion = { enCurso ->
                    cotizacion.retomar(enCurso)
                    retomada = true
                    cotizando = true
                },
                alCerrarSesion = cerrarSesion,
                modifier = conRelleno,
            )
        }
    }
}

/** "Lo que pagas", con el modal para declinar sus condiciones (BITS-337). */
@Composable
private fun LoQuePagas(
    cotizacion: NuevaCotizacionViewModel,
    alInicio: () -> Unit,
    alAceptar: () -> Unit,
    alCerrarSesion: () -> Unit,
    modifier: Modifier,
) {
    val estado = cotizacion.estado
    // AC-3: declinada, vuelve al Inicio, que ya no la encuentra.
    LaunchedEffect(estado.declinada) { if (estado.declinada) alInicio() }
    LaunchedEffect(estado.sesionVencida) { if (estado.sesionVencida) alCerrarSesion() }
    BackHandler(onBack = cotizacion::volverACoberturas)
    PantallaLoQuePagas(
        cotizacion = checkNotNull(estado.cotizacion),
        sinAutorizacion = estado.sinConsentimiento,
        conTuCasa = estado.conTuCasa,
        alAceptar = alAceptar,
        declinacion = Declinacion(
            confirmando = estado.confirmandoDeclinacion,
            declinando = estado.declinando,
            fallo = estado.falloAlDeclinar,
            alPedir = cotizacion::pedirDeclinacion,
            alDeclinar = cotizacion::declinar,
            alCancelar = cotizacion::cancelarDeclinacion,
        ),
        alCerrarSesion = alCerrarSesion,
        modifier = modifier,
    )
}

/** "Tu cedula": las dos caras antes de la selfie (BITS-275). */
@Composable
private fun TuCedula(
    cedula: CedulaViewModel,
    selfie: SelfieViewModel,
    camara: Camara,
    alVolver: () -> Unit,
    alCerrarSesion: () -> Unit,
    modifier: Modifier,
) {
    LaunchedEffect(cedula.estado.sesionVencida) { if (cedula.estado.sesionVencida) alCerrarSesion() }
    // Las dos caras estan listas y el cliente paso a la selfie (BITS-235).
    var enSelfie by rememberSaveable { mutableStateOf(false) }
    if (enSelfie) {
        TuSelfie(selfie, camara, alVolver = { enSelfie = false }, alCerrarSesion = alCerrarSesion, modifier = modifier)
        return
    }
    // La cara a la que se le esta tomando foto (AC-6). Null: se ve "Tu cedula".
    var camaraPara by rememberSaveable { mutableStateOf<CaraDeCedula?>(null) }
    val cara = camaraPara
    if (cara != null) {
        // El gesto atras cierra la camara sin enviar nada.
        BackHandler { camaraPara = null }
        camara(
            QueFoto.CEDULA,
            { foto ->
                camaraPara = null
                cedula.enviar(cara, foto)
            },
            modifier,
        )
        return
    }
    // Sin boton "Atras" (AC-6): el gesto del telefono vuelve a "Lo que pagas".
    BackHandler(onBack = alVolver)
    PantallaCedula(
        estado = cedula.estado,
        alTomarFoto = { camaraPara = it },
        alContinuar = {
            selfie.abrir()
            enSelfie = true
        },
        alCerrarSesion = alCerrarSesion,
        modifier = modifier,
    )
}

/** El Home: cada vez que se muestra consulta la cotizacion en curso. */
@Composable
private fun Inicio(
    inicio: InicioViewModel,
    alCotizar: () -> Unit,
    alVerCotizacion: (Cotizacion) -> Unit,
    alCerrarSesion: () -> Unit,
    modifier: Modifier,
) {
    LaunchedEffect(Unit) { inicio.consultar() }
    LaunchedEffect(inicio.estado.sesionVencida) { if (inicio.estado.sesionVencida) alCerrarSesion() }
    PantallaInicio(
        cotizacionEnCurso = inicio.estado.cotizacionEnCurso,
        alCotizar = alCotizar,
        alVerCotizacion = alVerCotizacion,
        alCerrarSesion = alCerrarSesion,
        modifier = modifier,
    )
}

/** "Tu selfie": confirmar la identidad despues de la cedula (BITS-235). */
@Composable
private fun TuSelfie(
    selfie: SelfieViewModel,
    camara: Camara,
    alVolver: () -> Unit,
    alCerrarSesion: () -> Unit,
    modifier: Modifier,
) {
    LaunchedEffect(selfie.estado.sesionVencida) { if (selfie.estado.sesionVencida) alCerrarSesion() }
    // Tomando la selfie con la camara frontal (AC-4).
    var tomando by rememberSaveable { mutableStateOf(false) }
    if (tomando) {
        // El gesto atras cierra la camara sin enviar nada.
        BackHandler { tomando = false }
        camara(
            QueFoto.SELFIE,
            { foto ->
                tomando = false
                selfie.enviar(foto)
            },
            modifier,
        )
        return
    }
    BackHandler(onBack = alVolver)
    PantallaSelfie(
        estado = selfie.estado,
        miniatura = selfie.miniatura,
        alTomarSelfie = { tomando = true },
        alVolverACedula = alVolver,
        // Lo que sigue es enviar la solicitud (BITS-278): por ahora no hace nada.
        alContinuar = {},
        alCerrarSesion = alCerrarSesion,
        modifier = modifier,
    )
}
