package Package.NEXA.logic
import Package.NEXA.cryptography.KeyManager
import Package.NEXA.data.DbProfileProvider
import Package.NEXA.data.UserProfile
import Package.NEXA.data.Waiting
import android.Manifest
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.app.Activity
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import java.io.IOException
import java.io.OutputStream
import java.util.UUID
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.Locale

/**
 * Class that facilitates connecting and sending of messages
 * @param context The current context of the system
 */
open class ConnectionManager(private  val context: Context){
    private val APP_NAME = "NEXA"
    private val APP_UID: UUID =UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private val TAG = "ConnectionManager"
    val bluetoothAdapter: BluetoothAdapter? by lazy { //by lazy ensures this code only runs once (when the user opens the app for the first time),
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    } // gets adapter
    var clientSocket: BluetoothSocket?=null

    enum class ConnectionState{ //enum for the different states the device could be in when trying to connect to a device
        NONE,
        LISTENING,
        CONNECTED
    }

    private var connectionState = ConnectionState.NONE //init the connectionState of the device to null at first

    interface ConnectionCallBack { //interface to handle  the callbacks recieved by the device for different outcomes
        fun onMsgRecieve(message: Message)
    }

    public var connectionCallBack: ConnectionCallBack? = null //set initial callback as null

    /**
     * Function to convert the Address to Uppercase for Normalisation
     * @param addr The address to be converted to uppercase
     */
    private fun norm(addr: String) = addr.uppercase(Locale.ROOT)

    /**
     * Function to check if Bluetooth is enabled on the device
     */
    fun isBluetoothEnabled(): Boolean { //check if the bluetoothAdapter is enabled
        return bluetoothAdapter?.isEnabled == true
    }

    /**
     * Function to find devices
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    fun findDevices(): Boolean {                          // main method that starts discovery
        val adapter = bluetoothAdapter ?: return false

        if (!adapter.isEnabled) {                            // checks if bluetooth working
            Log.e("ConnectionManager", "Bluetooth is OFF")
            return false
        }

        if (adapter.isDiscovering) {                       // checks if already discovering
            Log.d("ConnectionManager", "Already discovering, cancelling first")
            //adapter.cancelDiscovery()
        }

        val started = adapter.startDiscovery()              // returns true/false whether discovery was started
        Log.d("ConnectionManager", "Discovery started: $started !")
        return started
    }

    fun endSearch(): Boolean { //stop scanning for other devices
        return if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
            bluetoothAdapter?.cancelDiscovery() ?:false
        }
        else{
            false
        }
    }

    /**
     * Function to make a connection to other devices
     * @param device the Device which the source device is attempting to make a connection with
     */
    @Synchronized fun makeConnection(device: BluetoothDevice) {

        Log.d(TAG, "Connecting to ${device.address}")    // for logcat debugging

        if (!isBluetoothEnabled()) { // checks if bluetooth isn't enabled
            Log.e(TAG, "Bluetooth is not enabled")
            return
        }
        if (ActivityCompat.checkSelfPermission(context,
                android.Manifest.permission.BLUETOOTH_CONNECT
            )
            == PackageManager.PERMISSION_GRANTED
        ) { // checks if user has given permission to connect via bluetooth
            val socket = device.createInsecureRfcommSocketToServiceRecord(APP_UID)

            try {
                Log.d(TAG, "Client connecting to ${device.address}…")
                bluetoothAdapter?.cancelDiscovery()
                socket.connect()               // tries to connect to socket
                clientSocket = socket
                Log.d(TAG, "Client connected to ${device.address}")

                CoroutineScope(Dispatchers.IO).launch {
                    tContact(device)
                }

                announceMyPubKey(socket.outputStream)

                // peer stores it, then flush queued messages
                CoroutineScope(Dispatchers.IO).launch {
                    kotlinx.coroutines.delay(200)
                    sendQueuedMsg(device.address, socket.outputStream)
                }

                //Start reader after handshake kicked off
                startClientReader(socket)


                CoroutineScope(Dispatchers.IO).launch {
                    sendQueuedMsg(device.address, socket.outputStream)
                }
                announceMyPubKey(socket.outputStream)
                startClientReader(socket)

            }
            catch (e: IOException) {
                Log.e("connection", "unable to connect")
                //print("unable to connect")
                e.printStackTrace()
            }

        }
    }

    /**
     * Function to continuously listen for incoming messages from the connected socket
     * @param btSocket The socket from which messages are being recieved from
     */
    private fun startClientReader(btSocket: BluetoothSocket) {
        Thread {
            try {
                val reader = BufferedReader(InputStreamReader(btSocket.inputStream, Charsets.UTF_8))
                val addr = norm(btSocket.remoteDevice.address)
                CoroutineScope(Dispatchers.IO).launch {
                    tContact(btSocket.remoteDevice)
                }



                while (true) {                                                              //Loop in order to continuously receive messages
                    val json = reader.readLine() ?: break
                    val message = Gson().fromJson(json, Message::class.java)     //Convert the Received JSON to Message
                    when {
                        message.message.startsWith("PUBLIC_KEY:") -> {
                            val pubKey = message.message.removePrefix("PUBLIC_KEY:") //Retrieve the public key from the message
                            CoroutineScope(Dispatchers.IO).launch {
                                val db = DbProfileProvider.getDB(context)
                                val userProfile = db.userProfileRoomAccess()
                                userProfile.updatePubKey(addr, pubKey)     //Update the userProfile entity with its Public Key
                                Log.d(TAG, "Updated public key for $addr")
                            }
                            continue
                        }

                        message.message.startsWith("ENC_RSA:") -> {
                            val b64 = message.message.removePrefix("ENC_RSA:") //Obtain encrypted private key from Prefix
                            val privKey = KeyManager.getPrivKey()
                            if (privKey == null) {
                                Log.e(TAG, "Decrypt failed. Failed to get private key")
                                continue

                            }

                            val plainTxt = KeyManager.decrypt(b64, privKey) //Attempt to decrypt the Private Key
                            if (plainTxt == null) {
                                Log.e(TAG, "Decrypt failed (client side)")
                                continue
                            }

                            val received = Message(
                                id = java.util.UUID.randomUUID(),
                                author = message.author,
                                message = plainTxt,
                                status = Message.Status.RECEIVED,
                                isMine = false
                            )
                            // notify UI
                            connectionCallBack?.onMsgRecieve(received)
                            CoroutineScope(Dispatchers.IO).launch {
                                val dao = DbProfileProvider.getDB(context).userProfileRoomAccess()
                                val peer = dao.fetchProfileByAddress(addr)
                                if (peer != null) {
                                    // store under MAC address thread, mine=false
                                    peer.returnMessageHistory(addr).add(
                                        received.copy(isMine = false, status = Message.Status.RECEIVED)
                                    )
                                    dao.insertProfile(peer)
                                }
                            }
                            continue
                        }
                        else -> {
                            val received = Message(
                                id = java.util.UUID.randomUUID(),
                                author = message.author,
                                message = message.message,
                                status = Message.Status.RECEIVED,
                                isMine = false
                            )
                            // notify UI
                            connectionCallBack?.onMsgRecieve(received)
                            CoroutineScope(Dispatchers.IO).launch {
                                val dao = DbProfileProvider.getDB(context).userProfileRoomAccess()
                                val peer = dao.fetchProfileByAddress(addr)
                                if (peer != null) {
                                    // store under MAC address thread, mine=false
                                    peer.returnMessageHistory(addr).add(
                                        received.copy(isMine = false, status = Message.Status.RECEIVED)
                                    )
                                    dao.insertProfile(peer)
                                }
                            }

                        }

                    }
                }


            }
            catch (e: java.io.IOException) {
                Log.e(TAG, "Client read loop stopped: ${e.message}")
            }
        }.start()
    }

    /**
     * Function to send messages to destination device
     * @param message The Message to be sent
     * @param device The Destination Device for the message
     */
    open fun sendMessage(message: Message, device: BluetoothDevice) {
        val socket = clientSocket

        if (socket == null || !socket.isConnected) { //Checks if no connection is made
            // OFFLINE: queue + persist history so UI shows it
            CoroutineScope(Dispatchers.IO).launch {
                val wire = buildWireMessageForIO(device.address, message, out = null)
                val jsonLine = Gson().toJson(wire)
                enqueueForLater(device.address, jsonLine) //Adds message to Waiting Database

                val dao = DbProfileProvider.getDB(context).userProfileRoomAccess()
                val addr = norm(device.address)
                dao.fetchProfileByAddress(addr)?.let { peer -> //Obtain the User Profiles from the DB
                    peer.returnMessageHistory(addr).add(
                        message.copy(isMine = true, status = Message.Status.SENT) // or QUEUED if you add it
                    )
                    dao.insertProfile(peer)  //Inserts the user Profiles into the DB with updated message history
                }
                Log.w(TAG, "Not connected — queued and persisted. addr=$addr")
            }
            return
        }

        // CONNECTED: send once, queue if write fails
        CoroutineScope(Dispatchers.IO).launch {
            val out = socket.outputStream
            val wire = buildWireMessageForIO(device.address, message, out)
            val jsonLine = Gson().toJson(wire)
            val addr = norm(device.address)

            try {
                out.write(jsonLine.toByteArray(Charsets.UTF_8)) //Sends Messages using the socket
                out.write('\n'.code)
                out.flush()
                Log.d(TAG, "message sent to $addr")

                val dao = DbProfileProvider.getDB(context).userProfileRoomAccess()
                dao.fetchProfileByAddress(addr)?.let { peer ->
                    peer.returnMessageHistory(addr).add(
                        message.copy(isMine = true, status = Message.Status.SENT)
                    )
                    dao.insertProfile(peer) //Update the database for the peer with new message
                }
            } catch (e: IOException) {
                enqueueForLater(addr, jsonLine)
                Log.e(TAG, "Send failed — enqueued for later. addr=$addr", e)
            }
        }
    }


    /**
     * Convert message to JSON and sends via an OutputStream
     * @param out The OutputStream message is sent via
     * @param msg The message that is converted to JSON
     */
    fun writeToJson(out: OutputStream, msg: Message){
        val gson = Gson()
        val json = gson.toJson(msg)
        out.write(json.toByteArray(Charsets.UTF_8))
        out.write('\n'.code)
        out.flush()
    }

    /**
     * Function to transfer Public Key
     * @param out The OutputStream used to announce the public key
     */
    fun announceMyPubKey(out: OutputStream)
    {
        if(KeyManager.getPrivKey() == null) //If the private key is not generated
        {
            try {
                KeyManager.generateKeyPair()
            }
            catch(e: Exception)
            {
                Log.e(TAG, "Failed to generate key pair: ${e.message}")
            }
        }

        val myPub64 = KeyManager.getPublicKey64() //Attempt to get the Public Key
        if(myPub64.isNullOrBlank())
        {
            Log.e(TAG, "No pubKey")
            return
        }

        val msg = Message(          //Message containing the public key
            id = UUID.randomUUID(),
            author = "pubKey",
            message = "PUBLIC_KEY:$myPub64",
            status = Message.Status.SENT,
            isMine = true
        )
        writeToJson(out, msg)
        Log.d(TAG, "Sent pubKey")
    }


    /**
     * Function to make a device discoverable by other devices
     * @param duration The time that a device is discoverable for
     */
    fun makeDiscoverable(duration: Int = 300): Boolean {
        Log.d("connection manager", "make discoverable --- true")
        val discoverable = android.content.Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)
        discoverable.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, duration)
        (context as? Activity)?.startActivity(discoverable)
        return true

    }

    /**
     * Function to enable a device to Listen for Connections
     */
    @Synchronized
    fun listenForConnections() {
        if (!isBluetoothEnabled()) return

        if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) { // checks permissions

            Thread {        // starts background thread object
                try {
                    val serverSocket = bluetoothAdapter?.listenUsingInsecureRfcommWithServiceRecord(
                        APP_NAME,
                        APP_UID
                    )   // creates socket
                    Log.d("Connection Manager - adapter name:", bluetoothAdapter?.name.toString())
                    connectionState =
                        ConnectionState.LISTENING           // changes connection state -- waiting for connection

                    Log.d(TAG, "Server waiting for connection…")
                    val btSocket = serverSocket?.accept() // blocking until a device connects
                    Log.d(TAG, "Server accepted from ${btSocket?.remoteDevice?.address}")

                    serverSocket?.close()
                    Log.d(TAG, "Connection accepted")
                    if (btSocket != null) {                     // connection succcessfull
                        clientSocket = btSocket
                        connectionState = ConnectionState.CONNECTED

                        CoroutineScope(Dispatchers.IO).launch {
                            tContact(btSocket.remoteDevice)
                        }

                        announceMyPubKey(btSocket.outputStream)

                        //Small pause, then flush any queued messages for this MAC
                        CoroutineScope(Dispatchers.IO).launch {
                            kotlinx.coroutines.delay(200)
                            sendQueuedMsg(btSocket.remoteDevice.address, btSocket.outputStream)
                        }

                        CoroutineScope(Dispatchers.IO).launch {
                            sendQueuedMsg(btSocket.remoteDevice.address, btSocket.outputStream)
                        }


                        announceMyPubKey(btSocket.outputStream)

                        val reader =
                            BufferedReader(InputStreamReader(btSocket.inputStream, Charsets.UTF_8))
                        val remoteAddr = norm(btSocket.remoteDevice.address)

                        while (true) {
                            val json = reader.readLine() ?: break
                            val gson = Gson()
                            val message = gson.fromJson(json, Message::class.java)
                            //Checks if message is a public key
                            Log.d("Connection M", "you made it here")
                            Log.d("Connection M", message.message)
                            //Log.d("message received: ", "$message")

                            when {
                                message.message.startsWith("PUBLIC_KEY:") -> {
                                    val pubKey = message.message.removePrefix("PUBLIC_KEY:")
                                    CoroutineScope(Dispatchers.IO).launch {
                                        val db = DbProfileProvider.getDB(context)
                                        db.userProfileRoomAccess().updatePubKey(remoteAddr, pubKey)
                                        Log.d(TAG, "Updated pubKey")
                                    }
                                    continue
                                }

                                message.message.startsWith("ENC_RSA:") -> {
                                    val b64 = message.message.removePrefix("ENC_RSA:")
                                    val privKey = KeyManager.getPrivKey()
                                    if (privKey == null) {
                                        Log.e(TAG, "No privkey for decryption (server)")
                                        continue
                                    }

                                    val plain = KeyManager.decrypt(b64, privKey)
                                    if (plain == null) {
                                        Log.e(TAG, "Decrypt failed (server)")
                                        CoroutineScope(Dispatchers.IO).launch {
                                            val dao = DbProfileProvider.getDB(context).userProfileRoomAccess()
                                            dao.updatePubKey(remoteAddr, "")  // invalidate stale key
                                            Log.w(TAG, "Peer pubkey cleared for $remoteAddr after BAD_DECRYPT")
                                        }
                                        continue
                                    }

                                    val receivedMsg = Message(
                                        id = UUID.randomUUID(),
                                        author = message.author,
                                        message = plain,
                                        status = Message.Status.RECEIVED,
                                        isMine = false
                                    )

                                    CoroutineScope(Dispatchers.IO).launch {
                                        val userDAO = DbProfileProvider.getDB(context).userProfileRoomAccess()
                                        val profile = userDAO.fetchProfileByAddress(remoteAddr)
                                        if (profile != null) {
                                            profile.returnMessageHistory(remoteAddr).add(receivedMsg)
                                            userDAO.insertProfile(profile)
                                            Log.d(TAG, "saved to chat history")
                                        }
                                    }

                                    connectionCallBack?.onMsgRecieve(receivedMsg)
                                    continue
                                }



                                else -> {
                                    val rcvdMesg = Message(
                                        id = UUID.randomUUID(),
                                        author = message.author,
                                        message = message.message,
                                        status = Message.Status.RECEIVED,
                                        isMine = false
                                    )
                                    connectionCallBack?.onMsgRecieve(rcvdMesg)

                                    CoroutineScope(Dispatchers.IO).launch {
                                        val userDAO = DbProfileProvider.getDB(context).userProfileRoomAccess()
                                        val profile = userDAO.fetchProfileByAddress(remoteAddr)
                                        if (profile != null) {
                                            profile.returnMessageHistory(remoteAddr).add(
                                                rcvdMesg.copy(isMine = false, status = Message.Status.RECEIVED)
                                            )
                                            userDAO.insertProfile(profile)
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (e: IOException) {
                    Log.e(TAG, "Server listen failed: ${e.message}")
                }

            }.start()
        }
    }

    /**
     * Function to convert the Message into wire message before sending over Bluetooth
     * @param addrRaw The mac address of the destination address
     * @param message The message to be converted
     * @param out The output stream via which the message is sent from
     */
    private suspend fun buildWireMessageForIO(
        addrRaw: String,
        message: Message,
        out: OutputStream?
    ): Message { //message to be sent
        val addr = norm(addrRaw)
        val dao = DbProfileProvider.getDB(context).userProfileRoomAccess()
        val peer = dao.fetchProfileByAddress(addr)
        val peerPub64 = peer?.publicKey

        return if (!peerPub64.isNullOrBlank()) {
            val pub = KeyManager.decodePubKey(peerPub64)
            val ct = pub?.let { KeyManager.encrypt(message.message, it) }
            if (!ct.isNullOrBlank()) {
                message.copy(message = "ENC_RSA:$ct")
            } else {
                if (out != null) runCatching { announceMyPubKey(out) }
                message
            }
        } else {
            if (out != null) runCatching { announceMyPubKey(out) }
            message
        }
    }

    /**
     * Function that sends any queued messages from the database to the bluetoothsocket
     * @param addrRaw MAC address of destination device
     * @param out The output stream from which the message is sent
     */
    private suspend fun sendQueuedMsg(addrRaw: String, out:OutputStream) {
        val addr = norm(addrRaw)
        val db = DbProfileProvider.getDB(context)
        val items = db.waitingRoomAccess().fetchWaiting(addr)
        Log.d(TAG, "sendStoredMsg: addr=$addr count=${items.size}")
        for (item in items) {
            val ok = runCatching {
                out.write(item.jSon.toByteArray(Charsets.UTF_8))
                out.write('\n'.code)
                out.flush()
            }.isSuccess
            if (ok) {
                Log.d(TAG, "sendStoredMsg: sent ${item.id}")
                db.waitingRoomAccess().deleteWaiting(item.id)
            } else {
                Log.w(TAG, "sendStoredMsg: write failed for ${item.id}, will retry later")
                db.waitingRoomAccess().bump(item.id)
                break
            }
        }
    }

    /**
     * Function that Queues Message to send later
     * @param addrRaw The address of the destination device
     * @param jsonLine Message in Json Format
     */
    private fun enqueueForLater(addrRaw: String, jsonLine: String) {
        val addr = norm(addrRaw)
        Log.d(TAG, "enqueueForLater: addr=$addr jsonLen=${jsonLine.length}")
        CoroutineScope(Dispatchers.IO).launch {
            val db = DbProfileProvider.getDB(context)
            db.waitingRoomAccess().insertWaiting(Waiting(deviceAddress = addr, jSon = jsonLine))
        }
    }

    //Function used for ensuring a chat is created when connecting to a device
    suspend fun tContact(device: BluetoothDevice)
    {
        //Device address
        val addr = norm(device.address)
        //Init db
        val dao = DbProfileProvider.getDB(context).userProfileRoomAccess()
        //check the device name
        val name = (try { device.name } catch (_: SecurityException) { null })
            ?.removePrefix("NEXA-") ?: addr

        //check if the device is already in the database
        val exist = dao.fetchProfileByAddress(addr)
        if(exist == null)
        {
            //add device to the db
            dao.insertProfile(
                UserProfile(
                    username = name,
                    publicKey = "",
                    address = addr
                )
            )
            Log.d(TAG, "Contact added: $addr ($name)")
        }
        //else just fetch their profile
        else if(exist.username != name || exist.address != addr)
        {
            dao.insertProfile(exist.copy(username = name, address = addr))
            Log.d(TAG, "Contact updated: $addr ($name)")
        }
    }

}

