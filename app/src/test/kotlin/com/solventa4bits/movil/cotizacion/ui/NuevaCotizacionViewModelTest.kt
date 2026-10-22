package com.solventa4bits.movil.cotizacion.ui

import com.solventa4bits.movil.cotizacion.dominio.Cotizacion
import com.solventa4bits.movil.cotizacion.dominio.CreditoHipotecario
import com.solventa4bits.movil.cotizacion.dominio.CreditosHipotecarios
import com.solventa4bits.movil.cotizacion.dominio.DatosDelCredito
import com.solventa4bits.movil.cotizacion.dominio.EntidadFinanciera
import com.solventa4bits.movil.cotizacion.dominio.EstadoCreditos
import com.solventa4bits.movil.cotizacion.dominio.OrigenDeDatos
import com.solventa4bits.movil.cotizacion.dominio.ResultadoCotizacion
import com.solventa4bits.movil.cotizacion.dominio.ResultadoCreditos
import com.solventa4bits.movil.cotizacion.dominio.ResultadoDeclinacion
import com.solventa4bits.movil.cotizacion.dominio.ServicioDeCotizaciones
import com.solventa4bits.movil.cotizacion.dominio.ServicioDeCreditos
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
class NuevaCotizacionViewModelTest {

    private val bancolombia = EntidadFinanciera("bancolombia", "Bancolombia")
    private val davivienda = EntidadFinanciera("davivienda", "Davivienda")
    private val bancos = listOf(bancolombia, davivienda)
    private val hipoteca = CreditoHipotecario(
        entidadId = "bancolombia",
        entidadNombre = "BANCOLOMBIA S.A.",
        valorCredito = 180_000_000.0,
        saldoInsoluto = 98_000_000.0,
        plazoRestanteMeses = 96,
        cuotaMensual = 1_650_000.0,
    )
    private val conHipoteca = ResultadoCreditos.Exito(
        CreditosHipotecarios(EstadoCreditos.DISPONIBLE, listOf(hipoteca), bancos, "2026-10-09T12:00:00Z"),
    )
    private val fuenteCaida = ResultadoCreditos.Exito(
        CreditosHipotecarios(EstadoCreditos.NO_DISPONIBLE, emptyList(), bancos),
    )

    /** Doble de prueba: responde lo que tenga en [resultado] en ese momento. */
    private class ServicioFalso(var resultado: ResultadoCreditos) : ServicioDeCreditos {
        override suspend fun consultar() = resultado
    }

    /** Doble de prueba: responde [resultado] y guarda lo que se le envio. */
    private class CotizacionesFalsas : ServicioDeCotizaciones {
        var resultado: ResultadoCotizacion = ResultadoCotizacion.Fallo
        var enviados: DatosDelCredito? = null

        override suspend fun cotizar(datos: DatosDelCredito): ResultadoCotizacion {
            enviados = datos
            return resultado
        }

        override suspend fun consultarActual() = ResultadoCotizacion.SinCotizacion

        var declinacion: ResultadoDeclinacion = ResultadoDeclinacion.Declinada
        var declinaciones = 0

        override suspend fun declinarActual(): ResultadoDeclinacion {
            declinaciones++
            return declinacion
        }
    }

    private val cotizaciones = CotizacionesFalsas()

    private val cotizacionDePrueba = Cotizacion(
        vigente = true,
        datosCredito = DatosDelCredito("Bancolombia", 180_000_000, 98_000_000, 96),
        datosDeclarados = true,
        primaMensual = 11_412.0,
        primaBaseMensual = 13_426.0,
        sumaAsegurada = 98_000_000.0,
        coberturaMeses = 96,
        personalizada = true,
        factores = emptyList(),
    )

    @BeforeEach
    fun preparar() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterEach
    fun restaurar() {
        Dispatchers.resetMain()
    }

    private fun viewModelCon(resultado: ResultadoCreditos) =
        NuevaCotizacionViewModel(ServicioFalso(resultado), cotizaciones).apply { empezar() }

    private fun NuevaCotizacionViewModel.escribirDatos(monto: String, saldo: String, meses: String) {
        cambiarMonto(monto)
        cambiarSaldo(saldo)
        cambiarMeses(meses)
    }

    // --- AC-1 y AC-2: elegir banco y ver los datos que reporto.

    @Test
    @DisplayName("antes de consultar muestra que esta cargando y sabe que aun no empezo")
    fun empiezaCargando() {
        val viewModel = NuevaCotizacionViewModel(ServicioFalso(conHipoteca), cotizaciones)

        assertTrue(viewModel.estado.cargando)
        assertFalse(viewModel.empezo)

        viewModel.empezar()
        assertTrue(viewModel.empezo)
    }

    @Test
    @DisplayName("al entrar deja listos los bancos y aun no hay banco elegido")
    fun consultaLosBancos() {
        val viewModel = viewModelCon(conHipoteca)

        assertFalse(viewModel.estado.cargando)
        assertEquals(bancos, viewModel.estado.entidades)
        assertNull(viewModel.estado.entidadElegida)
        assertFalse(viewModel.estado.puedeCotizar)
    }

    @Test
    @DisplayName("al elegir el banco de la hipoteca muestra sus datos con la fecha de consulta, como verificados")
    fun eligeElBancoDeLaHipoteca() {
        val viewModel = viewModelCon(conHipoteca)

        viewModel.elegirEntidad(bancolombia)

        assertEquals(hipoteca, viewModel.estado.creditoElegido)
        assertEquals("2026-10-09T12:00:00Z", viewModel.estado.fechaConsulta)
        assertFalse(viewModel.estado.pideDatosAMano)
        assertTrue(viewModel.estado.puedeCotizar)
        assertEquals(OrigenDeDatos.VERIFICADO, viewModel.estado.origen)
    }

    // --- AC-3: la consulta falla.

    @Test
    @DisplayName("si la consulta falla lo informa y al reintentar se recupera")
    fun falloYReintento() {
        val servicio = ServicioFalso(ResultadoCreditos.Fallo)
        val viewModel = NuevaCotizacionViewModel(servicio, cotizaciones).apply { empezar() }
        assertTrue(viewModel.estado.fallo)

        servicio.resultado = conHipoteca
        viewModel.reintentar()

        assertFalse(viewModel.estado.fallo)
        assertEquals(bancos, viewModel.estado.entidades)
    }

    @Test
    @DisplayName("si la consulta falla puede escribir el banco y los datos el mismo")
    fun falloYEscribeAMano() {
        val viewModel = viewModelCon(ResultadoCreditos.Fallo)

        viewModel.escribirAMano()
        viewModel.cambiarBanco("Banco Caja Social")
        viewModel.escribirDatos(monto = "200000000", saldo = "150000000", meses = "120")

        assertTrue(viewModel.estado.otroBanco)
        assertTrue(viewModel.estado.puedeCotizar)
        assertEquals(OrigenDeDatos.DECLARADO, viewModel.estado.origen)
    }

    @Test
    @DisplayName("si la fuente no responde lo informa, y al reintentar conserva el banco elegido y lo escrito")
    fun fuenteNoDisponibleConservaElBanco() {
        val servicio = ServicioFalso(fuenteCaida)
        val viewModel = NuevaCotizacionViewModel(servicio, cotizaciones).apply { empezar() }
        assertTrue(viewModel.estado.fuenteNoDisponible)
        viewModel.elegirEntidad(bancolombia)
        viewModel.cambiarMonto("200000000")

        servicio.resultado = conHipoteca
        viewModel.reintentar()

        assertFalse(viewModel.estado.fuenteNoDisponible)
        assertEquals(bancolombia, viewModel.estado.entidadElegida)
        assertEquals("200000000", viewModel.estado.montoEscrito)
        assertEquals(hipoteca, viewModel.estado.creditoElegido)
    }

    @Test
    @DisplayName("al volver a entrar a la pantalla empieza de cero")
    fun empezarDeCero() {
        val viewModel = viewModelCon(conHipoteca)
        viewModel.elegirEntidad(bancolombia)

        viewModel.empezar()

        assertNull(viewModel.estado.entidadElegida)
    }

    // --- AC-4: captura manual.

    @Test
    @DisplayName("si su banco no reporto hipoteca escribe los datos y quedan como declarados")
    fun bancoSinHipoteca() {
        val viewModel = viewModelCon(conHipoteca)

        viewModel.elegirEntidad(davivienda)
        assertTrue(viewModel.estado.pideDatosAMano)
        assertFalse(viewModel.estado.puedeCotizar)
        assertNull(viewModel.estado.origen)

        viewModel.escribirDatos(monto = "200000000", saldo = "150000000", meses = "120")
        assertTrue(viewModel.estado.puedeCotizar)
        assertEquals(OrigenDeDatos.DECLARADO, viewModel.estado.origen)
    }

    @Test
    @DisplayName("si su banco no esta en la lista debe escribir tambien el nombre del banco")
    fun bancoFueraDeLaLista() {
        val viewModel = viewModelCon(conHipoteca)

        viewModel.elegirOtroBanco()
        viewModel.escribirDatos(monto = "200000000", saldo = "150000000", meses = "120")
        assertFalse(viewModel.estado.puedeCotizar)

        viewModel.cambiarBanco("Banco Caja Social")
        assertTrue(viewModel.estado.puedeCotizar)
    }

    @Test
    @DisplayName("puede preferir escribir los datos aunque su banco los haya reportado")
    fun prefiereEscribir() {
        val viewModel = viewModelCon(conHipoteca)
        viewModel.elegirEntidad(bancolombia)

        viewModel.escribirAMano()

        assertNull(viewModel.estado.creditoElegido)
        assertTrue(viewModel.estado.pideDatosAMano)
        assertEquals(bancolombia, viewModel.estado.entidadElegida)

        viewModel.elegirEntidad(bancolombia)
        assertEquals(hipoteca, viewModel.estado.creditoElegido)
    }

    @Test
    @DisplayName("de lo que se escribe en los campos numericos solo quedan los digitos")
    fun soloDigitos() {
        val viewModel = viewModelCon(conHipoteca)

        viewModel.cambiarMonto("$ 200.000.000")
        viewModel.cambiarMeses("12 meses")

        assertEquals("200000000", viewModel.estado.montoEscrito)
        assertEquals("12", viewModel.estado.mesesEscritos)
    }

    // --- AC-5: rangos.

    @Test
    @DisplayName("un credito de menos de 10 millones se senala y no deja cotizar")
    fun montoBajoElMinimo() {
        val viewModel = viewModelCon(conHipoteca).apply { elegirEntidad(davivienda) }

        viewModel.escribirDatos(monto = "9999999", saldo = "5000000", meses = "120")
        assertTrue(viewModel.estado.montoFueraDeRango)
        assertFalse(viewModel.estado.puedeCotizar)

        viewModel.cambiarMonto("10000000")
        assertFalse(viewModel.estado.montoFueraDeRango)
        assertTrue(viewModel.estado.puedeCotizar)
    }

    @Test
    @DisplayName("un saldo en cero o mayor que el monto total se senala y no deja cotizar")
    fun saldoFueraDeRango() {
        val viewModel = viewModelCon(conHipoteca).apply { elegirEntidad(davivienda) }

        viewModel.escribirDatos(monto = "100000000", saldo = "150000000", meses = "120")
        assertTrue(viewModel.estado.saldoFueraDeRango)
        assertFalse(viewModel.estado.puedeCotizar)

        viewModel.cambiarSaldo("0")
        assertTrue(viewModel.estado.saldoFueraDeRango)
    }

    @Test
    @DisplayName("un plazo por fuera de 1 a 480 meses se senala y no deja cotizar")
    fun mesesFueraDeRango() {
        val viewModel = viewModelCon(conHipoteca).apply { elegirEntidad(davivienda) }

        viewModel.escribirDatos(monto = "200000000", saldo = "150000000", meses = "0")
        assertTrue(viewModel.estado.mesesFueraDeRango)
        assertFalse(viewModel.estado.puedeCotizar)

        viewModel.cambiarMeses("481")
        assertTrue(viewModel.estado.mesesFueraDeRango)

        viewModel.cambiarMeses("480")
        assertFalse(viewModel.estado.mesesFueraDeRango)
        assertTrue(viewModel.estado.puedeCotizar)
    }

    @Test
    @DisplayName("un campo vacio no se senala como error, solo falta")
    fun campoVacioNoEsError() {
        val viewModel = viewModelCon(conHipoteca).apply { elegirEntidad(davivienda) }

        assertFalse(viewModel.estado.montoFueraDeRango)
        assertFalse(viewModel.estado.saldoFueraDeRango)
        assertFalse(viewModel.estado.mesesFueraDeRango)
        assertFalse(viewModel.estado.puedeCotizar)
    }

    // --- Cotizar (BITS-95).

    @Test
    @DisplayName("cotiza con los datos que trajo el banco y guarda la cotizacion que devuelve el backend")
    fun cotizaConDatosDelBanco() {
        cotizaciones.resultado = ResultadoCotizacion.Exito(cotizacionDePrueba)
        val viewModel = viewModelCon(conHipoteca).apply { elegirEntidad(bancolombia) }

        viewModel.cotizar()

        assertEquals(DatosDelCredito("Bancolombia", 180_000_000, 98_000_000, 96), cotizaciones.enviados)
        assertEquals(cotizacionDePrueba, viewModel.estado.cotizacion)
        assertFalse(viewModel.estado.cotizando)
    }

    @Test
    @DisplayName("cotiza con los datos escritos a mano, con el banco que escribio el cliente")
    fun cotizaConDatosAMano() {
        val viewModel = viewModelCon(conHipoteca)
        viewModel.elegirOtroBanco()
        viewModel.cambiarBanco("  Banco Caja Social ")
        viewModel.escribirDatos(monto = "200000000", saldo = "150000000", meses = "120")

        viewModel.cotizar()

        assertEquals(DatosDelCredito("Banco Caja Social", 200_000_000, 150_000_000, 120), cotizaciones.enviados)
    }

    @Test
    @DisplayName("al volver de la cotizacion al credito se conserva lo elegido")
    fun volverAlCredito() {
        cotizaciones.resultado = ResultadoCotizacion.Exito(cotizacionDePrueba)
        val viewModel = viewModelCon(conHipoteca).apply { elegirEntidad(bancolombia) }
        viewModel.cotizar()

        viewModel.volverAlCredito()

        assertNull(viewModel.estado.cotizacion)
        assertEquals(bancolombia, viewModel.estado.entidadElegida)
    }

    @Test
    @DisplayName("retoma desde el Home la cotizacion en curso y consulta los creditos por debajo")
    fun retomar() {
        val viewModel = viewModelCon(conHipoteca).apply { elegirEntidad(bancolombia) }

        viewModel.retomar(cotizacionDePrueba)

        assertEquals(cotizacionDePrueba, viewModel.estado.cotizacion)
        assertNull(viewModel.estado.entidadElegida)
        assertEquals(bancos, viewModel.estado.entidades)
        assertTrue(viewModel.empezo)
    }

    @Test
    @DisplayName("sabe cuando el cliente no autorizo la consulta financiera, tambien al retomar")
    fun sinConsentimiento() {
        val sinPermiso = ResultadoCreditos.Exito(
            CreditosHipotecarios(EstadoCreditos.SIN_CONSENTIMIENTO, emptyList(), bancos),
        )
        assertTrue(viewModelCon(sinPermiso).estado.sinConsentimiento)
        assertFalse(viewModelCon(conHipoteca).estado.sinConsentimiento)

        val retomada = NuevaCotizacionViewModel(ServicioFalso(sinPermiso), cotizaciones)
        retomada.retomar(cotizacionDePrueba)
        assertTrue(retomada.estado.sinConsentimiento)
    }

    @Test
    @DisplayName("al cotizar va a elegir coberturas con Tu casa desmarcada, y de ahi a lo que pagas")
    fun eligeCoberturas() {
        cotizaciones.resultado = ResultadoCotizacion.Exito(cotizacionDePrueba)
        val viewModel = viewModelCon(conHipoteca).apply { elegirEntidad(bancolombia) }
        viewModel.cotizar()
        assertFalse(viewModel.estado.conTuCasa)
        assertFalse(viewModel.estado.coberturasElegidas)

        viewModel.cambiarTuCasa(true)
        viewModel.continuarALoQuePagas()
        assertTrue(viewModel.estado.coberturasElegidas)

        viewModel.volverACoberturas()
        assertFalse(viewModel.estado.coberturasElegidas)
        assertTrue(viewModel.estado.conTuCasa)

        viewModel.volverAlCredito()
        assertFalse(viewModel.estado.conTuCasa)
    }

    @Test
    @DisplayName("al retomar desde el Home el cliente vuelve a elegir coberturas")
    fun retomarEligeCoberturas() {
        val viewModel = viewModelCon(conHipoteca)

        viewModel.retomar(cotizacionDePrueba)

        assertFalse(viewModel.estado.coberturasElegidas)
        assertFalse(viewModel.estado.conTuCasa)
    }

    @Test
    @DisplayName("sin datos completos no cotiza")
    fun sinDatosNoCotiza() {
        val viewModel = viewModelCon(conHipoteca).apply { elegirEntidad(davivienda) }

        viewModel.cotizar()

        assertNull(cotizaciones.enviados)
    }

    @Test
    @DisplayName("si el backend rechaza los datos muestra su mensaje, por ejemplo el limite de los 75 anos")
    fun datosRechazados() {
        cotizaciones.resultado = ResultadoCotizacion.DatosRechazados("El crédito terminaría a los 80 años")
        val viewModel = viewModelCon(conHipoteca).apply { elegirEntidad(bancolombia) }

        viewModel.cotizar()

        assertEquals(
            ErrorAlCotizar.DatosRechazados("El crédito terminaría a los 80 años"),
            viewModel.estado.errorAlCotizar,
        )
        assertNull(viewModel.estado.cotizacion)
    }

    @Test
    @DisplayName("si no se puede cotizar lo informa, y al volver a intentar se borra el aviso")
    fun falloAlCotizar() {
        val viewModel = viewModelCon(conHipoteca).apply { elegirEntidad(bancolombia) }
        viewModel.cotizar()
        assertEquals(ErrorAlCotizar.Fallo, viewModel.estado.errorAlCotizar)

        cotizaciones.resultado = ResultadoCotizacion.Exito(cotizacionDePrueba)
        viewModel.cotizar()

        assertNull(viewModel.estado.errorAlCotizar)
    }

    @Test
    @DisplayName("si la sesion vencio al cotizar lo informa para volver al inicio de sesion")
    fun sesionVencidaAlCotizar() {
        cotizaciones.resultado = ResultadoCotizacion.SesionVencida
        val viewModel = viewModelCon(conHipoteca).apply { elegirEntidad(bancolombia) }

        viewModel.cotizar()

        assertTrue(viewModel.estado.sesionVencida)
    }

    @Test
    @DisplayName("si la sesion vencio lo informa para volver al inicio de sesion")
    fun sesionVencida() {
        assertTrue(viewModelCon(ResultadoCreditos.SesionVencida).estado.sesionVencida)
    }

    // --- BITS-337: declinar las condiciones de la cotizacion.

    private fun enLoQuePagas() = viewModelCon(conHipoteca).apply { retomar(cotizacionDePrueba) }

    @Test
    @DisplayName("AC-1 y AC-2: el modal se abre y Volver lo cierra sin tocar la cotizacion")
    fun volverDelModal() {
        val viewModel = enLoQuePagas()

        viewModel.pedirDeclinacion()
        assertTrue(viewModel.estado.confirmandoDeclinacion)
        viewModel.cancelarDeclinacion()

        assertFalse(viewModel.estado.confirmandoDeclinacion)
        assertEquals(cotizacionDePrueba, viewModel.estado.cotizacion)
        assertEquals(0, cotizaciones.declinaciones)
    }

    @Test
    @DisplayName("AC-3: al declinar se elimina la cotizacion y avisa que toca ir al Inicio")
    fun declina() {
        val viewModel = enLoQuePagas().apply { pedirDeclinacion() }

        viewModel.declinar()

        assertTrue(viewModel.estado.declinada)
        assertFalse(viewModel.estado.confirmandoDeclinacion)
        assertEquals(1, cotizaciones.declinaciones)
    }

    @Test
    @DisplayName("si falla al declinar el modal sigue abierto con el aviso, y al reintentar se borra")
    fun falloAlDeclinar() {
        cotizaciones.declinacion = ResultadoDeclinacion.Fallo
        val viewModel = enLoQuePagas().apply { pedirDeclinacion() }

        viewModel.declinar()
        assertTrue(viewModel.estado.falloAlDeclinar)
        assertTrue(viewModel.estado.confirmandoDeclinacion)
        assertFalse(viewModel.estado.declinada)

        cotizaciones.declinacion = ResultadoDeclinacion.Declinada
        viewModel.declinar()

        assertFalse(viewModel.estado.falloAlDeclinar)
        assertTrue(viewModel.estado.declinada)
    }

    @Test
    @DisplayName("al cerrar el modal tras un fallo, el aviso no reaparece al abrirlo de nuevo")
    fun cerrarBorraElFallo() {
        cotizaciones.declinacion = ResultadoDeclinacion.Fallo
        val viewModel = enLoQuePagas().apply {
            pedirDeclinacion()
            declinar()
            cancelarDeclinacion()
        }

        viewModel.pedirDeclinacion()

        assertFalse(viewModel.estado.falloAlDeclinar)
    }

    @Test
    @DisplayName("si la sesion vencio al declinar lo informa para volver al inicio de sesion")
    fun sesionVencidaAlDeclinar() {
        cotizaciones.declinacion = ResultadoDeclinacion.SesionVencida
        val viewModel = enLoQuePagas().apply { pedirDeclinacion() }

        viewModel.declinar()

        assertTrue(viewModel.estado.sesionVencida)
        assertFalse(viewModel.estado.declinada)
    }
}
