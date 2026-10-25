package com.solventa4bits.movil.sesion.ui

import com.solventa4bits.movil.sesion.dominio.AlmacenEnMemoria
import com.solventa4bits.movil.sesion.dominio.AutenticadorDeHuella
import com.solventa4bits.movil.sesion.dominio.AutenticadorFalso
import com.solventa4bits.movil.sesion.dominio.PreferenciaDeHuella
import com.solventa4bits.movil.sesion.dominio.PreferenciasEnMemoria
import com.solventa4bits.movil.sesion.dominio.ResultadoHuella
import com.solventa4bits.movil.sesion.dominio.ResultadoInicioSesion
import com.solventa4bits.movil.sesion.dominio.ServicioDeSesion
import com.solventa4bits.movil.sesion.dominio.Sesion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    private val sesion = Sesion("jwt-de-acceso", "token-de-refresco", 3600)
    private val almacen = AlmacenEnMemoria()
    private val preferencias = PreferenciasEnMemoria()
    private val sensor = AutenticadorFalso()

    // El ViewModel trabaja en el hilo principal de Android, que no existe en
    // una prueba unitaria: se reemplaza por uno de prueba.
    @BeforeEach
    fun preparar() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterEach
    fun restaurar() {
        Dispatchers.resetMain()
    }

    @Test
    @DisplayName("el boton solo se habilita con correo y contrasena escritos")
    fun botonHabilitado() {
        val viewModel = viewModelQueResponde(ResultadoInicioSesion.Exito(sesion))
        assertFalse(viewModel.estado.puedeEnviar)

        viewModel.cambiarCorreo("ana@correo.com")
        assertFalse(viewModel.estado.puedeEnviar)

        viewModel.cambiarContrasena("Clave-Segura-123")
        assertTrue(viewModel.estado.puedeEnviar)
    }

    @Test
    @DisplayName("quita los espacios del inicio y el final del correo, pero no toca la contrasena")
    fun quitaEspaciosDelCorreo() {
        val viewModel = viewModelQueResponde(ResultadoInicioSesion.Exito(sesion))

        viewModel.cambiarCorreo(" ana@correo.com ")
        viewModel.cambiarContrasena(" Clave con espacios ")
        viewModel.iniciarSesion()

        assertEquals("ana@correo.com", correoEnviado)
        assertEquals(" Clave con espacios ", contrasenaEnviada)
    }

    @Test
    @DisplayName("con credenciales correctas guarda la sesion y marca que entro")
    fun credencialesCorrectas() {
        val viewModel = viewModelQueResponde(ResultadoInicioSesion.Exito(sesion))

        viewModel.escribirCredencialesEIniciar()

        assertTrue(viewModel.estado.sesionIniciada)
        assertEquals(sesion, almacen.leer())
    }

    @Test
    @DisplayName("con credenciales invalidas muestra el error y no guarda nada")
    fun credencialesInvalidas() {
        val viewModel = viewModelQueResponde(ResultadoInicioSesion.CredencialesInvalidas)

        viewModel.escribirCredencialesEIniciar()

        assertEquals(ErrorLogin.CREDENCIALES_INVALIDAS, viewModel.estado.error)
        assertFalse(viewModel.estado.sesionIniciada)
        assertNull(almacen.leer())
    }

    @Test
    @DisplayName("si el inicio de sesion falla muestra el error general")
    fun fallo() {
        val viewModel = viewModelQueResponde(ResultadoInicioSesion.Fallo)

        viewModel.escribirCredencialesEIniciar()

        assertEquals(ErrorLogin.FALLO, viewModel.estado.error)
        assertFalse(viewModel.estado.cargando)
    }

    @Test
    @DisplayName("al abrir con sesion guardada entra y la renueva por debajo")
    fun refrescaAlAbrir() {
        almacen.guardar(sesion)
        val nueva = Sesion("jwt-nuevo", "refresco-nuevo", 3600)

        val viewModel = viewModelQueResponde(refresco = ResultadoInicioSesion.Exito(nueva))

        assertTrue(viewModel.estado.sesionIniciada)
        assertEquals(nueva, almacen.leer())
    }

    @Test
    @DisplayName("si el token de refresco ya vencio borra la sesion y pide iniciar sesion")
    fun refrescoVencido() {
        almacen.guardar(sesion)

        val viewModel = viewModelQueResponde(refresco = ResultadoInicioSesion.CredencialesInvalidas)

        assertFalse(viewModel.estado.sesionIniciada)
        assertNull(almacen.leer())
    }

    @Test
    @DisplayName("si no se puede refrescar por falta de red conserva la sesion y entra")
    fun refrescoSinRed() {
        almacen.guardar(sesion)

        val viewModel = viewModelQueResponde(refresco = ResultadoInicioSesion.Fallo)

        assertTrue(viewModel.estado.sesionIniciada)
        assertEquals(sesion, almacen.leer())
    }

    @Test
    @DisplayName("al cerrar sesion borra los datos locales y vuelve al inicio de sesion")
    fun cerrarSesion() {
        val viewModel = viewModelQueResponde(ResultadoInicioSesion.Exito(sesion))
        viewModel.escribirCredencialesEIniciar()

        viewModel.cerrarSesion()

        assertNull(almacen.leer())
        assertEquals(EstadoLogin(), viewModel.estado)
    }

    @Nested
    @DisplayName("huella: activarla al entrar con contrasena (AC-1 y AC-3)")
    inner class OfrecerHuella {

        @Test
        @DisplayName("con sensor y sin decision previa, al entrar ofrece activar la huella en vez de pasar")
        fun ofreceAlEntrar() {
            val viewModel = viewModelQueResponde(ResultadoInicioSesion.Exito(sesion), autenticador = sensor)

            viewModel.escribirCredencialesEIniciar()

            assertEquals(PasoLogin.OFRECER_HUELLA, viewModel.estado.paso)
            assertFalse(viewModel.estado.sesionIniciada)
            assertEquals(sesion, almacen.leer())
        }

        @Test
        @DisplayName("sin sensor entra directo y no se habla de huella")
        fun sinSensorNoOfrece() {
            val viewModel = viewModelQueResponde(ResultadoInicioSesion.Exito(sesion))

            viewModel.escribirCredencialesEIniciar()

            assertTrue(viewModel.estado.sesionIniciada)
            assertNull(viewModel.estado.huella)
        }

        @Test
        @DisplayName("si ya dijo ahora no, entra directo sin volver a preguntar")
        fun rechazadaNoOfrece() {
            preferencias.guardarHuella(PreferenciaDeHuella.RECHAZADA)
            val viewModel = viewModelQueResponde(ResultadoInicioSesion.Exito(sesion), autenticador = sensor)

            viewModel.escribirCredencialesEIniciar()

            assertTrue(viewModel.estado.sesionIniciada)
            assertEquals(PreferenciaDeHuella.RECHAZADA, viewModel.estado.huella)
        }

        @Test
        @DisplayName("al activarla, el sensor la confirma, queda activada y entra")
        fun activar() {
            val viewModel = viewModelQueResponde(ResultadoInicioSesion.Exito(sesion), autenticador = sensor)
            viewModel.escribirCredencialesEIniciar()

            viewModel.decidirHuella(activar = true)

            assertEquals(1, sensor.veces)
            assertEquals(PreferenciaDeHuella.ACTIVADA, preferencias.huella())
            assertEquals(PreferenciaDeHuella.ACTIVADA, viewModel.estado.huella)
            assertTrue(viewModel.estado.sesionIniciada)
        }

        @Test
        @DisplayName("si el sensor no la confirma al activarla, entra igual y queda sin decidir")
        fun activarSinConfirmar() {
            sensor.respuesta = ResultadoHuella.CANCELADO
            val viewModel = viewModelQueResponde(ResultadoInicioSesion.Exito(sesion), autenticador = sensor)
            viewModel.escribirCredencialesEIniciar()

            viewModel.decidirHuella(activar = true)

            assertEquals(PreferenciaDeHuella.SIN_DECIDIR, preferencias.huella())
            assertTrue(viewModel.estado.sesionIniciada)
        }

        @Test
        @DisplayName("con ahora no entra sin tocar el sensor y queda rechazada")
        fun ahoraNo() {
            val viewModel = viewModelQueResponde(ResultadoInicioSesion.Exito(sesion), autenticador = sensor)
            viewModel.escribirCredencialesEIniciar()

            viewModel.decidirHuella(activar = false)

            assertEquals(0, sensor.veces)
            assertEquals(PreferenciaDeHuella.RECHAZADA, preferencias.huella())
            assertTrue(viewModel.estado.sesionIniciada)
        }
    }

    @Nested
    @DisplayName("huella: entrar con ella al abrir la app (AC-2)")
    inner class EntrarConHuella {

        @BeforeEach
        fun conHuellaActivada() {
            almacen.guardar(sesion)
            preferencias.guardarHuella(PreferenciaDeHuella.ACTIVADA)
        }

        @Test
        @DisplayName("abre pidiendo la huella, sin entrar ni refrescar todavia")
        fun abreEnLaHuella() {
            val viewModel = viewModelQueResponde(refresco = ResultadoInicioSesion.Exito(sesion), autenticador = sensor)

            assertEquals(PasoLogin.ENTRAR_CON_HUELLA, viewModel.estado.paso)
            assertFalse(viewModel.estado.sesionIniciada)
            assertEquals(0, refrescosPedidos)
        }

        @Test
        @DisplayName("si el telefono ya no tiene sensor entra directo, como sin huella")
        fun sinSensorEntraDirecto() {
            val viewModel = viewModelQueResponde(refresco = ResultadoInicioSesion.Exito(sesion))

            assertTrue(viewModel.estado.sesionIniciada)
            assertEquals(1, refrescosPedidos)
        }

        @Test
        @DisplayName("con la huella correcta entra y renueva la sesion por debajo")
        fun huellaCorrecta() {
            val nueva = Sesion("jwt-nuevo", "refresco-nuevo", 3600)
            val viewModel = viewModelQueResponde(refresco = ResultadoInicioSesion.Exito(nueva), autenticador = sensor)

            viewModel.entrarConHuella()

            assertTrue(viewModel.estado.sesionIniciada)
            assertEquals(nueva, almacen.leer())
        }

        @Test
        @DisplayName("con la huella correcta pero mas de 24 horas sin abrir, pide credenciales")
        fun huellaCorrectaSesionVencida() {
            val viewModel =
                viewModelQueResponde(refresco = ResultadoInicioSesion.CredencialesInvalidas, autenticador = sensor)

            viewModel.entrarConHuella()

            assertFalse(viewModel.estado.sesionIniciada)
            assertEquals(PasoLogin.CREDENCIALES, viewModel.estado.paso)
            assertNull(almacen.leer())
        }

        @Test
        @DisplayName("si el telefono no la reconoce pasa a credenciales y conserva la sesion guardada")
        fun huellaNoReconocida() {
            sensor.respuesta = ResultadoHuella.FALLO
            val viewModel = viewModelQueResponde(refresco = ResultadoInicioSesion.Exito(sesion), autenticador = sensor)

            viewModel.entrarConHuella()

            assertFalse(viewModel.estado.sesionIniciada)
            assertEquals(PasoLogin.CREDENCIALES, viewModel.estado.paso)
            assertEquals(sesion, almacen.leer())
            assertEquals(PreferenciaDeHuella.ACTIVADA, preferencias.huella())
        }

        @Test
        @DisplayName("si prefiere la contrasena, entra con ella y no se le vuelve a ofrecer la huella")
        fun prefiereContrasena() {
            val viewModel = viewModelQueResponde(ResultadoInicioSesion.Exito(sesion), autenticador = sensor)

            viewModel.entrarConContrasena()
            assertEquals(PasoLogin.CREDENCIALES, viewModel.estado.paso)

            viewModel.escribirCredencialesEIniciar()
            assertTrue(viewModel.estado.sesionIniciada)
            assertEquals(0, sensor.veces)
        }
    }

    @Nested
    @DisplayName("huella: apagarla y prenderla (AC-6 y menu)")
    inner class ApagarYPrender {

        @Test
        @DisplayName("al cerrar sesion la huella queda desactivada y se ofrece otra vez al entrar")
        fun cerrarSesionLaDesactiva() {
            preferencias.guardarHuella(PreferenciaDeHuella.ACTIVADA)
            val viewModel = viewModelQueResponde(ResultadoInicioSesion.Exito(sesion), autenticador = sensor)

            viewModel.cerrarSesion()

            assertEquals(PreferenciaDeHuella.SIN_DECIDIR, preferencias.huella())
            assertEquals(EstadoLogin(huella = PreferenciaDeHuella.SIN_DECIDIR), viewModel.estado)

            viewModel.escribirCredencialesEIniciar()
            assertEquals(PasoLogin.OFRECER_HUELLA, viewModel.estado.paso)
        }

        @Test
        @DisplayName("desde el menu la apaga sin tocar el sensor")
        fun apagarDesdeElMenu() {
            preferencias.guardarHuella(PreferenciaDeHuella.ACTIVADA)
            val viewModel = viewModelQueResponde(autenticador = sensor)

            viewModel.alternarHuella()

            assertEquals(0, sensor.veces)
            assertEquals(PreferenciaDeHuella.RECHAZADA, preferencias.huella())
            assertEquals(PreferenciaDeHuella.RECHAZADA, viewModel.estado.huella)
        }

        @Test
        @DisplayName("desde el menu la prende solo si el sensor la confirma")
        fun prenderDesdeElMenu() {
            preferencias.guardarHuella(PreferenciaDeHuella.RECHAZADA)
            val viewModel = viewModelQueResponde(autenticador = sensor)

            sensor.respuesta = ResultadoHuella.CANCELADO
            viewModel.alternarHuella()
            assertEquals(PreferenciaDeHuella.RECHAZADA, preferencias.huella())

            sensor.respuesta = ResultadoHuella.EXITO
            viewModel.alternarHuella()
            assertEquals(PreferenciaDeHuella.ACTIVADA, preferencias.huella())
            assertEquals(PreferenciaDeHuella.ACTIVADA, viewModel.estado.huella)
        }

        @Test
        @DisplayName("sin sensor el menu no hace nada")
        fun sinSensorNoHaceNada() {
            val viewModel = viewModelQueResponde()

            viewModel.alternarHuella()

            assertEquals(PreferenciaDeHuella.SIN_DECIDIR, preferencias.huella())
            assertNull(viewModel.estado.huella)
        }
    }

    private var correoEnviado: String? = null
    private var contrasenaEnviada: String? = null
    private var refrescosPedidos = 0

    private fun viewModelQueResponde(
        inicio: ResultadoInicioSesion = ResultadoInicioSesion.Fallo,
        refresco: ResultadoInicioSesion = ResultadoInicioSesion.Fallo,
        autenticador: AutenticadorDeHuella = AutenticadorFalso(disponible = false),
    ): LoginViewModel {
        val servicio = object : ServicioDeSesion {
            override suspend fun iniciarSesion(correo: String, contrasena: String): ResultadoInicioSesion {
                correoEnviado = correo
                contrasenaEnviada = contrasena
                return inicio
            }

            override suspend fun refrescar(tokenDeRefresco: String): ResultadoInicioSesion {
                refrescosPedidos++
                return refresco
            }
        }
        return LoginViewModel(servicio, almacen, autenticador, preferencias)
    }

    private fun LoginViewModel.escribirCredencialesEIniciar() {
        cambiarCorreo("ana@correo.com")
        cambiarContrasena("Clave-Segura-123")
        iniciarSesion()
    }
}
