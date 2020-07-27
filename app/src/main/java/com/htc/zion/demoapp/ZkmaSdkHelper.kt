package com.htc.zion.demoapp

import android.content.Context
import android.util.Log
import androidx.annotation.WorkerThread
import com.htc.htcwalletsdk.Export.HtcWalletSdkManager
import com.htc.htcwalletsdk.Export.RESULT
import com.htc.htcwalletsdk.Native.Type.ByteArrayHolder
import java.util.concurrent.atomic.AtomicBoolean

class ZkmaSdkHelper {

    companion object {
        private lateinit var zkmaManager: HtcWalletSdkManager
        private val zkmaSdkInited = AtomicBoolean(false)

        @WorkerThread
        @Synchronized
        fun initSdk(context: Context) {
            if (zkmaSdkInited.get()) return

            zkmaManager = HtcWalletSdkManager.getInstance()
            val result = zkmaManager.init(context.applicationContext)
            when (result) {
                RESULT.E_SDK_ROM_SERVICE_TOO_OLD, RESULT.E_SDK_ROM_TZAPI_TOO_OLD ->
                    Log.w(Utils.LOG_TAG, "Please update your device!")
                RESULT.E_TEEKM_TAMPERED ->
                    Log.w(Utils.LOG_TAG, "Your device is rooted!")
                RESULT.SUCCESS -> {
                    val uniqueId = zkmaManager.register(
                        context.packageName,
                        Utils.getCertificateSHA256Fingerprint(context)
                    )
                    Log.i(Utils.LOG_TAG, "ZKMA register() succeed, uniqueId=$uniqueId")
                    zkmaSdkInited.set(true)
                    Utils.setZkmaSdkUniqueId(context, uniqueId)
                }
                else ->
                    Log.e(Utils.LOG_TAG, "ZKMA init() fails, result=$result")
            }
        }

        @WorkerThread
        @Synchronized
        fun createSeed(uniqueId: Long): Int {
            if (!zkmaSdkInited.get()) throw RuntimeException("ZKMA not initialized!")
            return zkmaManager.createSeed(uniqueId)
        }

        @WorkerThread
        @Synchronized
        fun restoreSeed(uniqueId: Long): Int {
            if (!zkmaSdkInited.get()) throw RuntimeException("ZKMA not initialized!")
            return zkmaManager.restoreSeed(uniqueId)
        }

        @WorkerThread
        @Synchronized
        fun clearSeed(uniqueId: Long): Int {
            if (!zkmaSdkInited.get()) throw RuntimeException("ZKMA not initialized!")
            return zkmaManager.clearSeed(uniqueId)
        }

        @WorkerThread
        @Synchronized
        fun isSeedExists(uniqueId: Long): Boolean {
            if (!zkmaSdkInited.get()) throw RuntimeException("ZKMA not initialized!")
            return uniqueId != 0L && zkmaManager.isSeedExists(uniqueId) == RESULT.SUCCESS
        }

        @WorkerThread
        @Synchronized
        fun signMessage(
            uniqueId: Long,
            coinType: Int,
            strJson: String,
            byteArrayHolder: ByteArrayHolder
        ): Int {
            if (!zkmaSdkInited.get()) throw RuntimeException("ZKMA not initialized!")
            return zkmaManager.signMessage(uniqueId, coinType, strJson, byteArrayHolder)
        }
    }
}