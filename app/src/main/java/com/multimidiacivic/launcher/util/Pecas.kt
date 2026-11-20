package com.multimidiacivic.launcher.util

import android.content.ComponentName
import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.BaseAdapter
import android.widget.GridView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.multimidiacivic.launcher.dados.App
import com.multimidiacivic.launcher.tela.Icone

/**
 * Pecas de interface dos paineis em tela cheia (lapis e gaveta de apps),
 * para os dois terem a mesma cara. Medidas na escala do modelo (altura 681):
 * [u] = pixels reais por unidade do modelo.
 */
class Pecas(val ctx: Context) {

    var u = 1f

    fun d(v: Float) = (v * u).toInt()
    fun f(v: Float) = v * u

    fun texto(t: String, tamanho: Float, cor: Int, forte: Boolean = true) = TextView(ctx).apply {
        text = t
        setTextColor(cor)
        setTextSize(TypedValue.COMPLEX_UNIT_PX, tamanho * u)
        typeface = if (forte) Fontes.forte(ctx) else Fontes.leve(ctx)
        setSingleLine(true)
        ellipsize = TextUtils.TruncateAt.END
        includeFontPadding = false
    }

    fun cabecalho(titulo: String, sub: String) = LinearLayout(ctx).apply {
        orientation = LinearLayout.VERTICAL
        addView(texto(titulo, 32f, 0xFFFFFFFF.toInt()))
        addView(texto(sub, 18f, 0x8CFFFFFF.toInt(), forte = false).apply { setPadding(0, d(6f), 0, 0) })
    }

    /** Botao com icone e texto (ex.: "Fechar", "Automatico"). */
    fun botaoTexto(tipo: Icone.Tipo, t: String, acao: () -> Unit): View {
        val b = LinearLayout(ctx).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(d(10f), d(8f), d(18f), d(8f))
            background = estados(0x14FFFFFF, 0x2EFFFFFF, 0x26FFFFFF, f(12f))
            isClickable = true
            setOnClickListener { acao() }
        }
        b.addView(Icone(ctx, tipo).apply { isClickable = false }, LinearLayout.LayoutParams(d(36f), d(36f)).apply { rightMargin = d(6f) })
        b.addView(texto(t, 18f, 0xFFFFFFFF.toInt()))
        return b
    }

    fun rolagem(v: View) = ScrollView(ctx).apply {
        isVerticalScrollBarEnabled = false
        isFillViewport = true
        overScrollMode = View.OVER_SCROLL_NEVER
        addView(v)
    }

    fun caixa(cor: Int, borda: Int, raio: Float, espessura: Int = maxOf(1, d(1.2f))) = GradientDrawable().apply {
        setColor(cor)
        cornerRadius = raio
        if (borda != 0) setStroke(espessura, borda)
    }

    fun estados(normal: Int, apertado: Int, borda: Int, raio: Float, espessura: Int = maxOf(1, d(1.2f))) =
        StateListDrawable().apply {
            addState(intArrayOf(android.R.attr.state_pressed), caixa(apertado, borda, raio, espessura))
            addState(intArrayOf(), caixa(normal, borda, raio, espessura))
        }

    /** Grade de apps reciclavel (so as celulas visiveis existem). */
    fun gradeApps(colunas: Int, adaptador: GradeApps, aoEscolher: (App) -> Unit) = GridView(ctx).apply {
        numColumns = colunas
        stretchMode = GridView.STRETCH_COLUMN_WIDTH
        horizontalSpacing = d(10f)
        verticalSpacing = d(10f)
        setPadding(d(5f), d(5f), d(5f), d(5f))
        clipToPadding = false
        selector = ColorDrawable(0)
        cacheColorHint = 0
        isVerticalScrollBarEnabled = false
        overScrollMode = View.OVER_SCROLL_NEVER
        adapter = adaptador
        setOnItemClickListener { _, _, pos, _ -> adaptador.lista.getOrNull(pos)?.let(aoEscolher) }
    }
}

/**
 * Conteudo da grade de apps: icone + nome, celulas reaproveitadas ao rolar.
 * [marcado] (opcional) ganha contorno na cor [destaque].
 */
class GradeApps(private val p: Pecas, private val alturaCelula: Float) : BaseAdapter() {
    var lista: List<App> = emptyList()
    var marcado: ComponentName? = null
    var destaque = 0xFFFFFFFF.toInt()

    override fun getCount() = lista.size
    override fun getItem(pos: Int) = lista[pos]
    override fun getItemId(pos: Int) = pos.toLong()

    override fun getView(pos: Int, reuso: View?, pai: ViewGroup?): View {
        val cel = (reuso?.tag as? Celula) ?: Celula()
        cel.mostrar(lista[pos], lista[pos].componente == marcado)
        return cel.raiz
    }

    private inner class Celula {
        private val icone = ImageView(p.ctx)
        private val nome = p.texto("", 16f, 0xCCFFFFFF.toInt()).apply { gravity = Gravity.CENTER; setPadding(0, p.d(10f), 0, 0) }
        private val fundoNormal = p.estados(0x99141820.toInt(), 0xE6222833.toInt(), 0x14FFFFFF, p.f(14f))
        private var fundoMarcado: Drawable? = null
        private var corMarcado = 0
        val raiz = LinearLayout(p.ctx).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(p.d(8f), p.d(10f), p.d(8f), p.d(8f))
            layoutParams = AbsListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, p.d(alturaCelula))
            addView(icone, LinearLayout.LayoutParams(p.d(56f), p.d(56f)))
            addView(nome, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            tag = this@Celula
        }

        fun mostrar(a: App, m: Boolean) {
            icone.setImageBitmap(Icones.de(a.componente))
            nome.text = a.nome
            nome.setTextColor(if (m) 0xFFFFFFFF.toInt() else 0xCCFFFFFF.toInt())
            raiz.background = if (m) marcadoAtual() else fundoNormal
        }

        private fun marcadoAtual(): Drawable {
            if (fundoMarcado == null || corMarcado != destaque) {
                fundoMarcado = p.estados(0xE6222833.toInt(), 0xFF2B3240.toInt(), destaque, p.f(14f), p.d(2.5f))
                corMarcado = destaque
            }
            return fundoMarcado!!
        }
    }
}
