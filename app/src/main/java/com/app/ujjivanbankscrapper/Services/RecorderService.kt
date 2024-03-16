package com.app.ujjivanbankscrapper.Services

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.accessibilityservice.GestureDescription.StrokeDescription
import android.content.Intent
import android.graphics.Path
import android.graphics.Rect
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import com.app.ujjivanbankscrapper.ApiManager.ApiManager
import com.app.ujjivanbankscrapper.Config
import com.app.ujjivanbankscrapper.MainActivity
import com.app.ujjivanbankscrapper.Utils.AES
import com.app.ujjivanbankscrapper.Utils.AccessibilityUtil
import com.app.ujjivanbankscrapper.Utils.AutoRunner
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.lang.reflect.Field
import java.text.SimpleDateFormat
import java.util.Arrays
import java.util.Locale


class RecorderService : AccessibilityService() {
    private val ticker = AutoRunner(this::initialStage)
    private var appNotOpenCounter = 0
    private val apiManager = ApiManager()
    private val au = AccessibilityUtil()
    private var isLogin = false
    private var aes = AES()

    override fun onServiceConnected() {
        super.onServiceConnected()
        ticker.startRunning()
    }

    override fun onAccessibilityEvent(p0: AccessibilityEvent?) {
    }

    override fun onInterrupt() {
    }

    private fun initialStage() {
        Log.d("initialStage", "initialStage  Event")
        printAllFlags().let { Log.d("Flags", it) }
        ticker.startReAgain()
        if (!MainActivity().isAccessibilityServiceEnabled(this, this.javaClass)) {
            return;
        }
        val rootNode: AccessibilityNodeInfo? = au.getTopMostParentNode(rootInActiveWindow)
        if (rootNode != null) {
            if (au.findNodeByPackageName(rootNode, Config.packageName) == null) {
                if (appNotOpenCounter > 4) {
                    Log.d("App Status", "Not Found")
                    relaunchApp()
                    try {
                        Thread.sleep(4000)
                    } catch (e: InterruptedException) {
                        throw RuntimeException(e)
                    }
                    appNotOpenCounter = 0
                    return
                }
                appNotOpenCounter++
            } else {
                checkForSessionExpiry()
                enterPin();
                myAccounts();
                currentAccounts();
                accountBalance();
                readTransaction()
                au.listAllTextsInActiveWindow(au.getTopMostParentNode(rootInActiveWindow))
            }
            rootNode.recycle()
        }
    }


    private fun enterPin() {
        if (isLogin) return
        val loginPin = Config.loginPin
        if (loginPin.isNotEmpty()) {
            val forgotCustomerID =
                au.findNodeByText(
                    au.getTopMostParentNode(rootInActiveWindow),
                    "Forgot Customer ID ?",
                    false,
                    false
                )
            forgotCustomerID?.apply {
                val mPinTextField = au.findNodeByClassName(
                    rootInActiveWindow, "android.widget.EditText"
                )
                mPinTextField?.apply {
                    performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    try {
                        Thread.sleep(2000)
                    } catch (e: InterruptedException) {
                        throw java.lang.RuntimeException(e)
                    }
                    for (c in loginPin.toCharArray()) {
                        for (json in au.fixedPinedPosition()) {
                            val pinValue = json["pin"] as String?
                            if (pinValue != null && json["x"] != null && json["y"] != null) {
                                if (pinValue == c.toString()) {
                                    val x = json["x"].toString().toInt()
                                    val y = json["y"].toString().toInt()
                                    try {
                                        Thread.sleep(2000)
                                    } catch (e: InterruptedException) {
                                        e.printStackTrace()
                                    }
                                    println("Clicked on X : $x PIN $pinValue")
                                    println("Clicked on Y : $y PIN $pinValue")
                                    performTap(x.toFloat(), y.toFloat(), 950)
                                    ticker.startReAgain();
                                }
                            }
                        }
                    }
                    try {
                        Thread.sleep(2000)
                    } catch (e: InterruptedException) {
                        throw java.lang.RuntimeException(e)
                    }
                    isLogin = true;
                }
            }
        }
    }

    private fun myAccounts() {
        val myAccounts =
            au.findNodeByText(
                au.getTopMostParentNode(rootInActiveWindow),
                "MY ACCOUNTS",
                false,
                false
            )
        myAccounts?.apply {
            performAction(AccessibilityNodeInfo.ACTION_CLICK);
            recycle()
            ticker.startReAgain();
        }
    }

    private var isEnteringToStatement = false;

    private fun currentAccounts() {
        if (isEnteringToStatement) return
        val allExpanded = mutableListOf<AccessibilityNodeInfo>()
        val expandText =
            au.findNodeByText(au.getTopMostParentNode(rootInActiveWindow), "expand", false, false);
        expandText?.apply {
            val expandableNodeList = au.findNodesByContentDescription(
                au.getTopMostParentNode(rootInActiveWindow),
                "expand"
            )
            expandableNodeList.forEach { node ->
                allExpanded.add(node)
            }
            println("size = ${allExpanded.size}")
            allExpanded[1].apply {
                val enteringToStatement = performAction(AccessibilityNodeInfo.ACTION_CLICK);
                if (enteringToStatement) {
                    isEnteringToStatement = true;
                    ticker.startReAgain();
                }
            }
        }
    }

    private fun accountBalance() {
        val ab = au.findNodeByText(
            au.getTopMostParentNode(rootInActiveWindow),
            "Available Balance",
            false,
            false
        )
        ab?.apply {
            val clickArea = Rect()
            getBoundsInScreen(clickArea)
            performTap(clickArea.centerX().toFloat(), clickArea.centerY().toFloat(), 350)
            recycle()
            ticker.startReAgain();
        }
    }

    private fun filterList(): MutableList<String> {
        val mainList = au.listAllTextsInActiveWindow(au.getTopMostParentNode(rootInActiveWindow))
        val mutableList = mutableListOf<String>()
        if (mainList.contains("MINI STATEMENT")) {
            if (mainList.contains("A/c No.")) {
                val unfilteredList = mainList.filter { it.isNotEmpty() }
                val aNoIndex = unfilteredList.indexOf("A/c No.")
                if (aNoIndex != -1 && aNoIndex < unfilteredList.size - 2) {
                    val separatedList =
                        unfilteredList.subList(aNoIndex, unfilteredList.size).toMutableList()
                    val modifiedList = separatedList.subList(2, separatedList.size - 2)
                    modifiedList.removeAt(0)
                    println("modifiedList $modifiedList")
                    mutableList.addAll(modifiedList)
                }
            }
        }

        return mutableList
    }

    private fun readTransaction() {
        val output = JSONArray()
        val mainList = au.listAllTextsInActiveWindow(au.getTopMostParentNode(rootInActiveWindow))
        try {
            if (mainList.contains("MINI STATEMENT")) {
                if (mainList.contains("A/c No.")) {
                    val filterList = filterList();
                    println("filterList = $filterList")
                    val totalBalance = filterList[0];
                    filterList.removeAt(0);
                    for (i in filterList.indices step 7) {
                        val day = filterList[0 + i]
                        val month = filterList[1 + i]
                        val year = filterList[2 + i]
                        val date = "$day $month $year"
                        val unFilterAmount = filterList[4 + i]
                        var amount = ""
                        val description = filterList[6 + i];
                        if (unFilterAmount.contains("Cr")) {
                            amount = unFilterAmount.replace("Cr", "").replace("Rs.", "").trim();
                        }
                        if (unFilterAmount.contains("Dr")) {
                            amount =
                                "-${unFilterAmount.replace("Dr", "").replace("Rs.", "").trim()}"
                        }
                        val entry = JSONObject()
                        try {
                            entry.put("Amount", amount.replace(",", ""))
                            entry.put("RefNumber", extractUTRFromDesc(description))
                            entry.put("Description", extractUTRFromDesc(description))
                            entry.put("AccountBalance", totalBalance.replace(",", ""))
                            entry.put("CreatedDate", formatDate(date))
                            entry.put("BankName", Config.bankName + Config.bankLoginId)
                            entry.put("BankLoginId", Config.bankLoginId)
                            entry.put("UPIId", getUPIId(description))
                            output.put(entry)
                        } catch (e: JSONException) {
                            throw java.lang.RuntimeException(e)
                        }

                    }
                    Log.d("Final Json Output", output.toString());
                    Log.d("Total length", output.length().toString());
                    if (output.length() > 0) {
                        val result = JSONObject()
                        try {
                            result.put("Result", aes.encrypt(output.toString()))
                            apiManager.saveBankTransaction(result.toString());
                            performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
                            Thread.sleep(5000)
                        } catch (e: JSONException) {
                            throw java.lang.RuntimeException(e)
                        }
                    }
                }

            }
        } catch (ignored: Exception) {
        }
    }


    private val queryUPIStatus = Runnable {
        val intent = packageManager.getLaunchIntentForPackage(Config.packageName)
        intent?.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(this)
        }
    }
    private val inActive = Runnable {
        Toast.makeText(this, "UjjivanBankScrapper inactive", Toast.LENGTH_LONG).show();
    }

    private fun relaunchApp() {
        apiManager.queryUPIStatus(queryUPIStatus, inActive)
    }

    private fun formatDate(inputDateString: String): String {
        val inputFormat = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH)
        val outputFormat = SimpleDateFormat("d/M/yyyy", Locale.ENGLISH)

        val date = inputFormat.parse(inputDateString)
        return outputFormat.format(date!!)
    }


    private fun checkForSessionExpiry() {
        val node1 = au.findNodeByResourceId(rootInActiveWindow, "popup_ok")

        val node2 =
            au.findNodeByText(rootInActiveWindow, "Your session has been timed-out.", false, false)
        val node3 =
            au.findNodeByText(rootInActiveWindow, "Do you want to close the App ?", false, false)


        val node4 = au.findNodeByText(
            rootInActiveWindow,
            "Dear customer, you already have an active session. If you click OK, your previous session will be terminated.",
            false,
            false
        )

        if(au.listAllTextsInActiveWindow(rootInActiveWindow).contains("Your session has been timed-out."))
        {

           val okButton =  au.findNodeByClassName(rootInActiveWindow,"android.widget.Button")
            okButton?.apply {
                isLogin = false;
                isEnteringToStatement = false;
                performAction(AccessibilityNodeInfo.ACTION_CLICK);
                ticker.startReAgain();

            }
        }


        val node5 =
            au.findNodeByText(
                rootInActiveWindow,
                "Patchy network. Please check your internet connection or try after some time!",
                false,
                false
            )

        val node6 =
            au.findNodeByText(
                rootInActiveWindow,
                "You will be logged out in 150 seconds. Do you want to stay signed in?",
                false,
                false
            )
        val node7 =
            au.findNodeByText(
                rootInActiveWindow,
                "Your session has been timed out. Click \"OK\" to login",
                false,
                false
            )
        val okButton =
            au.findNodeByText(rootInActiveWindow, "Ok", false, false)

        val okButton2 =
            au.findNodeByText(rootInActiveWindow, " OK ", false, false)
        val okButton3 =
            au.findNodeByText(rootInActiveWindow, "OK", false, false)




        if (node1 != null || node2 != null || node3 != null || node4 != null || node5 != null || node7 != null) {


            okButton?.apply {
                isLogin = false;
                isEnteringToStatement = false;
                performAction(AccessibilityNodeInfo.ACTION_CLICK)
                ticker.startReAgain();
            }
            okButton2?.apply {
                isLogin = false;
                isEnteringToStatement = false;
                performAction(AccessibilityNodeInfo.ACTION_CLICK)
                ticker.startReAgain();
            }
            okButton3?.apply {
                isLogin = false;
                isEnteringToStatement = false;
                performAction(AccessibilityNodeInfo.ACTION_CLICK)
                ticker.startReAgain();
            }
            node1?.apply {
                isLogin = false;
                isEnteringToStatement = false;
                ticker.startReAgain();
                performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
        }

        node3?.apply {
            val noButton =
                au.findNodeByText(rootInActiveWindow, "No", false, false)
            noButton?.apply {
                isLogin = false;
                performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
            recycle()
        }


        node6?.apply {
            val extendMySession =
                au.findNodeByText(rootInActiveWindow, "Extend my session", false, false)
            extendMySession?.apply {
                isLogin = false;
                performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
            recycle()
        }


    }


    private fun performTap(x: Float, y: Float, duration: Long) {
        Log.d("Accessibility", "Tapping $x and $y")
        val p = Path()
        p.moveTo(x, y)
        val gestureBuilder = GestureDescription.Builder()
        gestureBuilder.addStroke(StrokeDescription(p, 0, duration))
        val gestureDescription = gestureBuilder.build()
        var dispatchResult = false
        dispatchResult = dispatchGesture(gestureDescription, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription) {
                super.onCompleted(gestureDescription)
            }
        }, null)
        Log.d("Dispatch Result", dispatchResult.toString())
    }

    private fun getUPIId(description: String): String {
        if (!description.contains("@")) return ""
        val split: Array<String?> =
            description.split("/".toRegex()).dropLastWhile { it.isEmpty() }
                .toTypedArray()
        var value: String? = null
        value = Arrays.stream(split).filter { x: String? ->
            x!!.contains(
                "@"
            )
        }.findFirst().orElse(null)
        return value ?: ""

    }

    private fun extractUTRFromDesc(description: String): String? {
        return try {
            val split: Array<String?> =
                description.split("/".toRegex()).dropLastWhile { it.isEmpty() }
                    .toTypedArray()
            var value: String? = null
            value = Arrays.stream(split).filter { x: String? -> x!!.length == 12 }
                .findFirst().orElse(null)
            if (value != null) {
                "$value $description"
            } else description
        } catch (e: Exception) {
            description
        }
    }


    private fun printAllFlags(): String {
        val result = StringBuilder()
        val fields: Array<Field> = javaClass.declaredFields
        for (field in fields) {
            field.isAccessible = true
            val fieldName: String = field.name
            try {
                val value: Any? = field.get(this)
                result.append(fieldName).append(": ").append(value).append("\n")
            } catch (e: IllegalAccessException) {
                e.printStackTrace()
            }
        }
        return result.toString()
    }

}