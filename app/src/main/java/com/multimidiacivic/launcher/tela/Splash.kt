package com.multimidiacivic.launcher.tela

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import com.multimidiacivic.launcher.util.Fontes
import com.multimidiacivic.launcher.util.Imagens

/**
 * Tela de boas-vindas que cobre a tela inicial enquanto ela carrega
 * (papel de parede, carro e apps). Desenhada direto no Canvas: aparece
 * no primeiro quadro, sem esperar nada.
 *
 * Logo no meio: se existir res/drawable-nodpi/logo.png (ex.: o logo da Honda),
 * ele e usado; senao aparece um emblema simples com "CIVIC".
 *
 * [sair] faz o fade e se remove da tela.
 */
class Splash(ctx: Context) : View(ctx) {

    private var logo: Bitmap? = null
    private var entrada = 0f // 0..1, animacao de entrada

    private val pLogo = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val pAro = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; color = 0xFFE6EAF2.toInt() }
    private val pMarca = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFE6EAF2.toInt(); textAlign = Paint.Align.CENTER; typeface = Fontes.forte(ctx)
    }
    private val pTitulo = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFFFFF.toInt(); textAlign = Paint.Align.CENTER; typeface = Fontes.forte(ctx)
    }
    private val pSub = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x8CFFFFFF.toInt(); textAlign = Paint.Align.CENTER; typeface = Fontes.leve(ctx)
    }
    private val area = RectF()
    private var saindo = false

    init {
        setBackgroundColor(0xFF05070C.toInt())
        isClickable = true // segura o toque enquanto carrega

        val id = resources.getIdentifier("logo", "drawable", ctx.packageName)
        if (id != 0) Imagens.emSegundoPlano(
            { Imagens.decodificarRecurso(resources, id, 600, 600, comTransparencia = true) },
            { bmp -> logo = bmp; invalidate() }
        )

        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 900
            interpolator = DecelerateInterpolator()
            addUpdateListener { entrada = it.animatedValue as Float; invalidate() }
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
        val cy = height / 2f - 40f * s

        // Logo (sobe um pouco e aparece)
        val aLogo = (entrada / 0.7f).coerceIn(0f, 1f)
        val sobe = (1f - aLogo) * 18f * s
        val img = logo
        if (img != null) {
            val alt = 150f * s
            val larg = alt * img.width / img.height
            area.set(cx - larg / 2f, cy - alt / 2f + sobe, cx + larg / 2f, cy + alt / 2f + sobe)
            pLogo.alpha = (aLogo * 255).toInt()
            c.drawBitmap(img, null, area, pLogo)
        } else {
            // Emblema simples: aro fino + "CIVIC"
            val r = 78f * s
            pAro.strokeWidth = maxOf(1.5f, 3f * s)
            pAro.alpha = (aLogo * 230).toInt()
            c.drawCircle(cx, cy + sobe, r, pAro)
            pMarca.textSize = 34f * s
            pMarca.alpha = (aLogo * 255).toInt()
            c.drawText("CIVIC", cx, cy + sobe - (pMarca.descent() + pMarca.ascent()) / 2f, pMarca)
        }

        // Textos (entram logo depois do logo)
        val aTexto = ((entrada - 0.3f) / 0.7f).coerceIn(0f, 1f)
        pTitulo.textSize = 40f * s
        pTitulo.alpha = (aTexto * 255).toInt()
        c.drawText("Bem-vindo a bordo", cx, cy + 150f * s, pTitulo)
        pSub.textSize = 20f * s
        pSub.alpha = (aTexto * 140).toInt()
        c.drawText("Você está no seu Honda Civic", cx, cy + 186f * s, pSub)
    }
}
