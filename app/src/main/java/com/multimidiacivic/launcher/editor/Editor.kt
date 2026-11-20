package com.multimidiacivic.launcher.editor

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
import android.graphics.drawable.GradientDrawable
import android.text.TextPaint
import android.text.TextUtils
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.multimidiacivic.launcher.R
import com.multimidiacivic.launcher.dados.Ajustes
import com.multimidiacivic.launcher.dados.App
import com.multimidiacivic.launcher.dados.Tema
import com.multimidiacivic.launcher.dados.Temas
import com.multimidiacivic.launcher.tela.Icone
import com.multimidiacivic.launcher.tela.Tela
import com.multimidiacivic.launcher.tela.Vidro
import com.multimidiacivic.launcher.util.Fontes
import com.multimidiacivic.launcher.util.GradeApps
import com.multimidiacivic.launcher.util.Icones
import com.multimidiacivic.launcher.util.Imagens
import com.multimidiacivic.launcher.util.Pecas
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
 * Desempenho (a central e fraca):
 *  - O painel e montado UMA vez ([preparar], chamado quando o app fica ocioso)
 *    e depois so aparece/some. Nada e remontado a cada toque: cada escolha
 *    atualiza so a selecao, os textos e as cores que mudaram.
 *  - A grade de apps e um GridView: so existem as celulas visiveis.
 *  - Icones dos apps viram imagem pronta uma vez so, em segundo plano.
 *  - As miniaturas das garagens sao lidas uma vez e reaproveitadas
 *    (inclusive na previa), entao trocar a cor nao le imagem nenhuma.
 *  - Aberto, a tela inicial por baixo fica escondida ([aoCobrirTela]),
 *    menos o papel de parede, que aparece atras do veu escuro.
 *
 * Tudo e medido na escala do modelo da tela (altura 681), igual ao resto.
 */
class Editor(
    private val act: Activity,
    private val todosApps: () -> List<App>,
    private val appsDosBotoes: () -> List<App?>,
    private val aoMudarTema: (Tema) -> Unit,
    private val aoMudarApps: () -> Unit,
    private val aoCobrirTela: (Boolean) -> Unit,
) {
    private enum class Aba { BOTOES, FUNDO }

    private var raiz: FrameLayout? = null
    var aberto = false
        private set
    private var aba = Aba.BOTOES
    private var botao = 0 // botao escolhido na aba BOTOES (0..5)
    private var u = 1f
    private var destaque = 0

    private val p = Pecas(act)

    // Imagens: lidas uma vez e guardadas (miniaturas ~2 MB, carro ~0,4 MB)
    private val miniaturas = HashMap<Int, Bitmap>()
    private var carro: Bitmap? = null

    // Menu
    private lateinit var itemBotoes: ItemMenu
    private lateinit var itemFundo: ItemMenu

    // Aba BOTOES
    private lateinit var abaBotoes: View
    private lateinit var previaBotoes: PreviaTela
    private lateinit var detIcone: ImageView
    private lateinit var detPosicao: TextView
    private lateinit var detNome: TextView
    private lateinit var detEtiqueta: TextView
    private lateinit var fundoEtiqueta: GradientDrawable
    private lateinit var detAuto: View
    private val adaptador = GradeApps(p, 118f)

    // Aba PAPEL DE PAREDE
    private lateinit var abaFundos: View
    private lateinit var previaFundos: PreviaTela
    private lateinit var detCor: GradientDrawable
    private lateinit var detTema: TextView
    private val blocosTema = ArrayList<BlocoTema>()

    private fun d(v: Float) = p.d(v)
    private fun f(v: Float) = p.f(v)
    private fun corDestaque(t: Tema) = Vidro.clarear(t.destaque, 0.20f, 0xFF)

    // ------------------------------------------------------------------ montar / abrir / fechar

    /** Monta o painel escondido (uma vez so). Pode ser chamado antes de abrir. */
    fun preparar() {
        if (raiz != null) return
        u = act.resources.displayMetrics.heightPixels / Tela.ALT_REF
        p.u = u
        destaque = corDestaque(Ajustes.tema(act))

        val r = FrameLayout(act).apply {
            isClickable = true // nao deixa o toque passar para a tela
            setBackgroundColor(0xE6070910.toInt()) // veu sobre o papel de parede real
            visibility = View.GONE
        }
        val linha = LinearLayout(act)
        linha.addView(montarMenu(), LinearLayout.LayoutParams(d(290f), ViewGroup.LayoutParams.MATCH_PARENT))
        linha.addView(View(act).apply { setBackgroundColor(0x14FFFFFF) },
            LinearLayout.LayoutParams(maxOf(1, d(1f)), ViewGroup.LayoutParams.MATCH_PARENT))
        val conteudo = FrameLayout(act).apply { setPadding(d(40f), d(34f), d(40f), d(26f)) }
        abaBotoes = montarBotoes()
        abaFundos = montarFundos().apply { visibility = View.GONE }
        conteudo.addView(abaBotoes)
        conteudo.addView(abaFundos)
        linha.addView(conteudo, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f))
        r.addView(linha)
        act.addContentView(r, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        raiz = r

        carregarImagens()
        atualizarLista()
    }

    fun abrir() {
        if (aberto) return
        // Tela mudou de tamanho desde que foi montado (ex.: barras do Android)? remonta.
        val novoU = act.resources.displayMetrics.heightPixels / Tela.ALT_REF
        if (raiz != null && Math.abs(novoU - u) > 0.01f) descartar()
        preparar()
        destaque = corDestaque(Ajustes.tema(act))
        mostrar(Aba.BOTOES)
        atualizarBotoes()
        atualizarFundos()
        raiz?.visibility = View.VISIBLE
        aberto = true
        aoCobrirTela(true)
    }

    fun fechar() {
        if (!aberto) return
        raiz?.visibility = View.GONE
        aberto = false
        aoCobrirTela(false)
    }

    /** Botao Voltar do Android. */
    fun voltar() = fechar()

    /** Lista de apps mudou (carregou / instalou / removeu): atualiza a grade e os icones. */
    fun atualizarLista() {
        if (raiz == null) return
        adaptador.lista = todosApps()
        adaptador.notifyDataSetChanged()
        Icones.gerar(adaptador.lista, d(56f))
        if (aberto) atualizarBotoes()
    }

    init {
        // Icones novos prontos: redesenha a grade e o icone do detalhe
        Icones.ouvir {
            adaptador.notifyDataSetChanged()
            if (aberto) atualizarBotoes()
        }
    }

    private fun descartar() {
        raiz?.let { (it.parent as? ViewGroup)?.removeView(it) }
        raiz = null
        blocosTema.clear()
    }

    private fun mostrar(a: Aba) {
        aba = a
        abaBotoes.visibility = if (a == Aba.BOTOES) View.VISIBLE else View.GONE
        abaFundos.visibility = if (a == Aba.FUNDO) View.VISIBLE else View.GONE
        itemBotoes.marcar(a == Aba.BOTOES)
        itemFundo.marcar(a == Aba.FUNDO)
    }

    // ------------------------------------------------------------------ imagens

    private fun carregarImagens() {
        Imagens.emSegundoPlano(
            { Imagens.decodificarRecurso(act.resources, R.drawable.carro, 480, 217, comTransparencia = true) },
            { bmp -> carro = bmp; previaBotoes.carro = bmp; previaFundos.carro = bmp }
        )
        // Miniaturas: a garagem atual primeiro (e a da previa)
        val atual = Ajustes.tema(act)
        (listOf(atual) + Temas.todos.filter { it.id != atual.id }).forEach { t ->
            Imagens.emSegundoPlano(
                { Imagens.decodificarRecurso(act.resources, t.imagem, 400, 200, comTransparencia = false) },
                { bmp ->
                    if (bmp == null) return@emSegundoPlano
                    miniaturas[t.imagem] = bmp
                    blocosTema.firstOrNull { it.tema.id == t.id }?.mini?.imagem = bmp
                    if (Ajustes.tema(act).id == t.id) { previaBotoes.fundo = bmp; previaFundos.fundo = bmp }
                }
            )
        }
    }

    // ------------------------------------------------------------------ menu lateral

    private fun montarMenu(): View {
        val menu = LinearLayout(act).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0x59000000)
            setPadding(d(22f), d(34f), d(22f), d(24f))
        }
        menu.addView(texto("Personalizar", 32f, 0xFFFFFFFF.toInt()))
        menu.addView(texto("Deixe a central do seu jeito", 17f, 0x8CFFFFFF.toInt(), forte = false).apply {
            setPadding(0, d(6f), 0, d(30f))
        })
        itemBotoes = ItemMenu(Icone.Tipo.APPS, "Botões", "Apps dos 6 atalhos") { mostrar(Aba.BOTOES) }
        itemFundo = ItemMenu(Icone.Tipo.IMAGEM, "Papel de parede", "${Temas.todos.size} garagens") { mostrar(Aba.FUNDO) }
        menu.addView(itemBotoes.raiz)
        menu.addView(itemFundo.raiz)
        menu.addView(View(act), LinearLayout.LayoutParams(1, 0, 1f))
        menu.addView(ItemMenu(Icone.Tipo.FECHAR, "Fechar", "Voltar para a tela inicial") { fechar() }.raiz)
        return menu
    }

    /** Item do menu lateral; [marcar] so troca cores (nada e recriado). */
    private inner class ItemMenu(tipo: Icone.Tipo, nome: String, sub: String, acao: () -> Unit) {
        private val fundoAtivo = estados(0x1FFFFFFF, 0x2EFFFFFF, 0, f(12f))
        private val fundoNormal = estados(0x00000000, 0x1AFFFFFF, 0, f(12f))
        private val barra = caixa(0x00000000, 0, f(3f))
        private val icone = Icone(act, tipo).apply { isClickable = false; alpha = 0.75f }
        private val titulo = texto(nome, 22f, 0xD9FFFFFF.toInt())
        private val linha = LinearLayout(act).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, d(12f), 0)
            background = fundoNormal
            isClickable = true
            setOnClickListener { acao() }
        }
        val raiz: View

        init {
            linha.addView(View(act).apply { background = barra },
                LinearLayout.LayoutParams(d(5f), d(38f)).apply { rightMargin = d(10f) })
            linha.addView(icone, LinearLayout.LayoutParams(d(48f), d(48f)).apply { rightMargin = d(10f) })
            val textos = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL }
            textos.addView(titulo)
            textos.addView(texto(sub, 15f, 0x80FFFFFF.toInt(), forte = false).apply { setPadding(0, d(3f), 0, 0) })
            linha.addView(textos, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            raiz = FrameLayout(act).apply {
                setPadding(0, 0, 0, d(8f))
                addView(linha, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, d(80f)))
            }
        }

        fun marcar(ativo: Boolean) {
            linha.background = if (ativo) fundoAtivo else fundoNormal
            barra.setColor(if (ativo) destaque else 0x00000000)
            icone.alpha = if (ativo) 1f else 0.75f
            titulo.setTextColor(if (ativo) 0xFFFFFFFF.toInt() else 0xD9FFFFFF.toInt())
        }
    }

    // ------------------------------------------------------------------ aba BOTOES

    private fun montarBotoes(): View {
        val corpo = LinearLayout(act)

        // Esquerda: previa (toque num botao) + detalhes do botao escolhido
        val esq = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL }
        esq.addView(cabecalho("Apps dos botões", "Toque num botão da prévia e escolha o app ao lado"))
        previaBotoes = PreviaTela(act, f(14f)).apply {
            aoTocarBotao = { i -> if (i != botao) { botao = i; atualizarBotoes() } }
        }
        esq.addView(previaBotoes, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            topMargin = d(22f)
        })

        val det = LinearLayout(act).apply {
            gravity = Gravity.CENTER_VERTICAL
            background = caixa(0xB3141820.toInt(), 0x1AFFFFFF, f(14f))
            setPadding(d(20f), d(16f), d(16f), d(16f))
        }
        detIcone = ImageView(act)
        det.addView(detIcone, LinearLayout.LayoutParams(d(60f), d(60f)).apply { rightMargin = d(18f) })
        val txt = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL }
        detPosicao = texto("", 14f, 0x80FFFFFF.toInt())
        detNome = texto("", 26f, 0xFFFFFFFF.toInt()).apply { setPadding(0, d(4f), 0, d(6f)) }
        fundoEtiqueta = caixa(0, 0, f(20f))
        detEtiqueta = texto("", 15f, 0xFFFFFFFF.toInt()).apply {
            setPadding(d(12f), d(5f), d(12f), d(5f))
            background = fundoEtiqueta
        }
        txt.addView(detPosicao)
        txt.addView(detNome)
        txt.addView(LinearLayout(act).apply { addView(detEtiqueta) })
        det.addView(txt, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        detAuto = botaoTexto(Icone.Tipo.AUTO, "Automático") {
            Ajustes.salvarAppDoBotao(act, botao, null); aoMudarApps(); atualizarBotoes()
        }
        det.addView(detAuto)
        esq.addView(det, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            topMargin = d(16f)
        })
        corpo.addView(esq, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1.05f).apply { rightMargin = d(32f) })

        // Direita: grade de apps (so as celulas visiveis existem)
        val dir = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL }
        dir.addView(rotulo("ESCOLHA O APP"))
        val grade = p.gradeApps(4, adaptador) { escolherApp(it) }
        val vazio = texto("Carregando apps...", 18f, 0x99FFFFFF.toInt(), forte = false)
        val caixaGrade = FrameLayout(act)
        caixaGrade.addView(grade)
        caixaGrade.addView(vazio)
        grade.emptyView = vazio
        dir.addView(caixaGrade, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f).apply { topMargin = d(5f) })
        corpo.addView(dir, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f))
        return corpo
    }

    private fun escolherApp(a: App) {
        if (Ajustes.appDoBotao(act, botao) == a.componente) return
        Ajustes.salvarAppDoBotao(act, botao, a.componente)
        aoMudarApps()
        atualizarBotoes()
    }

    /** Atualiza previa, detalhes e a marcacao da grade (sem recriar nada). */
    private fun atualizarBotoes() {
        val atuais = appsDosBotoes()
        val app = atuais.getOrNull(botao)
        val escolhido = Ajustes.appDoBotao(act, botao)

        previaBotoes.apps = atuais
        previaBotoes.selecionado = botao
        previaBotoes.invalidate()
        previaFundos.apps = atuais
        previaFundos.invalidate()

        val img = app?.let { Icones.de(it.componente) }
        detIcone.setImageBitmap(img)
        detIcone.background = if (app == null) caixa(0x14FFFFFF, 0, f(12f)) else null
        detPosicao.text = "BOTÃO ${botao % 3 + 1}  ·  ${if (botao < 3) "ESQUERDA" else "DIREITA"}"
        detNome.text = app?.nome ?: "Vazio"
        val forte = escolhido != null
        val cor = if (forte) destaque else 0xFFFFFFFF.toInt()
        detEtiqueta.text = if (forte) "Escolhido por você" else "Automático"
        detEtiqueta.setTextColor(cor)
        fundoEtiqueta.setColor((cor and 0x00FFFFFF) or 0x26000000)
        detAuto.visibility = if (forte) View.VISIBLE else View.GONE

        if (adaptador.marcado != app?.componente || adaptador.destaque != destaque) {
            adaptador.marcado = app?.componente
            adaptador.destaque = destaque
            adaptador.notifyDataSetChanged()
        }
    }

    // ------------------------------------------------------------------ aba PAPEL DE PAREDE

    private fun montarFundos(): View {
        val corpo = LinearLayout(act)

        val esq = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL }
        esq.addView(cabecalho("Papel de parede", "Toque numa garagem para aplicar na hora"))
        previaFundos = PreviaTela(act, f(14f))
        esq.addView(previaFundos, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            topMargin = d(22f)
        })
        val det = LinearLayout(act).apply {
            gravity = Gravity.CENTER_VERTICAL
            background = caixa(0xB3141820.toInt(), 0x1AFFFFFF, f(14f))
            setPadding(d(22f), d(18f), d(22f), d(18f))
        }
        detCor = GradientDrawable().apply { shape = GradientDrawable.OVAL }
        det.addView(View(act).apply { background = detCor },
            LinearLayout.LayoutParams(d(26f), d(26f)).apply { rightMargin = d(16f) })
        val txt = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL }
        txt.addView(texto("APLICADO AGORA", 14f, 0x80FFFFFF.toInt()))
        detTema = texto("", 26f, 0xFFFFFFFF.toInt()).apply { setPadding(0, d(4f), 0, 0) }
        txt.addView(detTema)
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
                val b = BlocoTema(t)
                blocosTema.add(b)
                l.addView(b.raiz, lp)
            }
            grade.addView(l)
        }
        dir.addView(rolagem(grade), LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f).apply { topMargin = d(10f) })
        corpo.addView(dir, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f))
        return corpo
    }

    private fun escolherTema(t: Tema) {
        if (Ajustes.tema(act).id == t.id) return
        Ajustes.salvarTema(act, t)
        aoMudarTema(t)
        destaque = corDestaque(t)
        itemBotoes.marcar(aba == Aba.BOTOES)
        itemFundo.marcar(aba == Aba.FUNDO)
        atualizarFundos()
        atualizarBotoes()
    }

    /** Atualiza previa, detalhes e a garagem marcada (sem recriar nada). */
    private fun atualizarFundos() {
        val atual = Ajustes.tema(act)
        val mini = miniaturas[atual.imagem]
        previaBotoes.fundo = mini
        previaFundos.fundo = mini
        previaFundos.apps = appsDosBotoes()
        previaFundos.invalidate()
        detCor.setColor(atual.destaque)
        detTema.text = "Garagem ${atual.nome}"
        blocosTema.forEach { it.marcar(it.tema.id == atual.id) }
    }

    private inner class BlocoTema(val tema: Tema) {
        val mini = Imagem(act, f(12f)).apply {
            proporcao = 0.5f
            espessura = f(3f)
            isClickable = true
            imagem = miniaturas[tema.imagem]
            setOnClickListener { escolherTema(tema) }
        }
        private val nome = texto(tema.nome, 17f, 0xB3FFFFFF.toInt(), forte = false)
        private var marcado: Boolean? = null
        val raiz = LinearLayout(act).apply {
            orientation = LinearLayout.VERTICAL
            addView(mini)
            val linha = LinearLayout(act).apply { gravity = Gravity.CENTER_VERTICAL; setPadding(d(4f), d(8f), 0, 0) }
            linha.addView(View(act).apply { background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(tema.destaque) } },
                LinearLayout.LayoutParams(d(12f), d(12f)).apply { rightMargin = d(8f) })
            linha.addView(nome)
            addView(linha)
        }

        fun marcar(m: Boolean) {
            mini.corContorno = destaque
            mini.selecionado = m
            if (marcado == m) return
            marcado = m
            nome.setTextColor(if (m) 0xFFFFFFFF.toInt() else 0xB3FFFFFF.toInt())
            nome.typeface = if (m) Fontes.forte(act) else Fontes.leve(act)
        }
    }

    // ------------------------------------------------------------------ pecas comuns

    private fun rotulo(t: String) = texto(t, 14f, 0x80FFFFFF.toInt()).apply { setPadding(d(6f), d(66f), 0, 0) }

    // Atalhos para as pecas compartilhadas com a gaveta de apps (util/Pecas.kt)
    private fun cabecalho(titulo: String, sub: String) = p.cabecalho(titulo, sub)
    private fun botaoTexto(tipo: Icone.Tipo, t: String, acao: () -> Unit) = p.botaoTexto(tipo, t, acao)
    private fun texto(t: String, tamanho: Float, cor: Int, forte: Boolean = true) = p.texto(t, tamanho, cor, forte)
    private fun rolagem(v: View) = p.rolagem(v)
    private fun caixa(cor: Int, borda: Int, raio: Float) = p.caixa(cor, borda, raio)
    private fun estados(normal: Int, apertado: Int, borda: Int, raio: Float) = p.estados(normal, apertado, borda, raio)
}

/**
 * Imagem recortada para preencher (cantos arredondados), com contorno quando
 * selecionada. [proporcao] > 0 = altura = largura x proporcao.
 */
private class Imagem(ctx: Context, private val raio: Float) : View(ctx) {

    var imagem: Bitmap? = null
        set(v) { if (field === v) return; field = v; prepararImagem(); invalidate() }
    var selecionado = false
        set(v) { if (field == v) return; field = v; invalidate() }
    var corContorno = 0xFFFFFFFF.toInt()
        set(v) { if (field == v) return; field = v; invalidate() }
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
        pinta.shader = sombreadorCorte(img, width.toFloat(), height.toFloat())
    }

    override fun drawableStateChanged() {
        super.drawableStateChanged()
        if (isClickable) alpha = if (isPressed) 0.75f else 1f
    }

    override fun onDraw(c: Canvas) {
        area.set(0f, 0f, width.toFloat(), height.toFloat())
        c.drawRoundRect(area, raio, raio, if (imagem != null) pinta else vazio)
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
        set(v) { if (field === v) return; field = v; preparar() }
    var carro: Bitmap? = null
        set(v) { field = v; invalidate() }
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
    private val pTexto = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFFFFF.toInt(); textAlign = Paint.Align.CENTER; typeface = Fontes.forte(ctx)
    }
    private val pHora = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFF5F8FF.toInt(); textAlign = Paint.Align.CENTER; typeface = Fontes.leve(ctx)
    }
    private val area = RectF()
    private val destinoCarro = RectF()
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
        val img = fundo
        if (img != null && width > 0) pFundo.shader = sombreadorCorte(img, width.toFloat(), height.toFloat())
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
            destinoCarro.set(x, 515f * s - h, x + w, 515f * s)
            c.drawBitmap(img, null, destinoCarro, pCarro)
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
            val mostrado = TextUtils.ellipsize(nome, pTexto, r.width() * 0.70f, TextUtils.TruncateAt.END)
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
private fun sombreadorCorte(img: Bitmap, w: Float, h: Float): BitmapShader {
    val esc = maxOf(w / img.width, h / img.height)
    val m = Matrix().apply {
        setScale(esc, esc)
        postTranslate((w - img.width * esc) / 2f, (h - img.height * esc) / 2f)
    }
    return BitmapShader(img, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP).apply { setLocalMatrix(m) }
}
