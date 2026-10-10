package com.solventa4bits.movil.cotizacion.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.solventa4bits.movil.cotizacion.dominio.CreditoHipotecario
import com.solventa4bits.movil.cotizacion.dominio.EntidadFinanciera
import com.solventa4bits.movil.cotizacion.dominio.EstadoCreditos
import com.solventa4bits.movil.cotizacion.dominio.OrigenDeDatos
import com.solventa4bits.movil.cotizacion.dominio.RangosDeCredito
import com.solventa4bits.movil.cotizacion.dominio.ResultadoCreditos
import com.solventa4bits.movil.cotizacion.dominio.ServicioDeCreditos
import kotlinx.coroutines.launch

/** Lo que la pantalla de nueva cotizacion necesita para dibujarse. */
data class EstadoNuevaCotizacion(
    val cargando: Boolean = true,
    /** No se pudo llegar al BFF: el cliente puede reintentar o escribir los datos. */
    val fallo: Boolean = false,
    /** El BFF respondio, pero la fuente (Open Finance) no: mismo trato. */
    val fuenteNoDisponible: Boolean = false,
    val sesionVencida: Boolean = false,
    val entidades: List<EntidadFinanciera> = emptyList(),
    val creditos: List<CreditoHipotecario> = emptyList(),
    val fechaConsulta: String? = null,
    val entidadElegida: EntidadFinanciera? = null,
    /** Su banco no esta en la lista: escribe el nombre en [bancoEscrito]. */
    val otroBanco: Boolean = false,
    val bancoEscrito: String = "",
    /** Prefiere escribir los datos el mismo, aunque su banco los haya reportado. */
    val aMano: Boolean = false,
    // Lo que el cliente escribe a mano. Son solo digitos: pesos y meses, sin decimales.
    val montoEscrito: String = "",
    val saldoEscrito: String = "",
    val mesesEscritos: String = "",
) {
    private val monto get() = montoEscrito.toLongOrNull() ?: 0
    private val saldo get() = saldoEscrito.toLongOrNull() ?: 0
    private val meses get() = mesesEscritos.toIntOrNull() ?: 0

    private val creditoDelBanco: CreditoHipotecario?
        get() = entidadElegida?.let { elegida -> creditos.find { it.entidadId == elegida.id } }

    /** La hipoteca que su banco reporto, si el cliente va a cotizar con ella. */
    val creditoElegido: CreditoHipotecario?
        get() = if (aMano) null else creditoDelBanco

    /** Va a escribir los datos: lo eligio, su banco no esta, o su banco no reporto nada. */
    val pideDatosAMano: Boolean
        get() = aMano || otroBanco || (entidadElegida != null && creditoDelBanco == null)

    // Un campo vacio no esta "fuera de rango": solo falta. El error se senala
    // cuando el cliente ya escribio algo.
    val montoFueraDeRango: Boolean
        get() = montoEscrito.isNotEmpty() && monto < RangosDeCredito.VALOR_MINIMO

    val saldoFueraDeRango: Boolean
        get() = saldoEscrito.isNotEmpty() && (saldo <= 0 || (montoEscrito.isNotEmpty() && saldo > monto))

    val mesesFueraDeRango: Boolean
        get() = mesesEscritos.isNotEmpty() && meses !in RangosDeCredito.MESES_MINIMO..RangosDeCredito.MESES_MAXIMO

    private val bancoValido: Boolean
        get() = entidadElegida != null || bancoEscrito.trim().length >= RangosDeCredito.LETRAS_MINIMAS_DEL_BANCO

    private val datosAManoValidos: Boolean
        get() = bancoValido &&
            listOf(montoEscrito, saldoEscrito, mesesEscritos).none { it.isEmpty() } &&
            !(montoFueraDeRango || saldoFueraDeRango || mesesFueraDeRango)

    /** Hay datos del credito para cotizar, traidos del banco o escritos a mano. */
    val puedeCotizar: Boolean
        get() = creditoElegido != null || (pideDatosAMano && datosAManoValidos)

    /** La marca que acompana a la cotizacion (BITS-95). Null mientras no haya datos completos. */
    val origen: OrigenDeDatos?
        get() = when {
            creditoElegido != null -> OrigenDeDatos.VERIFICADO
            puedeCotizar -> OrigenDeDatos.DECLARADO
            else -> null
        }
}

/** Decide que pasa en la pantalla de nueva cotizacion. La pantalla solo dibuja. */
class NuevaCotizacionViewModel(
    private val servicio: ServicioDeCreditos,
) : ViewModel(), AccionesDeCotizacion {

    var estado by mutableStateOf(EstadoNuevaCotizacion())
        private set

    /** Al entrar a la pantalla: empieza de cero y consulta. */
    fun empezar() = cargar(EstadoNuevaCotizacion())

    override fun reintentar() = cargar(estado)

    // Una sola llamada trae los bancos y las hipotecas. Elegir banco despues
    // no vuelve a llamar al BFF.
    private fun cargar(desde: EstadoNuevaCotizacion) {
        estado = desde.copy(cargando = true, fallo = false, fuenteNoDisponible = false)
        viewModelScope.launch {
            estado = when (val resultado = servicio.consultar()) {
                is ResultadoCreditos.Exito -> estado.copy(
                    cargando = false,
                    entidades = resultado.creditos.entidades,
                    creditos = resultado.creditos.creditos,
                    fechaConsulta = resultado.creditos.fechaConsulta,
                    fuenteNoDisponible = resultado.creditos.estado == EstadoCreditos.NO_DISPONIBLE,
                )
                ResultadoCreditos.SesionVencida -> estado.copy(cargando = false, sesionVencida = true)
                ResultadoCreditos.Fallo -> estado.copy(cargando = false, fallo = true)
            }
        }
    }

    override fun elegirEntidad(entidad: EntidadFinanciera) {
        estado = estado.copy(entidadElegida = entidad, otroBanco = false, aMano = false)
    }

    override fun elegirOtroBanco() {
        estado = estado.copy(entidadElegida = null, otroBanco = true)
    }

    override fun escribirAMano() {
        // Sin lista de bancos (la consulta fallo) el nombre tambien se escribe.
        estado = estado.copy(aMano = true, otroBanco = estado.otroBanco || estado.entidadElegida == null)
    }

    override fun cambiarBanco(texto: String) {
        estado = estado.copy(bancoEscrito = texto.take(RangosDeCredito.LETRAS_MAXIMAS_DEL_BANCO))
    }

    override fun cambiarMonto(texto: String) {
        estado = estado.copy(montoEscrito = soloDigitos(texto, MAXIMO_DIGITOS_PESOS))
    }

    override fun cambiarSaldo(texto: String) {
        estado = estado.copy(saldoEscrito = soloDigitos(texto, MAXIMO_DIGITOS_PESOS))
    }

    override fun cambiarMeses(texto: String) {
        estado = estado.copy(mesesEscritos = soloDigitos(texto, MAXIMO_DIGITOS_MESES))
    }
}

private const val MAXIMO_DIGITOS_PESOS = 12
private const val MAXIMO_DIGITOS_MESES = 3

private fun soloDigitos(texto: String, maximo: Int) = texto.filter(Char::isDigit).take(maximo)
