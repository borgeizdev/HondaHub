package com.multimidiacivic.launcher.gaveta

import android.app.Activity
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.multimidiacivic.launcher.dados.Ajustes
import com.multimidiacivic.launcher.dados.App
import com.multimidiacivic.launcher.dados.Apps
import com.multimidiacivic.launcher.tela.Icone
import com.multimidiacivic.launcher.tela.Tela
import com.multimidiacivic.launcher.tela.Vidro
import com.multimidiacivic.launcher.util.GradeApps
import com.multimidiacivic.launcher.util.Icones
import com.multimidiacivic.launcher.util.Pecas

/**
 * Gaveta de apps (botao de grade, canto superior direito): TODOS os apps
 * instalados na central, nao so os 6 dos botoes. Mesma cara do lapis:
 *
 *   | Aplicativos              [Todos 42] [Baixados 9] [Do sistema 33]  [x Fechar] |
 *   | 42 apps nesta central                                                          |
 *   |  [app] [app] [app] [app] [app] [app] [app]                                     |
 *   |  [app] [app] ...                                         (rola para baixo)     |
 *
 * Tocar num app abre ele; ao voltar para a tela inicial a gaveta ja esta fechada.
 * Montada uma vez so ([preparar]) e reaproveitada, igual ao lapis.
 */
class Gaveta(
    private val act: Activity,
    private val todosApps: () -> List<App>,
    private val aoCobrirTela: (Boolean) -> Unit,
) {
    private enum class Filtro(val nome: String) { TODOS("Todos"), BAIXADOS("Baixados"), SISTEMA("Do sistema") }

    private val p = Pecas(act)
    private val adaptador = GradeApps(p, 118f)
    private var raiz: FrameLayout? = null
    private var u = 1f
    private var filtro = Filtro.TODOS
    private var destaque = 0
    private lateinit var subtitulo: TextView
    private val chips = HashMap<Filtro, TextView>()

    var aberto = false
        private set

    init {
        Icones.ouvir { if (raiz != null) adaptador.notifyDataSetChanged() }
    }

    /** Monta a gaveta escondida (uma vez so). */
    fun preparar() {
        if (raiz != null) return
        u = act.resources.displayMetrics.heightPixels / Tela.ALT_REF
        p.u = u

        val r = FrameLayout(act).apply {
            isClickable = true // nao deixa o toque passar para a tela
            setBackgroundColor(0xE6070910.toInt()) // veu sobre o papel de parede real
            visibility = View.GONE
        }
        val col = LinearLayout(act).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(p.d(40f), p.d(34f), p.d(40f), 0)
        }

        // Topo: titulo | filtros | fechar
        val topo = LinearLayout(act).apply { gravity = Gravity.CENTER_VERTICAL }
        val titulo = p.cabecalho("Aplicativos", "")
        subtitulo = titulo.getChildAt(1) as TextView
        topo.addView(titulo, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        for (fl in Filtro.values()) {
            val c = p.texto(fl.nome, 17f, 0xB3FFFFFF.toInt()).apply {
                gravity = Gravity.CENTER
                setPadding(p.d(20f), p.d(11f), p.d(20f), p.d(11f))
                isClickable = true
                setOnClickListener { escolherFiltro(fl) }
            }
            chips[fl] = c
            topo.addView(c, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                rightMargin = p.d(10f)
            })
        }
        topo.addView(p.botaoTexto(Icone.Tipo.FECHAR, "Fechar") { fechar() },
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                leftMargin = p.d(22f)
            })
        col.addView(topo)

        // Grade com todos os apps (so as celulas visiveis existem)
        val grade = p.gradeApps(7, adaptador) { a -> Apps.abrir(act, a) }.apply {
            setPadding(p.d(5f), p.d(5f), p.d(5f), p.d(26f))
        }
        val vazio = p.texto("Carregando apps...", 18f, 0x99FFFFFF.toInt(), forte = false)
        val caixaGrade = FrameLayout(act)
        caixaGrade.addView(grade)
        caixaGrade.addView(vazio)
        grade.emptyView = vazio
        col.addView(caixaGrade, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f).apply {
            topMargin = p.d(22f)
        })

        r.addView(col)
        act.addContentView(r, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        raiz = r
        atualizarLista()
    }

    fun abrir() {
        if (aberto) return
        // Tela mudou de tamanho desde que foi montada? remonta.
        val novoU = act.resources.displayMetrics.heightPixels / Tela.ALT_REF
        raiz?.let { if (Math.abs(novoU - u) > 0.01f) { (it.parent as? ViewGroup)?.removeView(it); raiz = null; chips.clear() } }
        preparar()
        val cor = Vidro.clarear(Ajustes.tema(act).destaque, 0.20f, 0xFF)
        if (cor != destaque) { destaque = cor; marcarFiltros() }
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

    /** Lista de apps mudou (carregou / instalou / removeu). */
    fun atualizarLista() {
        if (raiz == null) return
        val todos = todosApps()
        Icones.gerar(todos, p.d(56f))
        val baixados = todos.count { it.doUsuario }
        chips[Filtro.TODOS]?.text = "Todos  ${todos.size}"
        chips[Filtro.BAIXADOS]?.text = "Baixados  $baixados"
        chips[Filtro.SISTEMA]?.text = "Do sistema  ${todos.size - baixados}"
        subtitulo.text = if (todos.isEmpty()) "Procurando os apps da central..."
                         else "${todos.size} apps nesta central"
        aplicarFiltro()
    }

    private fun escolherFiltro(fl: Filtro) {
        if (fl == filtro) return
        filtro = fl
        marcarFiltros()
        aplicarFiltro()
    }

    private fun aplicarFiltro() {
        val todos = todosApps()
        adaptador.lista = when (filtro) {
            Filtro.TODOS -> todos
            Filtro.BAIXADOS -> todos.filter { it.doUsuario }
            Filtro.SISTEMA -> todos.filter { !it.doUsuario }
        }
        adaptador.notifyDataSetChanged()
        marcarFiltros()
    }

    /** Filtro ativo na cor do papel de parede; os outros discretos. */
    private fun marcarFiltros() {
        for ((fl, c) in chips) {
            val ativo = fl == filtro
            if (ativo) {
                val fundo = destaque and 0x00FFFFFF
                c.background = p.estados(fundo or 0x26000000, fundo or 0x40000000, destaque, p.f(22f))
                c.setTextColor(0xFFFFFFFF.toInt())
            } else {
                c.background = p.estados(0x14FFFFFF, 0x2EFFFFFF, 0x1AFFFFFF, p.f(22f))
                c.setTextColor(0xB3FFFFFF.toInt())
            }
        }
    }
}
