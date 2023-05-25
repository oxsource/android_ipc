package pizzk.android.ipc.server

import android.os.Binder
import android.os.IBinder
import android.os.Parcel
import android.util.Log
import pizzk.android.ipc.client.Client
import pizzk.android.ipc.comm.Callback
import pizzk.android.ipc.comm.Invoker
import pizzk.android.ipc.comm.readParcelable
import pizzk.android.ipc.model.Protocol
import pizzk.android.ipc.model.Request
import pizzk.android.ipc.model.Response

class Server(private val descriptor: String) : Binder(), android.os.IInterface {
    companion object {
        private const val TAG = "QuickBinder.Server"
        const val TRANSACT_INVOKE_SYNC = FIRST_CALL_TRANSACTION + 0
        const val TRANSACT_INVOKE_ASYNC = FIRST_CALL_TRANSACTION + 1
    }

    init {
        attachInterface(this, descriptor)
    }

    private val fallback: Invoker = object : Invoker {
        private val value = Response()

        override fun invoke(request: Request): Response = value

        override fun invoke(request: Request, callback: Callback) {
            callback.invoke(value)
        }
    }

    private var invoker: Invoker = fallback

    override fun asBinder(): IBinder = this

    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
        reply ?: return super.onTransact(code, data, null, flags)
        when (code) {
            TRANSACT_INVOKE_SYNC -> {
                data.enforceInterface(descriptor)
                val response = kotlin.runCatching {
                    val request: Request = data.readParcelable()
                        ?: throw Exception("request params is null")
                    return@runCatching invoker.invoke(request)
                }.onFailure { exp ->
                    Log.w(TAG, "dispatch invoke panic.", exp)
                    Protocol.panic(msg = exp.message.orEmpty())
                }.getOrDefault(Response())
                reply.writeParcelable(response, 0)
                return true
            }

            TRANSACT_INVOKE_ASYNC -> {
                data.enforceInterface(descriptor)
                kotlin.runCatching {
                    val request: Request = data.readParcelable()
                        ?: throw Exception("request params is null")
                    val stub = Client.RemoteCallback.Stub(data.readStrongBinder())
                    invoker.invoke(request, stub)
                }.onFailure { exp ->
                    Log.w(TAG, "dispatch invoke panic.", exp)
                    Protocol.panic(msg = exp.message.orEmpty())
                }.getOrDefault(Response())
                reply.writeNoException()
                return true
            }

            else -> Unit
        }
        return super.onTransact(code, data, reply, flags)
    }

    fun attach(value: Invoker?) {
        invoker = value ?: fallback
    }

    class Stub(private val descriptor: String) : Invoker {
        private var binder: IBinder? = null

        fun with(value: IBinder?) {
            binder = value
        }

        override fun invoke(request: Request): Response {
            val data = Parcel.obtain()
            val reply = Parcel.obtain()
            val value = kotlin.runCatching {
                val service = binder ?: throw Exception("service is not available")
                data.writeInterfaceToken(descriptor)
                data.writeParcelable(request, 0)
                service.transact(TRANSACT_INVOKE_SYNC, data, reply, 0)
                val response: Response? = reply.readParcelable()
                return@runCatching response ?: throw Exception("response is null")
            }.onFailure { exp ->
                Log.e(TAG, "stub invoke sync panic: ${exp.message}")
            }.getOrDefault(Response())
            data.recycle()
            reply.recycle()
            return value
        }

        override fun invoke(request: Request, callback: Callback) {
            val data = Parcel.obtain()
            val reply = Parcel.obtain()
            kotlin.runCatching {
                val service = binder ?: throw Exception("service is not available")
                data.writeInterfaceToken(descriptor)
                data.writeParcelable(request, 0)
                data.writeStrongBinder(Client.RemoteCallback(callback))
                service.transact(TRANSACT_INVOKE_ASYNC, data, reply, 0)
                reply.readException()
            }.onFailure { exp ->
                Log.e(TAG, "stub invoke async panic: ${exp.message}")
                callback.invoke(Protocol.failure(exp.message.orEmpty()))
            }
            data.recycle()
            reply.recycle()
        }
    }
}