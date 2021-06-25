package com.example.joyfly.views

import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.example.joyfly.R
import com.example.joyfly.`view-model`.ViewModel
import com.example.joyfly.model.Model
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.nambimobile.widgets.efab.FabOption
import io.github.controlwear.virtual.joystick.android.JoystickView
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.cos
import kotlin.math.sin


class MainActivity : AppCompatActivity() {
    private lateinit var model: Model
    private lateinit var viewModel: ViewModel
    private lateinit var throttle_seek: SeekBar
    private lateinit var rudder_seek: SeekBar
    private lateinit var joystick: JoystickView
    private lateinit var connected_dis_status: TextView
    private lateinit var tryConProgress: LinearProgressIndicator
    private lateinit var devButton: FabOption
    private lateinit var devLinearLayout: LinearLayout
    private var changeView: Boolean = true
    private var devMode: Boolean = false

    /**
     * Create the application instance.
     * @param savedInstanceState
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // Add logo to top of application
        supportActionBar?.setDisplayShowHomeEnabled(true);
        supportActionBar?.setLogo(R.drawable.fly_joy_logo);
        supportActionBar?.setDisplayUseLogoEnabled(true);
        this.viewModel = ViewModel()
        this.tryConProgress = findViewById<LinearProgressIndicator>(R.id.tryConProgress)
        this.connected_dis_status = findViewById<TextView>(R.id.connected_dis_status)
        this.devButton = findViewById<FabOption>(R.id.developer_mode)
        this.devLinearLayout = findViewById<LinearLayout>(R.id.dev_linearLayout)
        // set what happens for each message we get from the model about the connection status.
        val connectObserver = Observer<String> { status ->
            when (status) {

                "CON" -> {
                    this.tryConProgress.visibility = View.INVISIBLE
                    this.connected_dis_status.text = "connected"
                    this.connected_dis_status.setTextColor(Color.GREEN)
                }
                "DISCON" -> {
                    this.tryConProgress.visibility = View.INVISIBLE
                    this.connected_dis_status.text = "disconnected"
                    this.connected_dis_status.setTextColor(Color.RED)
                }
                "TRYCON" -> {
                    this.tryConProgress.visibility = View.VISIBLE
                    this.connected_dis_status.text = "trying to connect..."
                    this.connected_dis_status.setTextColor(resources.getColor(R.color.design_default_color_primary))
                }
                "ERR" -> {
                    this.tryConProgress.visibility = View.INVISIBLE
                    this.connected_dis_status.text = "disconnected"
                    this.connected_dis_status.setTextColor(Color.RED)
                    alert(R.string.errConnectTitle, R.string.errConnectMessage)
                }
            }
        }
        // react to changes made in isConnected with connectObserver.
        this.viewModel.isConnected.observe(this, connectObserver)
        this.joystick = findViewById<JoystickView>(R.id.joystick)
        joystick.setOnMoveListener { angle, strength ->
            if (viewModel.isConnected.value == "CON") {
                viewModel.VM_Aileron =
                    (cos(Math.toRadians(angle.toDouble())) * strength / 200).toFloat()
                viewModel.VM_Elevator =
                    (sin(Math.toRadians(angle.toDouble())) * strength / 200).toFloat()
            }
        }

        this.throttle_seek = findViewById<SeekBar>(R.id.throttle_seek)
        this.throttle_seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            /**
             * If connected to Flight Gear, change the value of VM_Throttle to the value of the progress,
             * then it will change the value in the model which will send the command to Flight Gear.
             * Else, keep the progress at 0.
             */
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (viewModel.isConnected.value == "CON") {
                    viewModel.VM_Throttle = progress.toFloat() / 1000
                } else {
                    throttle_seek.progress = 0
                }
            }

            /**
             * Before starting to track check if the app is connected to Flight Gear.
             */
            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                if (checkStatus()) {
                    alert(R.string.notConnectedTitle, R.string.notConnectedMessage)
                }
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }
        })
        this.rudder_seek = findViewById<SeekBar>(R.id.rudder_seek)
        this.rudder_seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            /**
             * If connected to Flight Gear, change the value of VM_Rudder to the value of the progress,
             * then it will change the value in the model which will send the command to Flight Gear.
             * Else, keep the progress at 500.
             */
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (viewModel.isConnected.value == "CON") {
                    viewModel.VM_Rudder = (progress - 500).toFloat() / 500
                } else {
                    // reset to middle
                    rudder_seek.progress = 500
                }
            }

            /**
             * Before starting to track check if the app is connected to Flight Gear.
             */
            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                if (checkStatus()) {
                    alert(R.string.notConnectedTitle, R.string.notConnectedMessage)
                }
            }

            /**
             * When the user stops holding the seekbar it will set the value to the middle.
             */
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                // reset to middle.
                rudder_seek.progress = 500
            }
        })

    }

    /**
     * Check if the app is connected to Flight Gear.
     */
    private fun checkStatus(): Boolean {
        return viewModel.isConnected.value == "DISCON" || viewModel.isConnected.value == "ERR"
    }

    /**
     * Calculate the warp value needed for the correct time with default timezone
     * configurations on Flight Gear which is set to UTC+10:00
     * @param timeToSet The time you want to warp to.
     */
    private fun getWarp(timeToSet: Int): Int {
        val sdf = SimpleDateFormat("HH")
        val dateString: String = sdf.format(Date(System.currentTimeMillis()))
        val time = (dateString.toInt() - 10) % 24
        val delta = if (timeToSet < time) timeToSet % time else timeToSet - time
        // return hours in seconds.
        return delta * 3600
    }

    /**
     * Custom made alert for showing errors.
     * @param title The title of the alert.
     * @param message the message of the alert.
     */
    private fun alert(title: Int, message: Int) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setIconAttribute(android.R.attr.alertDialogIcon)
            .create()
            .show()
    }

    /**
     * On click will check the values entered in the edittext boxes for the IP and port,
     * if they are correct and in range we can try to connect to the Flight Gear server.
     */
    fun connectClicked(view: View) {
        if (checkStatus()) {
            val ip: String = findViewById<EditText>(R.id.edit_text_ip).text.toString()
            val portValue: String = findViewById<EditText>(R.id.edit_text_port).text.toString()
            var port = 0
            // Check if the ip address entered is valid
            if (!Patterns.IP_ADDRESS.matcher(ip).matches()) {
                alert(R.string.wrongIPTitle, R.string.wrongIPMessage)
                return
            }
//         Check if port is empty
            if (portValue != "") {
                // if not then try to convert into Int
                port = portValue.toInt()
                if (port !in 1025..65534) {
                    alert(R.string.wrongPortTitle, R.string.wrongPortMessage)
                    return
                }
            } else {
                alert(R.string.emptyPortTitle, R.string.emptyPortMessage)
                return
            }
            // Try to connect to the Flight Gear server.
            viewModel.connect(ip, port)
        } else {
            alert(R.string.alreadyConnectedTitle, R.string.alreadyConnectedMessage)
        }

    }

    /**
     * Change the view of the plane between first person and 3rd person.
     */
    fun onClickChangeView(view: View) {
        if (checkStatus()) {
            alert(R.string.notConnectedTitle, R.string.notConnectedMessage)
            return
        }
        changeView = if (changeView) {
            this.viewModel.sendInfo("/sim/current-view/view-number", "1")
            false
        } else {
            this.viewModel.sendInfo("/sim/current-view/view-number", "0")
            true
        }
    }

    /**
     * Set the Flight Gear time to morning time (11 a.m)
     */
    fun onClickSetMorning(view: View) {
        if (checkStatus()) {
            alert(R.string.notConnectedTitle, R.string.notConnectedMessage)
            return
        }
        this.viewModel.sendInfo("/sim/time/warp", getWarp(11).toString())
    }

    /**
     * Set the Flight Gear time to evening time (7 p.m)
     */
    fun onClickSetEvening(view: View) {
        if (checkStatus()) {
            alert(R.string.notConnectedTitle, R.string.notConnectedMessage)
            return
        }
        this.viewModel.sendInfo("/sim/time/warp", getWarp(19).toString())
    }

    /**
     * Auto start the Cessna C172P plane.
     * Used the same controls from the script found in its source code:
     * https://github.com/c172p-team/c172p/blob/master/Nasal/c172p.nas
     */
    fun onClickAutoStart(view: View) {
        if (checkStatus()) {
            alert(R.string.notConnectedTitle, R.string.notConnectedMessage)
            return
        }
        // Setting levers and switches for startup
        this.viewModel.sendInfo("/controls/switches/magnetos", "3")
        this.viewModel.sendInfo("/controls/engines/current-engine/throttle", "0.2")

        this.viewModel.sendInfo("/controls/engines/current-engine/mixture", "1")

        this.viewModel.sendInfo("/controls/flight/elevator-trim", "0.0")
        this.viewModel.sendInfo("/controls/switches/master-bat", "1")
        this.viewModel.sendInfo("/controls/switches/master-alt", "1")
        this.viewModel.sendInfo("/controls/switches/master-avionics", "1")

        //Setting lights
        this.viewModel.sendInfo("/controls/lighting/nav-lights", "1")
        this.viewModel.sendInfo("/controls/lighting/strobe", "1")
        this.viewModel.sendInfo("/controls/lighting/beacon", "1")

        // Setting flaps to 0
        this.viewModel.sendInfo("/controls/flight/flaps", "0.0")

        // Pre-flight inspection
        this.viewModel.sendInfo("/sim/model/c172p/cockpit/control-lock-placed", "0")
        this.viewModel.sendInfo("/sim/model/c172p/brake-parking", "0")
        this.viewModel.sendInfo("/sim/model/c172p/securing/chock", "0")
        this.viewModel.sendInfo("/sim/model/c172p/securing/cowl-plugs-visible", "0")
        this.viewModel.sendInfo("/sim/model/c172p/securing/pitot-cover-visible", "0")
        this.viewModel.sendInfo("/sim/model/c172p/securing/tiedownL-visible", "0")
        this.viewModel.sendInfo("/sim/model/c172p/securing/tiedownR-visible", "0")
        this.viewModel.sendInfo("/sim/model/c172p/securing/tiedownT-visible", "0")

        // removing any ice from the carburetor
        this.viewModel.sendInfo("/engines/active-engine/carb_ice", "0.0")
        this.viewModel.sendInfo("/engines/active-engine/carb_icing_rate", "0.0")
        this.viewModel.sendInfo("/engines/active-engine/volumetric-efficiency-factor", "0.85")

        // Removing any contamination from water
        this.viewModel.sendInfo("/consumables/fuel/tank[0]/water-contamination", "0.0")
        this.viewModel.sendInfo("/consumables/fuel/tank[1]/water-contamination", "0.0")
        this.viewModel.sendInfo("/consumables/fuel/tank[0]/sample-water-contamination", "0.0")
        this.viewModel.sendInfo("/consumables/fuel/tank[1]/sample-water-contamination", "0.0")

        this.viewModel.sendInfo("/controls/engines/engine[0]/primer-lever", "0")
        this.viewModel.sendInfo("/controls/engines/engine/primer", "3")

        // All set, starting engine
        this.viewModel.sendInfo("/controls/switches/starter", "1")
        this.viewModel.sendInfo("/engines/active-engine/auto-start", "1")

        this.viewModel.sendInfo("sim/model/open-pfuel-cap", "0")
        this.viewModel.sendInfo("sim/model/open-sfuel-cap", "0")
        this.viewModel.sendInfo("sim/model/open-pfuel-sump", "0")
        this.viewModel.sendInfo("sim/model/open-sfuel-sump", "0")
    }

    /**
     * Turns the developer mode on and off.
     */
    fun onClickDevMode(view: View) {
        devMode = if (devMode) {

            this.devButton.fabOptionColor = Color.parseColor("#9280FF")
            this.devButton.label.labelBackgroundColor = Color.parseColor("#B5A9FC")
            this.devButton.label.labelText = "Developer Mode: OFF"
            this.devLinearLayout.visibility = View.INVISIBLE
            false
        } else {
            this.devButton.fabOptionColor = Color.parseColor("#6EB63F")
            this.devButton.label.labelBackgroundColor = Color.parseColor("#80BA59")
            this.devButton.label.labelText = "Developer Mode: ON"
            this.devLinearLayout.visibility = View.VISIBLE
            true
        }

    }

    /**
     * On click the data inside the control and value will be sent to Flight Gear.
     */
    fun onClickDevSend(view: View) {
        if (checkStatus()) {
            alert(R.string.notConnectedTitle, R.string.notConnectedMessage)
            return
        }
        val control = findViewById<EditText>(R.id.dev_control).text.toString()
        val value = findViewById<EditText>(R.id.dev_value).text.toString()
        this.viewModel.sendInfo(control, value)
    }

}