package com.example.joyfly.model

import androidx.lifecycle.MutableLiveData
import java.io.IOException
import java.io.PrintWriter
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.ForkJoinPool

class Model {
    private lateinit var fgSocket: Socket
    private lateinit var out: PrintWriter
    private var throttle: Float = 0f
    private var rudder: Float = 0f
    private var elevator: Float = 0f
    private var aileron: Float = 0f
    // The live data object which will send status changes to the view.
    val isConnected: MutableLiveData<String> = MutableLiveData<String>("DISCON")

    // The thread pool.
    private val fj: ForkJoinPool =
        ForkJoinPool(1, ForkJoinPool.defaultForkJoinWorkerThreadFactory, null, true)

    var Throttle: Float
        get() = this.throttle
        set(value) {
            this.throttle = value
            sendInfo("/controls/engines/current-engine/throttle", value.toString())
        }

    var Rudder: Float
        get() = this.rudder
        set(value) {
            this.rudder = value
            sendInfo("/controls/flight/rudder", value.toString())
        }

    var Elevator: Float
        get() = this.elevator
        set(value) {
            this.elevator = value
            sendInfo("/controls/flight/elevator", value.toString())
        }

    var Aileron: Float
        get() = this.aileron
        set(value) {
            this.aileron = value
            sendInfo("/controls/flight/aileron", value.toString())
        }


    /**
     * Connect using IP and port.
     * @param ip ip of server.
     * @param port port of server.
     */
    fun connect(ip: String, port: Int) {
        fj.execute(
            Runnable {
                if (isConnected.value == "DISCON" || isConnected.value == "ERR") {
                    try {
                        isConnected.postValue("TRYCON")
                        // Create socket
                        fgSocket = Socket()
                        // Connect with 5s timeout
                        fgSocket.connect(InetSocketAddress(ip, port), 5000)
                        isConnected.postValue("CON")
                        out = PrintWriter(fgSocket.getOutputStream(), true)
                    } catch (e: Exception) {
                        isConnected.postValue("ERR")
                        println("Exception in connect.\nInfo: $e")
                    }
                }
            }
        )
    }

    /**
     * Send info to Flight Gear server using print writer and through the socket.
     */
    fun sendInfo(control: String, value: String) {
        fj.execute(
            Runnable {
                try {
                    out.print("set $control $value\r\n")
                    out.flush()
                    if (out.checkError()) throw IOException()
                } catch (e: Exception) {
                    println("Exception in sendInfo.\nInfo: $e")
                    fgSocket.close()
                    isConnected.postValue("DISCON")
                }
            })
    }
}