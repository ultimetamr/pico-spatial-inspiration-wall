package com.spatialapps.inspirationwall.platform

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.content.ContextCompat

class SpeechInput(private val context: Context) {
    private var recognizer: SpeechRecognizer? = null

    fun start(onPartial: (String) -> Unit, onResult: (String) -> Unit, onError: (String) -> Unit) {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onError("当前系统未提供离线语音识别，请使用键盘输入")
            return
        }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            (context as? Activity)?.requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), REQUEST_CODE)
            onError("已请求麦克风权限，授权后请再次点击")
            return
        }
        recognizer?.destroy()
        recognizer = SpeechRecognizer.createOnDeviceSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) = Unit
                override fun onBeginningOfSpeech() = Unit
                override fun onRmsChanged(rmsdB: Float) = Unit
                override fun onBufferReceived(buffer: ByteArray?) = Unit
                override fun onEndOfSpeech() = Unit
                override fun onError(error: Int) = onError("语音识别未完成（$error），可继续键盘输入")
                override fun onResults(results: Bundle?) {
                    val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                    if (text.isNullOrBlank()) onError("没有识别到内容") else onResult(text)
                }
                override fun onPartialResults(partialResults: Bundle?) {
                    partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()?.let(onPartial)
                }
                override fun onEvent(eventType: Int, params: Bundle?) = Unit
            })
            startListening(
                Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-CN")
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
                },
            )
        }
    }

    fun release() {
        recognizer?.destroy()
        recognizer = null
    }

    companion object { private const val REQUEST_CODE = 904 }
}
