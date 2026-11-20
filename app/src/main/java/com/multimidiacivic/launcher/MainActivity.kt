package com.multimidiacivic.launcher

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.widget.Toast
import com.multimidiacivic.launcher.dados.Ajustes
import com.multimidiacivic.launcher.dados.App
import com.multimidiacivic.launcher.dados.Apps
import com.multimidiacivic.launcher.dados.Tema
import com.multimidiacivic.launcher.editor.Editor
import com.multimidiacivic.launcher.gaveta.Gaveta
import com.multimidiacivic.launcher.musica.EstadoMusica
import com.multimidiacivic.launcher.musica.Musica
import com.multimidiacivic.launcher.tela.Botao
import com.multimidiacivic.launcher.tela.Carro
import com.multimidiacivic.launcher.tela.Fundo
import com.multimidiacivic.launcher.tela.Icone
import com.multimidiacivic.launcher.tela.Player
import com.multimidiacivic.launcher.tela.Relogio
import com.multimidiacivic.launcher.tela.Tela
import com.multimidiacivic.launcher.tela.Vidro
import com.multimidiacivic.launcher.util.Imagens

/**
 * TELA INICIAL (igual ao modelo):
 *
 *   [config] [wifi]        02:04        [bluetooth] [apps]
 *   [TEXTO]                                         [TEXTO]
 *   [TEXTO]               (carro)                   [TEXTO]
 *   [TEXTO]                                         [TEXTO]
 *          [ >  NOME MUSICA  ======o====  || ]           [lapis]
 *
 * Os 6 botoes "TEXTO" viram atalhos para os apps instalados
 * (Play Store primeiro, depois os que o usuario baixou).
 */
class MainActivity : Activity() {

    private lateinit var relogio: Relogio
    private lateinit var player: Player
    private lateinit var musica: Musica
    private lateinit var tela: Tela
    private lateinit var fundo: Fundo
    private lateinit var editor: Editor
    private lateinit var gaveta: Gaveta
    private val botoes = ArrayList<Botao>()
    private var todosApps: List<App> = emptyList()
    private var apps: List<App?> = emptyList()
    private var precisaRecarregar = true

    /** App instalado / removido / atualizado -> refaz os botoes. */
    private val avisoApps = object : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) {
            precisaRecarregar = true
            if (hasWindowFocus()) carregarApps()
        }
    }

    /** Muda o minuto / a hora / o fuso -> atualiza o relogio. */
    private val avisoHora = object : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) = relogio.atualizar()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(montarTela())
        aplicarTema(Ajustes.tema(this))
        editor = Editor(this, { todosApps }, { apps }, { t -> aplicarTema(t) }, { distribuirApps() }, { cobre -> cobrirTela(cobre) })
        gaveta = Gaveta(this, { todosApps }, { cobre -> cobrirTela(cobre) })
        // Monta o lapis e a gaveta escondidos quando o app ficar ocioso: abrir fica instantaneo
        Looper.myQueue().addIdleHandler { editor.preparar(); gaveta.preparar(); false }

        musica = Musica(this) { estado -> mostrarMusica(estado) }
        player.aoTocar = { musica.tocar() }
        player.aoPausar = { musica.pausar() }
        player.aoBuscar = { ms -> musica.irPara(ms) }
        player.aoAnterior = { musica.anterior() }
        player.aoProxima = { musica.proxima() }
        player.aoMudo = { player.mudo = musica.alternarMudo() }
        player.aoTocarNoTitulo = { if (!musica.temAcesso()) musica.pedirAcesso(this) }

        val filtro = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(avisoApps, filtro, Context.RECEIVER_EXPORTED)
        else registerReceiver(avisoApps, filtro)
    }

    private fun montarTela(): View {
        tela = Tela(this)
        fundo = Fundo(this)
        tela.colocar(fundo, 0f, 0f, Tela.LARG_REF, Tela.ALT_REF)

        // Carro: carro.png tem 960 x 434 -> em 634 de largura fica com ~287 de altura
        val carro = Carro(this)
        tela.colocar(carro, 362f, 228f, 996f, 515f)
        Imagens.emSegundoPlano(
            { Imagens.decodificarRecurso(resources, R.drawable.carro, 4000, 4000, comTransparencia = true) },
            { bmp -> if (bmp != null) carro.mostrar(bmp) }
        )

        // Icones: area de toque de 64 x 64 em volta do centro (o desenho ocupa metade)
        fun colocarIcone(tipo: Icone.Tipo, cx: Float, cy: Float, acao: () -> Unit) =
            tela.colocar(icone(tipo, acao), cx - 32f, cy - 32f, cx + 32f, cy + 32f)
        colocarIcone(Icone.Tipo.CONFIG, 67f, 42f) { abrirAjuste(Settings.ACTION_SETTINGS) }
        colocarIcone(Icone.Tipo.WIFI, 140f, 42f) { abrirAjuste(Settings.ACTION_WIFI_SETTINGS) }
        colocarIcone(Icone.Tipo.BLUETOOTH, 1217f, 42f) { abrirAjuste(Settings.ACTION_BLUETOOTH_SETTINGS) }
        colocarIcone(Icone.Tipo.APPS, 1288f, 42f) { gaveta.abrir() }
        colocarIcone(Icone.Tipo.EDITAR, 1287f, 618f) { editor.abrir() }

        // Relogio grande + data embaixo
        relogio = Relogio(this)
        tela.colocar(relogio, 430f, 16f, 930f, 150f)

        // 6 botoes: 3 de cada lado, no tamanho do modelo
        val linhas = arrayOf(133f to 230f, 274f to 376f, 418f to 521f)
        for ((topo, base) in linhas) tela.colocar(botao(true), 33f, topo, 326f, base)
        for ((topo, base) in linhas) tela.colocar(botao(false), 1036f, topo, 1328f, base)
        player = Player(this)
        tela.colocar(player, 272f - Player.FOLGA, 570f - Player.FOLGA, 1088f + Player.FOLGA, 666f + Player.FOLGA)
        return tela
    }

    private fun icone(tipo: Icone.Tipo, acao: () -> Unit) =
        Icone(this, tipo).apply { setOnClickListener { acao() } }

    private fun botao(ladoEsquerdo: Boolean): Botao {
        val b = Botao(this, ladoEsquerdo)
        val indice = botoes.size
        b.setOnClickListener {
            val app = apps.getOrNull(indice)
            if (app != null) Apps.abrir(this, app) else emBreve()
        }
        botoes.add(b)
        return b
    }

    override fun onStart() {
        super.onStart()
        val f = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
        }
        registerReceiver(avisoHora, f)
        relogio.atualizar()
    }

    override fun onStop() {
        unregisterReceiver(avisoHora)
        gaveta.fechar() // abriu um app pela gaveta: ao voltar, cai na tela inicial
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        if (precisaRecarregar) carregarApps()
        // Volta das configuracoes depois de liberar o acesso -> ja conecta
        if (!musica.conectar()) mostrarMusica(null)
        player.mudo = musica.estaMudo() // pode ter mudado pelo volante / outro app
    }

    override fun onDestroy() {
        musica.desconectar()
        unregisterReceiver(avisoApps)
        super.onDestroy()
    }

    private fun mostrarMusica(estado: EstadoMusica?) {
        val aviso = when {
            Build.VERSION.SDK_INT < 21 -> "Nome da música"
            !musica.temAcesso() -> "Toque para mostrar a música"
            else -> "Nenhuma música tocando"
        }
        player.mostrar(estado, aviso)
    }

    /** Busca os apps em segundo plano e distribui nos 6 botoes. */
    private fun carregarApps() {
        precisaRecarregar = false
        Imagens.emSegundoPlano({ Apps.todos(applicationContext) }) { lista ->
            todosApps = lista
            distribuirApps()
            editor.atualizarLista()
            gaveta.atualizarLista()
        }
    }

    /** Coloca o app de cada botao (escolhido no lapis ou automatico). */
    private fun distribuirApps() {
        apps = Apps.dosBotoes(this, todosApps)
        botoes.forEachIndexed { i, b ->
            val app = apps.getOrNull(i)
            b.texto = app?.nome ?: "Texto"
            b.apagado = app == null
        }
    }

    /** Lapis ou gaveta aberto: esconde as pecas da tela (menos o papel de parede, que fica atras do veu). */
    private fun cobrirTela(cobre: Boolean) {
        val vis = if (cobre) View.INVISIBLE else View.VISIBLE
        for (i in 0 until tela.childCount) {
            val v = tela.getChildAt(i)
            if (v !== fundo) v.visibility = vis
        }
    }

    /** Troca o papel de parede e tinge o vidro e o relogio com a cor dele. */
    private fun aplicarTema(t: Tema) {
        fundo.mostrar(t)
        Vidro.tingir(t.destaque)
        relogio.tingir(t.destaque)
        for (i in 0 until tela.childCount) tela.getChildAt(i).invalidate()
    }

    private fun abrirAjuste(acao: String) {
        try {
            startActivity(Intent(acao).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: Exception) {
            Toast.makeText(this, "Nao disponivel nesta central", Toast.LENGTH_SHORT).show()
        }
    }

    private fun emBreve() = Toast.makeText(this, "Em breve", Toast.LENGTH_SHORT).show()

    /** Tela cheia: esconde as barras do Android (quando a versao permite). */
    override fun onWindowFocusChanged(temFoco: Boolean) {
        super.onWindowFocusChanged(temFoco)
        if (temFoco && Build.VERSION.SDK_INT >= 19) {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
        }
    }

    /** Botao Home com a tela inicial ja aberta: fecha o que estiver por cima. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        gaveta.fechar()
        editor.fechar()
    }

    /** Botao Voltar NAO sai da tela inicial. */
    @Deprecated("Mantido de proposito: funciona em todas as versoes")
    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        // nao sai da tela inicial; so fecha a gaveta ou o painel do lapis
        if (gaveta.aberto) gaveta.fechar()
        else if (editor.aberto) editor.voltar()
    }
}
