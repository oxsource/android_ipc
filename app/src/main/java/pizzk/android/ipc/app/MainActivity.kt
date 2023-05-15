package pizzk.android.ipc.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import pizzk.android.ipc.app.service.FakeService
import pizzk.android.ipc.app.ui.theme.AndroidIPCTheme
import pizzk.android.ipc.client.QuickBinder
import pizzk.android.ipc.model.Request

class MainActivity : ComponentActivity() {
    @DelicateCoroutinesApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AndroidIPCTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val modifier = Modifier.clickable(
                        onClick = {
                            GlobalScope.launch {
                                val request = Request(
                                    identify = "ipc.app.greeting",
                                    action = FakeService.ACTION_ECHO,
                                    payload = "Hello IPC"
                                )
                                QuickBinder.invoke(FakeService.DESCRIPTOR, request)
                            }
                        }
                    )
                    Greeting("Android", modifier)
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier,
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    AndroidIPCTheme {
        Greeting("Android")
    }
}