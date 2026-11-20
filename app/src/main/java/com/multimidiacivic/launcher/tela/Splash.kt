package com.multimidiacivic.launcher.tela

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.graphics.Shader
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import com.multimidiacivic.launcher.util.Fontes
import com.multimidiacivic.launcher.util.Imagens

/**
 * Tela de boas-vindas que cobre a tela inicial enquanto ela carrega
 * (papel de parede, carro e apps). Desenhada direto no Canvas: aparece
 * no primeiro quadro, sem esperar nada.
 *
 * Logo GRANDE no meio: res/drawable-nodpi/logo.png (PNG com fundo transparente,
 * ex.: o simbolo da Honda). Ganha acabamento cromado ([CROMADO]) e um reflexo
 * de luz que passa uma vez por cima, como metal polido. Sem o arquivo aparece
 * um emblema simples "CIVIC".
 *
 * [sair] faz o fade e se remove da tela.
 */
class Splash(ctx: Context) : View(ctx) {

    companion object {
        /** true = pinta o logo de prata (qualquer cor vira cromado); false = cores originais. */
        const val CROMADO = false
        /** Altura do logo, em unidades do modelo (a tela tem 681). */
        const val ALTURA_LOGO = 300f
    }

    private var logo: Bitmap? = null
    private var entrada = 0f // 0..1, logo e textos aparecendo
    private var reflexo = -1f // 0..1, faixa de luz passando (fora disso nao desenha)

    private val pLogo = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val pCromo = Paint(Paint.ANTI_ALIAS_FLAG).apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN) }
    private val pReflexo = Paint(Paint.ANTI_ALIAS_FLAG).apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP) }
    private val pAro = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val pMarca = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFE6EAF2.toInt(); textAlign = Paint.Align.CENTER; typeface = Fontes.forte(ctx)
    }
    // Textos alinhados a esquerda: [espacado] desenha letra por letra com espaco extra
    private val pTitulo = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFFFFF.toInt(); typeface = Fontes.quadrada(ctx)
    }
    private val pSub = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFFFFF.toInt(); typeface = Fontes.forte(ctx)
    }
    private val area = RectF()
    private var cromoAlt = 0f
    private var saindo = false

    init {
        setBackgroundColor(0xFF05070C.toInt())
        isClickable = true // segura o toque enquanto carrega

        val id = resources.getIdentifier("logo", "drawable", ctx.packageName)
        if (id != 0) Imagens.emSegundoPlano(
            { Imagens.decodificarRecurso(resources, id, 1200, 1200, comTransparencia = true) },
            { bmp -> logo = bmp; invalidate() }
        )

        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1000
            interpolator = DecelerateInterpolator(1.6f)
            addUpdateListener { entrada = it.animatedValue as Float; invalidate() }
            start()
        }
        ValueAnimator.ofFloat(0f, 1f).apply {
            startDelay = 650
            duration = 1100
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { reflexo = it.animatedValue as Float; invalidate() }
            start()
        }
    }

    /** Some com um fade curto e sai da tela. */
    fun sair() {
        if (saindo) return
        saindo = true
        animate().alpha(0f).setDuration(450).withEndAction {
            (parent as? ViewGroup)?.removeView(this)
            logo?.recycle()
            logo = null
        }.start()
    }

    override fun onDraw(c: Canvas) {
        val s = height / Tela.ALT_REF
        val cx = width / 2f
        val cy = height / 2f - 55f * s

        // Logo: aparece crescendo de leve (94% -> 100%)
        val aLogo = (entrada / 0.75f).coerceIn(0f, 1f)
        val escala = 0.94f + 0.06f * aLogo
        val alt = ALTURA_LOGO * s * escala
        val img = logo
        if (img != null) {
            val larg = alt * img.width / img.height
            area.set(cx - larg / 2f, cy - alt / 2f, cx + larg / 2f, cy + alt / 2f)
            desenharLogo(c, img, (aLogo * 255).toInt())
        } else {
            // Emblema simples: aro fino + "CIVIC"
            val r = alt * 0.5f
            pAro.strokeWidth = maxOf(2f, 4f * s)
            pAro.color = 0xFFE6EAF2.toInt()
            pAro.alpha = (aLogo * 230).toInt()
            c.drawCircle(cx, cy, r, pAro)
            pMarca.textSize = 64f * s * escala
            pMarca.alpha = (aLogo * 255).toInt()
            c.drawText("CIVIC", cx, cy - (pMarca.descent() + pMarca.ascent()) / 2f, pMarca)
        }

        // Textos embaixo do logo (entram logo depois)
        val aTexto = ((entrada - 0.35f) / 0.65f).coerceIn(0f, 1f)
        val base = cy + ALTURA_LOGO * s / 2f
        pTitulo.textSize = 52f * s
        pTitulo.alpha = (aTexto * 255).toInt()
        espacado(c, "BEM-VINDO A BORDO", cx, base + 90f * s, pTitulo, 0.05f)
        pSub.textSize = 22f * s
        pSub.alpha = (aTexto * 165).toInt()
        espacado(c, "Você está no seu Honda Civic", cx, base + 130f * s, pSub, 0.04f)
    }

    /** Texto centralizado em [cx] com espaco extra entre as letras ([extra] x tamanho da fonte). */
    private fun espacado(c: Canvas, t: String, cx: Float, y: Float, p: Paint, extra: Float) {
        val folga = p.textSize * extra
        var largura = folga * (t.length - 1)
        for (ch in t) largura += p.measureText(ch.toString())
        var x = cx - largura / 2f
        for (ch in t) {
            val l = ch.toString()
            c.drawText(l, x, y, p)
            x += p.measureText(l) + folga
        }
    }

    /** Logo em [area]: cromado (prata escovado) + faixa de luz passando. */
    @Suppress("DEPRECATION")
    private fun desenharLogo(c: Canvas, img: Bitmap, alfa: Int) {
        pLogo.alpha = alfa
        if (!CROMADO && reflexo !in 0f..1f) { c.drawBitmap(img, null, area, pLogo); return }

        // Camada separada: o que vem depois so pinta onde o logo existe
        val camada = c.saveLayer(area, null, Canvas.ALL_SAVE_FLAG)
        c.drawBitmap(img, null, area, pLogo)

        if (CROMADO) {
            if (cromoAlt != area.height()) {
                cromoAlt = area.height()
                pCromo.shader = LinearGradient(0f, area.top, 0f, area.bottom,
                    intArrayOf(0xFFFFFFFF.toInt(), 0xFFD9DDE4.toInt(), 0xFF9AA2AF.toInt(), 0xFFC4CAD4.toInt(), 0xFFEEF0F4.toInt()),
                    floatArrayOf(0f, 0.38f, 0.52f, 0.70f, 1f), Shader.TileMode.CLAMP)
            }
            pCromo.alpha = alfa
            c.drawRect(area, pCromo)
        }

        // Reflexo: faixa diagonal clara cruzando da esquerda para a direita
        if (reflexo in 0f..1f) {
            val larg = area.width()
            val x = area.left - larg * 0.5f + reflexo * larg * 2f
            pReflexo.shader = LinearGradient(x - larg * 0.22f, area.top, x + larg * 0.22f, area.bottom,
                intArrayOf(0x00FFFFFF, 0x8CFFFFFF.toInt(), 0x00FFFFFF), floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP)
            c.drawRect(area, pReflexo)
        }
        c.restoreToCount(camada)
    }
}
