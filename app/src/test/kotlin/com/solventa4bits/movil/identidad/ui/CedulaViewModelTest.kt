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
class CedulaViewModelTest {

    private val foto = byteArrayOf(1, 2, 3)

    /** Doble de prueba: responde lo que tenga en ese momento y guarda lo que se le envio. */
    private class EvidenciasFalsas : ServicioDeEvidencias {
        var estado: ResultadoEstado = ResultadoEstado.Exito(emptyMap())
        var subida: ResultadoSubida = ResultadoSubida.Exito(Veredicto.Aceptada)

        /** Si no es null, la subida espera a que se complete: sirve para ver "revisando". */
        var enEspera: CompletableDeferred<Unit>? = null
        val enviadas = mutableListOf<Pair<CaraDeCedula, ByteArray>>()

        override suspend fun subir(cara: CaraDeCedula, imagen: ByteArray): ResultadoSubida {
            enviadas += cara to imagen
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
        CedulaViewModel(evidencias, preparar).apply { abrir() }

    private fun guardadas(frente: EstadoGuardado, reverso: EstadoGuardado) = ResultadoEstado.Exito(
        mapOf(CaraDeCedula.FRENTE to frente, CaraDeCedula.REVERSO to reverso),
    )

    @Test
    @DisplayName("antes de consultar lo guardado esta cargando y no deja continuar")
    fun empiezaCargando() {
        val viewModel = CedulaViewModel(evidencias) { it }

        assertTrue(viewModel.estado.cargando)
        assertFalse(viewModel.estado.puedeContinuar)
    }

    @Test
    @DisplayName("al abrir muestra lo que ya estaba guardado de cada cara")
    fun muestraLoGuardado() {
        evidencias.estado = guardadas(EstadoGuardado.ACEPTADA, EstadoGuardado.PENDIENTE)

        val estado = abierto().estado

        assertFalse(estado.cargando)
        assertEquals(EstadoDeCara.Aceptada, estado.estadoDe(CaraDeCedula.FRENTE))
        assertEquals(EstadoDeCara.SinFoto, estado.estadoDe(CaraDeCedula.REVERSO))
    }

    @Test
    @DisplayName("si la consulta falla las dos caras quedan sin foto, listas para subir")
    fun consultaFallida() {
        evidencias.estado = ResultadoEstado.Fallo

        val estado = abierto().estado

        assertFalse(estado.cargando)
        assertEquals(EstadoDeCara.SinFoto, estado.estadoDe(CaraDeCedula.FRENTE))
        assertEquals(EstadoDeCara.SinFoto, estado.estadoDe(CaraDeCedula.REVERSO))
    }

    @Test
    @DisplayName("AC-2: al enviar la cara queda revisando, con su foto, hasta que llega el resultado")
    fun revisando() {
        val espera = CompletableDeferred<Unit>().also { evidencias.enEspera = it }
        val viewModel = abierto()

        viewModel.enviar(CaraDeCedula.REVERSO, foto)
        assertEquals(EstadoDeCara.Revisando, viewModel.estado.estadoDe(CaraDeCedula.REVERSO))
        assertArrayEquals(foto, viewModel.estado.miniaturas[CaraDeCedula.REVERSO])

        espera.complete(Unit)

        assertEquals(EstadoDeCara.Aceptada, viewModel.estado.estadoDe(CaraDeCedula.REVERSO))
    }

    @Test
    @DisplayName("mientras una cara se revisa, tocarla otra vez no envia una segunda foto")
    fun noEnviaDosVeces() {
        evidencias.enEspera = CompletableDeferred()
        val viewModel = abierto()

        viewModel.enviar(CaraDeCedula.FRENTE, foto)
        viewModel.enviar(CaraDeCedula.FRENTE, foto)

        assertEquals(1, evidencias.enviadas.size)
    }

    @Test
    @DisplayName("envia la foto ya preparada, no la original")
    fun enviaLaPreparada() {
        val preparada = byteArrayOf(9)
        val viewModel = abierto { preparada }

        viewModel.enviar(CaraDeCedula.FRENTE, foto)

        assertArrayEquals(preparada, evidencias.enviadas.single().second)
    }

    @Test
    @DisplayName("AC-2: una foto que no sirve muestra el motivo y los intentos que quedan")
    fun reintentar() {
        evidencias.subida = ResultadoSubida.Exito(Veredicto.Reintentar(MotivoDeRechazo.BORROSA, 2))
        val viewModel = abierto()

        viewModel.enviar(CaraDeCedula.REVERSO, foto)

        assertEquals(
            EstadoDeCara.Reintentar(MotivoDeRechazo.BORROSA, 2),
            viewModel.estado.estadoDe(CaraDeCedula.REVERSO),
        )
    }

    @Test
    @DisplayName("AC-3: en revision manual la cara deja seguir")
    fun revisionManual() {
        evidencias.subida = ResultadoSubida.Exito(Veredicto.RevisionManual)
        val viewModel = abierto()

        viewModel.enviar(CaraDeCedula.REVERSO, foto)

        assertEquals(EstadoDeCara.RevisionManual, viewModel.estado.estadoDe(CaraDeCedula.REVERSO))
        assertTrue(viewModel.estado.estadoDe(CaraDeCedula.REVERSO).lista)
    }

    @Test
    @DisplayName("si falla la red la cara queda sin enviar y no se reintenta sola")
    fun falloDeRed() {
        evidencias.subida = ResultadoSubida.Fallo
        val viewModel = abierto()

        viewModel.enviar(CaraDeCedula.FRENTE, foto)

        assertEquals(EstadoDeCara.NoSeEnvio, viewModel.estado.estadoDe(CaraDeCedula.FRENTE))
        assertEquals(1, evidencias.enviadas.size)
    }

    @Test
    @DisplayName("si el archivo no es una imagen no se envia")
    fun noEsImagen() {
        val viewModel = abierto { null }

        viewModel.enviar(CaraDeCedula.FRENTE, foto)

        assertEquals(EstadoDeCara.NoSeEnvio, viewModel.estado.estadoDe(CaraDeCedula.FRENTE))
        assertTrue(evidencias.enviadas.isEmpty())
    }

    @Test
    @DisplayName("AC-4: continuar se activa solo con las dos caras listas, aceptadas o en revision manual")
    fun puedeContinuar() {
        val viewModel = abierto()
        evidencias.subida = ResultadoSubida.Exito(Veredicto.Aceptada)
        viewModel.enviar(CaraDeCedula.FRENTE, foto)
        assertFalse(viewModel.estado.puedeContinuar)

        evidencias.subida = ResultadoSubida.Exito(Veredicto.RevisionManual)
        viewModel.enviar(CaraDeCedula.REVERSO, foto)

        assertTrue(viewModel.estado.puedeContinuar)
    }

    @Test
    @DisplayName("AC-4: con una cara por reintentar, continuar sigue inactivo")
    fun noContinuaConUnaPorReintentar() {
        evidencias.estado = guardadas(EstadoGuardado.ACEPTADA, EstadoGuardado.PENDIENTE)
        evidencias.subida = ResultadoSubida.Exito(Veredicto.Reintentar(MotivoDeRechazo.REFLEJO, 1))
        val viewModel = abierto()

        viewModel.enviar(CaraDeCedula.REVERSO, foto)

        assertFalse(viewModel.estado.puedeContinuar)
    }

    @Test
    @DisplayName("al retomar con las dos caras guardadas ya deja continuar")
    fun retomaListo() {
        evidencias.estado = guardadas(EstadoGuardado.REVISION_MANUAL, EstadoGuardado.ACEPTADA)

        assertTrue(abierto().estado.puedeContinuar)
    }

    @Test
    @DisplayName("si la sesion vencio al consultar o al subir lo informa para volver al inicio de sesion")
    fun sesionVencida() {
        evidencias.estado = ResultadoEstado.SesionVencida
        assertTrue(abierto().estado.sesionVencida)

        evidencias.estado = ResultadoEstado.Exito(emptyMap())
        evidencias.subida = ResultadoSubida.SesionVencida
        val viewModel = abierto().apply { enviar(CaraDeCedula.FRENTE, foto) }

        assertTrue(viewModel.estado.sesionVencida)
    }
}
