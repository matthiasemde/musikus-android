/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
 *
 * Parts of this software are licensed under the MIT license
 *
 * Copyright (c) 2023 Oğuzhan Ekşi
 */

package app.musikus.recorder.presentation

import android.content.Intent
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService


// inspired by https://medium.com/@ouzhaneki/basic-background-playback-implementation-with-media3-mediasessionservice-4d571f15bdc2
@UnstableApi
class RecordingPlaybackService : MediaSessionService() {

    /**
     *  --------------- Local variables ---------------
     */

    private var _mediaSession: MediaSession? = null



    /**
     * --------------- Service Boilerplate ------------
     */

    override fun onCreate() {
        super.onCreate()

        // Create the ExoPlayer instance
        val player = ExoPlayer.Builder(this).build()

        // Create a MediaSession
        _mediaSession = MediaSession.Builder(this, player).build()
    }

    /**
     * This method is called when the system determines that the service is no longer used and is being removed.
     * It checks the player's state and if the player is not ready to play or there are no items in the media queue, it stops the service.
     *
     * @param rootIntent The original root Intent that was used to launch the task that is being removed.
     */
    override fun onTaskRemoved(rootIntent: Intent?) {
        _mediaSession?.run {
            // Check if the player is not ready to play or there are no items in the media queue
            if (!player.playWhenReady || player.mediaItemCount == 0) {
                // Stop the service
                stopSelf()
            }
        }
    }

    /**
     * This method is called when a MediaSession.ControllerInfo requests the MediaSession.
     * It returns the current MediaSession instance.
     *
     * @param controllerInfo The MediaSession.ControllerInfo that is requesting the MediaSession.
     * @return The current MediaSession instance.
     */
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return _mediaSession
    }

    override fun onDestroy() {
        _mediaSession?.run {
            // Release the player
            player.release()
            // Release the MediaSession instance
            release()
            // Set _mediaSession to null
            _mediaSession = null
        }
        // Call the superclass method
        super.onDestroy()
    }
}