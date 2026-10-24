package com.solventa4bits.movil.identidad.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.solventa4bits.movil.identidad.dominio.CaraDeCedula
import com.solventa4bits.movil.identidad.dominio.EstadoGuardado
import com.solventa4bits.movil.identidad.dominio.MotivoDeRechazo
import com.solventa4bits.movil.identidad.dominio.ResultadoEstado
import com.solventa4bits.movil.identidad.dominio.ResultadoSubida
import com.solventa4bits.movil.identidad.dominio.ServicioDeEvidencias
import com.solventa4bits.movil.identidad.dominio.Veredicto
import kotlinx.coroutines.launch

/** Lo que muestra cada cara en "Tu cedula" (Figma 03 a 03D). */
sealed interface EstadoDeCara {
    data object SinFoto : EstadoDeCara

    data object Revisando : EstadoDeCara

    /** "Se ve bien". */
    data object Aceptada : EstadoDeCara

    /** "Mejor otra foto": [motivo] es null si el servidor mando uno que la app no conoce. */
    data class Reintentar(val motivo: MotivoDeRechazo?, val intentosRestantes: Int) : EstadoDeCara

    /** "La revisara una persona. Puedes seguir." */
    data object RevisionManual : EstadoDeCara

    /** Fallo la red o el archivo no era una imagen: el cliente toma otra. */
    data object NoSeEnvio : EstadoDeCara
}

/** La cara ya no frena el paso: aceptada o en revision manual (BITS-275, AC-4). */
val EstadoDeCara.lista: Boolean
    get() = this == EstadoDeCara.Aceptada || this == EstadoDeCara.RevisionManual

data class EstadoCedula(
    /** Consultando lo que ya estaba guardado al abrir el paso. */
    val cargando: Boolean = true,
    val caras: Map<CaraDeCedula, EstadoDeCara> = emptyMap(),
    /** La foto ya preparada de cada cara, para verla en su recuadro. */
    val miniaturas: Map<CaraDeCedula, ByteArray> = emptyMap(),
    val sesionVencida: Boolean = false,
) {
    fun estadoDe(cara: CaraDeCedula): EstadoDeCara = caras[cara] ?: EstadoDeCara.SinFoto

    val puedeContinuar: Boolean
        get() = !cargando && CaraDeCedula.entries.all { estadoDe(it).lista }
}

/**
 * Decide que pasa en "Tu cedula". [preparar] deja la foto lista para enviar
 * (en la app, PreparadorDeFotos en segundo plano); viene de afuera porque usa
 * imagenes de Android, que no existen en las pruebas unitarias.
 */
class CedulaViewModel(
    private val evidencias: ServicioDeEvidencias,
    private val preparar: suspend (ByteArray) -> ByteArray?,
) : ViewModel() {

    var estado by mutableStateOf(EstadoCedula())
        private set

    /** Al entrar al paso: lo que ya estaba guardado de cada cara. */
    fun abrir() {
        estado = EstadoCedula()
        viewModelScope.launch {
            estado = when (val resultado = evidencias.consultar()) {
                is ResultadoEstado.Exito -> estado.copy(
                    cargando = false,
                    caras = resultado.caras.mapValues { (_, guardado) -> guardado.aEstado() },
                )
                ResultadoEstado.SesionVencida -> estado.copy(cargando = false, sesionVencida = true)
                // Sin saber que habia, las dos caras quedan por subir: una foto nueva reemplaza a la anterior.
                ResultadoEstado.Fallo -> estado.copy(cargando = false)
            }
        }
    }

    /** Prepara y sube una foto de [cara]. Cada envio es un intento: si falla, no se repite solo. */
    fun enviar(cara: CaraDeCedula, foto: ByteArray) {
        if (estado.estadoDe(cara) == EstadoDeCara.Revisando) return
        cambiar(cara, EstadoDeCara.Revisando)
        viewModelScope.launch {
            val lista = preparar(foto)
            if (lista == null) {
                cambiar(cara, EstadoDeCara.NoSeEnvio)
                return@launch
            }
            estado = estado.copy(miniaturas = estado.miniaturas + (cara to lista))
            when (val resultado = evidencias.subir(cara, lista)) {
                is ResultadoSubida.Exito -> cambiar(cara, resultado.veredicto.aEstado())
                ResultadoSubida.SesionVencida -> {
                    cambiar(cara, EstadoDeCara.NoSeEnvio)
                    estado = estado.copy(sesionVencida = true)
                }
                ResultadoSubida.Fallo -> cambiar(cara, EstadoDeCara.NoSeEnvio)
            }
        }
    }

    private fun cambiar(cara: CaraDeCedula, nuevo: EstadoDeCara) {
        estado = estado.copy(caras = estado.caras + (cara to nuevo))
    }

    private fun EstadoGuardado.aEstado(): EstadoDeCara = when (this) {
        EstadoGuardado.ACEPTADA -> EstadoDeCara.Aceptada
        EstadoGuardado.REVISION_MANUAL -> EstadoDeCara.RevisionManual
        EstadoGuardado.PENDIENTE -> EstadoDeCara.SinFoto
    }

    private fun Veredicto.aEstado(): EstadoDeCara = when (this) {
        Veredicto.Aceptada -> EstadoDeCara.Aceptada
        is Veredicto.Reintentar -> EstadoDeCara.Reintentar(motivo, intentosRestantes)
        Veredicto.RevisionManual -> EstadoDeCara.RevisionManual
    }
}
