package com.solventa4bits.movil

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.solventa4bits.movil.inicio.ui.PantallaInicio
import com.solventa4bits.movil.sesion.datos.AdaptadorSesion
import com.solventa4bits.movil.sesion.datos.AlmacenDeSesionSeguro
import com.solventa4bits.movil.sesion.datos.BovedaKeystore
import com.solventa4bits.movil.sesion.datos.crearSesionApi
import com.solventa4bits.movil.sesion.dominio.AlmacenDeSesion
import com.solventa4bits.movil.sesion.dominio.ServicioDeSesion
import com.solventa4bits.movil.sesion.ui.LoginViewModel
import com.solventa4bits.movil.sesion.ui.PantallaLogin
import com.solventa4bits.movil.traza.Traza
import com.solventa4bits.movil.ui.tema.TemaSolventa

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Aqui se arman las piezas de la app y se conectan entre si.
        val traza = Traza { origen, mensaje, causa -> Log.e(origen, mensaje, causa) }
        val servicio = AdaptadorSesion(crearSesionApi(BuildConfig.URL_BASE_BFF), traza)
        val almacen = AlmacenDeSesionSeguro(BovedaKeystore(applicationContext, traza), traza)

        setContent {
            TemaSolventa { AppSolventa(servicio, almacen) }
        }
    }
}

/** Muestra el inicio si hay sesion; si no, el inicio de sesion. */
@Composable
fun AppSolventa(servicio: ServicioDeSesion, almacen: AlmacenDeSesion) {
    val viewModel = viewModel { LoginViewModel(servicio, almacen) }
    Scaffold(modifier = Modifier.fillMaxSize()) { relleno ->
        if (viewModel.estado.sesionIniciada) {
            PantallaInicio(
                // La pantalla de nueva cotizacion llega en el siguiente paso de BITS-291.
                alCotizar = {},
                alCerrarSesion = viewModel::cerrarSesion,
                modifier = Modifier.padding(relleno),
            )
        } else {
            PantallaLogin(
                estado = viewModel.estado,
                alCambiarCorreo = viewModel::cambiarCorreo,
                alCambiarContrasena = viewModel::cambiarContrasena,
                alIniciarSesion = viewModel::iniciarSesion,
                modifier = Modifier.padding(relleno),
            )
        }
    }
}
