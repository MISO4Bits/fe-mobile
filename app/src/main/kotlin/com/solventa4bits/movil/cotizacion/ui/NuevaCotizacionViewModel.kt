package com.solventa4bits.movil.cotizacion.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.solventa4bits.movil.cotizacion.dominio.CreditoHipotecario
import com.solventa4bits.movil.cotizacion.dominio.EntidadFinanciera
import com.solventa4bits.movil.cotizacion.dominio.ResultadoCreditos
import com.solventa4bits.movil.cotizacion.dominio.ServicioDeCreditos
import kotlinx.coroutines.launch

/** Lo que la pantalla de nueva cotizacion necesita para dibujarse. */
data class EstadoNuevaCotizacion(
    val cargando: Boolean = true,
    /** No se pudo consultar: el cliente puede reintentar. */
    val fallo: Boolean = false,
    val sesionVencida: Boolean = false,
    val entidades: List<EntidadFinanciera> = emptyList(),
    val creditos: List<CreditoHipotecario> = emptyList(),
    val entidadElegida: EntidadFinanciera? = null,
    // Lo que el cliente escribe a mano cuando su banco no reporto la hipoteca.
    // Son solo digitos: valores en pesos y meses, sin decimales.
    val montoEscrito: String = "",
    val saldoEscrito: String = "",
    val mesesEscritos: String = "",
) {
    /** La hipoteca del cliente en el banco que eligio, si su banco la reporto. */
    val creditoElegido: CreditoHipotecario?
        get() = entidadElegida?.let { elegida -> creditos.find { it.entidadId == elegida.id } }

    /** Eligio banco y no hay datos de ese banco: los escribe el. */
    val pideDatosAMano: Boolean
        get() = entidadElegida != null && creditoElegido == null

    /** No se puede deber mas de lo que se pidio prestado. */
    val saldoMayorQueMonto: Boolean
        get() {
            val monto = montoEscrito.toLongOrNull()
            val saldo = saldoEscrito.toLongOrNull()
            return monto != null && saldo != null && saldo > monto
        }

    private val datosAManoValidos: Boolean
        get() = listOf(montoEscrito, saldoEscrito, mesesEscritos).all { (it.toLongOrNull() ?: 0) > 0 } &&
            !saldoMayorQueMonto

    /** Hay datos del credito para cotizar, traidos del banco o escritos a mano. */
    val puedeCotizar: Boolean
        get() = creditoElegido != null || (pideDatosAMano && datosAManoValidos)
}

/** Decide que pasa en la pantalla de nueva cotizacion. La pantalla solo dibuja. */
class NuevaCotizacionViewModel(
    private val servicio: ServicioDeCreditos,
) : ViewModel() {

    var estado by mutableStateOf(EstadoNuevaCotizacion())
        private set

    /**
     * Trae las hipotecas y los bancos en una sola llamada. Elegir banco
     * despues no vuelve a llamar al BFF.
     */
    fun consultar() {
        estado = EstadoNuevaCotizacion()
        viewModelScope.launch {
            estado = when (val resultado = servicio.consultar()) {
                is ResultadoCreditos.Exito -> EstadoNuevaCotizacion(
                    cargando = false,
                    entidades = resultado.creditos.entidades,
                    creditos = resultado.creditos.creditos,
                )
                ResultadoCreditos.SesionVencida -> EstadoNuevaCotizacion(cargando = false, sesionVencida = true)
                ResultadoCreditos.Fallo -> EstadoNuevaCotizacion(cargando = false, fallo = true)
            }
        }
    }

    fun elegirEntidad(entidad: EntidadFinanciera) {
        estado = estado.copy(entidadElegida = entidad)
    }

    fun cambiarMonto(texto: String) {
        estado = estado.copy(montoEscrito = soloDigitos(texto, MAXIMO_DIGITOS_PESOS))
    }

    fun cambiarSaldo(texto: String) {
        estado = estado.copy(saldoEscrito = soloDigitos(texto, MAXIMO_DIGITOS_PESOS))
    }

    fun cambiarMeses(texto: String) {
        estado = estado.copy(mesesEscritos = soloDigitos(texto, MAXIMO_DIGITOS_MESES))
    }

    private fun soloDigitos(texto: String, maximo: Int) = texto.filter(Char::isDigit).take(maximo)

    private companion object {
        const val MAXIMO_DIGITOS_PESOS = 12
        const val MAXIMO_DIGITOS_MESES = 3
    }
}
