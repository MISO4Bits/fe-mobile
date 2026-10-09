package com.solventa4bits.movil.fixtures

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.rules.ExternalResource

/**
 * Un BFF falso por prueba. Se levanta antes y se apaga despues aunque la
 * prueba falle, asi un puerto o una respuesta encolada no pasan a la siguiente.
 */
class BffFalso : ExternalResource() {

    private val servidor = MockWebServer()

    /** URL base para crear el cliente HTTP de la app contra este BFF. */
    val url: String
        get() = servidor.url("/").toString()

    fun responder(codigo: Int, cuerpo: String = "") {
        servidor.enqueue(MockResponse().setResponseCode(codigo).setBody(cuerpo))
    }

    override fun before() {
        servidor.start()
    }

    override fun after() {
        servidor.shutdown()
    }
}
