package Package.NEXA.logic

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Data class used to transfer messages from user to user
 */
data class Message(val id: UUID, val author: String, val message: String, var status: Status, var isMine: Boolean)  {
    enum class Status {                     // status of messages --> messages start in none state
        SENT, DELIVERED,READ, NONE, RECEIVED,STORED
    }


    /**
     * Function to claim message as the users
     */
    fun setMyMessage(bool: Boolean){
        isMine = bool
    }

    /**
     * Function to check if the message is from the source device
     */
    fun isMyMessage():Boolean{
        return isMine
    }

    /**
     * Function to convert the message into ByteArray Format
     * Message needs to be in ByteArray format to Send over bluetooth socket
     */
    fun convertMsg(): ByteArray { // converts message to byte array to send over bt
        val msg: ByteArray = message.toByteArray()
        return msg
    }

    /**
     * Obtain the actual text message sent
     */
    override fun toString(): String {
        val msg = message
        return msg
    }

}