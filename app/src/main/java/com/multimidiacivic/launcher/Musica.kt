package com.multimidiacivic.launcher

import android.annotation.TargetApi
import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.view.KeyEvent

/** O que esta tocando agora (vem do app de musica: Spotify, YouTube Music, radio...). */
class EstadoMusica(
    val titulo: String?,
    val artista: String?,
    val tocando: Boolean,
    val posicao: Long,      // ms
    val duracao: Long,      // ms (0 = desconhecida)
    val atualizadoEm: Long, // SystemClock.elapsedRealtime() da ultima posicao
    val velocidade: Float,
)

/**
 * Controla a musica de OUTROS apps.
 *
 * - Play/Pause funcionam sempre (simulam os botoes de midia).
 * - Nome da musica e barra de progresso precisam que o usuario libere
 *   "Acesso a notificacoes" para este app UMA vez (Android 5 ou mais novo).
 *   Sem isso o Android nao deixa um app ver o que o outro esta tocando.
 */
class Musica(private val ctx: Context, private val aoMudar: (EstadoMusica?) -> Unit) {

    private val sessoes: Any? = if (Build.VERSION.SDK_INT >= 21) Sessoes21(ctx, aoMudar) else null

    fun temAcesso(): Boolean {
        if (Build.VERSION.SDK_INT < 21) return false
        val lista = Settings.Secure.getString(ctx.contentResolver, "enabled_notification_listeners") ?: return false
        return lista.contains(ctx.packageName)
    }

    /** Liga a leitura do player. Retorna false se ainda falta a permissao. */
    fun conectar(): Boolean {
        if (Build.VERSION.SDK_INT < 21 || !temAcesso()) return false
        return (sessoes as Sessoes21).iniciar()
    }

    fun desconectar() {
        if (Build.VERSION.SDK_INT >= 21) (sessoes as Sessoes21).parar()
    }

    fun pedirAcesso(a: Activity) {
        try {
            a.startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
        } catch (e: Exception) {
            try { a.startActivity(Intent(Settings.ACTION_SETTINGS)) } catch (_: Exception) {}
        }
    }

    fun tocar() {
        if (Build.VERSION.SDK_INT >= 21 && (sessoes as Sessoes21).tocar()) return
        teclaMidia(KeyEvent.KEYCODE_MEDIA_PLAY)
    }

    fun pausar() {
        if (Build.VERSION.SDK_INT >= 21 && (sessoes as Sessoes21).pausar()) return
        teclaMidia(KeyEvent.KEYCODE_MEDIA_PAUSE)
    }

    fun irPara(ms: Long) {
        if (Build.VERSION.SDK_INT >= 21) (sessoes as Sessoes21).irPara(ms)
    }

    fun proxima() {
        if (Build.VERSION.SDK_INT >= 21 && (sessoes as Sessoes21).proxima()) return
        teclaMidia(KeyEvent.KEYCODE_MEDIA_NEXT)
    }

    fun anterior() {
        if (Build.VERSION.SDK_INT >= 21 && (sessoes as Sessoes21).anterior()) return
        teclaMidia(KeyEvent.KEYCODE_MEDIA_PREVIOUS)
    }

    // ---- Mudo (som de midia) ----
    private val audio = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var mudoLocal = false // Android < 6 nao informa se esta mudo

    fun estaMudo(): Boolean =
        if (Build.VERSION.SDK_INT >= 23) audio.isStreamMute(AudioManager.STREAM_MUSIC) else mudoLocal

    /** Liga/desliga o mudo e devolve o estado novo. */
    fun alternarMudo(): Boolean {
        val mutar = !estaMudo()
        if (Build.VERSION.SDK_INT >= 23) {
            audio.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                if (mutar) AudioManager.ADJUST_MUTE else AudioManager.ADJUST_UNMUTE, 0)
        } else {
            @Suppress("DEPRECATION")
            audio.setStreamMute(AudioManager.STREAM_MUSIC, mutar)
        }
        mudoLocal = mutar
        return mutar
    }

    /** Simula o botao de midia (como o do volante / fone bluetooth). */
    private fun teclaMidia(codigo: Int) {
        val agora = SystemClock.uptimeMillis()
        val desce = KeyEvent(agora, agora, KeyEvent.ACTION_DOWN, codigo, 0)
        val sobe = KeyEvent(agora, agora, KeyEvent.ACTION_UP, codigo, 0)
        if (Build.VERSION.SDK_INT >= 19) {
            audio.dispatchMediaKeyEvent(desce)
            audio.dispatchMediaKeyEvent(sobe)
        } else {
            for (ev in arrayOf(desce, sobe)) {
                ctx.sendOrderedBroadcast(Intent(Intent.ACTION_MEDIA_BUTTON).putExtra(Intent.EXTRA_KEY_EVENT, ev), null)
            }
        }
    }
}

/** Parte que so existe no Android 5+ (fica separada para nao quebrar no Android 4). */
@TargetApi(21)
private class Sessoes21(ctx: Context, private val aoMudar: (EstadoMusica?) -> Unit) {

    private val gerente = ctx.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
    private val componente = ComponentName(ctx, OuvinteNotificacoes::class.java)
    private val principal = Handler(Looper.getMainLooper())
    private var atual: MediaController? = null
    private var ligado = false

    private val retorno = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) = publicar()
        override fun onMetadataChanged(metadata: MediaMetadata?) = publicar()
        override fun onSessionDestroyed() = trocar(null)
    }

    private val ouvinte = MediaSessionManager.OnActiveSessionsChangedListener { escolher(it) }

    fun iniciar(): Boolean {
        if (ligado) return true
        return try {
            gerente.addOnActiveSessionsChangedListener(ouvinte, componente, principal)
            ligado = true
            escolher(gerente.getActiveSessions(componente))
            true
        } catch (e: SecurityException) {
            false
        }
    }

    fun parar() {
        if (!ligado) return
        try { gerente.removeOnActiveSessionsChangedListener(ouvinte) } catch (_: Exception) {}
        ligado = false
        trocar(null)
    }

    /** Prefere o app que esta TOCANDO; senao, o ultimo usado. */
    private fun escolher(lista: List<MediaController>?) {
        val c = lista?.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING } ?: lista?.firstOrNull()
        if (c?.sessionToken == atual?.sessionToken) publicar() else trocar(c)
    }

    private fun trocar(c: MediaController?) {
        atual?.unregisterCallback(retorno)
        atual = c
        c?.registerCallback(retorno, principal)
        publicar()
    }

    private fun publicar() {
        val c = atual ?: return aoMudar(null)
        val m = c.metadata
        val s = c.playbackState
        aoMudar(
            EstadoMusica(
                titulo = m?.getString(MediaMetadata.METADATA_KEY_TITLE),
                artista = m?.getString(MediaMetadata.METADATA_KEY_ARTIST),
                tocando = s?.state == PlaybackState.STATE_PLAYING,
                posicao = s?.position ?: 0L,
                duracao = m?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L,
                atualizadoEm = s?.lastPositionUpdateTime ?: SystemClock.elapsedRealtime(),
                velocidade = s?.playbackSpeed ?: 1f,
            )
        )
    }

    fun tocar(): Boolean = atual?.transportControls?.play()?.let { true } ?: false
    fun pausar(): Boolean = atual?.transportControls?.pause()?.let { true } ?: false
    fun irPara(ms: Long) { atual?.transportControls?.seekTo(ms) }
    fun proxima(): Boolean = atual?.transportControls?.skipToNext()?.let { true } ?: false
    fun anterior(): Boolean = atual?.transportControls?.skipToPrevious()?.let { true } ?: false
}

/**
 * Servico vazio: so existe para o Android listar este app em
 * "Acesso a notificacoes". E essa permissao que libera ler o player.
 */
@TargetApi(21)
class OuvinteNotificacoes : NotificationListenerService()
