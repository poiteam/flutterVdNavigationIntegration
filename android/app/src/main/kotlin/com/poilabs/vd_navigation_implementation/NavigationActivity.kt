package com.poilabs.vd_navigation_implementation

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.getpoi.android_vd_nav_ui.view.PoiVdNavigationActivity
import com.poilabs.vd.nav.non.ui.jsonclient.VDResponseListener
import com.poilabs.vd.nav.non.ui.models.PoiManager
import java.util.*

class NavigationActivity : AppCompatActivity() {


    companion object {
        private const val TAG = "MainActivity"
    }

    private var localeLanguage = Locale.forLanguageTag(Locale.getDefault().language).toString()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_navigation)
        initSDK()

    }

    private fun initSDK() {
        //Blindoors
        PoiManager.init(this,
            BuildConfig.APPID,
            BuildConfig.APPSECRET,
            localeLanguage,
            "App name",
            object :
                VDResponseListener {
                override fun onSuccess() {
                    Intent(this@NavigationActivity, PoiVdNavigationActivity::class.java).also {
                        startActivity(it)
                    }
                }

                override fun onFail(p0: Throwable?) {
                    Log.e(TAG, "onFail: $p0")
                }

            })
    }

}
