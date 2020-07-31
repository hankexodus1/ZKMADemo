package com.htc.zion.demoapp

import android.content.Context
import android.util.Log
import androidx.annotation.WorkerThread
import com.htc.htcwalletsdk.Export.HtcWalletSdkManager
import com.htc.htcwalletsdk.Export.RESULT
import com.htc.htcwalletsdk.Native.Type.ByteArrayHolder
import com.htc.htcwalletsdk.Utils.GenericUtils
import org.kethereum.crypto.CURVE
import java.util.concurrent.atomic.AtomicBoolean
import org.kethereum.crypto.signedMessageToKey
import org.kethereum.extensions.toBigInteger
import org.kethereum.extensions.toBytesPadded
import org.kethereum.extensions.toHexStringZeroPadded
import org.kethereum.model.PUBLIC_KEY_SIZE
import org.kethereum.model.SignatureData
import java.math.BigInteger

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

        @WorkerThread
        @Synchronized
        fun verifyEthMsgSignature(
            uniqueId: Long,
            message: String,
            signatureBytes: ByteArray
        ): Boolean {
            if (!zkmaSdkInited.get()) throw RuntimeException("ZKMA not initialized!")
            var verified = false
            try {
                val signature = GenericUtils.byteArrayToHex(signatureBytes)
                Log.d(Utils.LOG_TAG, "signature: $signature")
                val r = signature.slice(0..63).toBigInteger(16)
                val s = signature.slice(64..127).toBigInteger(16)
                val v = signature.slice(128..129).toBigInteger(16)
                val signatureData = SignatureData(r, s, v)
                val signedMsgBytes = message.toByteArray().let {
                    "\u0019Ethereum Signed Message:\n${it.size}".toByteArray() + it
                }
                val msgPubKey = compressKey(signedMessageToKey(signedMsgBytes, signatureData).key)
                val msgPubKeyHex = msgPubKey.toHexStringZeroPadded(66, false)
                Log.d(Utils.LOG_TAG, "msgPubKey: $msgPubKeyHex")
                val tzPubKey = zkmaManager.getSendPublicKey(uniqueId, 60).key
                    .toBigInteger(16)
                val tzPubKeyHex = tzPubKey.toHexStringZeroPadded(66, false)
                Log.d(Utils.LOG_TAG, "tzPubKey: $tzPubKeyHex")
                verified = tzPubKey == msgPubKey
            } catch (e: Exception) {
                Log.e(Utils.LOG_TAG, e.message, e)
            }
            return verified
        }

        fun compressKey(publicKey: BigInteger): BigInteger {
            val ret = publicKey.toBytesPadded(PUBLIC_KEY_SIZE + 1)
            ret[0] = 4
            val point = CURVE.decodePoint(ret)
            return point.encoded(true).toBigInteger()
        }
    }
}
