package Package.NEXA.logic

import Package.NEXA.data.UserProfile
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.ParcelUuid
import android.util.Log
import androidx.core.app.ActivityCompat
import com.google.gson.Gson
import java.util.UUID


/**
 * Class to handle device discovery
 * The main API that will be used for this is the BroadcastReceiver class
 * This will allow the device to receive intents from other devices
 */

class Discover(private val context: Context, private val connectionManager: ConnectionManager) {

    private val TAG = "Discover" //tag that will be used for logs
    private var isContactSaved = false; //will be used to save devices connected to as contacts
//    private val APP_NAME = "NEXA"
    private val APP_UID: UUID =UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    interface DiscoverCallBack { // interface for callbacks when scenarios occur
        fun userFound(user: String, device: BluetoothDevice)

    }
    private var discoverCallBack: DiscoverCallBack? = null //init discoverCallBack

    /**
     * Function to set the callback
     * @param callBack The DiscoverCallback
     */
    fun setDiscoverCallBack(callBack: DiscoverCallBack) {
        this.discoverCallBack = callBack;
    }

    /**
     * Function to find devices
     * Utilizes function defined in ConnectionManager class
     */
    fun findDevices() {
        if(connectionManager.findDevices()) {               // if there are nearby devices
            Log.d(TAG,"Looking for Devices")
        }
        else{
            Log.e(TAG,"Error when begin Searching for devices")
        }
    }



    private  val discoverReceiver = object : BroadcastReceiver() { //Init broadcastReceiver object
        /**
         * Function to handle recieving a response from nearby devices when searching for devices
         * @param context The current context of the system
         * @param intent the corresponding action
         */
        override fun onReceive(context: Context,intent: Intent) { //override the onReceive method
            when(intent.action) { //when (switch) the action for the intent
                BluetoothDevice.ACTION_FOUND -> { //when we find a device
                    @Suppress("DEPRECATION") val device : BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE) //extract the BluetoothDevice identifier from intent, Suppress for backwards compatibilty

                    device?.let { //execute block if device not null
                        if (ActivityCompat.checkSelfPermission(
                                context,
                                android.Manifest.permission.BLUETOOTH_CONNECT
                            ) ==
                            PackageManager.PERMISSION_GRANTED
                        ) {
                            Log.d(TAG, "Device Found : ${it.address}")

                            // following is to fetch userprofile that is broadcasted w/ the bt device
                            val username = it.name
                            if (username?.startsWith("NEXA-")==true) {    //
                                val cleanUsername = username.removePrefix("NEXA-")

                                discoverCallBack?.userFound(cleanUsername, device)
                                Log.d("Discover", "username found ${cleanUsername} and discovercallback.userfound() called")

                            }
                            else {
                                // if username not found
                                it.fetchUuidsWithSdp()
                            }


                        }
                    }
                }

                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR) //Extract identifying key for state value of intent, error if no key exists
                    when(state) {
                        BluetoothAdapter.STATE_OFF -> {
                            Log.d(TAG,"Bluetooth is off")
                            connectionManager.endSearch()
                        }
                        BluetoothAdapter.STATE_ON -> {
                            Log.d(TAG,"Bluetooth is on")
                        }
                    }
                }
                BluetoothDevice.ACTION_UUID->{
                    val device: BluetoothDevice? =
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    val uuids =
                        intent.getParcelableArrayExtra(BluetoothDevice.EXTRA_UUID) as? Array<ParcelUuid>

                    if (device != null && uuids != null) {
                        if (uuids.any { uuid ->
                                uuid.uuid.toString() == APP_UID.toString()
                            }
                        ) {
                            // Retry parsing profile from name

                            val username = device.name
                            if (username != null) {
                                discoverCallBack?.userFound(username, device)
                                Log.d("Discover", "username found ${username} and discovercallback.userfound() called")
                            }
                            else {
                                // if username not found
                                Log.e(TAG, "Device ${device.address} matched App UUID but username not found")
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Function to add contact to device
     * registers the receiver
     */
    fun addContact(){
        if(!isContactSaved) {
            val intentFilter = IntentFilter().apply{ //IntentFilter allows us to specify which broadcasts we want to accept from contacts
                addAction(BluetoothDevice.ACTION_FOUND) //Add the broadcast for when a device is found
                addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED) //when we begin looking for devices
                addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED) //when we end our search
                addAction(BluetoothAdapter.ACTION_STATE_CHANGED) //when there is a change in state of device

                addAction(BluetoothDevice.ACTION_UUID)  // add the broadcast for when a device has a UUID (and username couldn't be found initially)
            }
            context.registerReceiver(discoverReceiver,intentFilter)
            isContactSaved = true
            Log.d(TAG,"Contact saved")
        }
    }

    /**
     * Function to delete a contact
     * De-registers the device
     */
    fun deleteContact(){ //func to delete contact
        if(isContactSaved){
            try{
                context.unregisterReceiver(discoverReceiver)
                isContactSaved = false
                Log.d(TAG, "Contact Deleted")
            }
            catch (e: IllegalArgumentException){
                Log.d(TAG, "Contact does not exist")
            }
        }
    }
}
