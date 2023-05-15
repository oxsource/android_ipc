package pizzk.android.ipc.model

import android.os.Parcel
import android.os.Parcelable
import kotlinx.parcelize.Parceler
import kotlinx.parcelize.Parcelize

@Parcelize
data class Request(
    val identify: String,
    val action: String,
    val payload: String
) : Parcelable {
    private companion object : Parceler<Request> {
        override fun create(parcel: Parcel): Request =
            Request(
                parcel.readString().orEmpty(),
                parcel.readString().orEmpty(),
                parcel.readString().orEmpty()
            )

        override fun Request.write(parcel: Parcel, flags: Int) {
            parcel.writeString(identify)
            parcel.writeString(action)
            parcel.writeString(payload)
        }
    }
}