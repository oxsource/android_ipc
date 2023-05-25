package pizzk.android.ipc.app.service

import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import pizzk.android.ipc.comm.Callback
import pizzk.android.ipc.comm.Invoker
import pizzk.android.ipc.model.Protocol
import pizzk.android.ipc.model.Request
import pizzk.android.ipc.model.Response
import pizzk.android.ipc.server.Server

class FakeService : Service() {
    companion object {
        private const val TAG = "QuickBinder.FakeService"
        const val DESCRIPTOR = "pizzk.android.ipc.app.service.FakeService"
        const val ACTION_ECHO = "ECHO"
        private const val CALLBACK_DELAY = 2000L
    }

    private val server by lazy { Server(DESCRIPTOR) }
    private val handler = Handler(Looper.getMainLooper())
    private val fallbackResponse = Response()
    private val invoker = object : Invoker {

        override fun invoke(request: Request): Response {
            Log.d(TAG, "onInvoke sync: $request")
            return when (request.action) {
                ACTION_ECHO -> Protocol.success(msg = "got it right now!", payload = "SYNC")
                else -> fallbackResponse
            }
        }

        override fun invoke(request: Request, callback: Callback) {
            Log.d(TAG, "onInvoke async: $request")
            handler.postDelayed({
                val value = when (request.action) {
                    ACTION_ECHO -> Protocol.success(msg = "got it delay!", payload = "ASYNC")
                    else -> fallbackResponse
                }
                callback.invoke(value)
            }, CALLBACK_DELAY)
        }
    }

    override fun onCreate() {
        super.onCreate()
        server.attach(value = invoker)
    }

    override fun onBind(intent: Intent?): IBinder = server.asBinder()

    override fun onDestroy() {
        server.attach(value = null)
        super.onDestroy()
    }
}