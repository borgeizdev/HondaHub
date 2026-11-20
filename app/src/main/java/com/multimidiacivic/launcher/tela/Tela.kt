package com.multimidiacivic.launcher.tela

import android.content.Context
import android.graphics.RectF
import android.view.View
import android.view.ViewGroup

/**
 * Layout da tela inteira.
 *
 * Cada pedaco (relogio, botoes, carro, player...) e posicionado com as
 * coordenadas do MODELO da tela (1358 x 681 px). Na hora de mostrar, tudo e
 * esticado proporcionalmente para o tamanho real da tela da central.
 */
class Tela(ctx: Context) : ViewGroup(ctx) {

    companion object {
        const val LARG_REF = 1358f
        const val ALT_REF = 681f
    }

    private val posicoes = HashMap<View, RectF>()

    /** Adiciona [v] ocupando o retangulo (esq, topo, dir, base) do modelo. */
    fun colocar(v: View, esq: Float, topo: Float, dir: Float, base: Float, folga: Float = 0f) {
        posicoes[v] = RectF(esq - folga, topo - folga, dir + folga, base + folga)
        addView(v)
    }

    override fun onMeasure(wSpec: Int, hSpec: Int) {
        val w = MeasureSpec.getSize(wSpec)
        val h = MeasureSpec.getSize(hSpec)
        setMeasuredDimension(w, h)
        val sx = w / LARG_REF
        val sy = h / ALT_REF
        for (i in 0 until childCount) {
            val v = getChildAt(i)
            val r = posicoes[v] ?: continue
            v.measure(
                MeasureSpec.makeMeasureSpec((r.width() * sx).toInt(), MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec((r.height() * sy).toInt(), MeasureSpec.EXACTLY)
            )
        }
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val sx = width / LARG_REF
        val sy = height / ALT_REF
        for (i in 0 until childCount) {
            val v = getChildAt(i)
            val p = posicoes[v] ?: continue
            val x = (p.left * sx).toInt()
            val y = (p.top * sy).toInt()
            v.layout(x, y, x + v.measuredWidth, y + v.measuredHeight)
        }
    }
}
