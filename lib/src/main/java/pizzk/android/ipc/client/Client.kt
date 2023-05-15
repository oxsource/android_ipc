package pizzk.android.ipc.client

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.os.Parcel
import android.util.Log
import pizzk.android.ipc.model.Protocol
import pizzk.android.ipc.model.Request
import pizzk.android.ipc.model.Response
import pizzk.android.ipc.server.Server

@Suppress("DEPRECATION")
class Client(
    val descriptor: String,
    private val component: String,
) {
    companion object {
        private const val TAG = "QuickBinder.Client"
    }

    enum class Status {
        IDLE,
        CONNECTING,
        CONNECTED,
        SUSPEND;
    }

    private var status = Status.IDLE
    private var value: IBinder? = null

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            value = service
            status = Status.CONNECTED
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            value = null
            status = Status.SUSPEND
        }
    }

    fun bindService(context: Context) {
        val abort = when (status) {
            Status.CONNECTING,
            Status.CONNECTED -> true

            else -> false
        }
        if (abort) return
        kotlin.runCatching {
            Log.d(TAG, "binding service $descriptor, $component")
            val intent = Intent()
            intent.component = ComponentName.unflattenFromString(component)
            context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
            status = Status.CONNECTING
            return@runCatching
        }.onFailure { Log.w(TAG, "bindService failed", it) }
    }

    fun unbindService(context: Context) {
        val abort = when (status) {
            Status.IDLE,
            Status.SUSPEND -> true

            else -> false
        }
        if (abort) return
        kotlin.runCatching {
            context.unbindService(connection)
            status = Status.SUSPEND
            return@runCatching
        }.onFailure { Log.w(TAG, "unbindService failed", it) }
    }

    fun invoke(request: Request): Response {
        val service = value ?: return Protocol.failure(msg = "service is not available")
        val data = Parcel.obtain()
        val reply = Parcel.obtain()
        val value = kotlin.runCatching {
            data.writeInterfaceToken(descriptor)
            data.writeParcelable(request, 0)
            service.transact(Server.TRANSACT_INVOKE, data, reply, 0)
            val response: Response? = reply.readParcelable(Response::class.java.classLoader)
            return@runCatching response ?: throw Exception("response is null")
        }.onFailure { exp ->
            Log.e(TAG, "invoke panic: ${exp.message}")
        }.getOrDefault(Response())
        data.recycle()
        reply.recycle()
        return value
    }
}