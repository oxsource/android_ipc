package pizzk.android.ipc.app

import android.app.Application
import android.util.Log
import pizzk.android.ipc.client.QuickBinder

class MainApplication : Application() {
    companion object {
        private const val TAG = "DroidIPC.App"
        private const val REMOTE_PROCESS = ":remote"
    }

    override fun onCreate() {
        super.onCreate()
        val process = getProcessName()
        Log.d(TAG, "onCreate process: $process")
        if (!process.endsWith(REMOTE_PROCESS, ignoreCase = true)) {
            QuickBinder.setup(context = applicationContext)
        }
    }

    override fun onTerminate() {
        val process = getProcessName()
        if (!process.endsWith(REMOTE_PROCESS, ignoreCase = true)) {
            QuickBinder.release()
        }
        super.onTerminate()
    }
}