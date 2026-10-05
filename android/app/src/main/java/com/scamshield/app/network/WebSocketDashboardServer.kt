package com.scamshield.app.network

import android.content.Context
import android.net.wifi.WifiManager
import android.text.format.Formatter
import com.google.gson.Gson
import com.scamshield.core.network.dto.DashboardPayload
import org.java_websocket.WebSocket
import org.java_websocket.handshake.ClientHandshake
import org.java_websocket.server.WebSocketServer
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.util.Collections

class WebSocketDashboardServer(
    private val context: Context,
    private val port: Int = 8765
) : DashboardTransport {

    private val gson = Gson()
    private var server: InternalServer? = null

    private inner class InternalServer(address: InetSocketAddress) : WebSocketServer(address) {
        override fun onOpen(conn: WebSocket?, handshake: ClientHandshake?) {
            // Client connected (e.g. Laptop Dashboard)
        }

        override fun onClose(conn: WebSocket?, code: Int, reason: String?, remote: Boolean) {
            // Client disconnected
        }

        override fun onMessage(conn: WebSocket?, message: String?) {
            // Inbound messages (e.g. ping or handshake)
        }

        override fun onError(conn: WebSocket?, ex: Exception?) {
            ex?.printStackTrace()
        }

        override fun onStart() {
            // Server started
        }
    }

    override fun start(port: Int) {
        if (server != null) return
        try {
            val address = InetSocketAddress(port)
            server = InternalServer(address).apply {
                isReuseAddr = true
                start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun stop() {
        try {
            server?.stop()
        } catch (e: Exception) {
            // Ignored
        } finally {
            server = null
        }
    }

    override fun broadcast(payload: DashboardPayload) {
        try {
            val json = gson.toJson(payload)
            server?.broadcast(json)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun isConnected(): Boolean {
        return (server?.connections?.size ?: 0) > 0
    }

    override fun getLocalIpAddress(): String {
        try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            val ipInt = wifiManager?.connectionInfo?.ipAddress ?: 0
            if (ipInt != 0) {
                return Formatter.formatIpAddress(ipInt)
            }

            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (intf in interfaces) {
                val addrs = Collections.list(intf.inetAddresses)
                for (addr in addrs) {
                    if (!addr.isLoopbackAddress && addr is java.net.Inet4Address) {
                        return addr.hostAddress ?: "127.0.0.1"
                    }
                }
            }
        } catch (e: Exception) {
            // Ignored
        }
        return "127.0.0.1"
    }
}
