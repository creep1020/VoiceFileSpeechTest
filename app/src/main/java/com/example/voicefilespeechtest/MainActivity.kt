
package com.example.voicefilespeechtest

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.Spinner
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
    private lateinit var comparison: TextView
    private lateinit var referenceInput: EditText
    private lateinit var modeGroup: RadioGroup
    private lateinit var recordButton: Button
    private lateinit var exampleSpinner: Spinner

    private var recorder: MediaRecorder? = null
    private var player: MediaPlayer? = null
    private var audioFile: File? = null
    private var recording = false

    private val examples = listOf(
        "How are you doing today?",
        "What do you want to eat?",
        "I have to go to school.",
        "Would you like some coffee?",
        "I've been waiting for you."
    )

    private val permissionRequest =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
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
        setStatus("예문을 선택하고 녹음을 시작하세요.")
    }

    private fun buildScreen() {

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 32, 28, 24)
        }

        content.addView(TextView(this).apply {
            text = "영어 발음 자동 테스트"
            textSize = 24f
            setPadding(0, 0, 0, 16)
        })

        content.addView(TextView(this).apply {
            text = "① 예문 선택"
            textSize = 17f
        })

        referenceInput = EditText(this).apply {
            hint = "영어 문장을 직접 입력할 수도 있습니다."
            textSize = 18f
            minLines = 2
            setPadding(12, 12, 12, 12)
        }

        exampleSpinner = Spinner(this)

        val exampleLabels = examples.mapIndexed { index, sentence ->
            "${index + 1}. $sentence"
        }

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            exampleLabels
        ).apply {
            setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
            )
        }

        exampleSpinner.adapter = adapter

        exampleSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {

                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    referenceInput.setText(examples[position])
                    comparison.text = "새 예문을 선택했습니다."
                    result.text = "아직 인식 결과가 없습니다."
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                }
            }

        content.addView(exampleSpinner)
        content.addView(referenceInput)

        content.addView(TextView(this).apply {
            text = "② 인식 모드"
            textSize = 16f
            setPadding(0, 16, 0, 4)
        })

        modeGroup = RadioGroup(this).apply {
            orientation = RadioGroup.VERTICAL
        }

        modeGroup.addView(RadioButton(this).apply {
            id = 1
            text = "기본 모드"
            isChecked = true
        })

        modeGroup.addView(RadioButton(this).apply {
            id = 2
            text = "고급 모드"
        })

        content.addView(modeGroup)

        recordButton = Button(this).apply {
            text = "③ 녹음 시작"

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
            text = "저장된 파일 다시 인식"
            setOnClickListener {
                recognizeFile()
            }
        })

        status = TextView(this).apply {
            textSize = 15f
            setPadding(0, 18, 0, 10)
        }

        content.addView(status)

        content.addView(TextView(this).apply {
            text = "③ 음성인식 결과"
            textSize = 17f
            setPadding(0, 12, 0, 4)
        })

        result = TextView(this).apply {
            textSize = 19f
            setPadding(0, 8, 0, 12)
            text = "아직 인식 결과가 없습니다."
        }

        content.addView(result)

        content.addView(TextView(this).apply {
            text = "④ 단어별 비교 결과"
            textSize = 17f
            setPadding(0, 12, 0, 4)
        })

        comparison = TextView(this).apply {
            textSize = 17f
            setPadding(0, 8, 0, 20)
            text = "녹음 후 자동으로 비교합니다."
        }

        content.addView(comparison)

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

        val reference = referenceInput.text.toString().trim()

        if (reference.isEmpty()) {
            setStatus("먼저 기준 영어 문장을 입력하세요.")
            return
        }

        try {
            val file = audioFile
                ?: File(filesDir, "speech_test.m4a").also {
                    audioFile = it
                }

            if (file.exists()) {
                file.delete()
            }

            result.text = "녹음 후 자동으로 인식합니다."
            comparison.text = "비교 대기 중…"

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

            setStatus("녹음 중… 문장을 말한 뒤 종료하세요.")

        } catch (e: Exception) {

            try {
                recorder?.release()
            } catch (_: Exception) {
            }

            recorder = null
            recording = false
            recordButton.text = "③ 녹음 시작"

            setStatus("녹음 시작 실패: ${e.message}")
        }
    }

    private fun stopRecording() {

        try {
            recorder?.stop()
            recorder?.release()

            recorder = null
            recording = false

            recordButton.text = "③ 녹음 시작"

            val size = audioFile?.length() ?: 0L

            if (size == 0L) {
                setStatus("녹음 파일이 비어 있습니다.")
                return
            }

            setStatus("녹음 저장 완료. 자동 인식 중…")

            recognizeFile()

        } catch (e: Exception) {

            try {
                recorder?.release()
            } catch (_: Exception) {
            }

            recorder = null
            recording = false

            recordButton.text = "③ 녹음 시작"

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

    private fun normalize(text: String): List<String> {
        return text.lowercase(Locale.US)
            .replace(Regex("[^a-z0-9']"), " ")
            .trim()
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
    }

    private fun compareSentences(
        reference: String,
        spoken: String
    ): String {

        val expected = normalize(reference)
        val actual = normalize(spoken)

        if (expected.isEmpty() || actual.isEmpty()) {
            return "비교할 문장이 충분하지 않습니다."
        }

        val n = expected.size
        val m = actual.size

        val dp = Array(n + 1) {
            IntArray(m + 1)
        }

        for (i in 0..n) {
            dp[i][0] = i
        }

        for (j in 0..m) {
            dp[0][j] = j
        }

        for (i in 1..n) {
            for (j in 1..m) {

                val cost =
                    if (expected[i - 1] == actual[j - 1]) 0 else 1

                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,
                    dp[i][j - 1] + 1,
                    dp[i - 1][j - 1] + cost
                )
            }
        }

        val distance = dp[n][m]

        val score = (
            (1.0 - distance.toDouble() / maxOf(n, m)) * 100
        ).toInt().coerceIn(0, 100)

        val details = mutableListOf<String>()

        var i = n
        var j = m

        while (i > 0 || j > 0) {

            if (
                i > 0 &&
                j > 0 &&
                expected[i - 1] == actual[j - 1]
            ) {
                details.add(
                    "✓  ${expected[i - 1]}  →  ${actual[j - 1]}  일치"
                )
                i--
                j--

            } else if (
                i > 0 &&
                j > 0 &&
                dp[i][j] == dp[i - 1][j - 1] + 1
            ) {
                details.add(
                    "△  ${expected[i - 1]}  →  ${actual[j - 1]}  다른 단어"
                )
                i--
                j--

            } else if (
                i > 0 &&
                dp[i][j] == dp[i - 1][j] + 1
            ) {
                details.add(
                    "✕  ${expected[i - 1]}  →  누락"
                )
                i--

            } else {
                details.add(
                    "+  추가 단어: ${actual[j - 1]}"
                )
                j--
            }
        }

        details.reverse()

        val heading = if (distance == 0) {
            "완벽하게 일치합니다! 🎉"
        } else {
            "다시 읽어 보세요."
        }

        return buildString {
            appendLine(heading)
            appendLine()
            appendLine("문장 일치도: ${score}%")
            appendLine("기준 단어: ${n}개 / 인식 단어: ${m}개")
            appendLine()
            appendLine("단어별 결과")
            append(details.joinToString("\n"))
        }
    }

    private fun recognizeFile() {

        val file = audioFile

        if (file == null || !file.exists() || file.length() == 0L) {
            setStatus("먼저 영어 문장을 녹음해 주세요.")
            return
        }

        val reference = referenceInput.text.toString().trim()

        if (reference.isEmpty()) {
            setStatus("비교할 기준 문장을 입력하세요.")
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
                result.text = "음성인식 중…"
                comparison.text = "기준 문장과 비교 중…"

                setStatus("기기 내 음성인식 모델 확인 중…")

                val options = speechRecognizerOptions {
                    locale = Locale.US
                    preferredMode = mode
                }

                recognizer = SpeechRecognition.getClient(options)

                var modelStatus = recognizer.checkStatus()

                if (modelStatus == FeatureStatus.DOWNLOADABLE) {

                    setStatus("음성인식 모델 다운로드 중…")

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
                                setStatus("모델 다운로드 완료.")
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
                    setStatus(
                        "음성인식 모델을 사용할 수 없습니다. 상태: $modelStatus"
                    )
                    return@launch
                }

                setStatus("녹음 파일 분석 중…")

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

                                val spoken = recognized.toString().trim()

                                if (spoken.isEmpty()) {
                                    result.text = "인식된 문장이 없습니다."
                                    comparison.text = "비교할 수 없습니다."
                                    setStatus("음성인식 결과가 비어 있습니다.")
                                } else {
                                    result.text = spoken

                                    comparison.text =
                                        compareSentences(reference, spoken)

                                    setStatus("음성인식 및 단어 비교 완료!")
                                }
                            }

                            else -> Unit
                        }
                    }
                }

            } catch (e: Exception) {

                setStatus(
                    "인식 실패: ${e.javaClass.simpleName}: ${e.message}"
                )

                result.text = "인식 결과 없음"
                comparison.text = "비교하지 못했습니다."

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
