package com.solventa4bits.movil.fixtures

import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.rules.ExternalResource
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Un BFF falso por prueba. Se levanta antes y se apaga despues aunque la
 * prueba falle, asi un puerto o una respuesta encolada no pasan a la siguiente.
 */
class BffFalso : ExternalResource() {

    private val servidor = MockWebServer()
    private val enOrden = ConcurrentLinkedQueue<MockResponse>()
    private val porRuta = ConcurrentHashMap<String, MockResponse>()

    /** Lo que la app pidio, como "DELETE /v1/cotizaciones/actual", en orden de llegada. */
    val recibidas = ConcurrentLinkedQueue<String>()

    /** URL base para crear el cliente HTTP de la app contra este BFF. */
    val url: String
        get() = servidor.url("/").toString()

    /** Responde a la siguiente peticion que llegue, en el orden en que se encolan. */
    fun responder(codigo: Int, cuerpo: String = "") {
        enOrden.add(respuesta(codigo, cuerpo))
    }

    /**
     * Responde siempre lo mismo a una ruta, sin importar cuando llegue. Sirve
     * para las consultas que la app hace por su cuenta, como la del Home, que
     * de otro modo se quedarian con la respuesta encolada para otra peticion.
     * Con [metodo], solo a ese metodo: el DELETE y el GET de una ruta responden distinto.
     */
    fun responderSiempreEn(ruta: String, codigo: Int, cuerpo: String = "", metodo: String? = null) {
        porRuta[if (metodo == null) ruta else "$metodo $ruta"] = respuesta(codigo, cuerpo)
    }

    private fun respuesta(codigo: Int, cuerpo: String) = MockResponse().setResponseCode(codigo).setBody(cuerpo)

    override fun before() {
        servidor.dispatcher = object : Dispatcher() {
            // Sin nada preparado responde 404, en vez de dejar la peticion esperando.
            override fun dispatch(request: RecordedRequest): MockResponse {
                val pedida = "${request.method} ${request.path}"
                recibidas.add(pedida)
                return porRuta[pedida] ?: request.path?.let(porRuta::get) ?: enOrden.poll()
                    ?: respuesta(NO_ENCONTRADO, "")
            }
        }
        servidor.start()
    }

    override fun after() {
        servidor.shutdown()
    }

    private companion object {
        const val NO_ENCONTRADO = 404
    }
}
