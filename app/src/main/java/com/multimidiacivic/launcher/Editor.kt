package com.multimidiacivic.launcher

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Painel aberto pelo lapis (tela cheia, sobre o papel de parede escurecido):
 *
 *   | Personalizar        |  Titulo da aba                               |
 *   |  > Botoes           |  [ previa da tela inicial ]   [ grade de    |
 *   |    Papel de parede  |  [ detalhes             ]     [ apps/fundos ]|
 *   |                     |                                             |
 *   |    Fechar           |                                             |
 *
 * Tudo e medido na escala do modelo da tela (altura 681), igual ao resto.
 */
class Editor(
    private val act: Activity,
    private val todosApps: () -> List<App>,
    private val appsDosBotoes: () -> List<App?>,
    private val aoMudarTema: (Tema) -> Unit,
    private val aoMudarApps: () -> Unit,
) {
    private enum class Aba { BOTOES, FUNDO }

    private var raiz: FrameLayout? = null
    private lateinit var fundoPainel: Imagem
    private lateinit var menu: LinearLayout
    private lateinit var conteudo: FrameLayout
    private var aba = Aba.BOTOES
    private var botao = 0 // botao escolhido na aba BOTOES (0..5)
    private var u = 1f

    // Imagens usadas enquanto o painel esta aberto (liberadas ao fechar)
    private val miniaturas = HashMap<Int, Bitmap>()
    private var fundoGrande: Pair<Int, Bitmap>? = null
    private var carro: Bitmap? = null

    val aberto get() = raiz != null

    private fun d(v: Float) = (v * u).toInt()
    private fun f(v: Float) = v * u
    private val destaque get() = Vidro.clarear(Ajustes.tema(act).destaque, 0.20f, 0xFF)

    // ------------------------------------------------------------------ abrir / fechar

    fun abrir() {
        if (aberto) return
        u = act.resources.displayMetrics.heightPixels / Tela.ALT_REF

        val r = FrameLayout(act).apply { isClickable = true } // nao deixa o toque passar para a tela
        fundoPainel = Imagem(act, 0f).apply { veu = 0xE6070910.toInt() }
        r.addView(fundoPainel)

        val linha = LinearLayout(act)
        menu = LinearLayout(act).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0x59000000)
            setPadding(d(22f), d(34f), d(22f), d(24f))
        }
        linha.addView(menu, LinearLayout.LayoutParams(d(290f), ViewGroup.LayoutParams.MATCH_PARENT))
        linha.addView(View(act).apply { setBackgroundColor(0x14FFFFFF) },
            LinearLayout.LayoutParams(maxOf(1, d(1f)), ViewGroup.LayoutParams.MATCH_PARENT))
        conteudo = FrameLayout(act).apply { setPadding(d(40f), d(34f), d(40f), d(26f)) }
        linha.addView(conteudo, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f))
        r.addView(linha)

        act.addContentView(r, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        raiz = r

        Imagens.emSegundoPlano(
            { Imagens.decodificarRecurso(act.resources, R.drawable.carro, 480, 217, comTransparencia = true) },
            { bmp -> if (aberto && bmp != null) { carro = bmp; desenharTudo() } }
        )
        carregarFundoGrande()
        mostrar(Aba.BOTOES)
    }

    fun fechar() {
        val r = raiz ?: return
        (r.parent as? ViewGroup)?.removeView(r)
        raiz = null
        miniaturas.clear()
        fundoGrande = null
        carro = null
    }

    /** Botao Voltar do Android. */
    fun voltar() = fechar()

    private fun mostrar(a: Aba) {
        aba = a
        montarMenu()
        when (a) {
            Aba.BOTOES -> montarBotoes()
            Aba.FUNDO -> montarFundos()
        }
    }

    /** Remonta a aba atual mantendo a rolagem das grades. */
    private fun atualizar() {
        val rolagens = ArrayList<Int>()
        coletarRolagens(conteudo, rolagens)
        mostrar(aba)
        conteudo.post {
            val novas = ArrayList<ScrollView>()
            achaRolagens(conteudo, novas)
            novas.forEachIndexed { i, s -> rolagens.getOrNull(i)?.let { s.scrollTo(0, it) } }
        }
    }

    private fun coletarRolagens(v: View, saida: ArrayList<Int>) {
        if (v is ScrollView) saida.add(v.scrollY)
        if (v is ViewGroup) for (i in 0 until v.childCount) coletarRolagens(v.getChildAt(i), saida)
    }

    private fun achaRolagens(v: View, saida: ArrayList<ScrollView>) {
        if (v is ScrollView) saida.add(v)
        if (v is ViewGroup) for (i in 0 until v.childCount) achaRolagens(v.getChildAt(i), saida)
    }

    /** Papel de parede atual em tamanho medio (previa e fundo do painel). */
    private fun carregarFundoGrande() {
        val t = Ajustes.tema(act)
        fundoGrande?.let { if (it.first == t.imagem) { fundoPainel.imagem = it.second; return } }
        Imagens.emSegundoPlano(
            { Imagens.decodificarRecurso(act.resources, t.imagem, 800, 400, comTransparencia = false) },
            { bmp ->
                if (aberto && bmp != null && Ajustes.tema(act).imagem == t.imagem) {
                    fundoGrande = t.imagem to bmp
                    fundoPainel.imagem = bmp
                    desenharTudo()
                }
            }
        )
    }

    /** Repassa as imagens carregadas para as previas que estao na tela. */
    private fun desenharTudo() {
        val previas = ArrayList<PreviaTela>()
        fun busca(v: View) {
            if (v is PreviaTela) previas.add(v)
            if (v is ViewGroup) for (i in 0 until v.childCount) busca(v.getChildAt(i))
        }
        busca(conteudo)
        previas.forEach { it.fundo = fundoGrande?.second; it.carro = carro; it.invalidate() }
    }

    // ------------------------------------------------------------------ menu lateral

    private fun montarMenu() {
        menu.removeAllViews()
        menu.addView(texto("Personalizar", 32f, 0xFFFFFFFF.toInt()))
        menu.addView(texto("Deixe a central do seu jeito", 17f, 0x8CFFFFFF.toInt(), forte = false).apply {
            setPadding(0, d(6f), 0, d(30f))
        })
        menu.addView(itemMenu(Icone.Tipo.APPS, "Botões", "Apps dos 6 atalhos", aba == Aba.BOTOES) { mostrar(Aba.BOTOES) })
        menu.addView(itemMenu(Icone.Tipo.IMAGEM, "Papel de parede", "${Temas.todos.size} garagens", aba == Aba.FUNDO) { mostrar(Aba.FUNDO) })
        menu.addView(View(act), LinearLayout.LayoutParams(1, 0, 1f))
        menu.addView(itemMenu(Icone.Tipo.FECHAR, "Fechar", "Voltar para a tela inicial", false) { fechar() })
    }

    private fun itemMenu(tipo: Icone.Tipo, nome: String, sub: String, ativo: Boolean, acao: () -> Unit): View {
        val l = LinearLayout(act).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, d(12f), 0)
            background = if (ativo) estados(0x1FFFFFFF, 0x2EFFFFFF, 0, f(12f)) else estados(0x00000000, 0x1AFFFFFF, 0, f(12f))
            isClickable = true
            setOnClickListener { acao() }
        }
        val barra = View(act).apply {
            background = caixa(if (ativo) destaque else 0x00000000, 0, f(3f))
        }
        l.addView(barra, LinearLayout.LayoutParams(d(5f), d(38f)).apply { rightMargin = d(10f) })
        l.addView(Icone(act, tipo).apply { isClickable = false; alpha = if (ativo) 1f else 0.75f },
            LinearLayout.LayoutParams(d(48f), d(48f)).apply { rightMargin = d(10f) })
        val textos = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL }
        textos.addView(texto(nome, 22f, if (ativo) 0xFFFFFFFF.toInt() else 0xD9FFFFFF.toInt()))
        textos.addView(texto(sub, 15f, 0x80FFFFFF.toInt(), forte = false).apply { setPadding(0, d(3f), 0, 0) })
        l.addView(textos, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        return FrameLayout(act).apply {
            setPadding(0, 0, 0, d(8f))
            addView(l, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, d(80f)))
        }
    }

    // ------------------------------------------------------------------ aba BOTOES

    private fun montarBotoes() {
        val atuais = appsDosBotoes()
        val app = atuais.getOrNull(botao)
        val escolhido = Ajustes.appDoBotao(act, botao)

        val corpo = LinearLayout(act)

        // Esquerda: previa (toque num botao) + detalhes do botao escolhido
        val esq = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL }
        esq.addView(cabecalho("Apps dos botões", "Toque num botão da prévia e escolha o app ao lado"))
        val previa = PreviaTela(act, f(14f)).apply {
            apps = atuais
            selecionado = botao
            fundo = fundoGrande?.second
            carro = this@Editor.carro
            aoTocarBotao = { i -> botao = i; atualizar() }
        }
        esq.addView(previa, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            topMargin = d(22f)
        })

        val det = LinearLayout(act).apply {
            gravity = Gravity.CENTER_VERTICAL
            background = caixa(0xB3141820.toInt(), 0x1AFFFFFF, f(14f))
            setPadding(d(20f), d(16f), d(16f), d(16f))
        }
        val icone = ImageView(act).apply {
            if (app != null) setImageDrawable(copia(app.icone)) else background = caixa(0x14FFFFFF, 0, f(12f))
        }
        det.addView(icone, LinearLayout.LayoutParams(d(60f), d(60f)).apply { rightMargin = d(18f) })
        val txt = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL }
        val lado = if (botao < 3) "ESQUERDA" else "DIREITA"
        txt.addView(texto("BOTÃO ${botao % 3 + 1}  ·  $lado", 14f, 0x80FFFFFF.toInt()))
        txt.addView(texto(app?.nome ?: "Vazio", 26f, 0xFFFFFFFF.toInt()).apply { setPadding(0, d(4f), 0, d(6f)) })
        txt.addView(etiqueta(if (escolhido == null) "Automático" else "Escolhido por você", escolhido != null))
        det.addView(txt, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        if (escolhido != null) {
            det.addView(botaoTexto(Icone.Tipo.AUTO, "Automático") {
                Ajustes.salvarAppDoBotao(act, botao, null); aoMudarApps(); atualizar()
            })
        }
        esq.addView(det, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            topMargin = d(16f)
        })
        corpo.addView(esq, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1.05f).apply { rightMargin = d(32f) })

        // Direita: grade de apps
        val dir = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL }
        dir.addView(rotulo("ESCOLHA O APP"))
        val grade = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL }
        val lista = todosApps()
        val porLinha = 4
        lista.chunked(porLinha).forEach { grupo ->
            val l = LinearLayout(act)
            for (k in 0 until porLinha) {
                val a = grupo.getOrNull(k)
                val lp = LinearLayout.LayoutParams(0, d(118f), 1f).apply { setMargins(d(5f), d(5f), d(5f), d(5f)) }
                if (a == null) { l.addView(View(act), lp); continue }
                l.addView(blocoApp(a, a.componente == app?.componente) {
                    Ajustes.salvarAppDoBotao(act, botao, a.componente); aoMudarApps(); atualizar()
                }, lp)
            }
            grade.addView(l)
        }
        if (lista.isEmpty()) grade.addView(texto("Carregando apps...", 18f, 0x99FFFFFF.toInt(), forte = false))
        dir.addView(rolagem(grade), LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f).apply { topMargin = d(10f) })
        corpo.addView(dir, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f))

        trocarConteudo(corpo)
    }

    private fun blocoApp(a: App, marcado: Boolean, acao: () -> Unit): View {
        val b = LinearLayout(act).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(d(8f), d(10f), d(8f), d(8f))
            background = if (marcado) estados(0xE6222833.toInt(), 0xFF2B3240.toInt(), destaque, f(14f), d(2.5f))
                         else estados(0x99141820.toInt(), 0xE6222833.toInt(), 0x14FFFFFF, f(14f))
            isClickable = true
            setOnClickListener { acao() }
        }
        b.addView(ImageView(act).apply { setImageDrawable(copia(a.icone)) }, LinearLayout.LayoutParams(d(56f), d(56f)))
        b.addView(texto(a.nome, 16f, if (marcado) 0xFFFFFFFF.toInt() else 0xCCFFFFFF.toInt()).apply {
            gravity = Gravity.CENTER; setPadding(0, d(10f), 0, 0)
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        return b
    }

    // ------------------------------------------------------------------ aba PAPEL DE PAREDE

    private fun montarFundos() {
        val atual = Ajustes.tema(act)
        val corpo = LinearLayout(act)

        val esq = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL }
        esq.addView(cabecalho("Papel de parede", "Toque numa garagem para aplicar na hora"))
        esq.addView(PreviaTela(act, f(14f)).apply {
            apps = appsDosBotoes()
            fundo = fundoGrande?.second
            carro = this@Editor.carro
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            topMargin = d(22f)
        })
        val det = LinearLayout(act).apply {
            gravity = Gravity.CENTER_VERTICAL
            background = caixa(0xB3141820.toInt(), 0x1AFFFFFF, f(14f))
            setPadding(d(22f), d(18f), d(22f), d(18f))
        }
        det.addView(View(act).apply { background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(atual.destaque) } },
            LinearLayout.LayoutParams(d(26f), d(26f)).apply { rightMargin = d(16f) })
        val txt = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL }
        txt.addView(texto("APLICADO AGORA", 14f, 0x80FFFFFF.toInt()))
        txt.addView(texto("Garagem ${atual.nome}", 26f, 0xFFFFFFFF.toInt()).apply { setPadding(0, d(4f), 0, 0) })
        det.addView(txt, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        det.addView(texto("Botões e relógio\nacompanham a cor", 15f, 0x80FFFFFF.toInt(), forte = false).apply {
            setSingleLine(false); maxLines = 2; gravity = Gravity.END
        })
        esq.addView(det, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            topMargin = d(16f)
        })
        corpo.addView(esq, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1.05f).apply { rightMargin = d(32f) })

        val dir = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL }
        dir.addView(rotulo("CORES"))
        val grade = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL }
        val porLinha = 3
        Temas.todos.chunked(porLinha).forEach { grupo ->
            val l = LinearLayout(act)
            for (k in 0 until porLinha) {
                val t = grupo.getOrNull(k)
                val lp = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { setMargins(d(6f), d(4f), d(6f), d(12f)) }
                if (t == null) { l.addView(View(act), lp); continue }
                l.addView(blocoTema(t, t.id == atual.id), lp)
            }
            grade.addView(l)
        }
        dir.addView(rolagem(grade), LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f).apply { topMargin = d(10f) })
        corpo.addView(dir, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f))

        trocarConteudo(corpo)
    }

    private fun blocoTema(t: Tema, marcado: Boolean): View {
        val b = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL }
        val mini = Imagem(act, f(12f)).apply {
            proporcao = 0.5f
            selecionado = marcado
            corContorno = destaque
            espessura = f(3f)
            isClickable = true
            setOnClickListener {
                if (Ajustes.tema(act).id != t.id) {
                    Ajustes.salvarTema(act, t)
                    aoMudarTema(t)
                    carregarFundoGrande()
                    atualizar()
                }
            }
        }
        carregarMiniatura(t) { mini.imagem = it }
        b.addView(mini)
        val nome = LinearLayout(act).apply { gravity = Gravity.CENTER_VERTICAL; setPadding(d(4f), d(8f), 0, 0) }
        nome.addView(View(act).apply { background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(t.destaque) } },
            LinearLayout.LayoutParams(d(12f), d(12f)).apply { rightMargin = d(8f) })
        nome.addView(texto(t.nome, 17f, if (marcado) 0xFFFFFFFF.toInt() else 0xB3FFFFFF.toInt(), forte = marcado))
        b.addView(nome)
        return b
    }

    private fun carregarMiniatura(t: Tema, pronto: (Bitmap) -> Unit) {
        miniaturas[t.imagem]?.let { pronto(it); return }
        Imagens.emSegundoPlano(
            { Imagens.decodificarRecurso(act.resources, t.imagem, 400, 200, comTransparencia = false) },
            { bmp -> if (aberto && bmp != null) { miniaturas[t.imagem] = bmp; pronto(bmp) } }
        )
    }

    // ------------------------------------------------------------------ pecas comuns

    private fun trocarConteudo(v: View) {
        conteudo.removeAllViews()
        conteudo.addView(v)
    }

    private fun cabecalho(titulo: String, sub: String) = LinearLayout(act).apply {
        orientation = LinearLayout.VERTICAL
        addView(texto(titulo, 32f, 0xFFFFFFFF.toInt()))
        addView(texto(sub, 18f, 0x8CFFFFFF.toInt(), forte = false).apply { setPadding(0, d(6f), 0, 0) })
    }

    private fun rotulo(t: String) = texto(t, 14f, 0x80FFFFFF.toInt()).apply { setPadding(d(6f), d(66f), 0, 0) }

    /** Etiqueta arredondada ("Automatico" / "Escolhido por voce"). */
    private fun etiqueta(t: String, forte: Boolean): View {
        val cor = if (forte) destaque else 0xFFFFFFFF.toInt()
        val l = LinearLayout(act)
        l.addView(texto(t, 15f, cor).apply {
            setPadding(d(12f), d(5f), d(12f), d(5f))
            background = caixa((cor and 0x00FFFFFF) or 0x26000000, 0, f(20f))
        })
        return l
    }

    private fun botaoTexto(tipo: Icone.Tipo, t: String, acao: () -> Unit): View {
        val b = LinearLayout(act).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(d(10f), d(8f), d(18f), d(8f))
            background = estados(0x14FFFFFF, 0x2EFFFFFF, 0x26FFFFFF, f(12f))
            isClickable = true
            setOnClickListener { acao() }
        }
        b.addView(Icone(act, tipo).apply { isClickable = false }, LinearLayout.LayoutParams(d(36f), d(36f)).apply { rightMargin = d(6f) })
        b.addView(texto(t, 18f, 0xFFFFFFFF.toInt()))
        return b
    }

    private fun texto(t: String, tamanho: Float, cor: Int, forte: Boolean = true) = TextView(act).apply {
        text = t
        setTextColor(cor)
        setTextSize(TypedValue.COMPLEX_UNIT_PX, tamanho * u)
        typeface = if (forte) Fontes.forte(act) else Fontes.leve(act)
        setSingleLine(true)
        ellipsize = TextUtils.TruncateAt.END
        includeFontPadding = false
    }

    private fun rolagem(v: View) = ScrollView(act).apply {
        isVerticalScrollBarEnabled = false
        isFillViewport = true
        overScrollMode = View.OVER_SCROLL_NEVER
        addView(v)
    }

    private fun caixa(cor: Int, borda: Int, raio: Float, espessura: Int = maxOf(1, d(1.2f))) = GradientDrawable().apply {
        setColor(cor)
        cornerRadius = raio
        if (borda != 0) setStroke(espessura, borda)
    }

    private fun estados(normal: Int, apertado: Int, borda: Int, raio: Float, espessura: Int = maxOf(1, d(1.2f))) =
        StateListDrawable().apply {
            addState(intArrayOf(android.R.attr.state_pressed), caixa(apertado, borda, raio, espessura))
            addState(intArrayOf(), caixa(normal, borda, raio, espessura))
        }

    /** Cada ImageView precisa da sua propria copia do icone (o Drawable guarda o tamanho). */
    private fun copia(d: Drawable): Drawable = d.constantState?.newDrawable(act.resources)?.mutate() ?: d
}

/**
 * Imagem recortada para preencher (cantos arredondados), com veu escuro opcional
 * e contorno quando selecionada. [proporcao] > 0 = altura = largura x proporcao.
 */
private class Imagem(ctx: Context, private val raio: Float) : View(ctx) {

    var imagem: Bitmap? = null
        set(v) { field = v; prepararImagem(); invalidate() }
    var selecionado = false
        set(v) { field = v; invalidate() }
    var veu = 0
    var corContorno = 0xFFFFFFFF.toInt()
    var espessura = 3f
    var proporcao = 0f

    private val pinta = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val vazio = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF151922.toInt() }
    private val traco = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val area = RectF()

    override fun onMeasure(wSpec: Int, hSpec: Int) {
        if (proporcao > 0f) {
            val w = MeasureSpec.getSize(wSpec)
            setMeasuredDimension(w, (w * proporcao).toInt())
        } else super.onMeasure(wSpec, hSpec)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) = prepararImagem()

    private fun prepararImagem() {
        val img = imagem ?: return
        if (width == 0 || height == 0) return
        pinta.shader = sombreadorCorte(img, width.toFloat(), height.toFloat(), 0f, 0f)
    }

    override fun drawableStateChanged() {
        super.drawableStateChanged()
        if (isClickable) alpha = if (isPressed) 0.75f else 1f
    }

    override fun onDraw(c: Canvas) {
        area.set(0f, 0f, width.toFloat(), height.toFloat())
        c.drawRoundRect(area, raio, raio, if (imagem != null) pinta else vazio)
        if (veu != 0) c.drawColor(veu)
        if (selecionado) {
            traco.color = corContorno
            traco.strokeWidth = espessura
            val m = espessura / 2f
            area.inset(m, m)
            c.drawRoundRect(area, raio - m, raio - m, traco)
        }
    }
}

/**
 * Miniatura VIVA da tela inicial: papel de parede, carro, relogio, os 6 botoes
 * (com o nome do app) e o player, nas mesmas posicoes do modelo 1358 x 681.
 * Tocar num botao chama [aoTocarBotao].
 */
private class PreviaTela(ctx: Context, private val raio: Float) : View(ctx) {

    var fundo: Bitmap? = null
        set(v) { field = v; preparar() }
    var carro: Bitmap? = null
    var apps: List<App?> = emptyList()
    var selecionado = -1
    var aoTocarBotao: ((Int) -> Unit)? = null

    private val pFundo = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val pVazio = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF10141B.toInt() }
    private val pCarro = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val pSombra = Paint(Paint.ANTI_ALIAS_FLAG)
    private val pVidro = Paint(Paint.ANTI_ALIAS_FLAG)
    private val pMarca = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; color = 0xFFFFFFFF.toInt() }
    private val pBorda = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; color = 0x26FFFFFF }
    private val pTexto = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFFFFF.toInt(); textAlign = Paint.Align.CENTER; typeface = Fontes.forte(ctx)
    }
    private val pHora = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFF5F8FF.toInt(); textAlign = Paint.Align.CENTER; typeface = Fontes.leve(ctx)
    }
    private val area = RectF()
    private val caixas = Array(6) { RectF() }
    private val formas = Array(6) { Path() }
    private val hora = SimpleDateFormat("HH:mm", Locale.US)

    init { isClickable = true }

    override fun onMeasure(wSpec: Int, hSpec: Int) {
        val w = MeasureSpec.getSize(wSpec)
        setMeasuredDimension(w, (w * Tela.ALT_REF / Tela.LARG_REF).toInt())
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        val s = w / Tela.LARG_REF
        val linhas = arrayOf(133f to 230f, 274f to 376f, 418f to 521f)
        for (i in 0 until 6) {
            val (topo, base) = linhas[i % 3]
            val esq = i < 3
            caixas[i].set((if (esq) 33f else 1036f) * s, topo * s, (if (esq) 326f else 1328f) * s, base * s)
            val r = caixas[i]
            val incl = r.width() * 0.11f
            formas[i].reset()
            if (esq) {
                formas[i].moveTo(r.left, r.top); formas[i].lineTo(r.right - incl, r.top)
                formas[i].lineTo(r.right, r.bottom); formas[i].lineTo(r.left, r.bottom)
            } else {
                formas[i].moveTo(r.left + incl, r.top); formas[i].lineTo(r.right, r.top)
                formas[i].lineTo(r.right, r.bottom); formas[i].lineTo(r.left, r.bottom)
            }
            formas[i].close()
        }
        // Sombra do carro (igual a Carro.kt)
        val cw = 634f * s
        val ch = 287f * s
        pSombra.shader = RadialGradient(0f, 0f, 1f, intArrayOf(0x90000814.toInt(), 0), floatArrayOf(0.2f, 1f), Shader.TileMode.CLAMP)
            .apply { setLocalMatrix(Matrix().apply { setScale(cw * 0.52f, ch * 0.10f); postTranslate(679f * s, 515f * s - ch * 0.06f) }) }
        preparar()
    }

    private fun preparar() {
        val img = fundo ?: return
        if (width == 0) return
        pFundo.shader = sombreadorCorte(img, width.toFloat(), height.toFloat(), 0f, 0f)
        invalidate()
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        val cb = aoTocarBotao ?: return false
        if (e.action == MotionEvent.ACTION_UP) {
            val i = caixas.indexOfFirst { it.contains(e.x, e.y) }
            if (i >= 0) cb(i)
        }
        return true
    }

    override fun onDraw(c: Canvas) {
        val s = width / Tela.LARG_REF
        area.set(0f, 0f, width.toFloat(), height.toFloat())
        c.drawRoundRect(area, raio, raio, if (fundo != null) pFundo else pVazio)

        // Carro
        carro?.let { img ->
            c.drawRect(362f * s, 228f * s, 996f * s, 540f * s, pSombra)
            val esc = minOf(634f * s / img.width, 287f * s / img.height)
            val w = img.width * esc
            val h = img.height * esc
            val x = 679f * s - w / 2f
            c.drawBitmap(img, null, RectF(x, 515f * s - h, x + w, 515f * s), pCarro)
        }

        // Relogio
        pHora.textSize = 99f * s
        c.drawText(hora.format(Date()), width / 2f, 99f * s, pHora)

        // Botoes (vidro na cor do tema) com o nome do app
        for (i in 0 until 6) {
            val r = caixas[i]
            Vidro.preencher(pVidro, r.top, r.bottom, i == selecionado)
            c.drawPath(formas[i], pVidro)
            Vidro.borda(pVidro, maxOf(1f, r.height() * 0.018f))
            c.drawPath(formas[i], pVidro)
            if (i == selecionado) {
                pMarca.strokeWidth = maxOf(2f, 3.5f * s * 2f)
                c.drawPath(formas[i], pMarca)
            }
            val nome = apps.getOrNull(i)?.nome ?: "Texto"
            pTexto.textSize = r.height() * 0.26f
            pTexto.alpha = if (apps.getOrNull(i) == null) 90 else 240
            val incl = r.width() * 0.11f
            val cx = r.centerX() + (if (i < 3) -incl else incl) / 4f
            val mostrado = TextUtils.ellipsize(nome, android.text.TextPaint(pTexto), r.width() * 0.70f, TextUtils.TruncateAt.END)
            c.drawText(mostrado.toString(), cx, r.centerY() - (pTexto.descent() + pTexto.ascent()) / 2f, pTexto)
        }

        // Player
        area.set(272f * s, 570f * s, 1088f * s, 666f * s)
        Vidro.preencher(pVidro, area.top, area.bottom, false)
        c.drawRoundRect(area, 10f * s, 10f * s, pVidro)
        Vidro.borda(pVidro, maxOf(1f, 1.8f * s))
        c.drawRoundRect(area, 10f * s, 10f * s, pVidro)

        // Contorno fino da previa
        area.set(0f, 0f, width.toFloat(), height.toFloat())
        pBorda.strokeWidth = maxOf(1f, 2f * s)
        c.drawRoundRect(area, raio, raio, pBorda)
    }
}

/** Shader que desenha [img] preenchendo w x h (corta as sobras, centralizado). */
private fun sombreadorCorte(img: Bitmap, w: Float, h: Float, x: Float, y: Float): BitmapShader {
    val esc = maxOf(w / img.width, h / img.height)
    val m = Matrix().apply {
        setScale(esc, esc)
        postTranslate(x + (w - img.width * esc) / 2f, y + (h - img.height * esc) / 2f)
    }
    return BitmapShader(img, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP).apply { setLocalMatrix(m) }
}
