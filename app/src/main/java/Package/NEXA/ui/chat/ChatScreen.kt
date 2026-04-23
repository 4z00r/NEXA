package Package.NEXA.ui.chat

import Package.NEXA.cryptography.KeyManager
import Package.NEXA.data.DbProfileProvider
import Package.NEXA.data.UserProfile
import Package.NEXA.logic.ConnectionManager
import Package.NEXA.logic.Message
import Package.NEXA.views.UserViewModel
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.withContext

/**
 * Chat screen to display messages and send messages
 * @param user user profile of current user
 * @param address address of the device to chat with
 * @param connectionManager connection manager
 * @param userViewModel shared user view model
 */
@Composable
fun ChatScreen(
    user: UserProfile,
    address: String,
    connectionManager: ConnectionManager,
    userViewModel: UserViewModel
) {
    val otherUsername = remember(address) { address }    // username of recipient that current user is chatting with
    val ctx = LocalContext.current

    val initialHistory = remember(user, otherUsername) {    // initial history is the message history of the user with the device
        user.returnMessageHistory(otherUsername).toList()
    }
    val messages = remember(otherUsername) {                                // list of messages that are displayed on the screen
        mutableStateListOf<Message>().apply { addAll(initialHistory) }       // loads chat history to be displayed first
    }

    LaunchedEffect(address) {
        val dao = DbProfileProvider.getDB(ctx).userProfileRoomAccess()     // gets DB
        val hist = withContext(Dispatchers.IO) {
            val otherUserProfile = dao.fetchProfileByAddress(address)                          // fetches user profile of recipient
            otherUserProfile?.returnMessageHistory(address)?.toList() ?: emptyList()  // fetches message history of recipient
        }
        messages.clear()
        messages.addAll(hist)                                                       // adds message history to list of messages to be displayed
    }

    var textState by remember { mutableStateOf(TextFieldValue("")) }             // text state of the text field

    val device = remember(address) {                                                    // device to chat with
        connectionManager.bluetoothAdapter?.getRemoteDevice(address)
    }

    val callback = remember {
        object : ConnectionManager.ConnectionCallBack {
            // when a message is received -> decode public key and add message to list of messages
            override fun onMsgRecieve(message: Message) {
                if (message.message.startsWith("PUBLIC_KEY:")) {
                    val peerKeyString = message.message.removePrefix("PUBLIC_KEY:")
                    val peerKey = KeyManager.decodePubKey(peerKeyString)
                    if (peerKey != null) {
                        Log.d("ConnectionManager", "Peer key received successfully")
                    } else {
                        Log.e("ConnectionManager", "Failed to parse peer key")
                    }
                    return
                }

                CoroutineScope(Dispatchers.Main).launch {
                    if (messages.none { it.id == message.id }) {     // if messages already contains message -> do not add
                        messages.add(message.copy(isMine = false))   // adds copy of message to list of messages to be displayed and sets isMine to false
                    }
                }

                CoroutineScope(Dispatchers.IO).launch {
                    val dao = DbProfileProvider.getDB(ctx).userProfileRoomAccess()
                    val otherUserProfile = dao.fetchProfileByAddress(address)
                    if (otherUserProfile != null) {
                        otherUserProfile.returnMessageHistory(address).add(
                            message.copy(isMine = false, status = Message.Status.RECEIVED)          // adds message to other users profile to update local message history in db
                        )
                        dao.insertProfile(otherUserProfile)
                    }
                }
            }

        }
    }
    LaunchedEffect(device) {
        connectionManager.connectionCallBack = callback
        Log.d("ChatScreen", "attempting to connect")
        if (device != null) {
            connectionManager.makeConnection(device)             // makes connection to device if not null
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            //adds username to top of the chat
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ){
            Text(
                text = userViewModel.otherUser.value?.username ?: address,
                style = androidx.compose.material3.MaterialTheme.typography.headlineMedium
            )
        }
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(8.dp)
        ) {
            items(messages) { msg ->            // loops through messages to be displayed
                DisplayMsg(msg)
                Spacer(modifier = Modifier.height(4.dp))
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = textState,                      // text field to enter message
                onValueChange = { textState = it },     // user input is saved to textState
                placeholder = { Text("Type a message...") },
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = {
                if (textState.text.isNotBlank() && device != null) {   // creates message object w/ user input
                    val outgoing = Message(
                        id = UUID.randomUUID(),
                        author = user.username,
                        message = textState.text,
                        status = Message.Status.SENT,
                        isMine = true
                    )

                    messages.add(outgoing)                              // add to messages to be displayed
                    CoroutineScope(Dispatchers.IO).launch {
                        val dao = DbProfileProvider.getDB(ctx).userProfileRoomAccess()
                        val otherUserProfile = dao.fetchProfileByAddress(address)
                        if (otherUserProfile != null) {
                            otherUserProfile.returnMessageHistory(address).add(outgoing)    // adds message to other users profile to update local message history in db
                            dao.insertProfile(otherUserProfile)
                        }
                    }

                    user.returnMessageHistory(otherUsername).add(outgoing)
                    connectionManager.sendMessage(outgoing, device)       // sends message to device

                    textState = TextFieldValue("")                          // clears text field
                }
            }) {
                Text("Send")
            }
        }
    }
}
