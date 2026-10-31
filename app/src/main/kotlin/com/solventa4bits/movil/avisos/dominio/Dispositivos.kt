package com.solventa4bits.movil.avisos.dominio

import com.solventa4bits.movil.sesion.dominio.AlmacenDePreferencias

sealed interface ResultadoRegistro {
    data class Registrado(val dispositivoId: String) : ResultadoRegistro

    data object SesionVencida : ResultadoRegistro

    /** No llego al servidor o el servidor fallo. Se vuelve a intentar al proximo arranque. */
    data object Fallo : ResultadoRegistro
}

/** Los dispositivos del cliente para los avisos push, guardados en el servidor (BITS-229). */
interface ServicioDeDispositivos {
    /** Idempotente: el mismo token devuelve el mismo id. */
    suspend fun registrar(token: String, versionApp: String): ResultadoRegistro

    /** `true` si el servidor confirmo la baja. Quitar uno que ya no existe tambien lo confirma. */
    suspend fun darDeBaja(dispositivoId: String): Boolean
}

/** De donde sale el token de este telefono. Null si no se pudo obtener ahora. */
fun interface FuenteDeToken {
    suspend fun obtener(): String?
}

/** Registra y da de baja este telefono en el servidor, y recuerda el id que le dio. */
class RegistroDeDispositivo(
    private val servicio: ServicioDeDispositivos,
    private val fuente: FuenteDeToken,
    private val preferencias: AlmacenDePreferencias,
    private val versionApp: String,
) {
    /** Pide el token vigente y lo registra. Sin token no hay nada que hacer. */
    suspend fun registrar() {
        fuente.obtener()?.let { registrarToken(it) }
    }

    suspend fun registrarToken(token: String) {
        val resultado = servicio.registrar(token, versionApp)
        if (resultado is ResultadoRegistro.Registrado) {
            preferencias.guardarDispositivoId(resultado.dispositivoId)
        }
    }

    /**
     * Da de baja este telefono para que no reciba los avisos de quien cierra sesion. Si no hay
     * red el cierre sigue igual: el id local se olvida de todos modos, y el telefono vuelve a
     * quedar registrado, con el mismo token, cuando alguien inicie sesion de nuevo.
     */
    suspend fun darDeBaja() {
        val id = preferencias.dispositivoId() ?: return
        servicio.darDeBaja(id)
        preferencias.guardarDispositivoId(null)
    }
}
