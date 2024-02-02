/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
 */

package app.musikus.ui.activesession.metronome

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.util.Log
import androidx.annotation.RawRes
import app.musikus.R
import app.musikus.di.ApplicationScope
import app.musikus.di.IoScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.min

class MetronomePlayer(
    @ApplicationScope private val scope: CoroutineScope,
    @IoScope private val ioScope: CoroutineScope,
    context: Context
) {

    private var playerJob : Job? = null

    private val highNote by lazy { createNote(R.raw.beat_1, context) }
    private val mediumNote by lazy { createNote(R.raw.beat_2, context) }
    private val lowNote by lazy { createNote(R.raw.beat_3, context) }

    init {
//        ioScope.launch {
//            highNote
//            mediumNote
//            lowNote
//        }
    }

    fun play() {
        playerJob = createPlayerJob()
    }

    fun stop() {
        playerJob?.cancel()
    }

    private fun createPlayerJob() : Job {
        return scope.launch {
            val track = createTrack()

            val bufferSize = track.bufferSizeInFrames / 4
            val buffer = FloatArray(bufferSize)

            Log.d("MetronomePlayer", "bufferSize: ${bufferSize}")
            Log.d("MetronomePlayer", "noteSize: ${highNote.size}")



            track.play()

            /**
             * The loop playing the metronome
             */

            while (true) {
                if(!isActive) {
                    break
                }

                buffer.fill(0f)

                for (i in 0 until min(bufferSize, highNote.size)) {
                    buffer[i] += highNote[i]
                }

                // write() is a blocking call
                track.write(buffer, 0, bufferSize, AudioTrack.WRITE_BLOCKING)
            }

            track.stop()
            track.flush()
            track.release()
        }
    }
    companion object {
        fun createTrack() : AudioTrack {
            val nativeSampleRate = AudioTrack.getNativeOutputSampleRate(AudioManager.STREAM_MUSIC)
            val bufferSize = 5 * AudioTrack.getMinBufferSize(
                nativeSampleRate, AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_FLOAT
//                AudioFormat.ENCODING_PCM_16BIT
            )

            return AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
//                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .setSampleRate(nativeSampleRate)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
//                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
        }

        // source: https://github.com/thetwom/toc2/blob/master/app/src/main/java/de/moekadu/metronome/audio/AudioDecoder.kt
        // TODO sort copyright
        fun createNote(@RawRes noteId: Int, context: Context) : FloatArray {
            val inputStream = context.resources.openRawResource(noteId)
            val outputArray = ArrayList<Float>()

            inputStream.skip(44)
            val buffer = ByteArray(1024)
            val byteBuffer = ByteBuffer.wrap(buffer)
            byteBuffer.order(ByteOrder.LITTLE_ENDIAN)

            var numRead : Int? = null
            while(numRead == null || numRead > 0) {
                numRead = inputStream.read(buffer)
                for(i in 0 until numRead step 2) {
                    outputArray.add(byteBuffer.getShort(i).toFloat() / 32768.0f)
                }
            }
            return outputArray.toFloatArray()
        }
    }
}