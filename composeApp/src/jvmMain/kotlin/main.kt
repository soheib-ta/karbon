import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.soheib_ta.karbon.app.App

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "Karbon Demo") {
        App()
    }
}
