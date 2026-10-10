package com.solventa4bits.movil.sesion.ui

import com.solventa4bits.movil.sesion.dominio.AlmacenEnMemoria
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
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    private val sesion = Sesion("jwt-de-acceso", "token-de-refresco", 3600)
    private val almacen = AlmacenEnMemoria()

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

    private fun viewModelQueResponde(
        inicio: ResultadoInicioSesion = ResultadoInicioSesion.Fallo,
        refresco: ResultadoInicioSesion = ResultadoInicioSesion.Fallo,
    ): LoginViewModel {
        val servicio = object : ServicioDeSesion {
            override suspend fun iniciarSesion(correo: String, contrasena: String) = inicio
            override suspend fun refrescar(tokenDeRefresco: String) = refresco
        }
        return LoginViewModel(servicio, almacen)
    }

    private fun LoginViewModel.escribirCredencialesEIniciar() {
        cambiarCorreo("ana@correo.com")
        cambiarContrasena("Clave-Segura-123")
        iniciarSesion()
    }
}
