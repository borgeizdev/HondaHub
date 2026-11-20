package com.multimidiacivic.launcher

import android.content.Context
import android.graphics.Canvas
import android.graphics.CornerPathEffect
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.text.TextPaint
import android.text.TextUtils
import android.view.View

/**
 * Pecas de vidro azul (botao em trapezio e a caixa do player):
 * preenchimento translucido e borda fina. Sem brilho/neon em volta.
 */
object Vidro {
    // Cores derivadas da luz do papel de parede (padrao: azul)
    private var normalTopo = 0x403C6CC8
    private var normalBase = 0x50122E70
    private var apertadoTopo = 0x704A7FE0
    private var apertadoBase = 0x702A5CC0
    private var corBorda = 0x668AB4F0

    /** Tinge o vidro com a cor [destaque] do tema escolhido. */
    fun tingir(destaque: Int) {
        normalTopo = misturar(destaque, 0.78f, 0x40)
        normalBase = misturar(destaque, 0.30f, 0x50)
        apertadoTopo = misturar(destaque, 0.92f, 0x70)
        apertadoBase = misturar(destaque, 0.62f, 0x70)
        corBorda = clarear(destaque, 0.45f, 0x66)
    }

    fun preencher(p: Paint, topo: Float, base: Float, apertado: Boolean) {
        p.style = Paint.Style.FILL
        p.shader = if (apertado)
            LinearGradient(0f, topo, 0f, base, apertadoTopo, apertadoBase, Shader.TileMode.CLAMP)
        else
            LinearGradient(0f, topo, 0f, base, normalTopo, normalBase, Shader.TileMode.CLAMP)
    }

    fun borda(p: Paint, espessura: Float) {
        p.style = Paint.Style.STROKE
        p.shader = null
        p.color = corBorda
        p.strokeWidth = espessura
    }

    /** Escurece [c] pelo fator [f] e aplica a transparencia [alfa]. */
    private fun misturar(c: Int, f: Float, alfa: Int): Int {
        val r = ((c shr 16 and 255) * f).toInt()
        val g = ((c shr 8 and 255) * f).toInt()
        val b = ((c and 255) * f).toInt()
        return (alfa shl 24) or (r shl 16) or (g shl 8) or b
    }

    /** Puxa [c] para o branco pelo fator [f] e aplica a transparencia [alfa]. */
    fun clarear(c: Int, f: Float, alfa: Int): Int {
        fun canal(v: Int) = (v + (255 - v) * f).toInt()
        return (alfa shl 24) or (canal(c shr 16 and 255) shl 16) or (canal(c shr 8 and 255) shl 8) or canal(c and 255)
    }
}

/**
 * Botao em forma de trapezio ("TEXTO" no modelo).
 * [ladoEsquerdo] = true: a ponta inclinada fica para a direita (lado do carro).
 */
class Botao(ctx: Context, private val ladoEsquerdo: Boolean) : View(ctx) {

    var texto = ""
        set(v) { field = v; invalidate() }
    var apagado = false
        set(v) { field = v; invalidate() }

    private val forma = Path()
    private val pinta = Paint(Paint.ANTI_ALIAS_FLAG)
    private val letra = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFFFFF.toInt()
        typeface = Fontes.forte(ctx)
        textAlign = Paint.Align.CENTER
    }
    private var folga = 0f
    private var inclinacao = 0f

    init { isClickable = true }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        folga = h * 0.03f + 1f
        val r = RectF(folga, folga, w - folga, h - folga)
        inclinacao = r.width() * 0.11f
        forma.reset()
        if (ladoEsquerdo) {
            forma.moveTo(r.left, r.top); forma.lineTo(r.right - inclinacao, r.top)
            forma.lineTo(r.right, r.bottom); forma.lineTo(r.left, r.bottom)
        } else {
            forma.moveTo(r.left + inclinacao, r.top); forma.lineTo(r.right, r.top)
            forma.lineTo(r.right, r.bottom); forma.lineTo(r.left, r.bottom)
        }
        forma.close()
        pinta.pathEffect = CornerPathEffect(r.height() * 0.07f)
        letra.textSize = h * 0.26f // MESMO tamanho em todos os botoes
    }

    override fun onDraw(c: Canvas) {
        Vidro.preencher(pinta, folga, height - folga, isPressed)
        c.drawPath(forma, pinta)
        Vidro.borda(pinta, maxOf(1f, height * 0.018f))
        c.drawPath(forma, pinta)

        letra.alpha = if (apagado) 90 else 240
        // Nome comprido termina em "..." (em vez de encolher a letra e ficar desigual)
        val larguraUtil = (width - folga * 2 - inclinacao) * 0.80f
        val mostrado = TextUtils.ellipsize(texto, letra, larguraUtil, TextUtils.TruncateAt.END).toString()
        // Centro visual do trapezio: desconta metade da parte inclinada
        val cx = width / 2f + (if (ladoEsquerdo) -inclinacao else inclinacao) / 4f
        val y = height / 2f - (letra.descent() + letra.ascent()) / 2f
        c.drawText(mostrado, cx, y, letra)
    }

    /** Redesenha ao apertar/soltar (fica mais claro enquanto o dedo esta em cima). */
    override fun drawableStateChanged() {
        super.drawableStateChanged()
        invalidate()
    }
}
