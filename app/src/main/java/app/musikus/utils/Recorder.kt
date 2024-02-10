/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
 */

package app.musikus.utils

import android.content.ContentValues
import android.content.Context
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.FileDescriptor
import java.io.IOException
import java.time.ZonedDateTime

class Recorder(
    private val context: Context,
    private val timeProvider: TimeProvider
) {

    private var isRecording = false
    private var recordingUri: Uri? = null
    private var mediaRecorder: MediaRecorder? = null

    fun start(recordingName: String) : Result<Unit> {
        if(isRecording) {
            throw IllegalStateException("Recorder is already recording")
        }

        val startTime = timeProvider.now()

        val displayName = "${recordingName}_${startTime.musikusFormat(DateFormat.RECORDING)}"

        // initialize the content values for the new recording
        val contentValues = getContentValues(startTime, displayName)

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
                File(recordingDirectory, displayName).absolutePath
            )
        }


        // create the uri for the new recording from the current time and the provided display name
        val contentUri = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        context.contentResolver.insert(contentUri, contentValues)?.let { uri ->
            recordingUri = uri
            context.contentResolver.openFileDescriptor(uri, "w")?.use {
                initializeMediaRecorder(it.fileDescriptor)
            }
        } ?: throw IOException("Couldn't create MediaStore entry")

        mediaRecorder?.start() ?: throw IOException("Tried to start recording without initializing mediaRecorder")
        isRecording = true
        return Result.success(Unit)
    }


    fun stop() : Result<Unit> {
        try {
            if(!isRecording) {
                throw IllegalStateException("Recorder is not recording")
            }

            mediaRecorder?.apply {
                stop()
                release()
            } ?: throw  IllegalStateException("Tried to stop recording without initializing mediaRecorder")
            isRecording = false

            // finally update the recordings content values to mark it as no longer pending
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues().apply {
                    put(MediaStore.MediaColumns.IS_PENDING, false)
                    context.contentResolver.update(
                        recordingUri ?: throw IllegalStateException("Recording URI is null"),
                        this,
                        null,
                        null
                    )
                }
            }

            recordingUri = null
            mediaRecorder = null

        } catch (throwable: Throwable) {
            return Result.failure(throwable)
        }
        return Result.success(Unit)
    }

    /**
     * --------------- Private methods ---------------
     */

    private fun getContentValues(time: ZonedDateTime, displayName: String): ContentValues {
        return ContentValues().apply {

            put(MediaStore.Audio.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Audio.Media.TITLE, displayName)
            put(MediaStore.Audio.Media.MIME_TYPE, "audio/mp4")
            put(MediaStore.Audio.Media.ALBUM, "Musikus")
            put(MediaStore.Audio.Media.DATE_ADDED, time.toString())
            put(MediaStore.Audio.Media.DATE_MODIFIED, time.toString())
            put(MediaStore.Audio.Media.IS_MUSIC, 1)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Music/Musikus")
                put(MediaStore.MediaColumns.IS_PENDING, true)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                put(MediaStore.Audio.Media.GENRE, "Recording")
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                put(MediaStore.Audio.Media.IS_RECORDING, 1)
            }
        }
    }

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