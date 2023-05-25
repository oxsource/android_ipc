package pizzk.android.ipc.comm

import android.os.Parcel

@Suppress("DEPRECATION")
inline fun <reified T> Parcel.readParcelable(): T? {
    return readParcelable(T::class.java.classLoader)
}