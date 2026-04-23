package Package.NEXA.ui

import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon

/**
 * Data class for navigation items
 * @param route route to navigate to
 * @param title title to display on the nav bar
 */
data class NavItem(val route: String, val title: String)
/**
 * Navigation bar for the app
 * The user can navigate to the device list, chat list and profile screen
 * @param navController navigation controller
 */
@Composable
fun NavBar(navController: NavHostController) {
    val items = listOf(
        NavItem(route = "deviceList",title = "Discover"),   // title is what is displayed on the nav bar
        NavItem(route = "chatList",  title = "Chats"),
        NavItem(route="profileScreen",title = "Profile")
    )

    NavigationBar {
        val onProfileEasterTap = EasterEgg()
        val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route // gets current route
        items.forEach { item ->                                             // loops through items and creates a nav bar item for each
            NavigationBarItem(
                selected = currentRoute == item.route,
                onClick = {
                    navController.navigate(item.route) {                                // navigates to the route
                        popUpTo(navController.graph.startDestinationId) { saveState = true }    // saves state of screen
                        launchSingleTop = true                                                  // prevents copies of same screen being created
                        restoreState = true                                                     // restores state of screen
                    }
                    if(item.route == "profileScreen") onProfileEasterTap() // easter egg for profile screen")
                },
                icon = {
                    when (item.route) {
                        "deviceList" ->  Icon(Icons.Filled.People, contentDescription = null)            // people icon for the device list nav item
                        "chatList" -> Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null)    // chat icon for the chatlist icon
                        "profileScreen" -> Icon(Icons.Filled.Settings, contentDescription = null)       // settings icon for the profile screen
                    }
                },
                label = { Text(item.title) }
            )
        }
    }
}
