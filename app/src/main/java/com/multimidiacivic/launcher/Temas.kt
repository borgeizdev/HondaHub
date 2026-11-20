package com.multimidiacivic.launcher

import android.content.ComponentName
import android.content.Context

/**
 * Um papel de parede "garagem" (imagem em res/drawable-nodpi, gerada por
 * ferramentas/GerarFundos.java) + a cor da luz dele, que tambem tinge o
 * vidro dos botoes e os ":" do relogio para combinar com o fundo.
 */
class Tema(val id: String, val nome: String, val imagem: Int, val destaque: Int)

object Temas {

    val todos = listOf(
        Tema("azul", "Azul", R.drawable.fundo_azul, 0xFF3C82FF.toInt()),
        Tema("vermelho", "Vermelho", R.drawable.fundo_vermelho, 0xFFFF2D23.toInt()),
        Tema("ambar", "Âmbar", R.drawable.fundo_ambar, 0xFFFF8028.toInt()),
        Tema("laranja", "Laranja", R.drawable.fundo_laranja, 0xFFFF5F14.toInt()),
        Tema("amarelo", "Amarelo", R.drawable.fundo_amarelo, 0xFFFFC832.toInt()),
        Tema("verde", "Verde", R.drawable.fundo_verde, 0xFF32E66E.toInt()),
        Tema("ciano", "Ciano", R.drawable.fundo_ciano, 0xFF28D2EB.toInt()),
        Tema("roxo", "Roxo", R.drawable.fundo_roxo, 0xFF9650FF.toInt()),
        Tema("rosa", "Rosa", R.drawable.fundo_rosa, 0xFFFF46AA.toInt()),
        Tema("vinho", "Vinho", R.drawable.fundo_vinho, 0xFFC81E46.toInt()),
        Tema("branco", "Branco", R.drawable.fundo_branco, 0xFFE6E8EE.toInt()),
        Tema("cinza", "Cinza", R.drawable.fundo_cinza, 0xFFBEC8D7.toInt()),
        Tema("grafite", "Grafite", R.drawable.fundo_grafite, 0xFF96AFD2.toInt()),
        Tema("preto", "Preto", R.drawable.fundo_preto, 0xFFD2D2D7.toInt()),
    )

    fun porId(id: String?): Tema = todos.firstOrNull { it.id == id } ?: todos[0]
}

/** O que o usuario escolheu no lapis (fica salvo no aparelho). */
object Ajustes {

    private fun prefs(ctx: Context) = ctx.getSharedPreferences("ajustes", Context.MODE_PRIVATE)

    fun tema(ctx: Context): Tema = Temas.porId(prefs(ctx).getString("fundo", null))

    fun salvarTema(ctx: Context, t: Tema) = prefs(ctx).edit().putString("fundo", t.id).apply()

    /** App escolhido para o botao [i] (0..5), ou null = automatico. */
    fun appDoBotao(ctx: Context, i: Int): ComponentName? =
        prefs(ctx).getString("botao_$i", null)?.let { ComponentName.unflattenFromString(it) }

    fun salvarAppDoBotao(ctx: Context, i: Int, c: ComponentName?) {
        val e = prefs(ctx).edit()
        if (c == null) e.remove("botao_$i") else e.putString("botao_$i", c.flattenToString())
        e.apply()
    }
}
