package com.solventa4bits.movil.sesion.dominio

/** Doble de prueba: guarda la preferencia en una variable. */
class PreferenciasEnMemoria(
    private var huella: PreferenciaDeHuella = PreferenciaDeHuella.SIN_DECIDIR,
    private var dispositivoId: String? = null,
    private var permisoPedido: Boolean = false,
) : AlmacenDePreferencias {

    override fun huella(): PreferenciaDeHuella = huella

    override fun guardarHuella(preferencia: PreferenciaDeHuella) {
        huella = preferencia
    }

    override fun dispositivoId(): String? = dispositivoId

    override fun guardarDispositivoId(id: String?) {
        dispositivoId = id
    }

    override fun permisoDeAvisosPedido(): Boolean = permisoPedido

    override fun marcarPermisoDeAvisosPedido() {
        permisoPedido = true
    }
}
