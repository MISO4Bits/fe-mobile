package com.solventa4bits.movil.sesion.dominio

/** Doble de prueba: guarda la preferencia en una variable. */
class PreferenciasEnMemoria(
    private var huella: PreferenciaDeHuella = PreferenciaDeHuella.SIN_DECIDIR,
) : AlmacenDePreferencias {

    override fun huella(): PreferenciaDeHuella = huella

    override fun guardarHuella(preferencia: PreferenciaDeHuella) {
        huella = preferencia
    }
}
