package com.solventa4bits.movil

import android.os.Bundle
import android.util.Log
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
import androidx.fragment.app.FragmentActivity
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
import com.solventa4bits.movil.solicitud.datos.AdaptadorSolicitudes
import com.solventa4bits.movil.solicitud.datos.crearSolicitudesApi
import com.solventa4bits.movil.solicitud.dominio.ServicioDeSolicitudes
import com.solventa4bits.movil.solicitud.ui.AccionesDeSolicitud
import com.solventa4bits.movil.solicitud.ui.PantallaSolicitud
import com.solventa4bits.movil.solicitud.ui.SolicitudViewModel
import com.solventa4bits.movil.inicio.ui.PantallaInicio
import com.solventa4bits.movil.sesion.datos.AdaptadorSesion
import com.solventa4bits.movil.sesion.datos.AlmacenDeSesionSeguro
import com.solventa4bits.movil.sesion.datos.AutenticadorBiometrico
import com.solventa4bits.movil.sesion.datos.BovedaKeystore
import com.solventa4bits.movil.sesion.datos.ConSesion
import com.solventa4bits.movil.sesion.datos.PreferenciasDelTelefono
import com.solventa4bits.movil.sesion.datos.crearSesionApi
import com.solventa4bits.movil.sesion.dominio.AlmacenDePreferencias
import com.solventa4bits.movil.sesion.dominio.AlmacenDeSesion
import com.solventa4bits.movil.sesion.dominio.AutenticadorDeHuella
import com.solventa4bits.movil.sesion.dominio.ServicioDeSesion
import com.solventa4bits.movil.sesion.ui.LoginViewModel
import com.solventa4bits.movil.sesion.ui.PantallaActivarHuella
import com.solventa4bits.movil.sesion.ui.PantallaEntrarConHuella
import com.solventa4bits.movil.sesion.ui.PantallaLogin
import com.solventa4bits.movil.sesion.ui.PasoLogin
import com.solventa4bits.movil.traza.Traza
import com.solventa4bits.movil.ui.tema.TemaSolventa
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// FragmentActivity y no ComponentActivity: el dialogo de la huella lo exige.
class MainActivity : FragmentActivity() {
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
        val solicitudes = AdaptadorSolicitudes(crearSolicitudesApi(BuildConfig.URL_BASE_BFF), conSesion, traza)
        val autenticador = AutenticadorBiometrico(this)
        val preferencias = PreferenciasDelTelefono(applicationContext)

        setContent {
            TemaSolventa {
                AppSolventa(
                    sesion,
                    almacen,
                    creditos,
                    cotizaciones,
                    evidencias,
                    solicitudes,
                    autenticador,
                    preferencias,
                )
            }
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
    solicitudes: ServicioDeSolicitudes,
    autenticador: AutenticadorDeHuella,
    preferencias: AlmacenDePreferencias,
    camara: Camara = camaraReal,
) {
    val login = viewModel { LoginViewModel(sesion, almacen, preferencias, conHuella = autenticador.disponible()) }
    val cotizacion = viewModel { NuevaCotizacionViewModel(creditos, cotizaciones) }
    val inicio = viewModel { InicioViewModel(cotizaciones) }
    // Preparar la foto pesa: va en segundo plano para no congelar la pantalla.
    val preparar: suspend (ByteArray) -> ByteArray? = { foto ->
        withContext(Dispatchers.Default) { PreparadorDeFotos.preparar(foto) }
    }
    val cedula = viewModel { CedulaViewModel(evidencias, preparar) }
    val selfie = viewModel { SelfieViewModel(evidencias, preparar) }
    val solicitud = viewModel { SolicitudViewModel(evidencias, solicitudes) }
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
            !login.estado.sesionIniciada -> AntesDeEntrar(
                login = login,
                autenticador = autenticador,
                alUsarOtraCuenta = cerrarSesion,
                modifier = conRelleno,
            )

            cotizando && enCedula -> TuCedula(
                cedula = cedula,
                selfie = selfie,
                solicitud = solicitud,
                camara = camara,
                // La solicitud va con la cotizacion que el cliente vio y las coberturas que eligio.
                alAbrirSolicitud = {
                    solicitud.abrir(checkNotNull(cotizacion.estado.cotizacion), cotizacion.estado.conTuCasa)
                },
                alInicio = {
                    enCedula = false
                    cotizando = false
                },
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
                huellaActivada = login.estado.huellaActivada,
                alAlternarHuella = { login.alternarHuella(autenticador) },
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
    solicitud: SolicitudViewModel,
    camara: Camara,
    alAbrirSolicitud: () -> Unit,
    alInicio: () -> Unit,
    alVolver: () -> Unit,
    alCerrarSesion: () -> Unit,
    modifier: Modifier,
) {
    LaunchedEffect(cedula.estado.sesionVencida) { if (cedula.estado.sesionVencida) alCerrarSesion() }
    // Las dos caras estan listas y el cliente paso a la selfie (BITS-235).
    var enSelfie by rememberSaveable { mutableStateOf(false) }
    if (enSelfie) {
        TuSelfie(
            selfie = selfie,
            solicitud = solicitud,
            camara = camara,
            alAbrirSolicitud = alAbrirSolicitud,
            alInicio = alInicio,
            alVolver = { enSelfie = false },
            alCerrarSesion = alCerrarSesion,
            modifier = modifier,
        )
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

/**
 * Lo que se ve sin sesion: las credenciales o, con sensor de huella, la
 * oferta de activarla tras entrar y la pantalla de entrar con ella (BITS-353).
 */
@Composable
private fun AntesDeEntrar(
    login: LoginViewModel,
    autenticador: AutenticadorDeHuella,
    alUsarOtraCuenta: () -> Unit,
    modifier: Modifier,
) {
    when (login.estado.paso) {
        PasoLogin.OFRECER_HUELLA -> PantallaActivarHuella(
            cargando = login.estado.cargando,
            alActivar = { login.decidirHuella(activar = true, autenticador) },
            alAhoraNo = { login.decidirHuella(activar = false, autenticador) },
            modifier = modifier,
        )

        PasoLogin.ENTRAR_CON_HUELLA -> PantallaEntrarConHuella(
            alTocarSensor = { login.entrarConHuella(autenticador) },
            alEntrarConContrasena = login::entrarConContrasena,
            alUsarOtraCuenta = alUsarOtraCuenta,
            modifier = modifier,
        )

        PasoLogin.CREDENCIALES -> PantallaLogin(
            estado = login.estado,
            alCambiarCorreo = login::cambiarCorreo,
            alCambiarContrasena = login::cambiarContrasena,
            alIniciarSesion = login::iniciarSesion,
            modifier = modifier,
        )
    }
}

/** El Home: cada vez que se muestra consulta la cotizacion en curso. */
@Composable
private fun Inicio(
    inicio: InicioViewModel,
    huellaActivada: Boolean?,
    alAlternarHuella: () -> Unit,
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
        huellaActivada = huellaActivada,
        alAlternarHuella = alAlternarHuella,
    )
}

/** "Tu selfie": confirmar la identidad despues de la cedula (BITS-235). */
@Composable
private fun TuSelfie(
    selfie: SelfieViewModel,
    solicitud: SolicitudViewModel,
    camara: Camara,
    alAbrirSolicitud: () -> Unit,
    alInicio: () -> Unit,
    alVolver: () -> Unit,
    alCerrarSesion: () -> Unit,
    modifier: Modifier,
) {
    LaunchedEffect(selfie.estado.sesionVencida) { if (selfie.estado.sesionVencida) alCerrarSesion() }
    // Con la cedula y la selfie listas, el cliente revisa y envia su solicitud (BITS-278).
    var enSolicitud by rememberSaveable { mutableStateOf(false) }
    if (enSolicitud) {
        TuSolicitud(
            solicitud = solicitud,
            alInicio = alInicio,
            alVolver = { enSolicitud = false },
            alCerrarSesion = alCerrarSesion,
            modifier = modifier,
        )
        return
    }
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
        alContinuar = {
            alAbrirSolicitud()
            enSolicitud = true
        },
        alCerrarSesion = alCerrarSesion,
        modifier = modifier,
    )
}

/** "Revisa y envia tu solicitud": con la solicitud registrada, vuelve al Inicio. */
@Composable
private fun TuSolicitud(
    solicitud: SolicitudViewModel,
    alInicio: () -> Unit,
    alVolver: () -> Unit,
    alCerrarSesion: () -> Unit,
    modifier: Modifier,
) {
    val estado = solicitud.estado
    LaunchedEffect(estado.sesionVencida) { if (estado.sesionVencida) alCerrarSesion() }
    LaunchedEffect(estado.enviada) { if (estado.enviada != null) alInicio() }
    BackHandler(onBack = alVolver)
    PantallaSolicitud(
        estado = estado,
        acciones = AccionesDeSolicitud(
            alCambiarAceptacion = solicitud::cambiarAceptacion,
            alVerCondiciones = solicitud::verCondiciones,
            alCerrarCondiciones = solicitud::cerrarCondiciones,
            alAceptarCondiciones = solicitud::aceptarDesdeCondiciones,
            alEnviar = solicitud::enviar,
            alReintentar = solicitud::reintentar,
            alIrAlInicio = alInicio,
        ),
        alCerrarSesion = alCerrarSesion,
        modifier = modifier,
    )
}
