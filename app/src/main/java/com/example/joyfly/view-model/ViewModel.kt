package com.example.joyfly.`view-model`

import androidx.lifecycle.LiveData
import com.example.joyfly.model.Model

class ViewModel(model : Model) {
    private var fgModel:Model = model
    // Live data of a string to get changes to view from model.
    val isConnected: LiveData<String> get() = this.fgModel.isConnected

    var VM_Throttle: Float
        get() = this.fgModel.Throttle
        set(value) { this.fgModel.Throttle = value}

    var VM_Rudder: Float
        get() = this.fgModel.Rudder
        set(value) { this.fgModel.Rudder = value}

    var VM_Elevator: Float
        get() = this.fgModel.Elevator
        set(value) { this.fgModel.Elevator = value}

    var VM_Aileron: Float
        get() = this.fgModel.Aileron
        set(value) { this.fgModel.Aileron = value}

    /**
     * Connect using IP and port on the model.
     * @param ip ip of server.
     * @param port port of server.
     */
    fun connect(ip : String, port : Int){
        this.fgModel.connect(ip,port)
    }

    /**
     * Send info to model which sends the info the Flight Gear.
     * @param property the property you want to change.
     * @param value the value you want to set for the given property.
     */
    fun sendInfo(property: String, value: String){ fgModel.sendInfo(property,value)}

}