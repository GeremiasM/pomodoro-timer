package com.matias.pomodoro.graphics

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// ─────────────────────────────────────────────────────────────────────────────
// CONSTANTES Y CONFIGURACIÓN
// ─────────────────────────────────────────────────────────────────────────────

/** Número de metaballs a simular. Más = mayor riqueza visual pero más GPU. */
private const val NUM_METABALLS = 8

/** Umbral de isosuperficie para el algoritmo de metaballs. */
private const val METABALL_THRESHOLD = 1.0f

// ─────────────────────────────────────────────────────────────────────────────
// GLSL SHADERS
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Vertex Shader: Pasa los vértices del quad de pantalla completa.
 * Usamos un quad NDC simple ya que el fragment shader hace todo el trabajo.
 */
private val VERTEX_SHADER = """
    attribute vec2 aPosition;
    varying vec2 vTexCoord;
    
    void main() {
        // Convierte [-1,1] a coordenadas UV [0,1]
        vTexCoord = aPosition * 0.5 + 0.5;
        gl_Position = vec4(aPosition, 0.0, 1.0);
    }
""".trimIndent()

/**
 * Fragment Shader: Implementa el algoritmo de Metaballs (Blobby Objects).
 *
 * Algoritmo:
 * 1. Para cada fragmento, calculamos la suma de influencias de cada metaball.
 *    La influencia de una bola es: r² / d²  (radio al cuadrado / distancia al cuadrado)
 * 2. Si la suma supera un umbral (THRESHOLD), el fragmento está "dentro" de la blob.
 * 3. Aplicamos coloración basada en la densidad y posición para simular profundidad.
 * 4. Añadimos un gradiente suave para la ilusión de iluminación 3D.
 *
 * Paleta de colores:
 * - Verde musgo suave (#6B8E6B) para el fondo
 * - Ámbar cálido (#D4A96A) para los blobs principales
 * - Tonos bajos en azul para reducir fatiga visual
 */
private val FRAGMENT_SHADER_METABALLS = """
    #ifdef GL_ES
    precision mediump float;
    #endif
    
    uniform float uTime;
    uniform vec2  uResolution;
    uniform float uAspect;
    uniform vec2  uBallPositions[$NUM_METABALLS];
    uniform float uBallRadii[$NUM_METABALLS];
    uniform int   uMode;         // 0 = Lava normal, 1 = Focus Point (ochos/espirales)
    uniform vec3  uColorA;       // Color primario (ámbar cálido)
    uniform vec3  uColorB;       // Color secundario (verde musgo)
    uniform vec3  uColorBg;      // Color de fondo
    
    varying vec2 vTexCoord;
    
    // ── Función de suavizado (smooth-step personalizado) ──────────────────────
    float smootherstep(float edge0, float edge1, float x) {
        x = clamp((x - edge0) / (edge1 - edge0), 0.0, 1.0);
        return x * x * x * (x * (x * 6.0 - 15.0) + 10.0);
    }
    
    // ── Gradiente de Perlin simplificado para textura orgánica ────────────────
    float hash(vec2 p) {
        p = fract(p * vec2(234.34, 435.345));
        p += dot(p, p + 34.23);
        return fract(p.x * p.y);
    }
    
    float noise(vec2 p) {
        vec2 i = floor(p);
        vec2 f = fract(p);
        vec2 u = f * f * (3.0 - 2.0 * f); // Hermite
        
        float a = hash(i);
        float b = hash(i + vec2(1.0, 0.0));
        float c = hash(i + vec2(0.0, 1.0));
        float d = hash(i + vec2(1.0, 1.0));
        
        return mix(mix(a, b, u.x), mix(c, d, u.x), u.y);
    }
    
    // ── Cálculo principal de metaballs ────────────────────────────────────────
    float metaballField(vec2 uv) {
        float field = 0.0;
        
        for (int i = 0; i < $NUM_METABALLS; i++) {
            // Corrige el aspect ratio escalando el eje X ANTES de medir distancias:
            // dist = sqrt((p.x*aspect - ball.x*aspect)^2 + (p.y - ball.y)^2)
            vec2 p = vec2(uv.x * uAspect, uv.y);
            vec2 b = vec2(uBallPositions[i].x * uAspect, uBallPositions[i].y);
            vec2 diff = p - b;
            float dist2 = dot(diff, diff);
            float r2    = uBallRadii[i] * uBallRadii[i];
            
            // Función de influencia con falloff suave
            // Evitamos división por cero con max()
            field += r2 / max(dist2, 0.0001);
        }

        // Clamp de densidad: evita valores extremos en colisiones (píxeles blancos)
        // Nota: esto NO afecta el movimiento, solo estabiliza el shading.
        return min(field, 8.0);
    }
    
    // ── Cálculo de normal aproximada para iluminación ─────────────────────────
    vec2 metaballNormal(vec2 uv, float eps) {
        float dx = metaballField(uv + vec2(eps, 0.0)) - metaballField(uv - vec2(eps, 0.0));
        float dy = metaballField(uv + vec2(0.0, eps)) - metaballField(uv - vec2(0.0, eps));
        vec2 g = vec2(dx, dy);
        float gl = max(length(g), 1e-4); // epsilon contra colisiones/zonas planas
        return g / gl;
    }
    
    void main() {
        vec2 uv = vTexCoord;
        
        float field = metaballField(uv);
        
        // ── Suavizado del borde de la isosuperficie ───────────────────────────
        float threshold  = $METABALL_THRESHOLD;
        float softness   = 0.08; // Ancho del borde suavizado
        float blobMask   = smootherstep(threshold - softness, threshold + softness, field);
        
        // ── Iluminación procedural ────────────────────────────────────────────
        vec2  normal     = metaballNormal(uv, 0.005);
        vec3  lightDir   = normalize(vec3(0.5, 0.8, 1.0));
        float diffuse    = max(dot(vec3(normal, 0.0), lightDir), 0.0);
        
        // ── Textura orgánica interna ──────────────────────────────────────────
        vec2 uvNoise = vec2(uv.x * uAspect, uv.y);
        float n = noise(uvNoise * 4.0 + uTime * 0.1);
        float n2 = noise(uvNoise * 8.0 - uTime * 0.07);
        float organicTex = n * 0.6 + n2 * 0.4;
        
        // ── Coloración ────────────────────────────────────────────────────────
        // Mezcla entre colorA y colorB basada en la densidad del campo
        float colorMix = smootherstep(threshold, threshold * 2.5, field);
        vec3  blobColor = mix(uColorA, uColorB, colorMix);
        
        // Añadir detalle de textura orgánica
        blobColor = mix(blobColor, blobColor * 1.3, organicTex * 0.25);
        
        // Iluminación especular suave (tipo lava)
        float spec = pow(diffuse, 8.0) * 0.22;
        spec = min(spec, 0.12); // límite duro para evitar saturación a blanco
        blobColor += vec3(spec);
        
        // Oscurecer bordes para efecto de profundidad
        float edgeDarken = 1.0 - (1.0 - blobMask) * 0.3;
        blobColor *= edgeDarken;
        
        // ── Fondo con gradiente sutil ─────────────────────────────────────────
        float bgGradient = vTexCoord.y * 0.3 + noise(uvNoise * 2.0 + uTime * 0.02) * 0.05;
        vec3  bgColor    = uColorBg + vec3(bgGradient * 0.05);
        
        // ── Composición final ─────────────────────────────────────────────────
        vec3 finalColor = mix(bgColor, blobColor, blobMask);
        
        // Viñeta suave para enfocar el centro
        vec2  center  = vec2(0.5, 0.5);
        float vignette = 1.0 - smootherstep(0.3, 0.8, length(vTexCoord - center));
        finalColor    *= mix(0.85, 1.0, vignette);
        
        gl_FragColor = vec4(clamp(finalColor, 0.0, 1.0), 1.0);
    }
""".trimIndent()

// ─────────────────────────────────────────────────────────────────────────────
// DATOS DE METABALLS (estado CPU)
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Representa una metaball individual con su posición, velocidad y radio.
 * Las coordenadas están en espacio UV [0, aspectRatio] x [0, 1].
 */
data class MetaBall(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    val radius: Float,
    // Parámetros para el movimiento de Focus Point
    val orbitRadiusX: Float = 0f,
    val orbitRadiusY: Float = 0f,
    val orbitSpeed: Float = 0f,
    val orbitOffset: Float = 0f
)

/**
 * Modo de animación de las metaballs.
 */
enum class LavaMode {
    /** Movimiento libre, fluido, tipo lava lamp */
    LAVA,
    /** Las bolas trazan patrones de ojo (infinito/8) para estiramiento ocular */
    FOCUS_POINT_INFINITY,
    /** Las bolas trazan círculos concéntricos */
    FOCUS_POINT_CIRCLE
}

// ─────────────────────────────────────────────────────────────────────────────
// RENDERER OPENGL
// ─────────────────────────────────────────────────────────────────────────────

/**
 * GLSurfaceView.Renderer que ejecuta el shader de metaballs.
 *
 * Diseño:
 * - Un quad de pantalla completa (2 triángulos) cubre toda la superficie.
 * - El fragment shader calcula el campo de metaballs en cada fragmento.
 * - Las posiciones de las bolas se actualizan en la CPU cada frame y se
 *   pasan como uniforms al shader.
 *
 * Optimizaciones de batería:
 * - Usamos RENDERMODE_CONTINUOUSLY solo cuando la app está en primer plano.
 * - El renderer se puede pausar/reanudar desde el ViewModel.
 * - Precisión mediump en el shader para balance rendimiento/calidad.
 */
class LavaLampRenderer(
    private var mode: LavaMode = LavaMode.LAVA,
    private var colorScheme: ColorScheme = ColorScheme.AMBER_MOSS
) : GLSurfaceView.Renderer {

    // ── Handles de OpenGL ────────────────────────────────────────────────────
    private var programHandle = 0
    private var positionHandle = 0
    private var timeHandle = 0
    private var resolutionHandle = 0
    private var aspectHandle = 0
    private var ballPositionsHandle = 0
    private var ballRadiiHandle = 0
    private var modeHandle = 0
    private var colorAHandle = 0
    private var colorBHandle = 0
    private var colorBgHandle = 0

    // ── Geometría ────────────────────────────────────────────────────────────
    private lateinit var vertexBuffer: FloatBuffer

    // Quad de pantalla completa: 2 triángulos = 6 vértices
    private val quadVertices = floatArrayOf(
        -1f, -1f,
         1f, -1f,
        -1f,  1f,
        -1f,  1f,
         1f, -1f,
         1f,  1f
    )

    // ── Estado de simulación ─────────────────────────────────────────────────
    private var startTime = System.nanoTime()
    private var width = 1f
    private var height = 1f
    private val aspectRatio get() = width / height

    // Transición suave entre paletas (evita "flash" visual y mantiene batería)
    @Volatile private var targetScheme: ColorScheme = colorScheme
    private var schemeFrom: GlColorScheme = colorScheme.toGlsl()
    private var schemeTo: GlColorScheme = colorScheme.toGlsl()
    private var schemeBlendStartSec: Float = 0f
    private var schemeBlend: Float = 1f

    /** Lista de metaballs. Se inicializa en onSurfaceCreated. */
    private val balls = mutableListOf<MetaBall>()

    // ── API pública ──────────────────────────────────────────────────────────

    fun setMode(newMode: LavaMode) { mode = newMode; rebuildBalls() }
    fun setColorScheme(scheme: ColorScheme) { targetScheme = scheme }

    // ─────────────────────────────────────────────────────────────────────────
    // GLSurfaceView.Renderer
    // ─────────────────────────────────────────────────────────────────────────

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0f, 0f, 0f, 1f)

        // Compilar y enlazar shaders
        val vertShader = compileShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER)
        val fragShader = compileShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER_METABALLS)
        programHandle = linkProgram(vertShader, fragShader)

        // Obtener handles de uniforms y atributos
        positionHandle    = GLES20.glGetAttribLocation(programHandle, "aPosition")
        timeHandle        = GLES20.glGetUniformLocation(programHandle, "uTime")
        resolutionHandle  = GLES20.glGetUniformLocation(programHandle, "uResolution")
        aspectHandle      = GLES20.glGetUniformLocation(programHandle, "uAspect")
        ballPositionsHandle = GLES20.glGetUniformLocation(programHandle, "uBallPositions")
        ballRadiiHandle   = GLES20.glGetUniformLocation(programHandle, "uBallRadii")
        modeHandle        = GLES20.glGetUniformLocation(programHandle, "uMode")
        colorAHandle      = GLES20.glGetUniformLocation(programHandle, "uColorA")
        colorBHandle      = GLES20.glGetUniformLocation(programHandle, "uColorB")
        colorBgHandle     = GLES20.glGetUniformLocation(programHandle, "uColorBg")

        // Preparar buffer de vértices
        vertexBuffer = ByteBuffer
            .allocateDirect(quadVertices.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply {
                put(quadVertices)
                position(0)
            }
    }

    override fun onSurfaceChanged(gl: GL10?, w: Int, h: Int) {
        GLES20.glViewport(0, 0, w, h)
        width = w.toFloat()
        height = h.toFloat()
        rebuildBalls()
    }

    override fun onDrawFrame(gl: GL10?) {
        val elapsedSec = (System.nanoTime() - startTime) / 1_000_000_000f

        // Actualizar física de las bolas en CPU
        updateBallPhysics(elapsedSec)

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        GLES20.glUseProgram(programHandle)

        // Quad
        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glVertexAttribPointer(positionHandle, 2, GLES20.GL_FLOAT, false, 8, vertexBuffer)

        // Uniforms de tiempo y resolución
        GLES20.glUniform1f(timeHandle, elapsedSec)
        GLES20.glUniform2f(resolutionHandle, width, height)
        GLES20.glUniform1f(aspectHandle, aspectRatio)
        GLES20.glUniform1i(modeHandle, mode.ordinal)

        // Colores con crossfade suave (sin duplicar GLSurfaceView)
        if (targetScheme != colorScheme) {
            colorScheme = targetScheme
            schemeFrom = lerpScheme(schemeFrom, schemeTo, schemeBlend)
            schemeTo = colorScheme.toGlsl()
            schemeBlendStartSec = elapsedSec
            schemeBlend = 0f
        }

        // Duración corta: evita flashes sin gastar batería
        val blendDur = 0.55f
        schemeBlend = ((elapsedSec - schemeBlendStartSec) / blendDur).coerceIn(0f, 1f)
        val colors = lerpScheme(schemeFrom, schemeTo, schemeBlend)

        GLES20.glUniform3f(colorAHandle, colors.colorA.r, colors.colorA.g, colors.colorA.b)
        GLES20.glUniform3f(colorBHandle, colors.colorB.r, colors.colorB.g, colors.colorB.b)
        GLES20.glUniform3f(colorBgHandle, colors.colorBg.r, colors.colorBg.g, colors.colorBg.b)

        // Posiciones y radios de las bolas (flat arrays para OpenGL)
        val posArray  = FloatArray(NUM_METABALLS * 2)
        val radiiArray = FloatArray(NUM_METABALLS)
        balls.forEachIndexed { i, ball ->
            posArray[i * 2]     = ball.x / aspectRatio  // Normalizar a [0,1]
            posArray[i * 2 + 1] = ball.y
            radiiArray[i]       = ball.radius
        }
        GLES20.glUniform2fv(ballPositionsHandle, NUM_METABALLS, posArray, 0)
        GLES20.glUniform1fv(ballRadiiHandle, NUM_METABALLS, radiiArray, 0)

        // Dibujar
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6)
        GLES20.glDisableVertexAttribArray(positionHandle)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // FÍSICA DE METABALLS
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Inicializa las metaballs con posiciones/velocidades aleatorias o en
     * patrones de Focus Point según el modo actual.
     */
    private fun rebuildBalls() {
        balls.clear()
        val ar = if (aspectRatio.isNaN() || aspectRatio <= 0) 0.5625f else aspectRatio

        when (mode) {
            LavaMode.LAVA -> {
                // Bolas libres con velocidades aleatorias
                repeat(NUM_METABALLS) {
                    balls.add(
                        MetaBall(
                            x      = (0.1f + Math.random().toFloat() * 0.8f) * ar,
                            y      = 0.1f + Math.random().toFloat() * 0.8f,
                            vx     = (Math.random().toFloat() - 0.5f) * 0.002f,
                            vy     = (Math.random().toFloat() - 0.5f) * 0.002f,
                            radius = 0.06f + Math.random().toFloat() * 0.08f
                        )
                    )
                }
            }

            LavaMode.FOCUS_POINT_INFINITY -> {
                // Patrón de lemniscata (∞) para estiramiento horizontal
                val speeds = floatArrayOf(0.3f, 0.35f, 0.4f, 0.25f, 0.45f, 0.28f, 0.32f, 0.38f)
                repeat(NUM_METABALLS) { i ->
                    balls.add(
                        MetaBall(
                            x = ar * 0.5f, y = 0.5f,
                            vx = 0f, vy = 0f,
                            radius = 0.055f + i * 0.005f,
                            orbitRadiusX = 0.25f * ar,
                            orbitRadiusY = 0.12f,
                            orbitSpeed   = speeds[i],
                            orbitOffset  = (i.toFloat() / NUM_METABALLS) * 2f * PI.toFloat()
                        )
                    )
                }
            }

            LavaMode.FOCUS_POINT_CIRCLE -> {
                // Patrón circular concéntrico
                repeat(NUM_METABALLS) { i ->
                    val angle = (i.toFloat() / NUM_METABALLS) * 2f * PI.toFloat()
                    val r = 0.18f + (i % 3) * 0.06f
                    balls.add(
                        MetaBall(
                            x = ar * 0.5f, y = 0.5f,
                            vx = 0f, vy = 0f,
                            radius = 0.05f,
                            orbitRadiusX = r * ar,
                            orbitRadiusY = r,
                            orbitSpeed   = 0.25f + (i % 3) * 0.05f,
                            orbitOffset  = angle
                        )
                    )
                }
            }
        }
    }

    /**
     * Actualiza las posiciones de las bolas cada frame.
     * - Modo LAVA: movimiento browniano con rebote en paredes.
     * - Modos FOCUS POINT: movimiento paramétrico (lemniscata o círculo).
     */
    private fun updateBallPhysics(time: Float) {
        val ar = if (aspectRatio.isNaN() || aspectRatio <= 0) 0.5625f else aspectRatio

        balls.forEachIndexed { _, ball ->
            when (mode) {
                LavaMode.LAVA -> {
                    // Añadir pequeño ruido para variación orgánica
                    ball.vx += (Math.random().toFloat() - 0.5f) * 0.0002f
                    ball.vy += (Math.random().toFloat() - 0.5f) * 0.0002f

                    // Limitar velocidad máxima
                    val maxV = 0.003f
                    ball.vx = ball.vx.coerceIn(-maxV, maxV)
                    ball.vy = ball.vy.coerceIn(-maxV, maxV)

                    // Actualizar posición
                    ball.x += ball.vx
                    ball.y += ball.vy

                    // Rebote en límites con margen
                    val margin = ball.radius
                    if (ball.x < margin) { ball.x = margin; ball.vx = -ball.vx * 0.8f }
                    if (ball.x > ar - margin) { ball.x = ar - margin; ball.vx = -ball.vx * 0.8f }
                    if (ball.y < margin) { ball.y = margin; ball.vy = -ball.vy * 0.8f }
                    if (ball.y > 1f - margin) { ball.y = 1f - margin; ball.vy = -ball.vy * 0.8f }
                }

                LavaMode.FOCUS_POINT_INFINITY -> {
                    // Lemniscata de Bernoulli: x = a·cos(t) / (1+sin²(t)), y = a·sin(t)·cos(t) / (1+sin²(t))
                    val t = time * ball.orbitSpeed + ball.orbitOffset
                    val denom = 1f + sin(t) * sin(t)
                    ball.x = ar * 0.5f + ball.orbitRadiusX * cos(t) / denom
                    ball.y = 0.5f + ball.orbitRadiusY * sin(t) * cos(t) / denom
                }

                LavaMode.FOCUS_POINT_CIRCLE -> {
                    val t = time * ball.orbitSpeed + ball.orbitOffset
                    ball.x = ar * 0.5f + ball.orbitRadiusX * cos(t)
                    ball.y = 0.5f + ball.orbitRadiusY * sin(t)
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UTILIDADES OPENGL
    // ─────────────────────────────────────────────────────────────────────────

    private fun compileShader(type: Int, source: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, source)
        GLES20.glCompileShader(shader)

        val status = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, status, 0)
        if (status[0] == 0) {
            Log.e("Pomodoro/Shader", "Error compilando shader: ${GLES20.glGetShaderInfoLog(shader)}")
            GLES20.glDeleteShader(shader)
            return 0
        }
        return shader
    }

    private fun linkProgram(vertShader: Int, fragShader: Int): Int {
        val program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, vertShader)
        GLES20.glAttachShader(program, fragShader)
        GLES20.glLinkProgram(program)

        val status = IntArray(1)
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, status, 0)
        if (status[0] == 0) {
            Log.e("Pomodoro/Shader", "Error enlazando programa: ${GLES20.glGetProgramInfoLog(program)}")
        }

        // Los shaders ya están enlazados, podemos liberarlos
        GLES20.glDeleteShader(vertShader)
        GLES20.glDeleteShader(fragShader)
        return program
    }

    private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t

    private fun lerpColor(a: GlColor, b: GlColor, t: Float): GlColor = GlColor(
        r = lerp(a.r, b.r, t),
        g = lerp(a.g, b.g, t),
        b = lerp(a.b, b.b, t)
    )

    private fun lerpScheme(a: GlColorScheme, b: GlColorScheme, t: Float): GlColorScheme = GlColorScheme(
        colorA = lerpColor(a.colorA, b.colorA, t),
        colorB = lerpColor(a.colorB, b.colorB, t),
        colorBg = lerpColor(a.colorBg, b.colorBg, t)
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// ESQUEMAS DE COLOR
// ─────────────────────────────────────────────────────────────────────────────

/** Color en espacio lineal [0,1] para OpenGL. */
data class GlColor(val r: Float, val g: Float, val b: Float)

/** Resultado de color para el shader. */
data class GlColorScheme(val colorA: GlColor, val colorB: GlColor, val colorBg: GlColor)

/**
 * Paletas de color diseñadas para reducir la fatiga visual.
 * Bajas en luz azul, tonos cálidos y neutros.
 */
enum class ColorScheme {
    /** Ámbar dorado + verde musgo sobre fondo oscuro. Principal de Pomodoro. */
    AMBER_MOSS,
    /** Terracota suave + verde salvia. Cálido y relajante. */
    TERRACOTTA_SAGE,
    /** Malva apagado + gris azulado. Para ambientes con poca luz. */
    DUSK,
    /** Verde bosque + ámbar. Máxima relajación ocular. */
    FOREST,

    /** Simulación de luz de vela: cálida, mínima supresión de melatonina. */
    CANDLELIGHT,
    /** Papel antiguo: sepia/ocre con contraste suave. */
    SEPIA_PAPER,
    /** Verde profundo con acento mostaza suave. */
    MIDNIGHT_EMERALD;

    fun toGlsl(): GlColorScheme = when (this) {
        AMBER_MOSS -> GlColorScheme(
            colorA  = GlColor(0.831f, 0.663f, 0.416f),  // #D4A96A ámbar
            colorB  = GlColor(0.420f, 0.557f, 0.420f),  // #6B8E6B verde musgo
            colorBg = GlColor(0.082f, 0.102f, 0.090f)   // #151A17 casi negro verdoso
        )
        TERRACOTTA_SAGE -> GlColorScheme(
            colorA  = GlColor(0.780f, 0.478f, 0.357f),  // #C77A5B terracota
            colorB  = GlColor(0.518f, 0.627f, 0.494f),  // #84A07E salvia
            colorBg = GlColor(0.090f, 0.082f, 0.078f)   // #171514 marrón oscuro
        )
        DUSK -> GlColorScheme(
            colorA  = GlColor(0.588f, 0.459f, 0.620f),  // #967599 malva
            colorB  = GlColor(0.357f, 0.467f, 0.549f),  // #5B778C gris azulado
            colorBg = GlColor(0.063f, 0.071f, 0.102f)   // #10121A azul muy oscuro
        )
        FOREST -> GlColorScheme(
            colorA  = GlColor(0.298f, 0.502f, 0.298f),  // #4C804C verde bosque
            colorB  = GlColor(0.741f, 0.596f, 0.298f),  // #BD984C ámbar bosque
            colorBg = GlColor(0.047f, 0.082f, 0.047f)   // #0C150C verde muy oscuro
        )
        CANDLELIGHT -> GlColorScheme(
            colorA  = GlColor(0.902f, 0.494f, 0.133f),  // #E67E22 naranja/ámbar profundo
            colorB  = GlColor(0.690f, 0.227f, 0.180f),  // rojo ladrillo (acento)
            colorBg = GlColor(0.071f, 0.051f, 0.039f)   // #120D0A café muy oscuro
        )
        SEPIA_PAPER -> GlColorScheme(
            colorA  = GlColor(0.651f, 0.486f, 0.322f),  // #A67C52 ocre/sepia
            colorB  = GlColor(0.290f, 0.184f, 0.102f),  // marrón oscuro (acento)
            colorBg = GlColor(0.110f, 0.106f, 0.090f)   // #1C1B17 crema oscuro/grisáceo
        )
        MIDNIGHT_EMERALD -> GlColorScheme(
            colorA  = GlColor(0.176f, 0.353f, 0.153f),  // #2D5A27 verde bosque cálido
            colorB  = GlColor(0.722f, 0.608f, 0.290f),  // mostaza suave (acento)
            colorBg = GlColor(0.031f, 0.063f, 0.031f)   // #081008 verde-negro
        )
    }
}
