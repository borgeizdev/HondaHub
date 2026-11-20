package com.multimidiacivic.launcher

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.view.View
import kotlin.math.cos
import kotlin.math.sin

/**
 * Icones brancos de linha (engrenagem, wi-fi, bluetooth, grade, lapis),
 * desenhados no codigo: nitidos em qualquer tela e sem arquivos de imagem.
 */
class Icone(ctx: Context, private val tipo: Tipo) : View(ctx) {

    enum class Tipo { CONFIG, WIFI, BLUETOOTH, APPS, EDITAR, IMAGEM, FECHAR, AUTO }

    companion object { const val TAMANHO = 0.58f }

    private val linha = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFFFFF.toInt()
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val cheio = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt() }
    private val p = Path()

    init { isClickable = true }

    override fun drawableStateChanged() {
        super.drawableStateChanged()
        alpha = if (isPressed) 0.5f else 1f
    }

    override fun onDraw(c: Canvas) {
        // O icone ocupa so [TAMANHO] da view: o resto e area de toque (mais facil de acertar)
        val s = minOf(width, height) * TAMANHO
        val cx = width / 2f
        val cy = height / 2f
        linha.strokeWidth = s * 0.075f
        p.reset()
        when (tipo) {
            Tipo.CONFIG -> {
                val dentes = 8
                val r1 = s * 0.30f
                val r2 = s * 0.41f
                for (k in 0 until dentes) {
                    val a = Math.toRadians(k * 360.0 / dentes)
                    val pontos = listOf(r1 to -15.0, r2 to -9.0, r2 to 9.0, r1 to 15.0)
                    pontos.forEachIndexed { i, (r, d) ->
                        val ang = a + Math.toRadians(d)
                        val x = cx + r * cos(ang).toFloat()
                        val y = cy + r * sin(ang).toFloat()
                        if (k == 0 && i == 0) p.moveTo(x, y) else p.lineTo(x, y)
                    }
                }
                p.close()
                c.drawPath(p, linha)
                c.drawCircle(cx, cy, s * 0.13f, linha)
            }
            Tipo.WIFI -> {
                // base dos arcos abaixo do centro, para o desenho todo (arco maior + ponto) ficar centralizado
                val base = cy + s * 0.2575f
                for (r in floatArrayOf(0.19f, 0.385f, 0.58f)) {
                    val rr = s * r
                    c.drawArc(RectF(cx - rr, base - rr, cx + rr, base + rr), 225f, 90f, false, linha)
                }
                c.drawCircle(cx, base, s * 0.065f, cheio)
            }
            Tipo.BLUETOOTH -> {
                val a = s * 0.20f
                val b = s * 0.41f
                p.moveTo(cx - a, cy - a)
                p.lineTo(cx + a, cy + a)
                p.lineTo(cx, cy + b)
                p.lineTo(cx, cy - b)
                p.lineTo(cx + a, cy - a)
                p.lineTo(cx - a, cy + a)
                c.drawPath(p, linha)
            }
            Tipo.APPS -> {
                val q = s * 0.35f
                val v = s * 0.11f
                val ini = cx - q - v / 2
                val topo = cy - q - v / 2
                for (i in 0..1) for (j in 0..1) {
                    val x = ini + i * (q + v)
                    val y = topo + j * (q + v)
                    c.drawRoundRect(RectF(x, y, x + q, y + q), s * 0.05f, s * 0.05f, linha)
                }
            }
            Tipo.EDITAR -> {
                // lapis deitado e girado: ponta para baixo/esquerda
                c.save()
                c.rotate(135f, cx, cy)
                val m = s * 0.11f
                p.moveTo(cx - s * 0.42f, cy - m)
                p.lineTo(cx + s * 0.22f, cy - m)
                p.lineTo(cx + s * 0.44f, cy)
                p.lineTo(cx + s * 0.22f, cy + m)
                p.lineTo(cx - s * 0.42f, cy + m)
                p.close()
                c.drawPath(p, linha)
                c.drawLine(cx - s * 0.26f, cy - m, cx - s * 0.26f, cy + m, linha)
                c.restore()
            }
            Tipo.IMAGEM -> {
                // moldura + montanha + sol
                val q = RectF(cx - s * 0.42f, cy - s * 0.32f, cx + s * 0.42f, cy + s * 0.32f)
                c.drawRoundRect(q, s * 0.06f, s * 0.06f, linha)
                p.moveTo(q.left + s * 0.04f, q.bottom - s * 0.06f)
                p.lineTo(cx - s * 0.12f, cy - s * 0.02f)
                p.lineTo(cx + s * 0.06f, cy + s * 0.14f)
                p.lineTo(cx + s * 0.18f, cy + s * 0.04f)
                p.lineTo(q.right - s * 0.04f, q.bottom - s * 0.06f)
                c.drawPath(p, linha)
                c.drawCircle(cx + s * 0.18f, cy - s * 0.14f, s * 0.06f, cheio)
            }
            Tipo.FECHAR -> {
                val a = s * 0.28f
                c.drawLine(cx - a, cy - a, cx + a, cy + a, linha)
                c.drawLine(cx + a, cy - a, cx - a, cy + a, linha)
            }
            Tipo.AUTO -> {
                // seta circular (volta ao automatico)
                val r = s * 0.32f
                c.drawArc(RectF(cx - r, cy - r, cx + r, cy + r), -60f, 290f, false, linha)
                val ang = Math.toRadians(-60.0)
                val px = cx + r * cos(ang).toFloat()
                val py = cy + r * sin(ang).toFloat()
                val ponta = s * 0.15f
                p.moveTo(px - ponta, py - ponta * 0.15f)
                p.lineTo(px, py)
                p.lineTo(px + ponta * 0.2f, py - ponta)
                c.drawPath(p, linha)
            }
        }
    }
}
