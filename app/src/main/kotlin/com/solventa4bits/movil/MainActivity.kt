package com.solventa4bits.movil

import android.os.Bundle
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.solventa4bits.movil.cotizacion.ui.AccionesDeCotizacion
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
import com.solventa4bits.movil.red.Conexion
import com.solventa4bits.movil.red.ConexionDelTelefono
import com.solventa4bits.movil.red.GuardiaDeRed
import com.solventa4bits.movil.ui.SinConexion
import com.solventa4bits.movil.ui.tapadoSinRed
import com.solventa4bits.movil.seguro.datos.AlmacenDeSegurosRoom
import com.solventa4bits.movil.seguro.datos.BaseDeDatosSolventa
import com.solventa4bits.movil.seguro.dominio.AlmacenDeSeguros
import com.solventa4bits.movil.seguro.ui.PantallaMiPoliza
import com.solventa4bits.movil.solicitud.datos.AdaptadorSolicitudes
import com.solventa4bits.movil.solicitud.datos.crearSolicitudesApi
import com.solventa4bits.movil.solicitud.dominio.ServicioDeSolicitudes
import com.solventa4bits.movil.solicitud.ui.AccionesDeSolicitud
import com.solventa4bits.movil.solicitud.dominio.Solicitud
import com.solventa4bits.movil.solicitud.ui.PantallaResultado
import com.solventa4bits.movil.solicitud.ui.PantallaSolicitud
import com.solventa4bits.movil.solicitud.ui.ResultadoViewModel
import com.solventa4bits.movil.seguro.ui.SeguroViewModel
import com.solventa4bits.movil.seguro.ui.TuSeguro
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
        val guardados = AlmacenDeSegurosRoom(BaseDeDatosSolventa.crear(applicationContext).polizas())
        val conexion = ConexionDelTelefono(applicationContext)

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
                    guardados,
                    conexion,
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
    guardados: AlmacenDeSeguros,
    conexion: Conexion,
    camara: Camara = camaraReal,
    abrirEnlace: ((String) -> Unit)? = null,
) {
    val login = viewModel { LoginViewModel(sesion, almacen, preferencias, conHuella = autenticador.disponible()) }
    val cotizacion = viewModel { NuevaCotizacionViewModel(creditos, cotizaciones) }
    val inicio = viewModel { InicioViewModel(cotizaciones, solicitudes, guardados) }
    // Las acciones que necesitan el servidor esperan aqui si no hay red (BITS-217, AC-5).
    val red = remember { GuardiaDeRed(conexion) }
    // Preparar la foto pesa: va en segundo plano para no congelar la pantalla.
    val preparar: suspend (ByteArray) -> ByteArray? = { foto ->
        withContext(Dispatchers.Default) { PreparadorDeFotos.preparar(foto) }
    }
    val cedula = viewModel { CedulaViewModel(evidencias, preparar) }
    val selfie = viewModel { SelfieViewModel(evidencias, preparar) }
    val solicitud = viewModel { SolicitudViewModel(evidencias, solicitudes) }
    val resultado = viewModel { ResultadoViewModel(solicitudes) }
    val seguro = viewModel { SeguroViewModel(solicitudes) }
    var cotizando by rememberSaveable { mutableStateOf(false) }
    // Viendo el detalle de una poliza emitida (BITS-104).
    var viendoSeguro by rememberSaveable { mutableStateOf(false) }
    // Acepto las condiciones y esta cargando la cedula (BITS-275).
    var enCedula by rememberSaveable { mutableStateOf(false) }
    // Viendo el resultado de la solicitud, al enviarla o desde el Inicio (BITS-97).
    var viendoSolicitud by rememberSaveable { mutableStateOf(false) }
    // La cotizacion se retomo desde el Home: "Atras" vuelve al Home.
    var retomada by rememberSaveable { mutableStateOf(false) }
    val cerrarSesion = {
        cotizando = false
        enCedula = false
        viendoSolicitud = false
        viendoSeguro = false
        red.descartar()
        inicio.olvidarSeguros()
        login.cerrarSesion()
    }
    val alInicioSinRed = {
        red.descartar()
        cotizando = false
        enCedula = false
        viendoSolicitud = false
        viendoSeguro = false
        inicio.consultar()
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { relleno ->
        val conRelleno = Modifier.padding(relleno)
        Box(Modifier.fillMaxSize().tapadoSinRed(red)) {
            when {
                !login.estado.sesionIniciada -> AntesDeEntrar(
                    login = login,
                    autenticador = autenticador,
                    alUsarOtraCuenta = cerrarSesion,
                    modifier = conRelleno,
                )

                viendoSolicitud -> TuResultado(
                    resultado = resultado,
                    alInicio = { viendoSolicitud = false },
                    alCerrarSesion = cerrarSesion,
                    modifier = conRelleno,
                )

                viendoSeguro -> TuSeguro(
                    seguro = seguro,
                    abrirEnlace = abrirEnlace,
                    alInicio = { viendoSeguro = false },
                    alCerrarSesion = cerrarSesion,
                    modifier = conRelleno,
                )

                cotizando && enCedula -> TuCedula(
                    cedula = cedula,
                    selfie = selfie,
                    solicitud = solicitud,
                    camara = camara,
                    red = red,
                    // La solicitud va con la cotizacion que el cliente vio y las coberturas que eligio.
                    alAbrirSolicitud = {
                        solicitud.abrir(checkNotNull(cotizacion.estado.cotizacion), cotizacion.estado.conTuCasa)
                    },
                    alInicio = {
                        enCedula = false
                        cotizando = false
                    },
                    alVerResultado = { enviada ->
                        resultado.mostrar(enviada)
                        enCedula = false
                        cotizando = false
                        viendoSolicitud = true
                    },
                    alVolver = { enCedula = false },
                    alCerrarSesion = cerrarSesion,
                    modifier = conRelleno,
                )

                cotizando &&
                    cotizacion.estado.cotizacion != null &&
                    !cotizacion.estado.coberturasElegidas -> TusCoberturas(
                    cotizacion = cotizacion,
                    retomada = retomada,
                    alInicio = { cotizando = false },
                    alCerrarSesion = cerrarSesion,
                    modifier = conRelleno,
                )

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

                cotizando -> NuevaCotizacion(
                    cotizacion = cotizacion,
                    red = red,
                    alVolver = { cotizando = false },
                    alCerrarSesion = cerrarSesion,
                    modifier = conRelleno,
                )

                else -> Inicio(
                    inicio = inicio,
                    huellaActivada = login.estado.huellaActivada,
                    alAlternarHuella = { login.alternarHuella(autenticador) },
                    // Cada vez que se entra desde el inicio, la cotizacion empieza de cero.
                    alCotizar = {
                        red.conRed {
                            cotizacion.empezar()
                            retomada = false
                            cotizando = true
                        }
                    },
                    alVerCotizacion = { enCurso ->
                        red.conRed {
                            cotizacion.retomar(enCurso)
                            retomada = true
                            cotizando = true
                        }
                    },
                    alVerSolicitud = { enviada ->
                        resultado.abrir(enviada.id)
                        viendoSolicitud = true
                    },
                    alVerSeguro = { emitida ->
                        seguro.mostrar(emitida, inicio.estado.actualizadosEn)
                        viendoSeguro = true
                    },
                    alCerrarSesion = cerrarSesion,
                    modifier = conRelleno,
                )
            }
        }
        SinConexion(red, alInicio = alInicioSinRed, alCerrarSesion = cerrarSesion, modifier = conRelleno)
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
    red: GuardiaDeRed,
    alAbrirSolicitud: () -> Unit,
    alInicio: () -> Unit,
    alVerResultado: (Solicitud) -> Unit,
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
            red = red,
            alAbrirSolicitud = alAbrirSolicitud,
            alInicio = alInicio,
            alVerResultado = alVerResultado,
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

/**
 * El Home: cada vez que se muestra consulta la cotizacion en curso. Sin
 * conexion, muestra las polizas guardadas en el telefono (BITS-217, AC-2).
 */
@Composable
private fun Inicio(
    inicio: InicioViewModel,
    huellaActivada: Boolean?,
    alAlternarHuella: () -> Unit,
    alCotizar: () -> Unit,
    alVerCotizacion: (Cotizacion) -> Unit,
    alVerSolicitud: (Solicitud) -> Unit,
    alVerSeguro: (Solicitud) -> Unit,
    alCerrarSesion: () -> Unit,
    modifier: Modifier,
) {
    LaunchedEffect(Unit) { inicio.consultar() }
    LaunchedEffect(inicio.estado.sesionVencida) { if (inicio.estado.sesionVencida) alCerrarSesion() }
    if (inicio.estado.sinConexion) {
        PantallaMiPoliza(
            seguros = inicio.estado.seguros,
            actualizadosEn = inicio.estado.actualizadosEn,
            alVerSeguro = alVerSeguro,
            alReintentar = inicio::consultar,
            alCerrarSesion = alCerrarSesion,
            modifier = modifier,
        )
        return
    }
    PantallaInicio(
        cotizacionEnCurso = inicio.estado.cotizacionEnCurso,
        alCotizar = alCotizar,
        alVerCotizacion = alVerCotizacion,
        alCerrarSesion = alCerrarSesion,
        modifier = modifier,
        huellaActivada = huellaActivada,
        alAlternarHuella = alAlternarHuella,
        solicitudReciente = inicio.estado.solicitudReciente,
        alVerSolicitud = alVerSolicitud,
        seguros = inicio.estado.seguros,
        alVerSeguro = alVerSeguro,
    )
}

/** "Nueva cotizacion": los datos del credito, del banco o escritos por el cliente (BITS-219). */
@Composable
private fun NuevaCotizacion(
    cotizacion: NuevaCotizacionViewModel,
    red: GuardiaDeRed,
    alVolver: () -> Unit,
    alCerrarSesion: () -> Unit,
    modifier: Modifier,
) {
    // Al girar el telefono esto se vuelve a ejecutar: no debe
    // borrar lo que el cliente ya escribio. Solo consulta si la
    // pantalla se restauro sin haber consultado nunca.
    LaunchedEffect(Unit) { if (!cotizacion.empezo) cotizacion.empezar() }
    // La sesion ya no se pudo refrescar: toca iniciar sesion de nuevo.
    LaunchedEffect(cotizacion.estado.sesionVencida) {
        if (cotizacion.estado.sesionVencida) alCerrarSesion()
    }
    BackHandler(onBack = alVolver)
    PantallaNuevaCotizacion(
        estado = cotizacion.estado,
        acciones = object : AccionesDeCotizacion by cotizacion {
            override fun cotizar() = red.conRed(cotizacion::cotizar)
        },
        alVolver = alVolver,
        alCerrarSesion = alCerrarSesion,
        modifier = modifier,
    )
}

/** "Tu selfie": confirmar la identidad despues de la cedula (BITS-235). */
@Composable
private fun TuSelfie(
    selfie: SelfieViewModel,
    solicitud: SolicitudViewModel,
    camara: Camara,
    red: GuardiaDeRed,
    alAbrirSolicitud: () -> Unit,
    alInicio: () -> Unit,
    alVerResultado: (Solicitud) -> Unit,
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
            red = red,
            alInicio = alInicio,
            alVerResultado = alVerResultado,
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

/** "Revisa y envia tu solicitud": con la solicitud registrada, muestra su resultado. */
@Composable
private fun TuSolicitud(
    solicitud: SolicitudViewModel,
    red: GuardiaDeRed,
    alInicio: () -> Unit,
    alVerResultado: (Solicitud) -> Unit,
    alVolver: () -> Unit,
    alCerrarSesion: () -> Unit,
    modifier: Modifier,
) {
    val estado = solicitud.estado
    LaunchedEffect(estado.sesionVencida) { if (estado.sesionVencida) alCerrarSesion() }
    LaunchedEffect(estado.enviada) { estado.enviada?.let(alVerResultado) }
    BackHandler(onBack = alVolver)
    PantallaSolicitud(
        estado = estado,
        acciones = AccionesDeSolicitud(
            alCambiarAceptacion = solicitud::cambiarAceptacion,
            alVerCondiciones = solicitud::verCondiciones,
            alCerrarCondiciones = solicitud::cerrarCondiciones,
            alAceptarCondiciones = solicitud::aceptarDesdeCondiciones,
            alEnviar = { red.conRed(solicitud::enviar) },
            alReintentar = solicitud::reintentar,
            alIrAlInicio = alInicio,
        ),
        alCerrarSesion = alCerrarSesion,
        modifier = modifier,
    )
}

/** El resultado de la solicitud: en revision, no aprobada o aprobada (BITS-97). */
@Composable
private fun TuResultado(
    resultado: ResultadoViewModel,
    alInicio: () -> Unit,
    alCerrarSesion: () -> Unit,
    modifier: Modifier,
) {
    val estado = resultado.estado
    LaunchedEffect(estado.sesionVencida) { if (estado.sesionVencida) alCerrarSesion() }
    LaunchedEffect(estado.noExiste) { if (estado.noExiste) alInicio() }
    BackHandler(onBack = alInicio)
    PantallaResultado(
        estado = estado,
        alReintentar = resultado::reintentar,
        alInicio = alInicio,
        alCerrarSesion = alCerrarSesion,
        modifier = modifier,
    )
}

/** "Tus coberturas" (BITS-96). Si se retomo desde el Inicio, "Atras" vuelve al Inicio. */
@Composable
private fun TusCoberturas(
    cotizacion: NuevaCotizacionViewModel,
    retomada: Boolean,
    alInicio: () -> Unit,
    alCerrarSesion: () -> Unit,
    modifier: Modifier,
) {
    val atras = if (retomada) {
        {
            cotizacion.volverAlCredito()
            alInicio()
        }
    } else {
        cotizacion::volverAlCredito
    }
    BackHandler(onBack = atras)
    PantallaCoberturas(
        cotizacion = checkNotNull(cotizacion.estado.cotizacion),
        conTuCasa = cotizacion.estado.conTuCasa,
        alCambiarTuCasa = cotizacion::cambiarTuCasa,
        alContinuar = cotizacion::continuarALoQuePagas,
        alVolver = atras,
        alCerrarSesion = alCerrarSesion,
        modifier = modifier,
    )
}
