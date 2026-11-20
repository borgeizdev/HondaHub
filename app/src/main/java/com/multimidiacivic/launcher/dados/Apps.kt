package com.multimidiacivic.launcher.dados

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.graphics.drawable.Drawable

/** Um botao de app: nome, icone e qual tela abrir. */
class App(val nome: String, val icone: Drawable, val componente: ComponentName, val doUsuario: Boolean)

/**
 * Apps que viram botao na tela inicial.
 *  - O usuario pode escolher o app de cada botao pelo lapis (fica salvo).
 *  - Botao sem escolha = automatico: a Play Store primeiro e depois os apps
 *    que o usuario instalou (Spotify, Waze...), em ordem alfabetica.
 */
object Apps {

    private const val PLAY_STORE = "com.android.vending"
    const val QUANTIDADE = 6

    /** Todos os apps com icone (inclusive os de sistema), em ordem alfabetica. */
    fun todos(ctx: Context): List<App> {
        val pm = ctx.packageManager
        val comIcone = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return pm.queryIntentActivities(comIcone, 0).mapNotNull { r ->
            val info = r.activityInfo
            val pacote = info.packageName
            if (pacote == ctx.packageName) return@mapNotNull null
            val doUsuario = info.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM == 0
            App(r.loadLabel(pm).toString(), r.loadIcon(pm), ComponentName(pacote, info.name), doUsuario)
        }.sortedBy { it.nome.lowercase() }
    }

    /** O app de cada um dos 6 botoes (null = botao vazio). */
    fun dosBotoes(ctx: Context, todos: List<App>): List<App?> {
        val porComponente = todos.associateBy { it.componente }
        val escolhidos = (0 until QUANTIDADE).map { i -> Ajustes.appDoBotao(ctx, i)?.let { porComponente[it] } }
        val usados = escolhidos.filterNotNull().map { it.componente }.toSet()
        val automaticos = todos
            .filter { (it.doUsuario || it.componente.packageName == PLAY_STORE) && it.componente !in usados }
            .sortedBy { it.componente.packageName != PLAY_STORE }
            .iterator()
        return escolhidos.map { it ?: if (automaticos.hasNext()) automaticos.next() else null }
    }

    fun abrir(ctx: Context, app: App) {
        val i = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .setComponent(app.componente)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        try { ctx.startActivity(i) } catch (e: Exception) { /* app removido nesse meio tempo */ }
    }
}
