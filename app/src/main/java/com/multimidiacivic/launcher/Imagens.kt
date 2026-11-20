package com.multimidiacivic.launcher

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import java.util.concurrent.Executors

/**
 * Utilitarios de imagem.
 *
 * Regra de ouro: NUNCA carregar a imagem original inteira na memoria.
 * Primeiro lemos so o tamanho (inJustDecodeBounds), depois decodificamos
 * ja reduzida para o tamanho da tela (inSampleSize).
 */
object Imagens {

    /** Uma unica thread de fundo para decodificar imagens (nao trava a tela). */
    private val fundo = Executors.newSingleThreadExecutor()
    private val principal = Handler(Looper.getMainLooper())

    /** Executa [tarefa] em segundo plano e entrega o resultado na thread da tela. */
    fun <T> emSegundoPlano(tarefa: () -> T, pronto: (T) -> Unit) {
        fundo.execute {
            val r = try { tarefa() } catch (e: Throwable) { null }
            if (r != null) principal.post { pronto(r) }
        }
    }

    /** Calcula o fator de reducao (potencia de 2) para caber em larg x alt. */
    fun fatorReducao(origL: Int, origA: Int, larg: Int, alt: Int): Int {
        var fator = 1
        while (origL / (fator * 2) >= larg && origA / (fator * 2) >= alt) fator *= 2
        return fator
    }

    /**
     * Decodifica um recurso (res/drawable-nodpi) ja no tamanho pedido.
     * [comTransparencia] = true para PNG com fundo transparente (ex.: carro).
     */
    fun decodificarRecurso(res: Resources, id: Int, larg: Int, alt: Int, comTransparencia: Boolean): Bitmap? {
        val op = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeResource(res, id, op)
        op.inJustDecodeBounds = false
        op.inSampleSize = fatorReducao(op.outWidth, op.outHeight, larg, alt)
        op.inScaled = false
        // RGB_565 usa METADE da memoria, mas nao tem transparencia
        op.inPreferredConfig = if (comTransparencia) Bitmap.Config.ARGB_8888 else Bitmap.Config.RGB_565
        return BitmapFactory.decodeResource(res, id, op)
    }
}
