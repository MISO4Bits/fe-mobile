package com.solventa4bits.movil.avisos.dominio

/**
 * El numero propio de un aviso (BITS-372, AC-1). Android lo usa para la notificacion y para lo que
 * abre al tocarla: si dos avisos comparten numero, el nuevo pisa al anterior y tocar el primero
 * abre el destino del segundo. Sale del id del mensaje de Firebase; si no trae, de cuando llego.
 */
fun numeroDeAviso(idDelMensaje: String?, recibidoEnMs: Long): Int =
    idDelMensaje?.hashCode() ?: (recibidoEnMs % Int.MAX_VALUE).toInt()
