package com.multimidiacivic.launcher.tela

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.view.View
import com.multimidiacivic.launcher.dados.Tema
import com.multimidiacivic.launcher.util.Imagens

/**
 * Papel de parede (garagem) da tela inicial.
 * A imagem e decodificada em segundo plano, ja no tamanho da tela, e
 * desenhada preenchendo tudo (corta as sobras se a proporcao for diferente).
 */
class Fundo(ctx: Context) : View(ctx) {

    private var imagem: Bitmap? = null
    private var pedido = 0
    private var tema: Tema? = null
    private val origem = Rect()
    private val destino = Rect()
    private val pinta = Paint(Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG)

    fun mostrar(t: Tema) {
        tema = t
        carregar()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) = carregar()

    private fun carregar() {
        val t = tema ?: return
        val w = width
        val h = height
        if (w == 0 || h == 0) return
        val meu = ++pedido
        Imagens.emSegundoPlano(
            { Imagens.decodificarRecurso(resources, t.imagem, w, h, comTransparencia = false) },
            { bmp ->
                if (bmp != null && meu == pedido) {
                    imagem?.recycle()
                    imagem = bmp
                    ajustarCorte()
                    invalidate()
                } else bmp?.recycle()
            }
        )
    }

    /** Recorte central da imagem com a mesma proporcao da tela. */
    private fun ajustarCorte() {
        val img = imagem ?: return
        val escala = maxOf(width / img.width.toFloat(), height / img.height.toFloat())
        val cw = (width / escala).toInt()
        val ch = (height / escala).toInt()
        val x = (img.width - cw) / 2
        val y = (img.height - ch) / 2
        origem.set(x, y, x + cw, y + ch)
        destino.set(0, 0, width, height)
    }

    override fun onDraw(c: Canvas) {
        val img = imagem
        if (img == null) c.drawColor(0xFF05070C.toInt())
        else c.drawBitmap(img, origem, destino, pinta)
    }
}
