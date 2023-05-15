package pizzk.android.ipc.app.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import pizzk.android.ipc.model.Protocol
import pizzk.android.ipc.model.Request
import pizzk.android.ipc.model.Response
import pizzk.android.ipc.server.Server

class FakeService : Service() {
    companion object {
        private const val TAG = "DroidIPC.FakeService"
        const val DESCRIPTOR = "pizzk.android.ipc.app.service.FakeService"
        const val ACTION_ECHO = "ECHO"
    }

    private val server by lazy { Server(DESCRIPTOR) }

    override fun onCreate() {
        super.onCreate()
        server.setCallback(block = this::onInvoke)
    }

    private fun onInvoke(request: Request): Response {
        Log.d(TAG, "onInvoke: $request")
        return when (request.action) {
            ACTION_ECHO -> {
                Protocol.success(msg = "got it!", payload = toString())
            }

            else -> Response()
        }
    }

    override fun onBind(intent: Intent?): IBinder = server.asBinder()

    override fun onDestroy() {
        server.setCallback(block = null)
        super.onDestroy()
    }
}