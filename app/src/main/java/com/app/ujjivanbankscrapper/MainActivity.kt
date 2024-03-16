package com.app.ujjivanbankscrapper
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.view.accessibility.AccessibilityManager
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.app.UjjivanBankScrapper.R
import com.app.ujjivanbankscrapper.ApiManager.ApiManager
import com.app.ujjivanbankscrapper.Services.RecorderService

class MainActivity : AppCompatActivity() {
    private lateinit var editText1: EditText
    private lateinit var editText2: EditText
    private lateinit var editText3: EditText
    private val apiManager = ApiManager();
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (!isAccessibilityServiceEnabled(this, RecorderService::class.java)) {
            showAccessibilityDialog()
        }

        val serviceIntent = Intent(this, RecorderService::class.java)
        startService(serviceIntent)

        editText1 = findViewById(R.id.editText1)
        editText2 = findViewById(R.id.editText2)
        editText3 = findViewById(R.id.editText3)

        sharedPreferences = getSharedPreferences(Config.packageName, MODE_PRIVATE)
        editText1.setText(sharedPreferences.getString("loginId", ""))
        editText2.setText(sharedPreferences.getString("loginPin", ""))
        editText3.setText(sharedPreferences.getString("bankLoginId", ""))
    }

    fun onAppFlowStarted(view: View) {
        val text1 = editText1.text.toString().trim()
        val text2 = editText2.text.toString().trim()
        val text3 = editText3.text.toString().trim()

        if (text1.isEmpty() || text2.isEmpty() || text3.isEmpty()) {
            Toast.makeText(this, "Fields required.", Toast.LENGTH_SHORT).show()
            return
        }

        Config.loginId = text1
        Config.loginPin = text2
        Config.bankLoginId = text3

        val editor = sharedPreferences.edit()
        editor.putString("loginId", text1)
        editor.putString("loginPin", text2)
        editor.putString("bankLoginId", text3)
        editor.apply()

        val active = Runnable {
            val intent = packageManager.getLaunchIntentForPackage(Config.packageName)
            intent?.apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(this)
            }
            val handler = Handler(Looper.getMainLooper())
            handler.postDelayed({
                runOnUiThread {
                    if (!isAccessibilityServiceEnabled(this, RecorderService::class.java)) {
                        showAccessibilityDialog()
                    } else {
                        val serviceIntent = Intent(this, RecorderService::class.java)
                        startService(serviceIntent)
                    }
                }
            }, 1000)
        }
        val inActive = Runnable {
            runOnUiThread {
                Toast.makeText(this, "UjjivanBankScrapper inactive", Toast.LENGTH_LONG).show()
            }
        }
        apiManager.queryUPIStatus(active,inActive)
    }

     fun isAccessibilityServiceEnabled(context: Context, serviceClass: Class<*>): Boolean {
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager?
        am?.let {
            val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_GENERIC)
            for (service in enabledServices) {
                val enabledServiceComponentName = ComponentName(service.resolveInfo.serviceInfo.packageName, service.resolveInfo.serviceInfo.name)
                val expectedServiceComponentName = ComponentName(context, serviceClass)
                if (enabledServiceComponentName == expectedServiceComponentName) {
                    return true
                }
            }
        }
        return false
    }

    private fun showAccessibilityDialog() {
        AlertDialog.Builder(this)
            .setTitle("Accessibility Permission Required")
            .setMessage("To use this app, you need to enable accessibility service. Go to Settings to enable it.")
            .setPositiveButton("Settings") { dialog, _ -> openAccessibilitySettings() }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .setCancelable(false)
            .show()
    }

    private fun openAccessibilitySettings() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        startActivity(intent)
    }
}