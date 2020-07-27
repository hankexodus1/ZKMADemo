package com.htc.zion.demoapp

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.security.cert.CertificateEncodingException
import java.security.cert.CertificateException
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate

class Utils {

    companion object {
        val LOG_TAG = "ZkmaDemo"

        fun getCertificateSHA256Fingerprint(context: Context): String? {
            val pm: PackageManager = context.packageManager
            val packageName: String = context.packageName
            var packageInfo: PackageInfo? = null
            try {
                packageInfo = pm.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
            } catch (e: PackageManager.NameNotFoundException) {
                Log.e(LOG_TAG, "getPackageInfo() fails, e=$e")
                return null
            }
            val signatures: Array<Signature> = packageInfo!!.signatures
            val cert: ByteArray = signatures[0].toByteArray()
            val input: InputStream = ByteArrayInputStream(cert)
            var certificateFactory: CertificateFactory? = null
            try {
                certificateFactory = CertificateFactory.getInstance("X509")
            } catch (e: CertificateException) {
                Log.e(LOG_TAG, "getInstance(\"X509\") fails, e=$e")
            }
            var x509Certificate: X509Certificate? = null
            try {
                x509Certificate = certificateFactory?.generateCertificate(input) as X509Certificate
            } catch (e: CertificateException) {
                Log.e(LOG_TAG, "generateCertificate() fails, e=$e")
            }
            var hexString: String? = null
            try {
                val md: MessageDigest = MessageDigest.getInstance("SHA-256")
                val publicKey: ByteArray = md.digest(x509Certificate?.encoded!!)
                hexString = byte2HexFormatted(publicKey, true)
            } catch (e1: NoSuchAlgorithmException) {
                Log.e(LOG_TAG, "getInstance(\"SHA-256\") fails, e=$e1")
            } catch (e: CertificateEncodingException) {
                Log.e(LOG_TAG, "digest() fails, e=$e")
            }
            return hexString
        }

        fun byte2HexFormatted(bytes: ByteArray, withColon: Boolean): String? {
            val stringBuilder = StringBuilder(bytes.size * 2)
            for (i in bytes.indices) {
                var h = Integer.toHexString(bytes[i].toInt())
                val l = h.length
                if (l == 1) h = "0$h"
                if (l > 2) h = h.substring(l - 2, l)
                stringBuilder.append(h.toUpperCase())
                if (i < bytes.size - 1 && withColon) stringBuilder.append(':')
            }
            return stringBuilder.toString()
        }

        fun String.sha256(): String? {
            return try {
                val digest = MessageDigest.getInstance("SHA-256")
                val hash = digest.digest(this.toByteArray(charset("UTF-8")))
                val hexString = StringBuffer()
                for (i in hash.indices) {
                    val hex = Integer.toHexString(0xFF and hash[i].toInt())
                    if (hex.length == 1) hexString.append('0')
                    hexString.append(hex)
                }
                hexString.toString()
            } catch (e: Exception) {
                throw java.lang.RuntimeException(e)
            }
        }

        fun setZkmaSdkUniqueId(context: Context, uniqueId: Long) {
            val prefs = context.getSharedPreferences("zkma_demo_prefs", MODE_PRIVATE)
            prefs.edit()
                .putString("zkma_unique_id", uniqueId.toString())
                .apply()
        }

        fun getZkmaSdkUniqueId(context: Context): Long {
            val prefs = context.getSharedPreferences("zkma_demo_prefs", MODE_PRIVATE)
            return prefs.getString("zkma_unique_id", null)?.toLong() ?: 0L
        }

        fun loadImage(context: Context, uri: Uri): Bitmap? {
            val filePathColumn =
                arrayOf(MediaStore.Images.Media.DATA)

            val cursor: Cursor? =
                context.contentResolver.query(uri, filePathColumn, null, null, null)
            cursor?.let {
                it.use { it ->
                    it.moveToFirst()
                    val columnIndex = it.getColumnIndex(filePathColumn[0])
                    val photoPath = it.getString(columnIndex)
                    val options = BitmapFactory.Options()
                    // Use RGB_565 for better performance
                    options.inPreferredConfig = Bitmap.Config.RGB_565
                    return BitmapFactory.decodeFile(photoPath, options)
                }
            }
            return null
        }

        fun Bitmap.compressAndConvertToByteArray(): ByteArray {
            val baos = ByteArrayOutputStream()
            this.compress(Bitmap.CompressFormat.JPEG, 80, baos)
            return baos.toByteArray()
        }

        fun Bitmap.convertToHex(): String? {
            val bytes = this.compressAndConvertToByteArray()
            return byte2HexFormatted(bytes, false)
        }
    }
}
