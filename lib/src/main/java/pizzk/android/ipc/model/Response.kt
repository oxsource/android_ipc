package pizzk.android.ipc.model

import android.os.Parcel
import android.os.Parcelable
import kotlinx.parcelize.Parceler
import kotlinx.parcelize.Parcelize

@Parcelize
data class Response(
    val code: Int = Protocol.FAILURE,
    val msg: String = Protocol.MSG_NOT_SUPPORT,
    val payload: String = ""
) : Parcelable {

    private companion object : Parceler<Response> {
        override fun Response.write(parcel: Parcel, flags: Int) {
            parcel.writeInt(code)
            parcel.writeString(msg)
            parcel.writeString(payload)
        }

        override fun create(parcel: Parcel): Response = Response(
            parcel.readInt(),
            parcel.readString().orEmpty(),
            parcel.readString().orEmpty()
        )
    }
}