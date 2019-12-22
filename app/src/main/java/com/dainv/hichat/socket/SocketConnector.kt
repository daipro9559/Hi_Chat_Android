package com.dainv.hichat.socket

import android.content.Context
import android.util.Log
import com.dainv.hichat.const.SocketConstant
import com.dainv.hichat.util.SocketCallback
import com.github.nkzawa.socketio.client.Ack
import com.github.nkzawa.socketio.client.IO
import com.github.nkzawa.socketio.client.Socket
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SocketConnector
@Inject constructor(private val context: Context) {
    var socket: Socket? = null
    private var currentRoom: String? = null
    fun connectToSocket() {
        val opts = IO.Options()
        opts.forceNew = true
        opts.reconnection = false
        socket = IO.socket("http://192.168.1.199:3000", opts)
        socket?.on(Socket.EVENT_CONNECT) {
            Timber.i("Socket connected")
        }
        socket?.on("room_test") { objectData ->
            Timber.i("room test event: $objectData")
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }
        socket?.on(Socket.EVENT_DISCONNECT) {
            Timber.i("Socket disconnect")
        }
        socket?.connect()
    }

    fun connectToRoom(room: String, socketCallback: SocketCallback<String>) {
        if (socket?.connected()!!) {
            socket?.emit(SocketConstant.ROOM_INIT, room, Ack {
                currentRoom = room
                socketCallback.invoke(it[0].toString())
            })
        }
    }

    fun disconnectRoom() {
        if (currentRoom != null) {
        }
    }

    fun disconnect() {
        socket?.disconnect()!!
    }

    fun leaveRoom(){

    }

    fun sendDataOnRoom(event: String, data: String) {
        socket?.apply {
            emit(event, currentRoom, data)
        }
    }


}