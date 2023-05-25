package pizzk.android.ipc.comm

import pizzk.android.ipc.model.Response

interface Callback {
    fun invoke(value: Response)
}