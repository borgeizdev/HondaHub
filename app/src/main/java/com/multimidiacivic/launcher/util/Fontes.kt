package com.multimidiacivic.launcher.util

import android.content.Context
import android.graphics.Typeface

/**
 * Fontes que vao junto no app em assets/fontes: Exo 2 (moderna, com todos os
 * acentos) no dia a dia e Orbitron (quadrada) so nos titulos grandes.
 * Carregada uma vez so e reaproveitada (cada Typeface ocupa memoria).
 */
object Fontes {
    private var leve: Typeface? = null
    private var forte: Typeface? = null
    private var quadrada: Typeface? = null

    /** Exo 2 Medium: numeros grandes do relogio. */
    fun leve(ctx: Context): Typeface =
        leve ?: carregar(ctx, "fontes/exo2_medium.ttf", Typeface.NORMAL).also { leve = it }

    /** Exo 2 SemiBold: botoes, data e nome da musica. */
    fun forte(ctx: Context): Typeface =
        forte ?: carregar(ctx, "fontes/exo2_semibold.ttf", Typeface.BOLD).also { forte = it }

    /** Orbitron Black: letras quadradas e encorpadas, titulos de impacto (boas-vindas). */
    fun quadrada(ctx: Context): Typeface =
        quadrada ?: carregar(ctx, "fontes/orbitron_black.ttf", Typeface.BOLD).also { quadrada = it }

    private fun carregar(ctx: Context, arquivo: String, reserva: Int): Typeface = try {
        Typeface.createFromAsset(ctx.assets, arquivo)
    } catch (e: Exception) {
        Typeface.create(Typeface.SANS_SERIF, reserva)
    }
}
