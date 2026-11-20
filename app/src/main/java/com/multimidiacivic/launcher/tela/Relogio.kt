package com.multimidiacivic.launcher.tela

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.view.View
import com.multimidiacivic.launcher.util.Fontes
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Relogio do topo:
 *
 *        14:27        <- numeros grandes, ":" azul
 *    SEX · 20 NOV     <- data pequena e espacada
 */
class Relogio(ctx: Context) : View(ctx) {

    private val ptBR = Locale("pt", "BR")
    private val fmtHora = SimpleDateFormat("HH", Locale.US)
    private val fmtMin = SimpleDateFormat("mm", Locale.US)
    private val fmtDia = SimpleDateFormat("EEE", ptBR)
    private val fmtMes = SimpleDateFormat("MMM", ptBR)

    private var horas = ""
    private var minutos = ""
    private var data = ""

    private val numero = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFF5F8FF.toInt()
        typeface = Fontes.leve(ctx)
    }
    private val pontos = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF5FA8FF.toInt()
        typeface = Fontes.leve(ctx)
    }
    private val legenda = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xB3DCE6F5.toInt()
        typeface = Fontes.forte(ctx)
        textAlign = Paint.Align.CENTER
    }

    /** Cor dos ":" acompanha a luz do papel de parede. */
    fun tingir(destaque: Int) {
        pontos.color = Vidro.clarear(destaque, 0.25f, 0xFF)
        invalidate()
    }

    /** Atualiza hora e data (chamado tambem quando muda a hora/fuso do sistema). */
    fun atualizar() {
        val agora = Date()
        val dia = fmtDia.format(agora).replace(".", "").uppercase(ptBR)
        val mes = fmtMes.format(agora).replace(".", "").uppercase(ptBR)
        val dataNova = espacar("$dia  ·  ${Calendar.getInstance().get(Calendar.DAY_OF_MONTH)} $mes")
        val h = fmtHora.format(agora)
        val m = fmtMin.format(agora)
        if (h != horas || m != minutos || dataNova != data) {
            horas = h; minutos = m; data = dataNova
            invalidate()
        }
    }

    /** "SEX" -> "S E X" com espaco fino (letterSpacing so existe no Android 5+). */
    private fun espacar(t: String) = t.toCharArray().joinToString(" ")

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        numero.textSize = h * 0.74f
        pontos.textSize = h * 0.74f
        legenda.textSize = h * 0.15f
    }

    override fun onDraw(c: Canvas) {
        // Hora: "HH" + ":" + "mm", centralizado pelo conjunto
        val wH = numero.measureText(horas)
        val wP = pontos.measureText(":")
        val wM = numero.measureText(minutos)
        val folgaP = height * 0.02f
        var x = (width - (wH + wP + wM + folgaP * 2)) / 2f
        val yHora = height * 0.62f
        c.drawText(horas, x, yHora, numero)
        x += wH + folgaP
        c.drawText(":", x, yHora - height * 0.04f, pontos)
        x += wP + folgaP
        c.drawText(minutos, x, yHora, numero)

        // Data
        c.drawText(data, width / 2f, height * 0.93f, legenda)
    }
}
