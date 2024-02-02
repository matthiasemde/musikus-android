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
import kotlin.properties.Delegates
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class MetronomePlayer(
    @ApplicationScope private val applicationScope: CoroutineScope,
    context: Context
) {

    private var playerJob : Job? = null

    private val highNote by lazy { createNote(R.raw.beat_1, context) }
    private val mediumNote by lazy { createNote(R.raw.beat_2, context) }
    private val lowNote by lazy { createNote(R.raw.beat_3, context) }

    private var settingsInitialized = false
    private var beatsPerBar by Delegates.notNull<Int>()
    private var clicksPerBeat by Delegates.notNull<Int>()
    private var clicksPerBar by Delegates.notNull<Int>()
    private var nanosecondsBetweenClicks by Delegates.notNull<Long>()

    init {
//        ioScope.launch {
//            highNote
//            mediumNote
//            lowNote
//        }
    }

    fun updateSettings(settings: MetronomeSettings) {
        beatsPerBar = settings.beatsPerBar
        clicksPerBeat = settings.clicksPerBeat
        clicksPerBar = beatsPerBar * clicksPerBeat
        nanosecondsBetweenClicks = (1.minutes.inWholeNanoseconds / (settings.bpm * clicksPerBeat))
        settingsInitialized = true
    }

    fun play() {
        if (!settingsInitialized) return
        playerJob = createPlayerJob()
    }

    fun stop() {
        playerJob?.cancel()
    }

    private fun createPlayerJob() : Job {
        return applicationScope.launch {
            val track = createTrack()

            val nanosecondsPerFrame = 1.seconds.inWholeNanoseconds / track.sampleRate

            val bufferSize = track.bufferSizeInFrames / 4
            val buffer = FloatArray(bufferSize)

            Log.d("MetronomePlayer", "bufferSize: $bufferSize")
            Log.d("MetronomePlayer", "noteSize: ${highNote.size}")


            track.play()

            var currentClick = 1
            var framesUntilNextClick = 0
            var clickSampleFrame : Int? = null

            /**
             * The loop playing the metronome
             */

            while (true) {
                if(!isActive) {
                    break
                }

                // initialize buffer with 0 (silence)
                buffer.fill(0f)

                var nextBufferFrame = 0

                while(nextBufferFrame < bufferSize) {
                    val currentClickSample = when(currentClick) {
                        1 -> highNote
                        else -> lowNote
//                        2 -> mediumNote
//                        3 -> lowNote
                    }

                    // if there is no clickSampleFrame aka no click is being written to the
                    // buffer, initiate the clickSampleFrame and calculate the number of frames
                    if (clickSampleFrame == null) {
                        val remainingBufferFrames = bufferSize - nextBufferFrame

                        // if the number of frames until the next click is larger or equal
                        // than the remaining buffer frames, break the loop and wait for the next buffer
                        if (framesUntilNextClick >= remainingBufferFrames) {
                            framesUntilNextClick -= remainingBufferFrames
                            break
                        }

                        // otherwise, initiate clickSampleFrame to 0,
                        // advance the nextBufferFrame,
                        // and calculate the number of frames until the next click
                        clickSampleFrame = 0
                        nextBufferFrame += framesUntilNextClick
                        framesUntilNextClick = (nanosecondsBetweenClicks / nanosecondsPerFrame).toInt()

                    }

                    // calculate how many frames can be written by taking the min
                    // of the remaining frames in the clickSample
                    // and the remaining frames until either the next click starts or the buffer is full
                    val remainingClickFrames = currentClickSample.size - clickSampleFrame
                    val remainingFramesUntilNextClickOrEndOfBuffer = min(
                        framesUntilNextClick,
                        bufferSize - nextBufferFrame
                    )

                    val framesToWrite = min(
                        remainingClickFrames,
                        remainingFramesUntilNextClickOrEndOfBuffer
                    )

                    // write the frames to the buffer
                    for(i in 0 until framesToWrite) {
                        buffer[nextBufferFrame + i] = currentClickSample[clickSampleFrame + i]
                    }

                    // update the buffer frame counter
                    nextBufferFrame += framesToWrite

                    // check if the clickSample is finished
                    clickSampleFrame = if(framesToWrite < remainingClickFrames) {
                        // if it is not finished, update the clickSampleFrame
                        clickSampleFrame + framesToWrite
                    } else {
                        // if it is finished, update the click count
                        // and reset clickSampleFrame back to null
                        currentClick = currentClick % clicksPerBar + 1
                        null
                    }
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