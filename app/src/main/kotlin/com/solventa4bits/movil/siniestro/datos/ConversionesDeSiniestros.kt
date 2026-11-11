package com.solventa4bits.movil.siniestro.datos

import com.solventa4bits.movil.siniestro.dominio.ArchivoDelReporte
import com.solventa4bits.movil.siniestro.dominio.Beneficiario
import com.solventa4bits.movil.siniestro.dominio.ClaseDeArchivo
import com.solventa4bits.movil.siniestro.dominio.DetalleDeSiniestro
import com.solventa4bits.movil.siniestro.dominio.EstadoDeSiniestro
import com.solventa4bits.movil.siniestro.dominio.EventoSismo
import com.solventa4bits.movil.siniestro.dominio.EvidenciaPedida
import com.solventa4bits.movil.siniestro.dominio.LineaDePago
import com.solventa4bits.movil.siniestro.dominio.PagoDelSiniestro
import com.solventa4bits.movil.siniestro.dominio.ReporteDeSiniestro
import com.solventa4bits.movil.siniestro.dominio.SiniestroResumen
import com.solventa4bits.movil.siniestro.dominio.TipoDeSiniestro

/*
 * Del dominio al contrato del BFF y de vuelta. Un tipo, un estado o un beneficiario que la app no
 * conoce no la rompe: queda en null.
 */

private const val TRANSFERENCIA = "TRANSFERENCIA"

internal fun ReporteDeSiniestro.aDto() = ReportarSiniestroDatosDto(
    polizaId = polizaId,
    tipo = tipo.name,
    fechaOcurrencia = fechaOcurrencia.toString(),
    narracion = narracion,
    archivos = archivos.map { it.aDto() },
    destinoPago = destinoPago?.let {
        DestinoPagoDto(TRANSFERENCIA, it.banco, it.tipoCuenta.name, it.numeroCuenta)
    },
)

internal fun ArchivoDelReporte.aDto() = ArchivoReporteDto(clase.name, capturadoEn, latitud, longitud)

internal fun SiniestroDto.aDominio() = DetalleDeSiniestro(
    resumen = SiniestroResumenDto(id, numero, polizaId, tipo, estado, automatico, reportadoEn, actualizadoEn)
        .aDominio(),
    fechaOcurrencia = fechaOcurrencia,
    fechaLimite = fechaLimite,
    documentos = archivos.count { it.clase == ClaseDeArchivo.DOCUMENTO.name },
    fotos = archivos.count { it.clase == ClaseDeArchivo.FOTO.name },
    evidenciaPedida = evidenciaPedida?.let { EvidenciaPedida(it.detalle, it.pedidaEn) },
    motivoRechazo = motivoRechazo,
    pago = pago?.let { pago ->
        PagoDelSiniestro(
            pagadoEn = pago.pagadoEn,
            lineas = pago.lineas.map { linea ->
                LineaDePago(Beneficiario.entries.find { it.name == linea.beneficiario }, linea.valor, linea.destino)
            },
        )
    },
    evento = evento?.let { EventoSismo(it.fecha, it.municipio, it.magnitud) },
)

internal fun SiniestroResumenDto.aDominio() = SiniestroResumen(
    id = id,
    numero = numero,
    polizaId = polizaId,
    tipo = TipoDeSiniestro.entries.find { it.name == tipo },
    estado = EstadoDeSiniestro.entries.find { it.name == estado },
    automatico = automatico,
    reportadoEn = reportadoEn,
    actualizadoEn = actualizadoEn,
)
