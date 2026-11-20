package com.multimidiacivic.launcher.tela

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.view.View

/** Imagem do carro (centralizada, mantendo a proporcao) com uma sombra leve embaixo. */
class Carro(ctx: Context) : View(ctx) {

    private var imagem: Bitmap? = null
    private val destino = RectF()
    private val pinta = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
    private val sombra = Paint(Paint.ANTI_ALIAS_FLAG)

    fun mostrar(bmp: Bitmap) {
        imagem = bmp
        preparar()
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) = preparar()

    private fun preparar() {
        val img = imagem ?: return
        if (width == 0 || height == 0) return
        val escala = minOf(width / img.width.toFloat(), height / img.height.toFloat())
        val w = img.width * escala
        val h = img.height * escala
        val x = (width - w) / 2f
        destino.set(x, height - h, x + w, height.toFloat())

        // Sombra suave embaixo das rodas
        sombra.shader = RadialGradient(0f, 0f, 1f,
            intArrayOf(0x90000814.toInt(), 0x00000000), floatArrayOf(0.2f, 1f), Shader.TileMode.CLAMP
        ).apply { setLocalMatrix(Matrix().apply { setScale(w * 0.52f, h * 0.10f); postTranslate(width / 2f, height - h * 0.06f) }) }
    }

    override fun onDraw(c: Canvas) {
        val img = imagem ?: return
        c.drawRect(0f, 0f, width.toFloat(), height.toFloat(), sombra)
        c.drawBitmap(img, null, destino, pinta)
    }
}
