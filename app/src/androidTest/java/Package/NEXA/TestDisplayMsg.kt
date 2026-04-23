package Package.NEXA.ui.chat

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test
import Package.NEXA.logic.Message
import java.util.*

class TestDisplayMsg {
    @get:Rule
    val composeTestRule = createComposeRule()
    @Test
    fun testShowsText() {
        val msg = Message(id = UUID.randomUUID(),author = "Alice",message = "Hello NEXA!",status = Message.Status.SENT,isMine = true)
        composeTestRule.setContent {
            DisplayMsg(msg)
        }
        // Verify message text appears
        composeTestRule.onNodeWithText("Hello NEXA").assertIsDisplayed()
    }
    @Test
    fun testShowsStatusIcon() {
        val msg = Message(id = UUID.randomUUID(),author = "Alice",message = "Test msg",status = Message.Status.SENT,isMine = true)
        composeTestRule.setContent {
            DisplayMsg(msg)
        }
        // Verify icon is displayed with correct content description
        composeTestRule.onNodeWithContentDescription("SENT").assertIsDisplayed()
    }
    @Test
    fun testNoStatusIcon() {
        val msg = Message(id = UUID.randomUUID(),author = "Bob",message = "Hey there",status = Message.Status.READ,isMine = false)
        composeTestRule.setContent {
            DisplayMsg(msg)
        }
        // Should only show text, not icon since message is not mine
        composeTestRule.onNodeWithText("Hey there").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("READ").assertDoesNotExist()
    }
}
