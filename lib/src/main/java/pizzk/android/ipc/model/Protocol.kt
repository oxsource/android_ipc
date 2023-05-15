package pizzk.android.ipc.model

object Protocol {
    const val SUCCESS = 1
    const val FAILURE = 2
    const val PANIC = 3

    const val MSG_NOT_SUPPORT = "not support"

    fun failure(msg: String, payload: String = "") =
        Response(code = FAILURE, msg = msg, payload = payload)

    fun success(msg: String = "", payload: String = "") =
        Response(code = SUCCESS, msg = msg, payload = payload)

    fun panic(msg: String) =
        Response(code = PANIC, msg = msg, payload = "")

    fun Response.success(): Boolean = SUCCESS == code
}