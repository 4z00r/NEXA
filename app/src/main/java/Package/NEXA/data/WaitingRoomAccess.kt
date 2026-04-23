package Package.NEXA.data

import androidx.room.*

/**
 * Data access object used with objects in the roomDB
 * Basic interface used for RoomDb that will
 * Basic sql operations
 */
@Dao
interface WaitingRoomAccess {

    /**
     * Function to insert a message waiting to be sent
     * @param item Message waiting to be sent
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWaiting(item: Waiting)

    /**
     * Function to find a message waiting to be sent to a specific device address
     * @param addr The address that the message needs to be sent to
     */
    @Query("select * from waiting where deviceAddress = :addr order by createdAt asc")
    suspend fun fetchWaiting(addr: String): List<Waiting>

    /**
     * Function to delete a waiting message from the DB
     * @param id The Id of the string that needs to removed
     */
    @Query("delete from waiting where id = :id")
    suspend fun deleteWaiting(id: String)

    /**
     * Function increase the retries amount for a specific waiting message
     * @param id The id of the message to bump
     */
    @Query("update waiting set retries = retries + 1 where id = :id")
    suspend fun bump(id: String)
}
