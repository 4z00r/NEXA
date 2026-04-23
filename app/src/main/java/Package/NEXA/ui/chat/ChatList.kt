package Package.NEXA.ui.chat

import Package.NEXA.data.DbProfileProvider
import Package.NEXA.views.UserViewModel
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
/**
 * Chat list screen to display a list of contacts and the last message sent in the chat
 * @param navController navigation controller to switch between screens
 * @param sharedUserViewModel shared user view model to store current and selected user profiles
 */
@Composable
fun ChatList(
    navController: NavController,
    sharedUserViewModel: UserViewModel
) {
    val context = LocalContext.current
    val dao = remember { DbProfileProvider.getDB(context.applicationContext).userProfileRoomAccess() } // data access object for user profiles
    val contacts by dao.fetchAllProfilesFlow().collectAsState(initial = emptyList())                   // fetches user profiles from db

    val deduped = remember(contacts) {
        contacts
            .filter { it.address != null }
            .distinctBy { it.address!!.uppercase() } // removes duplicate contacts
    }

    if (deduped.isEmpty()) {
        Text("No contacts yet. Discover a device to start a chat.", modifier = Modifier.padding(16.dp))  // if no contacts display no contacts message
        return
    }

    LazyColumn {
        items(deduped) { peer ->                  // loops through contacts to get last message sent
            val lastMsg = peer.address?.let { addr ->
                peer.chatHistory[addr]?.lastOrNull()?.message
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        sharedUserViewModel.selectUser(peer)             // selects user to chat with
                        navController.navigate("chat/${peer.address}")  // navigates to chat screen of selected user
                    }
                    .padding(16.dp)
            ) {
                Text(
                    peer.username,
                    style = androidx.compose.material3.MaterialTheme.typography.titleMedium.copy(
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        color = Color.Black
                    )

                )
                if (!lastMsg.isNullOrBlank())               // displays last message if not null
                    Text(
                        lastMsg,
                        maxLines = 1,                       // makes sure message does not go over 1 line
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis    // adds ellipse if message is too long
                    )
            }
        }
    }
}
