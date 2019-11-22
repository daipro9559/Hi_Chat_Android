package com.dainv.hichat

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import com.dainv.hichat.base.BaseAppActivity
import com.dainv.hichat.socket.SocketConnector
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.android.synthetic.main.activity_main.*
import javax.inject.Inject

class MainActivity : BaseAppActivity() {
    @Inject
    lateinit var socketConnector: SocketConnector

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        socketConnector.connectToSocket()
        btnConnect.setOnClickListener {
            val intent = Intent(applicationContext, CallActivity::class.java)
            intent.putExtra("room", edtRoom.text.toString())
            startActivity(intent)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        socketConnector.disconnect()
    }
}
