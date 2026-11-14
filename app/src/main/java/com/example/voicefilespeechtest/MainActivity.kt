
package com.example.voicefilespeechtest

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.mlkit.genai.common.DownloadStatus
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.common.audio.AudioSource
import com.google.mlkit.genai.speechrecognition.SpeechRecognition
import com.google.mlkit.genai.speechrecognition.SpeechRecognizer
import com.google.mlkit.genai.speechrecognition.SpeechRecognizerOptions
import com.google.mlkit.genai.speechrecognition.SpeechRecognizerResponse
import com.google.mlkit.genai.speechrecognition.speechRecognizerOptions
import com.google.mlkit.genai.speechrecognition.speechRecognizerRequest
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

class MainActivity : ComponentActivity() {

    private lateinit var status: TextView
    private lateinit var result: TextView
    private lateinit var modeGroup: RadioGroup
    private lateinit var recordButton: Button

    private var recorder: MediaRecorder? = null
    private var player: MediaPlayer? = null
    private var audioFile: File? = null
    private var recording = false

    private val permissionRequest =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                startRecording()
            } else {
                setStatus("마이크 권한이 필요합니다.")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        audioFile = File(filesDir, "speech_test.m4a")

        buildScreen()
        setStatus("준비됨. 영어 문장을 한 번 말해 보세요.")
    }

    private fun buildScreen() {

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 32, 28, 24)
        }

        content.addView(TextView(this).apply {
            text = "무료 음성 파일 인식 테스트"
            textSize = 23f
            setPadding(0, 0, 0, 18)
        })

        content.addView(TextView(this).apply {
            text = "인식 모드"
            textSize = 16f
        })

        modeGroup = RadioGroup(this).apply {
            orientation = RadioGroup.VERTICAL
        }

        modeGroup.addView(RadioButton(this).apply {
            id = 1
            text = "기본 모드 (기기 내 음성인식)"
            isChecked = true
        })

        modeGroup.addView(RadioButton(this).apply {
            id = 2
            text = "고급 모드 (지원 기기에서만)"
        })

        content.addView(modeGroup)

        recordButton = Button(this).apply {
            text = "녹음 시작"

            setOnClickListener {
                if (recording) {
                    stopRecording()
                } else if (
                    ContextCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.RECORD_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    startRecording()
                } else {
                    permissionRequest.launch(
                        Manifest.permission.RECORD_AUDIO
                    )
                }
            }
        }

        content.addView(recordButton)

        content.addView(Button(this).apply {
            text = "녹음 재생"
            setOnClickListener {
                playRecording()
            }
        })

        content.addView(Button(this).apply {
            text = "저장된 녹음 파일 인식"
            setOnClickListener {
                recognizeFile()
            }
        })

        status = TextView(this).apply {
            textSize = 15f
            setPadding(0, 18, 0, 12)
        }

        content.addView(status)

        result = TextView(this).apply {
            textSize = 19f
            setPadding(0, 8, 0, 20)
            text = "인식 결과가 여기에 표시됩니다."
        }

        content.addView(result)

        val scroll = ScrollView(this)
        scroll.addView(content)

        setContentView(scroll)
    }

    private fun setStatus(message: String) {
        runOnUiThread {
            status.text = message
        }
    }

    private fun startRecording() {

        try {
            val file = audioFile
                ?: File(filesDir, "speech_test.m4a").also {
                    audioFile = it
                }

            if (file.exists()) {
                file.delete()
            }

            val newRecorder = MediaRecorder()

            newRecorder.setAudioSource(
                MediaRecorder.AudioSource.MIC
            )

            newRecorder.setOutputFormat(
                MediaRecorder.OutputFormat.MPEG_4
            )

            newRecorder.setAudioEncoder(
                MediaRecorder.AudioEncoder.AAC
            )

            newRecorder.setAudioSamplingRate(44100)
            newRecorder.setAudioEncodingBitRate(96000)
            newRecorder.setOutputFile(file.absolutePath)

            newRecorder.prepare()
            newRecorder.start()

            recorder = newRecorder
            recording = true

            recordButton.text = "녹음 종료"

            setStatus(
                "녹음 중… 영어 문장을 한 번 말한 뒤 녹음 종료를 누르세요."
            )

        } catch (e: Exception) {

            recording = false

            try {
                recorder?.release()
            } catch (_: Exception) {
            }

            recorder = null
            recordButton.text = "녹음 시작"

            setStatus("녹음 시작 실패: ${e.message}")
        }
    }

    private fun stopRecording() {

        try {
            recorder?.stop()
            recorder?.reset()
            recorder?.release()

            recorder = null
            recording = false

            recordButton.text = "녹음 시작"

            val size = audioFile?.length() ?: 0L

            setStatus(
                "녹음 저장 완료 (${size} bytes). 이제 재생하거나 인식할 수 있어요."
            )

        } catch (e: Exception) {

            try {
                recorder?.release()
            } catch (_: Exception) {
            }

            recorder = null
            recording = false

            recordButton.text = "녹음 시작"

            setStatus("녹음 저장 실패: ${e.message}")
        }
    }

    private fun playRecording() {

        val file = audioFile

        if (file == null || !file.exists() || file.length() == 0L) {
            setStatus("재생할 녹음 파일이 없습니다.")
            return
        }

        try {
            player?.release()

            player = MediaPlayer().apply {
                setDataSource(file.absolutePath)

                setOnCompletionListener {
                    it.release()
                    player = null
                }

                prepare()
                start()
            }

            setStatus("녹음 재생 중…")

        } catch (e: Exception) {
            setStatus("재생 실패: ${e.message}")
        }
    }

    private fun recognizeFile() {

        val file = audioFile

        if (file == null || !file.exists() || file.length() == 0L) {
            setStatus("먼저 영어 문장을 녹음해 주세요.")
            return
        }

        val mode =
            if (modeGroup.checkedRadioButtonId == 2) {
                SpeechRecognizerOptions.Mode.MODE_ADVANCED
            } else {
                SpeechRecognizerOptions.Mode.MODE_BASIC
            }

        lifecycleScope.launch {

            var recognizer: SpeechRecognizer? = null

            try {
                result.text = "인식 준비 중…"
                setStatus("기기 내 영어 음성인식 모델 상태 확인 중…")

                val options = speechRecognizerOptions {
                    locale = Locale.US
                    preferredMode = mode
                }

                recognizer = SpeechRecognition.getClient(options)

                var modelStatus = recognizer.checkStatus()

                if (modelStatus == FeatureStatus.DOWNLOADABLE) {

                    setStatus(
                        "음성인식 모델 다운로드 중… 최초 1회 인터넷 연결이 필요할 수 있어요."
                    )

                    recognizer.download().collect { downloadStatus ->

                        when (downloadStatus) {

                            is DownloadStatus.DownloadStarted -> {
                                setStatus("모델 다운로드 시작…")
                            }

                            is DownloadStatus.DownloadProgress -> {
                                setStatus(
                                    "모델 다운로드 중… ${downloadStatus.totalBytesDownloaded} bytes"
                                )
                            }

                            is DownloadStatus.DownloadCompleted -> {
                                setStatus("모델 다운로드 완료. 인식 준비 중…")
                            }

                            is DownloadStatus.DownloadFailed -> {
                                setStatus(
                                    "모델 다운로드 실패: ${downloadStatus.e.message}"
                                )
                            }
                        }
                    }

                    modelStatus = recognizer.checkStatus()
                }

                if (modelStatus != FeatureStatus.AVAILABLE) {

                    val message = when (modelStatus) {
                        FeatureStatus.UNAVAILABLE ->
                            "이 기기에서 선택한 음성인식 모드를 사용할 수 없습니다."

                        FeatureStatus.DOWNLOADING ->
                            "모델이 아직 다운로드 중입니다. 잠시 후 다시 시도해 주세요."

                        FeatureStatus.DOWNLOADABLE ->
                            "음성인식 모델을 아직 사용할 수 없습니다."

                        else ->
                            "모델 준비 상태를 확인할 수 없습니다. 상태 코드: $modelStatus"
                    }

                    setStatus(message)
                    result.text = "인식 결과 없음"
                    return@launch
                }

                setStatus("녹음 파일을 기기에서 분석 중…")

                val recognized = StringBuilder()

                AudioSource.fromFile(file).use { source ->

                    val request = speechRecognizerRequest {
                        audioSource = source
                    }

                    recognizer.startRecognition(request).collect { response ->

                        when (response) {

                            is SpeechRecognizerResponse.PartialTextResponse -> {
                                result.text = response.text
                            }

                            is SpeechRecognizerResponse.FinalTextResponse -> {

                                if (recognized.isNotEmpty()) {
                                    recognized.append(" ")
                                }

                                recognized.append(response.text)

                                result.text = recognized.toString()
                            }

                            is SpeechRecognizerResponse.ErrorResponse -> {
                                setStatus(
                                    "음성인식 오류: ${response.e.message}"
                                )
                            }

                            is SpeechRecognizerResponse.CompletedResponse -> {

                                if (recognized.isEmpty()) {
                                    setStatus(
                                        "인식된 문장이 없습니다. 더 또렷하고 길게 말해 다시 시도해 주세요."
                                    )
                                } else {
                                    setStatus(
                                        "인식 완료. 녹음 파일은 기기 안에 저장되어 있습니다."
                                    )
                                }
                            }

                            else -> Unit
                        }
                    }
                }

            } catch (e: Exception) {

                setStatus(
                    "인식 실행 실패: ${e.javaClass.simpleName}: ${e.message}"
                )

                result.text = "인식 결과 없음"

            } finally {

                try {
                    recognizer?.close()
                } catch (_: Exception) {
                }
            }
        }
    }

    override fun onDestroy() {

        try {
            recorder?.release()
        } catch (_: Exception) {
        }

        try {
            player?.release()
        } catch (_: Exception) {
        }

        super.onDestroy()
    }
}
