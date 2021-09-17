package com.poilabs.vd_navigation_implementation

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.getpoi.android_vd_nav_ui.PoiColorManager
import com.getpoi.android_vd_nav_ui.VDBaseFragment
import com.poilabs.vd.nav.non.ui.jsonclient.VDResponseListener
import com.poilabs.vd.nav.non.ui.models.PoiManager
import java.util.*

class NavigationActivity: AppCompatActivity() {


    companion object {
        private const val TAG = "MainActivity"
        private const val REQUEST_FOREGROUND_REQUEST_CODE = 56
        private const val REQUEST_COARSE_LOCATION = 57
        private const val REQUEST_ENABLE_BT = 58
        private const val REQUEST_LOCATION_PERMISSION_SETTINGS = 59
        private const val REQUEST_LOCATION_SERVICE = 60
    }
    private var localeLanguage = Locale.forLanguageTag(Locale.getDefault().language).toString()
    private var isCheckingServices = false
    private var currentAlertDialog: AlertDialog? = null


    override fun onResume() {
        super.onResume()
        askLocationPermission()
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_navigation)
        initSDK()

    }

    fun replaceFragment(
        layoutId: Int,
        fragment: Fragment?
    ) {
        fragment ?: return
        supportFragmentManager
            .beginTransaction()
            .replace(layoutId, fragment)
            .commit()
    }


    private fun askLocationPermission() {
        if (isCheckingServices) {
            return
        }
        isCheckingServices = true
        if (Build.VERSION.SDK_INT >= 29) {
            val hasFineLocation = ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
            if (hasFineLocation != PackageManager.PERMISSION_GRANTED) {
                if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
                    showLocationPermissionDialog()
                    return
                }
                requestLocationPermission()
            } else {
                checkForLocationService()
            }
        } else {
            val hasLocalPermission = ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
            if (hasLocalPermission != PackageManager.PERMISSION_GRANTED) {
                if (Build.VERSION.SDK_INT >= 23 && shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION)) {
                    showLocationPermissionDialog()
                    return
                }
                requestLocationPermission()
            } else {
                checkForLocationService()
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        if (grantResults.isEmpty()) {
            showLocationPermissionDialog()
            return
        }
        if (requestCode == REQUEST_FOREGROUND_REQUEST_CODE) {
            if (PackageManager.PERMISSION_GRANTED == grantResults[0]
            ) {     // Permission Granted
                Log.i(TAG, "Foreground and background location enabled.")
                checkForLocationService()
            } else {
                showLocationPermissionDialog()
                Log.e(
                    TAG,
                    " Permission was denied, but is needed for core functionality."
                )
            }
        } else if (requestCode == REQUEST_COARSE_LOCATION) {
            if (PackageManager.PERMISSION_GRANTED == grantResults[0]) {     // Permission Granted
                Log.i(TAG, "Location permission was granted")
                checkForLocationService()
            } else {
                showLocationPermissionDialog()
                Log.e(
                    TAG,
                    " Permission was denied, but is needed for core functionality."
                )
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_ENABLE_BT) {
            isCheckingServices = false
            checkBluetoothService()
        } else if (requestCode == REQUEST_LOCATION_PERMISSION_SETTINGS) {
            isCheckingServices = false
            checkForLocationService()
        } else if (requestCode == REQUEST_LOCATION_SERVICE) {
            checkBluetoothService()
            isCheckingServices = false
        }
    }


    private fun showLocationPermissionDialog() {
        currentAlertDialog?.dismiss()
        currentAlertDialog = AlertDialog.Builder(this)
            .setTitle(R.string.locationPermissionDeniedTitle)
            .setMessage(R.string.locationPermissionDeniedDescription) // Specifying a listener allows you to take an action before dismissing the dialog.
            // The dialog is automatically dismissed when a dialog button is clicked.
            .setPositiveButton(
                R.string.ok
            ) { _, _ ->
                askForLocationPermission()
            } // A null listener allows the button to dismiss the dialog and take no further action.
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setCancelable(false)
            .show()
    }


    private fun askForLocationPermission() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION) || shouldShowRequestPermissionRationale(
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            ) {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri: Uri = Uri.fromParts("package", packageName, null)
                intent.data = uri
                startActivityForResult(intent, REQUEST_LOCATION_PERMISSION_SETTINGS)
            } else {
                requestLocationPermission()
            }
        } else {
            requestLocationPermission()
        }
    }

    private fun requestLocationPermission() {
        if (Build.VERSION.SDK_INT >= 29) {
            val hasFineLocation = ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
            if (hasFineLocation != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ),
                    REQUEST_FOREGROUND_REQUEST_CODE
                )
                return
            }
            checkForLocationService()
        } else {
            val hasLocalPermission = ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
            if (hasLocalPermission != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),
                    REQUEST_COARSE_LOCATION
                )
                return
            }
            checkForLocationService()
        }
    }

    private fun checkForLocationService() {
        val lm = getSystemService(LOCATION_SERVICE) as LocationManager
        var gps_enabled = false
        var network_enabled = false

        try {
            gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
        } catch (ex: Exception) {
        }

        try {
            network_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        } catch (ex: Exception) {
        }

        if (!gps_enabled && !network_enabled) {
            currentAlertDialog?.dismiss()
            currentAlertDialog = AlertDialog.Builder(this)
                .setTitle(R.string.locationServicesAreDisabledTitle)
                .setMessage(R.string.locationServicesAreDisabledDescription)
                .setPositiveButton(R.string.ok,
                    DialogInterface.OnClickListener { paramDialogInterface, paramInt ->
                        startActivityForResult(
                            Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS),
                            REQUEST_LOCATION_SERVICE
                        )
                    })
                .setCancelable(false)
                .show()
        } else {
            checkBluetoothService()
        }
    }

    private fun checkBluetoothService() {
        val mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (mBluetoothAdapter == null) {
            currentAlertDialog?.dismiss()
            currentAlertDialog = AlertDialog.Builder(this)
                .setMessage(R.string.bluetoothNeededForThisApplication)
                .setPositiveButton(R.string.ok,
                    DialogInterface.OnClickListener { paramDialogInterface, paramInt ->
                        finish()
                    })
                .setCancelable(false)
                .show()
        } else if (!mBluetoothAdapter.isEnabled) {
            currentAlertDialog?.dismiss()
            currentAlertDialog = AlertDialog.Builder(this)
                .setTitle(R.string.bluetoothDisabledTitle)
                .setMessage(R.string.bluetoothDisabledDescription)
                .setPositiveButton(R.string.ok,
                    DialogInterface.OnClickListener { paramDialogInterface, paramInt ->
                        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
                    })
                .setCancelable(false)
                .show()
        } else {
            isCheckingServices = false
        }
    }

    private fun initSDK() {
        //Blindoors
        PoiManager.init(this,
            BuildConfig.APPID,
            BuildConfig.APPSECRET,
            localeLanguage,
            "Blindoors",
            object :
                VDResponseListener {
                override fun onSuccess() {
                    PoiColorManager.primaryColor = Color.parseColor("#644391")
                    PoiColorManager.secondaryColor = Color.BLACK
                    PoiColorManager.textColor = Color.WHITE
                    replaceFragment(
                        R.id.main_act_layout,
                        VDBaseFragment(applicationContext)
                    )
                }

                override fun onFail() {
                }
            })
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            askLocationPermission()
        }
    }
}
