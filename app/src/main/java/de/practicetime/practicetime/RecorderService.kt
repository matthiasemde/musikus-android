package de.practicetime.practicetime

import android.app.Service
import android.content.Intent
import android.media.MediaRecorder
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import java.io.FileDescriptor
import java.util.*
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import java.lang.Exception

class RecorderService : Service() {

    companion object {
        var recording = false
        var recordingName: String? = null
    }

    private var recordingStartTime: Long? = null
    private val recordingTimeHandler = Handler(Looper.getMainLooper())

    override fun onBind(intent: Intent?): Binder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("RecService", "Started")
        val fileDescriptor = intent?.extras?.getString("URI")?.let {
            try {
                val uri = Uri.parse(it)
                recordingName = uri.lastPathSegment?.split('/')?.last()
                contentResolver.openFileDescriptor(
                    uri,
                    "w"
                )?.fileDescriptor
            } catch (e: Exception) {
                null
            }
        }

        if(fileDescriptor != null)
            startRecording(fileDescriptor)
        else
            Log.d("REC_SERVICE", "onStart: No valid file descriptor passed")

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        stopRecording()
        Log.d("RecService", "Destroyed")
    }

    private lateinit var recorder: MediaRecorder

    private fun startRecording(outputFile: FileDescriptor) {
        recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(this)
        } else {
            MediaRecorder()
        }

        recorder.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_WB)
            setAudioChannels(2)
            setAudioEncodingBitRate(256_000)
            setAudioSamplingRate(44_100)
            setOutputFile(outputFile)
            prepare()
            start()
        }
        recording = true
        recordingStartTime = Date().time
        recordingTimeHandler.post(incrementRecordingTime)
    }

    private fun stopRecording() {
        if(recording) {
            recorder.apply {
                stop()
                release()
            }
            recording = false
            recordingName = null
            recordingStartTime = null
        }
    }

    private val incrementRecordingTime = object : Runnable {
        override fun run() {
            if(recording) {
                recordingStartTime?.also {
                    val diff = (Date().time - it).toInt()
                    val intent = Intent("RecordingDurationUpdate")
                    intent.putExtra("DURATION", diff.toString())
                    LocalBroadcastManager
                        .getInstance(this@RecorderService)
                        .sendBroadcast(intent)
                }
                recordingTimeHandler.postDelayed(this, 20L)
            }
        }
    }
}