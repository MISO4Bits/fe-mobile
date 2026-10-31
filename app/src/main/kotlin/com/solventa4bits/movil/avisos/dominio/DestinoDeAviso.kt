package com.solventa4bits.movil.avisos.dominio

/**
 * A donde lleva un aviso al tocarlo. Los datos vienen en la intencion de la notificacion, es decir,
 * de fuera de la app: solo se aceptan los destinos conocidos y un identificador con la forma de uno.
 */
data class DestinoDeAviso(val id: String) {
    companion object {
        const val DESTINO_SEGURO = "seguro"
        const val CLAVE_DESTINO = "destino"
        const val CLAVE_ID = "id"
        private val FORMA_DEL_ID = Regex("^[\\w-]{1,64}$")

        /** Null si falta algo o no es un destino que la app conozca. */
        fun desde(destino: String?, id: String?): DestinoDeAviso? =
            if (destino == DESTINO_SEGURO && id != null && FORMA_DEL_ID.matches(id)) DestinoDeAviso(id) else null
    }
}
