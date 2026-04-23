package Package.NEXA

import Package.NEXA.logic.Message
import org.junit.Assert.assertArrayEquals
import org.junit.Test
import java.util.UUID
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals

class TestMessage {
    @Test
    fun testSetAndGetIsMine() {
        val msg = Message(UUID.randomUUID(), "Alice", "Hello", Message.Status.NONE, false)
        assertFalse(msg.isMyMessage())

        msg.setMyMessage(true)
        assertTrue(msg.isMyMessage())
    }
    @Test
    fun testConvertMsgToByteArray() {
        val text = "Hello World"
        val msg = Message(UUID.randomUUID(), "Alice", text, Message.Status.NONE, true)
        val bytes = msg.convertMsg()
        assertArrayEquals(text.toByteArray(), bytes)
    }
    @Test
    fun testToStringReturnsMessageText() {
        val text = "Hi there"
        val msg = Message(UUID.randomUUID(), "Bob", text, Message.Status.SENT, true)
        assertEquals(text, msg.toString())
    }
    @Test
    fun testStatusEnumValues() {
        val statuses = Message.Status.entries.toTypedArray()
        assertTrue(statuses.contains(Message.Status.SENT))
        assertTrue(statuses.contains(Message.Status.DELIVERED))
        assertTrue(statuses.contains(Message.Status.READ))
        assertTrue(statuses.contains(Message.Status.NONE))
        assertTrue(statuses.contains(Message.Status.RECEIVED))
        assertTrue(statuses.contains(Message.Status.STORED))
    }
    @Test
    fun testMessageEquality() {
        val id = UUID.randomUUID()
        val msg1 = Message(id, "Alice", "Hi", Message.Status.NONE, true)
        val msg2 = Message(id, "Alice", "Hi", Message.Status.NONE, true)
        assertEquals(msg1, msg2)
    }
}