package com.matias.pomodoro.graphics

import android.opengl.GLSurfaceView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner

/**
 * Composable que envuelve un GLSurfaceView para mostrar la animación de la lámpara de lava.
 *
 * Utiliza [LavaLampRenderer] para renderizar metaballs mediante shaders de OpenGL ES 2.0.
 *
 * @param mode El modo de animación (Lava, Focus Point Infinito, Focus Point Círculo).
 * @param colorScheme El esquema de colores a aplicar.
 * @param modifier Modificador para el tamaño y disposición del view.
 */
@Composable
fun LavaLampView(
    mode: LavaMode,
    colorScheme: ColorScheme,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Recordamos el renderer para mantener el estado de la animación entre recomposiciones.
    val renderer = remember { LavaLampRenderer(mode, colorScheme) }

    // Actualizamos los parámetros del renderer cuando cambian las propiedades del Composable.
    DisposableEffect(mode) {
        renderer.setMode(mode)
        onDispose { }
    }

    DisposableEffect(colorScheme) {
        renderer.setColorScheme(colorScheme)
        onDispose { }
    }

    AndroidView(
        factory = { ctx ->
            GLSurfaceView(ctx).apply {
                // Configuramos el contexto de OpenGL ES 2.0
                setEGLContextClientVersion(2)
                
                // Asignamos el renderer
                setRenderer(renderer)
                
                // Renderizado continuo para una animación fluida (60 FPS aprox.)
                renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
            }
        },
        modifier = modifier,
        update = { view ->
            // El estado ya se actualiza mediante el objeto renderer compartido.
        }
    )

    // Gestión del ciclo de vida del GLSurfaceView para ahorrar batería y recursos.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    // Reanudar el hilo de renderizado cuando la app está en primer plano.
                }
                Lifecycle.Event.ON_PAUSE -> {
                    // Pausar el hilo de renderizado cuando la app va al fondo.
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}
