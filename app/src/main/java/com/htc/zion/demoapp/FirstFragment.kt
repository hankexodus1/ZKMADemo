package com.htc.zion.demoapp

import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.htc.htcwalletsdk.Native.Type.ByteArrayHolder
import com.htc.zion.demoapp.Utils.Companion.LOG_TAG
import com.htc.zion.demoapp.Utils.Companion.convertToHex
import com.htc.zion.demoapp.Utils.Companion.sha256
import com.htc.zion.demoapp.data.EthereumJsonTemplate
import com.htc.zion.demoapp.data.Message
import com.htc.zion.demoapp.databinding.FragmentFirstBinding
import kotlinx.android.synthetic.main.fragment_first.*
import java.util.concurrent.Executors

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class FirstFragment : Fragment() {
    val SELECT_PHOTO = 1
    val COIN_TYPE_ETHEREUM = 60

    private val tzExecutor by lazy {
        Executors.newSingleThreadExecutor()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val binding = FragmentFirstBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initZkmaSdk(this.requireContext())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btn_create_seed.setOnClickListener {
            tzExecutor.execute {
                val uniqueId = Utils.getZkmaSdkUniqueId(view.context)
                if (ZkmaSdkHelper.isSeedExists(uniqueId)) {
                    activity?.runOnUiThread {
                        Snackbar.make(view, R.string.msg_seed_exists, Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show()
                    }
                } else {
                    val result = ZkmaSdkHelper.createSeed(uniqueId)
                    Log.i(LOG_TAG, "createSeed(), result=$result")
                }
            }
        }

        btn_restore_seed.setOnClickListener {
            tzExecutor.execute {
                val uniqueId = Utils.getZkmaSdkUniqueId(view.context)
                if (ZkmaSdkHelper.isSeedExists(uniqueId)) {
                    activity?.runOnUiThread {
                        Snackbar.make(view, R.string.msg_seed_exists, Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show()
                    }
                } else {
                    val result = ZkmaSdkHelper.restoreSeed(uniqueId)
                    Log.i(LOG_TAG, "restoreSeed(), result=$result")
                }
            }
        }

        btn_sign_photo.setOnClickListener {
            val intent = Intent(
                Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            )
            intent.type = "image/*"
            startActivityForResult(intent, SELECT_PHOTO)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == SELECT_PHOTO && resultCode == RESULT_OK) {
            data?.data?.let { uri ->
                tzExecutor.execute {
                    Utils.loadImage(this.requireContext(), uri)?.let { bitmap ->
                        bitmap.convertToHex()
                    }?.let { bitmapHex ->
                        bitmapHex.sha256()
                    }?.let { bitmapHash ->
                        val photoData = EthereumJsonTemplate(Message(bitmapHash))
                        Log.i(LOG_TAG, "EthereumJson=$photoData")
                        val sig = ByteArrayHolder()
                        val uniqueId = Utils.getZkmaSdkUniqueId(this.requireContext())
                        val result = ZkmaSdkHelper.signMessage(
                            uniqueId,
                            COIN_TYPE_ETHEREUM,
                            Gson().toJson(photoData),
                            sig
                        )
                        Log.i(
                            LOG_TAG,
                            "signMessage(), result=$result, sig=${sig.byteArray.toString()}"
                        )
                        val verified = ZkmaSdkHelper.verifyEthMsgSignature(
                            uniqueId,
                            bitmapHash,
                            sig.byteArray
                        )
                        Log.i(
                            LOG_TAG,
                            "verifyEthMsgSignature(), verified=$verified"
                        )
                    }
                }
            }
        }
    }

    private fun initZkmaSdk(context: Context) {
        tzExecutor.execute {
            ZkmaSdkHelper.initSdk(context)
        }
    }
}
