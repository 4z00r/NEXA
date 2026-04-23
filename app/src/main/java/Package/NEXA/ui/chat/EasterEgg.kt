package Package.NEXA.ui

import android.content.Intent
import android.net.Uri
import android.os.SystemClock
import android.widget.Toast
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext

//Class used for profile easter egg
@Composable
fun EasterEgg(
    tap: Int = 7, //Taps needed inorder to work

    time: Long = 1500L,//time to be completed in

    links: List<String> = listOf(
        "https://www.youtube.com/watch?v=0WEA3zHJl28&list=RD0WEA3zHJl28&start_radio=1",                           // YouTube
        //"https://open.spotify.com/track/3Ucr6hQQuY8cZ0UqXV8uO2?si=86e83de75052426f"   // Spotify
    )): () -> Unit {
    val context = LocalContext.current
    var count by rememberSaveable { mutableStateOf(0) } //Number of taps
    var timeStart by rememberSaveable { mutableStateOf(0L) }

    return {
        val tNow = SystemClock.elapsedRealtime()
        //if its first touch or the time has expired
        if (timeStart == 0L || tNow - timeStart > time) {
            // Start a fresh streak
            timeStart = tNow
            count = 1
        } else {
            //For counting the taps and ensuring there is enough
            count++
            if (count >= tap) {
                //Reset taps and the start time
                count = 0
                timeStart = 0L
                try {
                    //Launches intent
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse(links.random()))
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                } catch (_: Exception) {
                    Toast.makeText(context, "Couldn't open link", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
