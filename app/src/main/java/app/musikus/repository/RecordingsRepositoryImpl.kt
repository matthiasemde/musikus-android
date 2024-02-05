/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
 */

package app.musikus.repository

import android.content.ContentResolver
import android.content.ContentUris
import android.database.ContentObserver
import android.os.Build
import android.provider.MediaStore
import app.musikus.di.IoScope
import app.musikus.usecase.recordings.Recording
import app.musikus.usecase.recordings.RecordingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.time.Duration.Companion.milliseconds

class RecordingsRepositoryImpl(
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

    private fun <T> subscribeTo(query: () -> T): Flow<T> {
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

    private fun getRecordings() : List<Recording> {
        val recordingList = mutableListOf<Recording>()

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
                        id = id,
                        title = displayName,
                        duration = duration.milliseconds,
                        date = ZonedDateTime.ofInstant(Instant.ofEpochSecond(date), ZoneId.systemDefault()),
                        contentUri
                    )
                )
            }
        }

        return recordingList
    }

}