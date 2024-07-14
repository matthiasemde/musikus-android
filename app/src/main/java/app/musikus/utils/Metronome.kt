/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
 */

package app.musikus.utils

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import androidx.annotation.RawRes
import app.musikus.R
import app.musikus.di.ApplicationScope
import app.musikus.metronome.presentation.MetronomeSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.min
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class Metronome(
    @ApplicationScope private val applicationScope: CoroutineScope,
    context: Context
) {

    private var playerJob : Job? = null

    private val highNote by lazy { createNote(R.raw.beat_1, context) }
    private val mediumNote by lazy { createNote(R.raw.beat_2, context) }
    private val lowNote by lazy { createNote(R.raw.beat_3, context) }

    private var beatsPerBar = MetronomeSettings.DEFAULT.beatsPerBar
    private var clicksPerBeat = MetronomeSettings.DEFAULT.clicksPerBeat
    private var clicksPerBar = beatsPerBar * clicksPerBeat
    private var nanosecondsBetweenClicks = (
        1.minutes.inWholeNanoseconds / (MetronomeSettings.DEFAULT.bpm * clicksPerBeat)
    )

    fun updateSettings(settings: MetronomeSettings) {
        beatsPerBar = settings.beatsPerBar
        clicksPerBeat = settings.clicksPerBeat
        clicksPerBar = beatsPerBar * clicksPerBeat
        nanosecondsBetweenClicks = (1.minutes.inWholeNanoseconds / (settings.bpm * clicksPerBeat))
//        Log.d("Metronome","nanosecondsBetweenClicks: $nanosecondsBetweenClicks")
    }

    fun play() {
        playerJob = createPlayerJob()
    }

    fun stop() {
        playerJob?.cancel()
    }


    private fun createPlayerJob() : Job {
        return applicationScope.launch {
            val track = createTrack()


            val nanosecondsPerFrame = 1.seconds.inWholeNanoseconds / track.sampleRate

            // frame size = sample size in bytes * number of channels (4 byte float * 1 channel)
            // buffer size is 1/4 of the track buffer size to avoid overfilling
            val bufferSize = track.bufferSizeInFrames / 4
            val buffer = FloatArray(bufferSize)


//            Log.d("Metronome", "Sample rate: ${track.sampleRate}")
//            Log.d("Metronome", "Buffer size: $bufferSize")
//            Log.d("Metronome", "Note size: ${highNote.size}")

            track.play()

            var currentClick = 0
            var framesUntilNextClick = 0
            var clickSampleFramePointer : Int? = null

            /**
             * The loop playing the metronome
             */

            while (true) {
                if(!isActive) {
                    break
                }

                // initialize buffer with 0 (silence)
                buffer.fill(0f)

                var bufferFramePointer = 0

                while(bufferFramePointer < bufferSize) {
                    val currentClickSample = when {
                        currentClick == 0 -> highNote
                        (currentClick % clicksPerBeat) != 0 -> lowNote
                        else -> mediumNote
                    }

                    // if there is no clickSampleFrame aka no click is being written to the
                    // buffer, initiate the clickSampleFrame and calculate the number of frames
                    if (clickSampleFramePointer == null) {
                        val remainingBufferFrames = bufferSize - bufferFramePointer

                        // if the number of frames until the next click is larger or equal
                        // than the remaining buffer frames, break the loop and wait for the next buffer
                        if (framesUntilNextClick >= remainingBufferFrames) {
                            framesUntilNextClick -= remainingBufferFrames
                            break
                        } else {
                            // otherwise, move the bufferFramePointer to the beginning of the next click
                            bufferFramePointer += framesUntilNextClick
                        }

                        // to write the next click sample to the buffer,
                        // initiate the clickSampleFramePointer to 0
                        clickSampleFramePointer = 0

                        // now calculate the number of frames until the next click
                        framesUntilNextClick = (nanosecondsBetweenClicks / nanosecondsPerFrame).toInt().coerceAtLeast(1)
//                        Log.d("Metronome", "Frames until next click: $framesUntilNextClick, starting at $bufferFramePointer")
                    }

                    // calculate how many frames can at most be written by taking the min
                    // of the remaining frames until either the next click starts or the buffer is full
                    val remainingClickFrames = currentClickSample.size - clickSampleFramePointer
                    val remainingFramesUntilNextClickOrEndOfBuffer = min(
                        framesUntilNextClick,
                        bufferSize - bufferFramePointer
                    )

                    // calculate how many frames should be written by taking the min
                    // of the remaining frames in the click sample and the max frames that can be written
                    val framesToWrite = min(
                        remainingClickFrames,
                        remainingFramesUntilNextClickOrEndOfBuffer
                    )

//                    Log.d("Metronome", "Constraints: remainingClickFrames: $remainingClickFrames, framesUntilNextClick: $framesUntilNextClick, remainingBufferFrames: ${bufferSize - bufferFramePointer}")
//                    Log.d("Metronome", "Writing $framesToWrite frames to buffer at $bufferFramePointer")

                    // write the frames to the buffer
                    for(i in 0 until framesToWrite) {
                        buffer[bufferFramePointer + i] = currentClickSample[clickSampleFramePointer + i]
                    }

                    // update the buffer frame pointer and the frames until the next click counter
                    bufferFramePointer += framesToWrite
                    framesUntilNextClick -= framesToWrite

                    // check if the clickSample is finished or not
                    if(framesToWrite >= remainingClickFrames || framesUntilNextClick == 0) {
                        // if it is finished, update the click count
                        // and reset clickSampleFramePointer back to null
                        currentClick++
                        if(currentClick >= clicksPerBar) {
                            currentClick = 0
                        }
                        clickSampleFramePointer = null
                    } else {
                        // if it is not finished, update the click sample frame pointer
                        clickSampleFramePointer += framesToWrite
                    }
                }


                // write() is a blocking call
                track.write(buffer, 0, bufferSize, AudioTrack.WRITE_BLOCKING)
//                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && track.underrunCount > 0) {
//                    Log.w("Metronome", "Underrun: ${track.underrunCount}")
//                }
            }

            track.stop()
            track.flush()
            track.release()
        }
    }
    companion object {
        fun createTrack() : AudioTrack {
            val nativeSampleRate = AudioTrack.getNativeOutputSampleRate(AudioManager.STREAM_MUSIC)
            val bufferSize = 4 * AudioTrack.getMinBufferSize(
                nativeSampleRate, AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_FLOAT
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
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .setSampleRate(nativeSampleRate)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
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