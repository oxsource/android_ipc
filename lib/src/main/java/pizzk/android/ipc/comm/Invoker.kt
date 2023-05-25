package pizzk.android.ipc.comm

import pizzk.android.ipc.model.Request
import pizzk.android.ipc.model.Response

interface Invoker {
    fun invoke(request: Request): Response

    fun invoke(request: Request, callback: Callback)
}