package pizzk.android.ipc.client

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Binder
import android.os.IBinder
import android.os.Parcel
import android.util.Log
import pizzk.android.ipc.comm.Callback
import pizzk.android.ipc.comm.readParcelable
import pizzk.android.ipc.model.Request
import pizzk.android.ipc.model.Response
import pizzk.android.ipc.server.Server

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
    private val server = Server.Stub(descriptor)

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            server.with(value = service)
            status = Status.CONNECTED
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            server.with(value = null)
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
        }.onFailure { Log.w(TAG, "bind service failed", it) }
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
        }.onFailure { Log.w(TAG, "unbind service failed", it) }
    }

    fun invoke(request: Request): Response = server.invoke(request)

    fun invoke(request: Request, callback: Callback) = server.invoke(request, callback)

    class RemoteCallback(private val callback: Callback) : Binder(), Callback {
        companion object {
            private const val TAG = "QuickBinder.Callback"
            const val TRANSACT_INVOKE = FIRST_CALL_TRANSACTION + 0
        }

        override fun onTransact(
            code: Int,
            data: Parcel,
            reply: Parcel?,
            flags: Int
        ): Boolean {
            if (TRANSACT_INVOKE == code) {
                data.readParcelable<Response>()?.let(this::invoke)
                return true
            }
            return super.onTransact(code, data, reply, flags)
        }

        override fun invoke(value: Response) = callback.invoke(value)

        class Stub(private val binder: IBinder) : Callback {
            override fun invoke(value: Response) {
                val data = Parcel.obtain()
                val reply = Parcel.obtain()
                kotlin.runCatching {
                    data.writeParcelable(value, 0)
                    binder.transact(TRANSACT_INVOKE, data, reply, 0)
                    reply.readException()
                }.onFailure { Log.e(TAG, "stub invoke failed.", it) }
                data.recycle()
                reply.recycle()
            }
        }
    }
}