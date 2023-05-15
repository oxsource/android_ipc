package pizzk.android.ipc.server

import android.os.Binder
import android.os.IBinder
import android.os.Parcel
import android.util.Log
import pizzk.android.ipc.model.Protocol
import pizzk.android.ipc.model.Request
import pizzk.android.ipc.model.Response

class Server(private val descriptor: String) : Binder(), android.os.IInterface {
    companion object {
        private const val TAG = "QuickBinder.Server"
        const val TRANSACT_INVOKE = FIRST_CALL_TRANSACTION + 0
    }

    init {
        attachInterface(this, descriptor)
    }

    private val emptyCallback: (Request) -> Response = { Response() }

    private var callback: (Request) -> Response = emptyCallback

    override fun asBinder(): IBinder = this

    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
        reply ?: return super.onTransact(code, data, null, flags)
        when (code) {
            TRANSACT_INVOKE -> {
                data.enforceInterface(descriptor)
                val response = dispatchInvoke(data)
                reply.writeParcelable(response, 0)
                return true
            }

            else -> Unit
        }
        return super.onTransact(code, data, reply, flags)
    }

    fun setCallback(block: ((Request) -> Response)?) {
        callback = block ?: emptyCallback
    }

    @Suppress("DEPRECATION")
    private fun dispatchInvoke(data: Parcel): Response {
        return kotlin.runCatching {
            val request: Request = data.readParcelable(Request::class.java.classLoader)
                ?: throw Exception("request params is null")
            return@runCatching callback.invoke(request)
        }.onFailure { exp ->
            Log.w(TAG, "dispatch invoke panic.", exp)
            Protocol.panic(msg = exp.message.orEmpty())
        }.getOrDefault(Response())
    }
}