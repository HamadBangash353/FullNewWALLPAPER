package video.wallpaper.videotolive.maker.activities

import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.firebase.crashlytics.FirebaseCrashlytics
import video.wallpaper.videotolive.maker.R
import video.wallpaper.videotolive.maker.`interface`.OnBaseVideoListener
import video.wallpaper.videotolive.maker.`interface`.OnTrimVideoListener
import video.wallpaper.videotolive.maker.apputils.App
import video.wallpaper.videotolive.maker.apputils.Constants.EXTRA_INPUT_URI
import video.wallpaper.videotolive.maker.apputils.Constants.VIDEO_TOTAL_DURATION
import video.wallpaper.videotolive.maker.apputils.Constants.dstFile
import video.wallpaper.videotolive.maker.apputils.MediaScanner
import video.wallpaper.videotolive.maker.databinding.ActivityTrimmerBinding
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.nio.channels.FileChannel

class TrimmingActivity : AppCompatActivity(), OnTrimVideoListener,
    OnBaseVideoListener {

    private var mVideoTrimmerView: BaseVideoTrimmerView? = null
    private var mProgressDialog: ProgressDialog? = null

    private var mInterstitialAd: InterstitialAd? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        } else {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
        }
        val binding = ActivityTrimmerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val extraIntent: Intent = intent
        var path: String? = ""
        var maxDuration = 10
        if (extraIntent != null) {
            path = extraIntent.getStringExtra(EXTRA_INPUT_URI)
            maxDuration = extraIntent.getIntExtra(VIDEO_TOTAL_DURATION, 10)
        }

        //setting progressbar
        mProgressDialog = ProgressDialog(this)
        mProgressDialog!!.setCancelable(false)
        mProgressDialog!!.setMessage(getString(R.string.trimming_progress))
        mVideoTrimmerView = findViewById(R.id.timeLine)
        if (mVideoTrimmerView != null) {
            /**
             * get total duration of video file
             */
            Log.e("tg", "maxDuration = $maxDuration")
            //mVideoTrimmer.setMaxDuration(maxDuration);
            mVideoTrimmerView?.setMaxDuration(maxDuration)
            mVideoTrimmerView?.setOnTrimVideoListener(this)
            mVideoTrimmerView?.setOnBaseVideoListener(this)
            mVideoTrimmerView?.setVideoURI(Uri.fromFile(File(path)))
            mVideoTrimmerView?.setVideoInformationVisibility(true)
        }


        val adRequest = AdRequest.Builder().build()

        InterstitialAd.load(this, getString(R.string.interstitialId), adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    mInterstitialAd = null

                }

                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    mInterstitialAd = interstitialAd
                }
            }
        )


    }

    override fun onTrimStarted() {
        runOnUiThread {
            mProgressDialog!!.show()
        }
    }


    private fun showAd() {
        if (mInterstitialAd != null) {
            mInterstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    val intents = Intent(this@TrimmingActivity, PreviewActivity::class.java)
                    // pass argument here
                    startActivity(intents)
                    Log.d("Dismissed", "Dismissed")
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError?) {
                    val intents = Intent(this@TrimmingActivity, PreviewActivity::class.java)
                    // pass argument here
                    startActivity(intents)
                }

                override fun onAdShowedFullScreenContent() {
                    mInterstitialAd = null
                }
            }

            mInterstitialAd?.show(this@TrimmingActivity)
        } else {
            val intents = Intent(this@TrimmingActivity, PreviewActivity::class.java)
            // pass argument here
            startActivity(intents)
        }
    }

    override fun onFinishedTrimming(contentUri: Uri?) {
        runOnUiThread {
            mProgressDialog!!.cancel()
            App.customEvents(this, "file_trimmedsuccess")
            showAd()
        }
        mProgressDialog?.dismiss()


        if ("file".equals(contentUri?.scheme, ignoreCase = true)) {
            Toast.makeText(this, "Choose start and end to cut video", Toast.LENGTH_SHORT).show()
            return
        }


        Toast.makeText(this,"Triming Frinshed",Toast.LENGTH_SHORT).show()
        try {
            copyFile(File(contentUri.toString()), File("${dstFile}/file.mp4"))
        } catch (e1: Exception){
           Log.e("exception", "video copy exception")
        }


    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.e("tg", "resultCode = $resultCode data $data")
    }

    override fun cancelAction() {
        mProgressDialog!!.cancel()
        mVideoTrimmerView?.destroy()
        finish()
    }

    override fun onError(message: String?) {
        mProgressDialog!!.cancel()
        runOnUiThread {
            // Toast.makeText(TrimmerActivity.this, message, Toast.LENGTH_SHORT).show();
        }
    }

    override fun onVideoPrepared() {
    }

    override fun onSkip() {
    }

    private fun copyFile(fromFile: File?, toFile: File?) {
        var fileInputStream: FileInputStream? = null
        var fileOutputStream: FileOutputStream? = null
        var fileChannelInput: FileChannel? = null
        var fileChannelOutput: FileChannel? = null
        try {
            fileInputStream = FileInputStream(fromFile)
            fileOutputStream = FileOutputStream(toFile)
            fileChannelInput = fileInputStream.channel
            fileChannelOutput = fileOutputStream.channel
            fileChannelInput.transferTo(
                0, fileChannelInput.size(), fileChannelOutput
            )
            fileChannelInput.close()
            fromFile?.delete()
            Log.e("file created","File created"+ toFile.toString())
            App.customEvents(this, "file_created")


             MediaScanner().SingleMediaScanner(this,toFile)

        } catch (e: IOException) {
            e.printStackTrace()
            FirebaseCrashlytics.getInstance().recordException(e)
        } finally {
            try {
                fileInputStream?.close()
                fileChannelInput?.close()
                fileOutputStream?.close()
                fileChannelOutput?.close()
            } catch (e: IOException) {
                e.printStackTrace()
                FirebaseCrashlytics.getInstance().recordException(e)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        App.setCurrentScreen(this, "trimming_screen")
    }
}