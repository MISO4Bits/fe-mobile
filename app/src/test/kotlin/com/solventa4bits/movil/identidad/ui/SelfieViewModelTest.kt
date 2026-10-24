package com.solventa4bits.movil.identidad.ui

import com.solventa4bits.movil.identidad.dominio.CaraDeCedula
import com.solventa4bits.movil.identidad.dominio.EstadoGuardado
import com.solventa4bits.movil.identidad.dominio.MotivoDeRechazo
import com.solventa4bits.movil.identidad.dominio.ResultadoEstado
import com.solventa4bits.movil.identidad.dominio.ResultadoSubida
import com.solventa4bits.movil.identidad.dominio.ServicioDeEvidencias
import com.solventa4bits.movil.identidad.dominio.Veredicto
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SelfieViewModelTest {

    private val foto = byteArrayOf(1, 2, 3)

    /** Doble de prueba: responde lo que tenga en ese momento y cuenta las selfies enviadas. */
    private class EvidenciasFalsas : ServicioDeEvidencias {
        var estado: ResultadoEstado = ResultadoEstado.Exito(emptyMap())
        var subida: ResultadoSubida = ResultadoSubida.Exito(Veredicto.Aceptada)

        /** Si no es null, la subida espera a que se complete: sirve para ver "revisando". */
        var enEspera: CompletableDeferred<Unit>? = null
        val selfies = mutableListOf<ByteArray>()

        override suspend fun subir(cara: CaraDeCedula, imagen: ByteArray) = subida

        override suspend fun subirSelfie(imagen: ByteArray): ResultadoSubida {
            selfies += imagen
            enEspera?.await()
            return subida
        }

        override suspend fun consultar() = estado
    }

    private val evidencias = EvidenciasFalsas()

    @BeforeEach
    fun preparar() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterEach
    fun restaurar() {
        Dispatchers.resetMain()
    }

    // En las pruebas la foto ya viene lista: preparar la devuelve igual.
    private fun abierto(preparar: suspend (ByteArray) -> ByteArray? = { it }) =
        SelfieViewModel(evidencias, preparar).apply { abrir() }

    private fun conSelfieGuardada(selfie: EstadoGuardado) = ResultadoEstado.Exito(emptyMap(), selfie)

    @Test
    @DisplayName("antes de consultar esta cargando y no deja continuar")
    fun empiezaCargando() {
        val viewModel = SelfieViewModel(evidencias) { it }

        assertTrue(viewModel.estado.cargando)
        assertFalse(viewModel.estado.puedeContinuar)
    }

    @Test
    @DisplayName("al abrir sin selfie guardada queda sin foto y no deja continuar")
    fun sinSelfie() {
        val estado = abierto().estado

        assertFalse(estado.cargando)
        assertEquals(EstadoDeCara.SinFoto, estado.selfie)
        assertFalse(estado.puedeContinuar)
    }

    @Test
    @DisplayName("al abrir con la selfie ya guardada la muestra y deja continuar")
    fun retomaLaSelfie() {
        evidencias.estado = conSelfieGuardada(EstadoGuardado.REVISION_MANUAL)

        val estado = abierto().estado

        assertEquals(EstadoDeCara.RevisionManual, estado.selfie)
        assertTrue(estado.puedeContinuar)
    }

    @Test
    @DisplayName("si la consulta falla queda sin foto, lista para tomarla")
    fun consultaFallida() {
        evidencias.estado = ResultadoEstado.Fallo

        val estado = abierto().estado

        assertFalse(estado.cargando)
        assertEquals(EstadoDeCara.SinFoto, estado.selfie)
    }

    @Test
    @DisplayName("AC-4: al enviar queda revisando, con su foto, hasta que llega el resultado")
    fun revisando() {
        val espera = CompletableDeferred<Unit>().also { evidencias.enEspera = it }
        val viewModel = abierto()

        viewModel.enviar(foto)
        assertEquals(EstadoDeCara.Revisando, viewModel.estado.selfie)
        assertArrayEquals(foto, viewModel.miniatura)

        espera.complete(Unit)

        assertEquals(EstadoDeCara.Aceptada, viewModel.estado.selfie)
        assertTrue(viewModel.estado.puedeContinuar)
    }

    @Test
    @DisplayName("mientras se revisa, tocar otra vez no envia una segunda selfie")
    fun noEnviaDosVeces() {
        evidencias.enEspera = CompletableDeferred()
        val viewModel = abierto()

        viewModel.enviar(foto)
        viewModel.enviar(foto)

        assertEquals(1, evidencias.selfies.size)
    }

    @Test
    @DisplayName("envia la selfie ya preparada, no la original")
    fun enviaLaPreparada() {
        val preparada = byteArrayOf(9)
        val viewModel = abierto { preparada }

        viewModel.enviar(foto)

        assertArrayEquals(preparada, evidencias.selfies.single())
    }

    @Test
    @DisplayName("AC-4: una selfie que no sirve muestra el motivo, los intentos y no deja continuar")
    fun reintentar() {
        evidencias.subida = ResultadoSubida.Exito(Veredicto.Reintentar(MotivoDeRechazo.NO_COINCIDE_CON_CEDULA, 2))
        val viewModel = abierto()

        viewModel.enviar(foto)

        assertEquals(EstadoDeCara.Reintentar(MotivoDeRechazo.NO_COINCIDE_CON_CEDULA, 2), viewModel.estado.selfie)
        assertFalse(viewModel.estado.puedeContinuar)
    }

    @Test
    @DisplayName("AC-5: en revision manual deja seguir")
    fun revisionManual() {
        evidencias.subida = ResultadoSubida.Exito(Veredicto.RevisionManual)
        val viewModel = abierto()

        viewModel.enviar(foto)

        assertEquals(EstadoDeCara.RevisionManual, viewModel.estado.selfie)
        assertTrue(viewModel.estado.puedeContinuar)
    }

    @Test
    @DisplayName("si falla la red queda sin enviar y no se reintenta sola")
    fun falloDeRed() {
        evidencias.subida = ResultadoSubida.Fallo
        val viewModel = abierto()

        viewModel.enviar(foto)

        assertEquals(EstadoDeCara.NoSeEnvio, viewModel.estado.selfie)
        assertEquals(1, evidencias.selfies.size)
    }

    @Test
    @DisplayName("si el archivo no es una imagen no se envia")
    fun noEsImagen() {
        val viewModel = abierto { null }

        viewModel.enviar(foto)

        assertEquals(EstadoDeCara.NoSeEnvio, viewModel.estado.selfie)
        assertTrue(evidencias.selfies.isEmpty())
    }

    @Test
    @DisplayName("si la sesion vencio al consultar o al subir lo informa para volver al inicio de sesion")
    fun sesionVencida() {
        evidencias.estado = ResultadoEstado.SesionVencida
        assertTrue(abierto().estado.sesionVencida)

        evidencias.estado = ResultadoEstado.Exito(emptyMap())
        evidencias.subida = ResultadoSubida.SesionVencida
        val viewModel = abierto().apply { enviar(foto) }

        assertTrue(viewModel.estado.sesionVencida)
    }
}
