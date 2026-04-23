package Package.NEXA.ui.chat

import Package.NEXA.logic.Message
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.Color

/**
 * Display message function to display a message in the chat screen
 * @param msg message to be displayed
 */
@Composable
fun DisplayMsg(msg: Message) {
    // displays different bubble color and text color depending on who sent the message
    val bubbleColor = if (msg.isMine)
        MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (msg.isMine)
        MaterialTheme.colorScheme.onPrimary
    else MaterialTheme.colorScheme.onSurfaceVariant
    // displays different icon depending on status of message
    val status = when(msg.status){
        Message.Status.SENT -> Icons.Filled.Done
        Message.Status.DELIVERED -> Icons.Filled.DoneAll
        Message.Status.READ -> Icons.Filled.DoneAll
        Message.Status.NONE -> Icons.Filled.Schedule
        Message.Status.RECEIVED -> Icons.Filled.DoneAll
        Message.Status.STORED -> Icons.Filled.Schedule
    }
    val statusColor = when(msg.status){        // if message read -> status icon changes to green colour otherwise icon is grey
        Message.Status.READ -> Color.Green
        else -> Color.Gray
    }
    Row(modifier =
        Modifier.fillMaxWidth().padding(8.dp),  // sets padding around each message and makes sure message fills width of screen
        horizontalArrangement = if (msg.isMine) Arrangement.End else Arrangement.Start            // if current user sent message, aligns to right side of screen, other user's msg goes to left
    )
    {
        Spacer(modifier = Modifier.width(8.dp))
        Column(horizontalAlignment = if (msg.isMine) Alignment.End else Alignment.Start) {

            Surface(
                color = bubbleColor,        // sets colour to bubble color
                shape = MaterialTheme.shapes.medium,
                shadowElevation = 2.dp           // makes text bubbles pop a bit more
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(4.dp)) {
                    // displays message text and status icon
                    Text(
                        text = msg.message,
                        color = textColor,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(all = 4.5.dp)
                    )
                    if (msg.isMine) {           // only displays message status if it is current users message
                        Icon(
                            imageVector = status,
                            contentDescription = msg.status.name,
                            tint = statusColor,
                            modifier=Modifier.padding(all=4.dp).size(14.dp).height(5.dp),
                        )
                    }
                }
            }
        }
    }
}