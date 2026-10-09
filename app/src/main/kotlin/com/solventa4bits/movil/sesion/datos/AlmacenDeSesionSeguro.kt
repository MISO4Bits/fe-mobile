package com.solventa4bits.movil.sesion.datos

import com.solventa4bits.movil.sesion.dominio.AlmacenDeSesion
import com.solventa4bits.movil.sesion.dominio.Sesion
import com.solventa4bits.movil.traza.Traza
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/**
 * Guarda la sesion en la [Boveda]. Aqui vive la decision de que se guarda,
 * que se borra y que hacer si lo guardado esta danado; el cifrado es asunto
 * de la boveda.
 */
class AlmacenDeSesionSeguro(
    private val boveda: Boveda,
    private val traza: Traza,
) : AlmacenDeSesion {

    override fun guardar(sesion: Sesion) {
        val guardada = SesionGuardada(sesion.tokenDeAcceso, sesion.tokenDeRefresco, sesion.segundosDeVigencia)
        boveda.guardar(CLAVE, Json.encodeToString(guardada))
    }

    override fun leer(): Sesion? {
        val texto = boveda.leer(CLAVE) ?: return null
        return try {
            val guardada = Json.decodeFromString<SesionGuardada>(texto)
            Sesion(guardada.tokenDeAcceso, guardada.tokenDeRefresco, guardada.segundosDeVigencia)
        } catch (e: SerializationException) {
            // Una sesion ilegible no sirve: se descarta y el cliente vuelve a entrar.
            traza.error(ORIGEN, "La sesion guardada no se pudo leer y se descarto", e)
            boveda.borrar(CLAVE)
            null
        }
    }

    override fun borrar() {
        boveda.borrar(CLAVE)
    }

    @Serializable
    private data class SesionGuardada(
        val tokenDeAcceso: String,
        val tokenDeRefresco: String,
        val segundosDeVigencia: Long,
    )

    private companion object {
        const val ORIGEN = "AlmacenDeSesionSeguro"
        const val CLAVE = "sesion"
    }
}
