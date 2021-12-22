package de.practicetime.practicetime

import android.app.Service
import android.content.Intent

class RecorderService : Service() {
    override fun onBind(intent: Intent?): Nothing? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {

        super.onDestroy()
    }

}