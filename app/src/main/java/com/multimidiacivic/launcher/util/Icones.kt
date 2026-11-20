package com.multimidiacivic.launcher.util

import android.content.ComponentName
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import com.multimidiacivic.launcher.dados.App

/**
 * Icones dos apps ja desenhados como imagem (uma vez so, em segundo plano).
 * Desenhar o Drawable original a cada quadro custa caro numa central fraca;
 * a imagem pronta e so copiada para a tela. Usado pelo lapis e pela gaveta.
 */
object Icones {

    private val prontos = HashMap<ComponentName, Bitmap>()
    private val pedidos = HashSet<ComponentName>()
    private val ouvintes = ArrayList<() -> Unit>()
    private var tamanho = 0

    fun de(c: ComponentName): Bitmap? = prontos[c]

    /** [f] e chamado (na thread da tela) sempre que chegam icones novos. */
    fun ouvir(f: () -> Unit) { ouvintes.add(f) }

    /** Gera, em [t] x [t] px, os icones da [lista] que ainda nao existem. */
    fun gerar(lista: List<App>, t: Int) {
        if (t != tamanho) { prontos.clear(); pedidos.clear(); tamanho = t }
        val faltam = lista.filter { it.componente !in prontos && it.componente !in pedidos }
        if (faltam.isEmpty()) return
        faltam.forEach { pedidos.add(it.componente) }
        // Mesma thread unica das imagens: nunca desenha o mesmo Drawable em duas threads
        Imagens.emSegundoPlano(
            { faltam.mapNotNull { a -> paraImagem(a.icone, t)?.let { a.componente to it } }.toMap() },
            { novos ->
                if (t != tamanho) return@emSegundoPlano
                prontos.putAll(novos)
                ouvintes.forEach { it() }
            }
        )
    }

    private fun paraImagem(d: Drawable, t: Int): Bitmap? = try {
        val bmp = Bitmap.createBitmap(t, t, Bitmap.Config.ARGB_8888)
        val iw = d.intrinsicWidth
        val ih = d.intrinsicHeight
        val esc = if (iw > 0 && ih > 0) minOf(t / iw.toFloat(), t / ih.toFloat()) else 1f
        val w = if (iw > 0) (iw * esc).toInt() else t
        val h = if (ih > 0) (ih * esc).toInt() else t
        val antes = d.copyBounds()
        d.setBounds((t - w) / 2, (t - h) / 2, (t + w) / 2, (t + h) / 2)
        d.draw(Canvas(bmp))
        d.bounds = antes
        bmp
    } catch (e: Exception) {
        null // icone com defeito: a celula fica sem imagem
    }
}
