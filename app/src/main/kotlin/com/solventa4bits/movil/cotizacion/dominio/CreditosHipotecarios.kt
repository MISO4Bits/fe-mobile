package com.solventa4bits.movil.cotizacion.dominio

/** Un banco del mercado, para el campo "Banco o entidad". */
data class EntidadFinanciera(
    val id: String,
    val nombre: String,
)

/** Una hipoteca del cliente, traida de su banco por Open Finance. Valores en COP. */
data class CreditoHipotecario(
    /** Null cuando el banco de la hipoteca no esta en la lista de entidades. */
    val entidadId: String?,
    val entidadNombre: String,
    val valorCredito: Double,
    val saldoInsoluto: Double,
    val plazoRestanteMeses: Int,
    val cuotaMensual: Double,
)

/** Le dice a la pantalla que mostrar. Son los estados del contrato de bff-mobile. */
enum class EstadoCreditos {
    /** Hay hipotecas: se muestran sus datos. */
    DISPONIBLE,

    /** El cliente no tiene hipotecas abiertas: escribe los datos a mano. */
    SIN_HIPOTECAS,

    /** El cliente no autorizo Open Finance: escribe los datos a mano. */
    SIN_CONSENTIMIENTO,

    /** La fuente no respondio: puede reintentar o escribir los datos. */
    NO_DISPONIBLE,
}

data class CreditosHipotecarios(
    val estado: EstadoCreditos,
    val creditos: List<CreditoHipotecario>,
    val entidades: List<EntidadFinanciera>,
    /** Cuando se trajeron los datos de la fuente, en ISO 8601. Null si no hay datos. */
    val fechaConsulta: String? = null,
)

/** Si los datos del credito vienen del banco o los declaro el cliente (BITS-219, AC-6). */
enum class OrigenDeDatos { VERIFICADO, DECLARADO }

/**
 * Rangos de los datos escritos a mano. Son los que valida bff-web en
 * POST /v1/cotizaciones (openapi/openapi.yaml, DatosCredito).
 */
object RangosDeCredito {
    const val VALOR_MINIMO = 10_000_000L
    const val MESES_MINIMO = 12
    const val MESES_MAXIMO = 480
    const val LETRAS_MINIMAS_DEL_BANCO = 2
    const val LETRAS_MAXIMAS_DEL_BANCO = 80
}

/** Todo lo que puede pasar al consultar las hipotecas del cliente. */
sealed interface ResultadoCreditos {
    data class Exito(val creditos: CreditosHipotecarios) : ResultadoCreditos

    /** No hay token, o el BFF lo rechazo con 401. */
    data object SesionVencida : ResultadoCreditos

    /** No se pudo completar: no hay red o el servidor fallo. */
    data object Fallo : ResultadoCreditos
}

/** Lo que la app necesita para empezar una cotizacion, sin saber como se consulta. */
interface ServicioDeCreditos {
    suspend fun consultar(): ResultadoCreditos
}
