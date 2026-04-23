package Package.NEXA.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID


/**
 * Class that holds the Waiting Messages to be sent.
 * Will be used to store in a Room Database
 */

/**
 * Class that contains the Entity to be stored the Waiting Database
 */
@Entity(tableName = "waiting")
data class Waiting(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val deviceAddress: String,
    val jSon: String,
    val createdAt: Long = System.currentTimeMillis(),
    val retries: Int = 0
)
