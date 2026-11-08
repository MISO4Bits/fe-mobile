package com.solventa4bits.movil.siniestro.ui

import com.solventa4bits.movil.siniestro.dominio.DetalleDeSiniestro
import com.solventa4bits.movil.siniestro.dominio.EstadoDeSiniestro
import com.solventa4bits.movil.siniestro.dominio.ResultadoDetalle
import com.solventa4bits.movil.siniestro.dominio.ResultadoSiniestros
import com.solventa4bits.movil.siniestro.dominio.SiniestroResumen
import com.solventa4bits.movil.siniestro.dominio.TipoDeSiniestro
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
class MisSiniestrosViewModelTest {

    private val enEstudio =
        SiniestroResumen("s1", "SIN-1", "poliza-1", TipoDeSiniestro.INCAPACIDAD_TOTAL, EstadoDeSiniestro.EN_ESTUDIO)
    private val detalle = DetalleDeSiniestro(enEstudio, "2026-11-02", "2026-12-02T14:12:00Z", 3, 0)
    private val servicio = SiniestrosFalsos()

    @BeforeEach
    fun preparar() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterEach
    fun restaurar() {
        Dispatchers.resetMain()
    }

    @Test
    @DisplayName("BITS-111, AC-2A: trae los siniestros del cliente")
    fun lista() {
        servicio.alListar = ResultadoSiniestros.Exito(listOf(enEstudio))
        val mis = MisSiniestrosViewModel(servicio)

        mis.listar()

        assertEquals(listOf(enEstudio), mis.estado.siniestros)
        assertTrue(mis.estado.listos)
    }

    @Test
    @DisplayName("si la lista falla, queda vacia y se puede reportar igual: el servidor cuida el en curso")
    fun listaQueFalla() {
        servicio.alListar = ResultadoSiniestros.Fallo
        val mis = MisSiniestrosViewModel(servicio)

        mis.listar()

        assertTrue(mis.estado.siniestros.isEmpty())
        assertTrue(mis.estado.listos)
    }

    @Test
    @DisplayName("listar de nuevo olvida una sesion vencida de antes")
    fun olvidaLaSesionVencida() {
        servicio.alListar = ResultadoSiniestros.SesionVencida
        val mis = MisSiniestrosViewModel(servicio)
        mis.listar()
        assertTrue(mis.estado.sesionVencida)

        servicio.alListar = ResultadoSiniestros.Exito(emptyList())
        mis.listar()

        assertFalse(mis.estado.sesionVencida)
    }

    @Test
    @DisplayName("abre el detalle de un siniestro")
    fun detalle() {
        servicio.alConsultar = ResultadoDetalle.Exito(detalle)
        val mis = MisSiniestrosViewModel(servicio)

        mis.abrir("s1")

        assertEquals(detalle, mis.estado.detalle)
        assertFalse(mis.estado.cargandoDetalle)
        assertEquals(listOf("s1"), servicio.consultados)
    }

    @Test
    @DisplayName("si el detalle falla lo dice, y reintentar pide el mismo siniestro")
    fun detalleQueFalla() {
        val mis = MisSiniestrosViewModel(servicio)
        mis.abrir("s1")
        assertTrue(mis.estado.falloDetalle)
        assertNull(mis.estado.detalle)

        servicio.alConsultar = ResultadoDetalle.Exito(detalle)
        mis.reintentarDetalle()

        assertEquals(detalle, mis.estado.detalle)
        assertEquals(listOf("s1", "s1"), servicio.consultados)
    }

    @Test
    @DisplayName("con la sesion vencida en el detalle lo marca para volver a entrar")
    fun detalleConSesionVencida() {
        servicio.alConsultar = ResultadoDetalle.SesionVencida
        val mis = MisSiniestrosViewModel(servicio)

        mis.abrir("s1")

        assertTrue(mis.estado.sesionVencida)
    }
}
