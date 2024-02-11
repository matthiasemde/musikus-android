/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
 */

package app.musikus.repository

import android.app.Application
import android.content.ContentResolver
import android.content.ContentUris
import android.database.ContentObserver
import android.media.MediaCodec
import android.media.MediaCodecList
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.media3.common.MediaItem
import app.musikus.di.IoScope
import app.musikus.usecase.recordings.Recording
import app.musikus.usecase.recordings.RecordingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.time.Duration.Companion.milliseconds


private const val DECODER_INPUT_BUFFER_SIZE = 1 shl 13 // 1 MB
private const val EXTRACTOR_OUTPUT_BUFFER_SIZE = 1 shl 10 // 1 KB

class RecordingsRepositoryImpl(
    private val application: Application,
    private val contentResolver: ContentResolver,
    @IoScope private val ioScope: CoroutineScope
) : RecordingsRepository {


    private val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
    } else {
        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
    }
    override val recordings: Flow<List<Recording>>
        get() = subscribeTo { getRecordings() }


    // inspired by: https://stackoverflow.com/questions/55212362/how-to-decode-an-m4a-audio-on-android
    // and: https://imnotyourson.com/enhance-poor-performance-of-decoding-audio-with-mediaextractor-and-mediacodec-to-pcm/

    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun getRawRecording(contentUri: Uri): Result<ShortArray> {

        val mediaExtractor = MediaExtractor()

        Log.d("RecordingsRepository", "getRawRecording: $contentUri")

        val recordingBuffer = ArrayList<Short>()
        var mediaFormat : MediaFormat? = null

        mediaExtractor.setDataSource(application, contentUri, null)

        repeat(mediaExtractor.trackCount) { trackIndex ->
            mediaFormat = mediaExtractor.getTrackFormat(trackIndex)
            if(mediaFormat?.getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true) {
                mediaExtractor.selectTrack(trackIndex)
                return@repeat
            }
        }

        // make sure, the mediaFormat is not null
        val nonNullMediaFormat = mediaFormat
            ?: return Result.failure(Exception("No audio track found"))

        // we get the mime type from the mediaFormat
        val mimeType = nonNullMediaFormat.getString(MediaFormat.KEY_MIME)
            ?: return Result.failure(Exception("No mime type found"))


        val codec = MediaCodec.createDecoderByType(mimeType)


        nonNullMediaFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, DECODER_INPUT_BUFFER_SIZE) // huge throughput

        codec.configure(nonNullMediaFormat, null, null, 0)

        codec.start()


        while(true) {

            // input buffer
            val inputBufferId = codec.dequeueInputBuffer(10)

            Log.d("RecordingsRepository", "Dequeued inputBufferId: $inputBufferId")

            if(inputBufferId >= 0) {
                val inputBuffer = codec.getInputBuffer(inputBufferId) ?: break

                var bufferChunkSize = 0
                var presentationTime = 0L

                val temporaryBuffer = ByteBuffer.allocate(EXTRACTOR_OUTPUT_BUFFER_SIZE)

                while(true) {
                    val sampleSize = mediaExtractor.readSampleData(temporaryBuffer, 0)

                    if(sampleSize > 0) {
                        inputBuffer.put(temporaryBuffer)

                        bufferChunkSize += sampleSize
                        presentationTime = mediaExtractor.sampleTime

                        mediaExtractor.advance()
                    }

                    if(bufferChunkSize > (DECODER_INPUT_BUFFER_SIZE - EXTRACTOR_OUTPUT_BUFFER_SIZE) || sampleSize == -1) {
                        break
                    }
                }

                Log.d("RecordingsRepository", "Enqueued buffer: $inputBufferId, size: $bufferChunkSize, presentationTime: $presentationTime")
                if(bufferChunkSize > 0) {
                    codec.queueInputBuffer(inputBufferId, 0, bufferChunkSize, presentationTime, 0)
                } else {
                    codec.queueInputBuffer(inputBufferId, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                }
            }

            // output buffer
            val bufferInfo = MediaCodec.BufferInfo()
            val outputBufferId = codec.dequeueOutputBuffer(bufferInfo, 1000)

            Log.d("RecordingsRepository", "Dequeued outputBuffer Id: $outputBufferId")


            if (outputBufferId >= 0) {
                val outputBuffer = codec.getOutputBuffer(outputBufferId) ?: break
                if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                    break
                }
                for (i in 0 until bufferInfo.size step 2) {
                    recordingBuffer.add(outputBuffer.getShort(i))
                }
                Log.d("RecordingsRepository", "Released outputBuffer Id: $outputBufferId, presentationTime: ${bufferInfo.presentationTimeUs}")
                codec.releaseOutputBuffer(outputBufferId, false)
            }
        }
        codec.stop()
        codec.release()
        return Result.success(recordingBuffer.toShortArray())
    }

    // query has to be non-blocking
    private fun <T> subscribeTo(query: suspend () -> T): Flow<T> {
        val notify = MutableSharedFlow<String>()

        var observer: ContentObserver? = null

        return flow {
            observer = observer ?: (object : ContentObserver(null) {
                override fun onChange(selfChange: Boolean) {
                    ioScope.launch {
                        notify.emit("changed")
                    }
                }
            }).also {
                contentResolver.registerContentObserver(collection, true, it)
            }

            // emit the initial value
            emit(query())

            notify.collect {
                emit(query())
            }
        }.onCompletion {
            observer?.let {
                contentResolver.unregisterContentObserver(it)
            }
        }
    }

    private suspend fun getRecordings() : List<Recording> {
        val recordingList = mutableListOf<Recording>()

        withContext(Dispatchers.IO) {
            contentResolver.query(
                collection,
                arrayOf(
                    MediaStore.Audio.Media._ID,
                    MediaStore.Audio.Media.DISPLAY_NAME,
                    MediaStore.Audio.Media.DURATION,
                    MediaStore.Audio.Media.DATE_ADDED,
                ),
                "${MediaStore.Audio.Media.ALBUM} = 'Musikus'",
                null,
                "${MediaStore.Audio.Media.DATE_ADDED} DESC"
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val displayNameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
                val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)

                while(cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val displayName = cursor.getString(displayNameColumn)
                    val duration = cursor.getInt(durationColumn)
                    val date = cursor.getLong(dateColumn)
                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        id
                    )
                    recordingList.add(
                        Recording(
                            mediaItem = MediaItem.fromUri(contentUri),
                            title = displayName,
                            duration = duration.milliseconds,
                            date = ZonedDateTime.ofInstant(Instant.ofEpochSecond(date), ZoneId.systemDefault()),
                            contentUri = contentUri
                        )
                    )
                }
            }
        }

        return recordingList
    }

}