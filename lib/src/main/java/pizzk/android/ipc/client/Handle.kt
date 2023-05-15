package pizzk.android.ipc.client

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import androidx.lifecycle.MutableLiveData
import pizzk.android.ipc.R
import pizzk.android.ipc.model.Request
import pizzk.android.ipc.model.Response
import java.util.Collections

object Handle {
    private const val TAG = "DroidIPC.Handle"
    private const val DEBUG = true
    private const val SPLIT = ";"
    private const val WHAT_SETUP = 0x01
    private const val WHAT_RELEASE = 0x02

    private val clients: MutableMap<String, Client> = Collections.synchronizedMap(mutableMapOf())
    private val context: MutableLiveData<Context> = MutableLiveData()

    private val handler = Handler(Looper.getMainLooper()) { msg ->
        when (msg.what) {
            WHAT_SETUP -> onSetup(msg)
            WHAT_RELEASE -> onRelease()
            else -> Unit
        }
        return@Handler true
    }

    private fun onSetup(msg: Message) {
        Log.d(TAG, "onSetup")
        if (clients.isNotEmpty()) return
        val ctx = msg.obj as? Context ?: return
        context.value = ctx.applicationContext
        val services = ctx.resources.getStringArray(R.array.support_android_ipc_services)
        val clients = services.mapNotNull { e ->
            val items = e.split(SPLIT)
            val descriptor = items.getOrNull(0) ?: return@mapNotNull null
            val component = items.getOrNull(1) ?: return@mapNotNull null
            return@mapNotNull Pair(descriptor, Client(descriptor, component))
        }.toMap()
        this.clients.putAll(clients)
        this.clients.values.forEach { e ->
            Log.d(TAG, "${e.descriptor} bindService")
            e.bindService(ctx)
        }
    }

    private fun onRelease() {
        Log.d(TAG, "onRelease")
        val ctx = context.value ?: return
        context.value = null
        if (clients.isEmpty()) return
        clients.values.forEach { e ->
            Log.d(TAG, "${e.descriptor} unbindService")
            e.unbindService(ctx)
        }
    }

    fun setup(context: Context) {
        handler.removeMessages(WHAT_SETUP)
        handler.sendMessage(Message.obtain(handler, WHAT_SETUP, context))
    }

    fun release() {
        handler.removeMessages(WHAT_RELEASE)
        handler.sendMessage(Message.obtain(handler, WHAT_RELEASE))
    }

    fun invoke(descriptor: String, request: Request): Response {
        val e = clients[descriptor] ?: return Response()
        if (DEBUG) {
            Log.d(TAG, "invoke <${e.descriptor}> request: $request")
        }
        val result = e.invoke(request)
        if (DEBUG) {
            Log.d(TAG, "invoke response: $result")
        }
        return result
    }
}