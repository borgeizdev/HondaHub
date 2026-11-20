package com.multimidiacivic.launcher.tela

import android.content.Context
import android.graphics.Canvas
import android.graphics.CornerPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.os.SystemClock
import android.text.TextPaint
import android.text.TextUtils
import android.view.MotionEvent
import android.view.View
import com.multimidiacivic.launcher.musica.EstadoMusica
import com.multimidiacivic.launcher.util.Fontes

/**
 * Caixa do player na parte de baixo, da esquerda para a direita:
 *
 *   [|<]  [som]   Nome da musica      [>/||]  [>|]
 *                 ======o==========
 *
 * - |<  musica anterior        - som  muta / desmuta
 * - >/|| um botao so: toca ou pausa (o icone troca)
 * - >|  proxima musica
 * - a barra de progresso da para arrastar (avancar/voltar)
 *
 * Coordenadas internas seguem o modelo: caixa de 816 x 96 px + [FOLGA] de cada lado.
 */
class Player(ctx: Context) : View(ctx) {

    companion object {
        const val FOLGA = 14f
        private const val LARG = 816f + FOLGA * 2
        private const val ALT = 96f + FOLGA * 2

        // Centro (x) de cada botao dentro da caixa de 816
        private const val X_ANTERIOR = 50f
        private const val X_MUDO = 118f
        private const val X_TOCAR = 698f
        private const val X_PROXIMA = 766f
        private const val TRILHO_INI = 172f
        private const val TRILHO_FIM = 644f
    }

    var aoTocar: () -> Unit = {}
    var aoPausar: () -> Unit = {}
    var aoAnterior: () -> Unit = {}
    var aoProxima: () -> Unit = {}
    var aoMudo: () -> Unit = {}
    var aoBuscar: (Long) -> Unit = {}
    var aoTocarNoTitulo: () -> Unit = {}

    /** Som mutado? (muda o icone do alto-falante) */
    var mudo = false
        set(v) { field = v; invalidate() }

    private enum class Alvo { NADA, ANTERIOR, MUDO, TOCAR, PROXIMA, BARRA, TITULO }

    private var estado: EstadoMusica? = null
    private var aviso = "Nome da música"
    // Sem acesso ao player do outro app nao sabemos se esta tocando:
    // guardamos o que o usuario apertou por ultimo.
    private var tocandoLocal: Boolean? = null
    private var arrastando = -1f // fracao 0..1 enquanto o dedo arrasta a barra
    private var apertado = Alvo.NADA

    private val pinta = Paint(Paint.ANTI_ALIAS_FLAG)
    private val branco = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt() }
    private val traco = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFFFFF.toInt(); style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND
    }
    private val trilho = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x66C9D6EE; strokeCap = Paint.Cap.ROUND; style = Paint.Style.STROKE
    }
    private val feito = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFEFF4FF.toInt(); strokeCap = Paint.Cap.ROUND; style = Paint.Style.STROKE
    }
    private val letra = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xF0FFFFFF.toInt()
        typeface = Fontes.forte(ctx)
        textAlign = Paint.Align.CENTER
    }
    private val caixa = RectF()
    private val p = Path()

    // Atualiza a barra 2x por segundo enquanto toca
    private val tique = object : Runnable {
        override fun run() {
            invalidate()
            if (estado?.tocando == true) postDelayed(this, 500)
        }
    }

    /** [aviso] aparece no lugar do nome quando nao tem musica. */
    fun mostrar(e: EstadoMusica?, aviso: String) {
        estado = e
        if (e != null) tocandoLocal = null // o app de musica informou o estado real
        this.aviso = aviso
        removeCallbacks(tique)
        if (e?.tocando == true) post(tique)
        invalidate()
    }

    private fun tocando() = tocandoLocal ?: estado?.tocando ?: false

    override fun onDetachedFromWindow() {
        removeCallbacks(tique)
        super.onDetachedFromWindow()
    }

    // Converte coordenada do modelo (caixa de 816 x 96) para pixels da view
    private fun x(v: Float) = (v + FOLGA) * width / LARG
    private fun y(v: Float) = (v + FOLGA) * height / ALT
    private fun k(v: Float) = v * height / ALT // so tamanho (sem deslocar)

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        caixa.set(x(0f), y(0f), x(816f), y(96f))
        trilho.strokeWidth = k(5f)
        feito.strokeWidth = k(5f)
        traco.strokeWidth = k(3.2f)
        branco.pathEffect = CornerPathEffect(k(2.5f))
    }

    override fun onDraw(c: Canvas) {
        val raio = k(10f)
        Vidro.preencher(pinta, caixa.top, caixa.bottom, false)
        c.drawRoundRect(caixa, raio, raio, pinta)
        Vidro.borda(pinta, maxOf(1f, k(1.8f)))
        c.drawRoundRect(caixa, raio, raio, pinta)

        val cy = y(48f)
        desenharPulo(c, x(X_ANTERIOR), cy, false, apertado == Alvo.ANTERIOR)
        desenharSom(c, x(X_MUDO), cy, apertado == Alvo.MUDO)
        desenharTocar(c, x(X_TOCAR), cy, apertado == Alvo.TOCAR)
        desenharPulo(c, x(X_PROXIMA), cy, true, apertado == Alvo.PROXIMA)

        // Nome da musica
        val titulo = estado?.titulo?.takeIf { it.isNotBlank() } ?: aviso
        letra.textSize = k(26f)
        val ini = x(TRILHO_INI)
        val fim = x(TRILHO_FIM)
        val mostrado = TextUtils.ellipsize(titulo, letra, fim - ini, TextUtils.TruncateAt.END).toString()
        c.drawText(mostrado, (ini + fim) / 2f, y(36f) - (letra.descent() + letra.ascent()) / 2f, letra)

        // Barra de progresso
        val f = fracao()
        val ty = y(68f)
        val px = ini + (fim - ini) * f
        c.drawLine(ini, ty, fim, ty, trilho)
        if (f > 0f) c.drawLine(ini, ty, px, ty, feito)
        branco.alpha = 255
        c.drawCircle(px, ty, k(9f), branco)
    }

    /** |<  ou  >|  (barrinha + triangulo). */
    private fun desenharPulo(c: Canvas, cx: Float, cy: Float, proxima: Boolean, apertado: Boolean) {
        branco.alpha = if (apertado) 110 else 255
        val d = if (proxima) 1f else -1f
        val a = k(13f)
        p.reset()
        p.moveTo(cx - d * a * 0.85f, cy - a)
        p.lineTo(cx + d * a * 0.55f, cy)
        p.lineTo(cx - d * a * 0.85f, cy + a)
        p.close()
        c.drawPath(p, branco)
        val bx = cx + d * a * 0.62f
        val bw = k(4.5f)
        c.drawRect(minOf(bx, bx + d * bw), cy - a, maxOf(bx, bx + d * bw), cy + a, branco)
    }

    /** Alto-falante: com ondas (som ligado) ou com X (mudo). */
    private fun desenharSom(c: Canvas, cx: Float, cy: Float, apertado: Boolean) {
        val alfa = if (apertado) 110 else 255
        branco.alpha = alfa
        traco.alpha = alfa
        val a = k(13f)
        val ox = cx - a * 0.35f // desloca para a esquerda: as ondas/X ficam a direita
        p.reset()
        p.moveTo(ox - a * 0.95f, cy - a * 0.42f)
        p.lineTo(ox - a * 0.45f, cy - a * 0.42f)
        p.lineTo(ox + a * 0.15f, cy - a)
        p.lineTo(ox + a * 0.15f, cy + a)
        p.lineTo(ox - a * 0.45f, cy + a * 0.42f)
        p.lineTo(ox - a * 0.95f, cy + a * 0.42f)
        p.close()
        c.drawPath(p, branco)
        if (mudo) {
            val x0 = ox + a * 0.55f
            val x1 = ox + a * 1.25f
            val m = a * 0.35f
            c.drawLine(x0, cy - m, x1, cy + m, traco)
            c.drawLine(x0, cy + m, x1, cy - m, traco)
        } else {
            for (r in floatArrayOf(0.55f, 1.0f)) {
                val rr = a * r
                c.drawArc(RectF(ox + a * 0.15f - rr, cy - rr, ox + a * 0.15f + rr, cy + rr), -45f, 90f, false, traco)
            }
        }
    }

    /** Toca (triangulo) quando esta parado; pausa (||) quando esta tocando. */
    private fun desenharTocar(c: Canvas, cx: Float, cy: Float, apertado: Boolean) {
        branco.alpha = if (apertado) 110 else 255
        val a = k(16f)
        if (tocando()) {
            val bw = a * 0.36f
            val v = a * 0.22f
            c.drawRect(cx - v - bw, cy - a, cx - v, cy + a, branco)
            c.drawRect(cx + v, cy - a, cx + v + bw, cy + a, branco)
        } else {
            p.reset()
            p.moveTo(cx - a * 0.65f, cy - a)
            p.lineTo(cx + a * 0.95f, cy)
            p.lineTo(cx - a * 0.65f, cy + a)
            p.close()
            c.drawPath(p, branco)
        }
    }

    private fun fracao(): Float {
        if (arrastando >= 0f) return arrastando
        val e = estado ?: return 0f
        if (e.duracao <= 0) return 0f
        var pos = e.posicao
        if (e.tocando) pos += ((SystemClock.elapsedRealtime() - e.atualizadoEm) * e.velocidade).toLong()
        return (pos.toFloat() / e.duracao).coerceIn(0f, 1f)
    }

    /** Qual botao fica embaixo do dedo (divide a caixa em faixas). */
    private fun alvo(ex: Float, ey: Float): Alvo = when {
        ex < x((X_ANTERIOR + X_MUDO) / 2) -> Alvo.ANTERIOR
        ex < x((X_MUDO + TRILHO_INI) / 2) -> Alvo.MUDO
        ex > x((X_TOCAR + X_PROXIMA) / 2) -> Alvo.PROXIMA
        ex > x((TRILHO_FIM + X_TOCAR) / 2) -> Alvo.TOCAR
        ey > y(50f) && (estado?.duracao ?: 0L) > 0L -> Alvo.BARRA
        else -> Alvo.TITULO
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                apertado = alvo(e.x, e.y)
                if (apertado == Alvo.BARRA) arrastando = fracaoDoDedo(e.x)
                invalidate()
            }
            MotionEvent.ACTION_MOVE -> {
                if (apertado == Alvo.BARRA) { arrastando = fracaoDoDedo(e.x); invalidate() }
            }
            MotionEvent.ACTION_UP -> {
                when (apertado) {
                    Alvo.ANTERIOR -> aoAnterior()
                    Alvo.MUDO -> aoMudo()
                    Alvo.PROXIMA -> aoProxima()
                    Alvo.TOCAR -> {
                        val agora = tocando()
                        if (agora) aoPausar() else aoTocar()
                        tocandoLocal = !agora // troca o icone na hora
                    }
                    Alvo.BARRA -> estado?.let { aoBuscar((arrastando * it.duracao).toLong()) }
                    Alvo.TITULO -> aoTocarNoTitulo()
                    Alvo.NADA -> {}
                }
                apertado = Alvo.NADA
                arrastando = -1f
                invalidate()
            }
            MotionEvent.ACTION_CANCEL -> { apertado = Alvo.NADA; arrastando = -1f; invalidate() }
        }
        return true
    }

    private fun fracaoDoDedo(ex: Float) = ((ex - x(TRILHO_INI)) / (x(TRILHO_FIM) - x(TRILHO_INI))).coerceIn(0f, 1f)
}
