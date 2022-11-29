package com.github.tvbox.osc.service

import android.app.Activity
import android.app.Service
import android.content.Intent
import android.os.IBinder
import catvod.Catvod

class GoService : Service() {
    companion object {
        const val actionStartService = "startGoService"
        @JvmStatic
        fun startThis(activity: Activity) {
            val intent = Intent(activity, GoService::class.java)
            intent.action = actionStartService
            activity.startService(intent)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent!!.action
        if (action != null) {
            when (action) {
                actionStartService -> upServer()
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    fun upServer(){
        Catvod.startServer(":8999")
    }

    override fun onBind(p0: Intent?): IBinder? {
        TODO("Not yet implemented")
    }
}