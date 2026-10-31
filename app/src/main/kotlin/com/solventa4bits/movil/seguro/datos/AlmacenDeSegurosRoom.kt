package com.solventa4bits.movil.seguro.datos

import com.solventa4bits.movil.cotizacion.dominio.CodigoCobertura
import com.solventa4bits.movil.seguro.dominio.AlmacenDeSeguros
import com.solventa4bits.movil.seguro.dominio.SegurosGuardados
import com.solventa4bits.movil.solicitud.dominio.CreditoAsegurado
import com.solventa4bits.movil.solicitud.dominio.EstadoDeSolicitud
import com.solventa4bits.movil.solicitud.dominio.Solicitud
import com.solventa4bits.movil.solicitud.dominio.Vigencia

/** Guarda las polizas emitidas en la tabla "polizas" de Room. */
class AlmacenDeSegurosRoom(private val dao: PolizasDao) : AlmacenDeSeguros {

    override suspend fun guardar(seguros: List<Solicitud>, en: String) {
        dao.reemplazar(seguros.map { it.aFila(en) })
    }

    override suspend fun leer(): SegurosGuardados? {
        val filas = dao.todas()
        if (filas.isEmpty()) return null
        return SegurosGuardados(filas.map { it.aDominio() }, filas.first().guardadaEn)
    }

    override suspend fun borrar() {
        dao.borrarTodas()
    }

    private fun Solicitud.aFila(guardadaEn: String) = PolizaGuardada(
        id = id,
        numero = numero,
        creadaEn = creadaEn,
        emitidaEn = emitidaEn,
        vigenciaDesde = vigencia?.desde,
        vigenciaHasta = vigencia?.hasta,
        primaMensual = primaMensual,
        banco = credito?.banco,
        saldo = credito?.saldo,
        plazoMeses = credito?.plazoMeses,
        primaVida = primasPorCobertura[CodigoCobertura.VIDA],
        primaHogar = primasPorCobertura[CodigoCobertura.HOGAR],
        guardadaEn = guardadaEn,
    )

    private fun PolizaGuardada.aDominio() = Solicitud(
        id = id,
        estado = EstadoDeSolicitud.EMITIDA,
        creadaEn = creadaEn,
        emitidaEn = emitidaEn,
        pendientes = emptyList(),
        motivosDeRechazo = emptyList(),
        numero = numero,
        vigencia = if (vigenciaDesde != null && vigenciaHasta != null) Vigencia(vigenciaDesde, vigenciaHasta) else null,
        primaMensual = primaMensual,
        credito = if (banco != null && saldo != null && plazoMeses != null) {
            CreditoAsegurado(banco, saldo, plazoMeses)
        } else {
            null
        },
        primasPorCobertura = listOfNotNull(
            primaVida?.let { CodigoCobertura.VIDA to it },
            primaHogar?.let { CodigoCobertura.HOGAR to it },
        ).toMap(),
    )
}
