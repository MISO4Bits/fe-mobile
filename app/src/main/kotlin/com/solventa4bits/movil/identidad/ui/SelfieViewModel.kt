package com.solventa4bits.movil.identidad.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.solventa4bits.movil.identidad.dominio.EstadoGuardado
import com.solventa4bits.movil.identidad.dominio.ResultadoEstado
import com.solventa4bits.movil.identidad.dominio.ResultadoSubida
import com.solventa4bits.movil.identidad.dominio.ServicioDeEvidencias
import com.solventa4bits.movil.identidad.dominio.Veredicto
import kotlinx.coroutines.launch

data class EstadoSelfie(
    /** Consultando si ya habia una selfie guardada al abrir el paso. */
    val cargando: Boolean = true,
    /** Los mismos estados de cada cara de la cedula (Figma 08). */
    val selfie: EstadoDeCara = EstadoDeCara.SinFoto,
    val sesionVencida: Boolean = false,
) {
    /** Aceptada o en revision manual: se puede seguir (BITS-235, AC-4 y AC-5). */
    val puedeContinuar: Boolean
        get() = !cargando && selfie.lista
}

/**
 * Decide que pasa en "Tu selfie". Como en la cedula, [preparar] viene de
 * afuera porque usa imagenes de Android.
 */
class SelfieViewModel(
    private val evidencias: ServicioDeEvidencias,
    private val preparar: suspend (ByteArray) -> ByteArray?,
) : ViewModel() {

    var estado by mutableStateOf(EstadoSelfie())
        private set

    /** La selfie ya preparada, para verla en su tarjeta. */
    var miniatura by mutableStateOf<ByteArray?>(null)
        private set

    /** Al entrar al paso: si ya habia una selfie guardada. */
    fun abrir() {
        estado = EstadoSelfie()
        viewModelScope.launch {
            estado = when (val resultado = evidencias.consultar()) {
                is ResultadoEstado.Exito -> estado.copy(cargando = false, selfie = resultado.selfie.aEstado())
                ResultadoEstado.SesionVencida -> estado.copy(cargando = false, sesionVencida = true)
                // Sin saber que habia, se toma de nuevo: la nueva reemplaza a la anterior.
                ResultadoEstado.Fallo -> estado.copy(cargando = false)
            }
        }
    }

    /** Prepara y sube la selfie. Cada envio es un intento: si falla, no se repite solo. */
    fun enviar(foto: ByteArray) {
        if (estado.selfie == EstadoDeCara.Revisando) return
        estado = estado.copy(selfie = EstadoDeCara.Revisando)
        viewModelScope.launch {
            val lista = preparar(foto)
            if (lista == null) {
                estado = estado.copy(selfie = EstadoDeCara.NoSeEnvio)
                return@launch
            }
            miniatura = lista
            estado = when (val resultado = evidencias.subirSelfie(lista)) {
                is ResultadoSubida.Exito -> estado.copy(selfie = resultado.veredicto.aEstado())
                ResultadoSubida.SesionVencida -> estado.copy(selfie = EstadoDeCara.NoSeEnvio, sesionVencida = true)
                ResultadoSubida.Fallo -> estado.copy(selfie = EstadoDeCara.NoSeEnvio)
            }
        }
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
