/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
 */

package app.musikus.recorder.presentation

import android.content.ContentValues
import android.content.Context
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import app.musikus.R
import app.musikus.core.domain.TimeProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import java.io.File
import java.io.FileDescriptor
import java.time.ZonedDateTime

enum class RecorderState {
    UNINITIALIZED, IDLE, RECORDING, PAUSED
}

class IllegalRecorderStateException(override val message: String) : IllegalStateException()

class Recorder(
    private val context: Context,
    private val timeProvider: TimeProvider
) {

    var state = MutableStateFlow(RecorderState.IDLE)
    private var recordingUri: Uri? = null
    private var mediaRecorder: MediaRecorder? = null

    @Throws(IllegalRecorderStateException::class)
    fun start() {
        if (state.value == RecorderState.RECORDING) {
            throw IllegalRecorderStateException(
                context.getString(R.string.recorder_illegal_state_exception_start_while_recording)
            )
        }

        if (state.value == RecorderState.PAUSED) {
            throw IllegalRecorderStateException(
                context.getString(R.string.recorder_illegal_state_exception_start_while_paused)
            )
        }

        val startTime = timeProvider.now()

        // initialize the content values for the new recording
        val contentValues = getContentValues(startTime)

        // if the build version is less than Q, manually create the directory for the new recording
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            val recordingDirectory = File(
                Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_MUSIC
                ).toString() + "/Musikus"
            )
            if (!recordingDirectory.exists()) {
                recordingDirectory.mkdirs()
            }
            contentValues.put(
                MediaStore.Audio.Media.DATA,
                File(recordingDirectory, "musikus_active_recording").absolutePath
            )
        }


        // create the uri for the new recording from the current time and the provided display name
        val contentUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        context.contentResolver.insert(contentUri, contentValues)?.let { uri ->
            recordingUri = uri
            context.contentResolver.openFileDescriptor(uri, "w")?.use {
                initializeMediaRecorder(it.fileDescriptor)
            }
        } ?: throw RecorderException.MediaStoreInsertFailed(context)

        mediaRecorder?.start()
            ?: throw IllegalRecorderStateException(
                context.getString(R.string.recorder_illegal_state_exception_start_while_uninitialized)
            )
        state.update { RecorderState.RECORDING }
    }

    @Throws(IllegalRecorderStateException::class)
    fun pause() {
        if(state.value != RecorderState.RECORDING) {
            throw IllegalRecorderStateException(
                context.getString(R.string.recorder_illegal_state_exception_pause_while_not_recording)
            )
        }
        mediaRecorder?.pause() ?: throw  IllegalRecorderStateException(
            context.getString(R.string.recorder_illegal_state_exception_pause_while_uninitialized)
        )
        state.update { RecorderState.PAUSED }
    }

    @Throws(IllegalRecorderStateException::class)
    fun resume() {
        if(state.value != RecorderState.PAUSED) {
            throw IllegalRecorderStateException(
                context.getString(R.string.recorder_illegal_state_exception_resume_while_not_paused)
            )
        }
        mediaRecorder?.resume() ?: throw  IllegalRecorderStateException(
            context.getString(R.string.recorder_illegal_state_exception_resume_while_uninitialized)
        )
        state.update { RecorderState.RECORDING }
    }


    @Throws(IllegalRecorderStateException::class)
    fun delete() {
        if(state.value != RecorderState.PAUSED) {
            throw IllegalRecorderStateException(
                context.getString(R.string.recorder_illegal_state_exception_delete_while_not_paused)
            )
        }
        mediaRecorder?.apply {
            stop()
            release()
        } ?: throw  IllegalRecorderStateException(
            context.getString(R.string.recorder_illegal_state_exception_delete_while_uninitialized)
        )
        recordingUri?.let {
            context.contentResolver.delete(it, null, null)
        }

        reset()
    }

    @Throws(IllegalRecorderStateException::class)
    fun save(recordingName: String) {
        if(state.value != RecorderState.PAUSED) {
            throw IllegalRecorderStateException(
                context.getString(R.string.recorder_illegal_state_exception_save_while_not_paused)
            )
        }

        if(recordingName.isBlank()) {
            throw RecorderException.SaveWithEmptyName(context)
        }

        mediaRecorder?.apply {
            stop()
            release()
        } ?: throw  IllegalRecorderStateException(
            context.getString(R.string.recorder_illegal_state_exception_save_while_uninitialized)
        )

        // finally update the recordings content values to mark it as no longer pending
        ContentValues().apply {
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.IS_PENDING, false)
            }
            put(MediaStore.MediaColumns.DISPLAY_NAME, recordingName)
            put(MediaStore.MediaColumns.TITLE, recordingName)
            context.contentResolver.update(
                recordingUri ?: throw RecorderException.MediaStoreUpdateFailed(context),
                this,
                null,
                null
            )
        }

        reset()
    }

    /**
     * --------------- Private methods ---------------
     */

    private fun reset() {
        recordingUri = null
        mediaRecorder = null
        state.update { RecorderState.IDLE }
    }

    private fun getContentValues(time: ZonedDateTime): ContentValues {
        return ContentValues().apply {

            put(MediaStore.Audio.Media.MIME_TYPE, "audio/mp4")
            put(MediaStore.Audio.Media.ALBUM, context.getString(R.string.recorder_content_values_album))
            put(MediaStore.Audio.Media.DATE_ADDED, time.toString())
            put(MediaStore.Audio.Media.DATE_MODIFIED, time.toString())
            put(MediaStore.Audio.Media.IS_MUSIC, 1)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Music/Musikus")
                put(MediaStore.MediaColumns.IS_PENDING, true)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                put(MediaStore.Audio.Media.GENRE, context.getString(R.string.recorder_content_values_genre))
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                put(MediaStore.Audio.Media.IS_RECORDING, 1)
            }
        }
    }

    // TODO check hardware compatibility with settings of MediaRecorder
    private fun initializeMediaRecorder(outputFile: FileDescriptor) {
        mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.HE_AAC)
            setAudioChannels(2)
            setAudioEncodingBitRate(192_000)
            setAudioSamplingRate(44_100)
            setOutputFile(outputFile)
            prepare()
        }
    }
}