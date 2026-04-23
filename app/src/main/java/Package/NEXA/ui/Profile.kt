package Package.NEXA.ui

import Package.NEXA.data.UserProfile
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Profile screen to view the user's profile
 * The user can see their name and ID
 * @param user user profile
 */
@Composable
fun ProfileScreen(user: UserProfile)
{
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ){
        Text(
            text = "Profile",
            style = MaterialTheme.typography.headlineLarge,     // displays profile header
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Icon(
            imageVector = Icons.Default.Person,     // displays person icon
            contentDescription = null,
            modifier = Modifier.size(128.dp)
        )
        Text(

            text = user.username,                           // displays username
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Text(

            text = "Local ID: "+user.localID,                      // displays local id
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))

    }


}