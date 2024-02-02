/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
 */

package app.musikus.utils

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import java.io.FileDescriptor
import java.util.Date

class Recorder(
    private val context: Context
) {

    private var isRecording = false
    private var recordingStartTime: Long? = null

    private lateinit var mediaRecorder: MediaRecorder

//    fun startRecording(uri: Uri) {
//        try {
//            contentResolver.openFileDescriptor(uri,"w")?.fileDescriptor
//        } catch (e: Exception) {
//            null
//        }
//
//        if(fileDescriptor != null) {
//            startRecording(fileDescriptor)
//
//        }
//        else
//            Log.d("REC_SERVICE", "onStart: No valid file descriptor passed")
//
//    }

    @Suppress("DEPRECATION")
    private fun start(outputFile: FileDescriptor) {
        mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            MediaRecorder()
        }

        mediaRecorder.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.HE_AAC)
            setAudioChannels(2)
            setAudioEncodingBitRate(256_000)
            setAudioSamplingRate(44_100)
            setOutputFile(outputFile)
            prepare()
            start()
        }
        isRecording = true
        recordingStartTime = Date().time
    }


    fun stop() {
        Log.d("RecService", "Stop reocrding")
        if(isRecording) {
            mediaRecorder.apply {
                stop()
                release()
            }
            isRecording = false
            recordingStartTime = null
        }
    }
}