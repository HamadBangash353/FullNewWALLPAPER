package video.wallpaper.videotolive.maker.activities


import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.ads.*
import com.google.android.gms.ads.initialization.InitializationStatus
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.gowtham.library.utils.LogMessage
import com.gowtham.library.utils.TrimVideo
import video.wallpaper.videotolive.maker.BuildConfig
import video.wallpaper.videotolive.maker.R
import video.wallpaper.videotolive.maker.apputils.App
import video.wallpaper.videotolive.maker.apputils.App.customEvents
import video.wallpaper.videotolive.maker.apputils.AppRater
import video.wallpaper.videotolive.maker.apputils.Constants.EXTRA_INPUT_URI
import video.wallpaper.videotolive.maker.apputils.Constants.REQUEST_VIDEO_TRIMMER
import video.wallpaper.videotolive.maker.apputils.Constants.VIDEO_TOTAL_DURATION
import video.wallpaper.videotolive.maker.apputils.SharedPref
import video.wallpaper.videotolive.maker.apputils.SharedPref.savePermissionDenied
import video.wallpaper.videotolive.maker.databinding.ActivityMainBinding
import video.wallpaper.videotolive.maker.native_ads.NativeTemplateStyle
import video.wallpaper.videotolive.maker.native_ads.TemplateView
import video.wallpaper.videotolive.maker.videoutils.UriUtils
import java.io.File
import java.util.*


class MainActivity : AppCompatActivity() {

    private var mInterstitialAd: InterstitialAd? = null
    private var resultUri: Intent? = null

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

        MobileAds.initialize(this) { initializationStatus: InitializationStatus? ->
            val adLoader =
                AdLoader.Builder(this, resources.getString(R.string.nativeId))
                    .forNativeAd { NativeAd: NativeAd ->
                        val styles: NativeTemplateStyle = NativeTemplateStyle.Builder().build()
                        val template: TemplateView = findViewById(R.id.my_template)
                        template.setStyles(styles)
                        template.setNativeAd(NativeAd)

                        // Show the ad.
                        if (isDestroyed) {
                            NativeAd.destroy()
                        }
                    }
                    .withAdListener(object : AdListener() {
                        override fun onAdFailedToLoad(adError: LoadAdError) {
                            // Handle the failure by logging, altering the UI, and so on.
                        }
                    })
                    .withNativeAdOptions(
                        NativeAdOptions.Builder() // Methods in the NativeAdOptions.Builder class can be
                            // used here to specify individual options settings.
                            .build()
                    )
                    .build()
            adLoader.loadAd(AdRequest.Builder().build())
        }

        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        getPermission()
        binding.showSettings.setOnClickListener {
            openSettings()
        }


//        binding.rateUs.setOnClickListener {
//            AppRater.showRateDialog(this)
//        }

        binding.selectVideo.setOnClickListener {
            openVideo()
        }

        binding.ivMenu.setOnClickListener(View.OnClickListener {
            val popup = PopupMenu(this@MainActivity, binding.ivMenu)

            popup.menuInflater
                .inflate(R.menu.my_menu, popup.menu)

            popup.setOnMenuItemClickListener { item ->
                if (item.title.equals("Share App")) {
                    sharingApplication(this)
                }
                if (item.title.equals("Rate Us")) {
                    AppRater.showRateDialog(this)
                }
                if (item.title.equals("Privacy Policy")) {
                    privacyPolicy(this)
                }
                true
            }

            popup.show()
        })

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

    override fun onResume() {
        super.onResume()
        App.setCurrentScreen(this, "main_screen ")
    }

    private fun openSettings() {
        val intent = Intent(this, SettingActivity::class.java)
        startActivity(intent)
    }

    private fun openVideo() {
        if (App.hasPermissions(this)) {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "video/*"
            startActivityForResult(
                Intent.createChooser(intent, "Select Video"),
                REQUEST_VIDEO_TRIMMER
            )
        } else {
            showSettingsAlert()
        }
    }

    private fun showSettingsAlert() {

        val alertDialog: AlertDialog.Builder = AlertDialog.Builder(this)
        alertDialog.setTitle(getString(R.string.enable))
        alertDialog.setMessage(getString(R.string.permission_enable))
        alertDialog.setPositiveButton(
            this
                .getString(R.string.ok)
        ) { dialog, _ ->
            if (this@MainActivity.let {
                    ActivityCompat.checkSelfPermission(
                        it,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    )
                } != PackageManager.PERMISSION_GRANTED
            ) this@MainActivity.let {
                ActivityCompat.requestPermissions(
                    it,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    1
                )
            }
        }

        alertDialog.setNegativeButton(
            this
                .getString(R.string.cancel)
        )
        { dialog, _ ->
            dialog.cancel()
        }
        alertDialog.show()
    }

    private fun getPermission() {

//        if (Build.VERSION.SDK_INT >= 30) {
//            if (!Environment.isExternalStorageManager()) {
//                val intent = Intent()
//                intent.action = Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION
//                val uri: Uri = Uri.fromParts("package", this.packageName, null)
//                intent.data = uri
//                startActivity(intent)
//            }
//
//        }

        if (this@MainActivity.let {
                ActivityCompat.checkSelfPermission(
                    it,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            } != PackageManager.PERMISSION_GRANTED
        ) this@MainActivity.let {
            ActivityCompat.requestPermissions(
                it,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                1
            )
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.isNotEmpty()) {
            val locationAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED
            run {
                var i = 0
                val len: Int = permissions.size
                while (i < len) {
                    val permission = permissions[i]
                    if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                        val showRationale = shouldShowRequestPermissionRationale(permission)
                        if (!showRationale) {
                            SharedPref.saveNeverPermission(this@MainActivity, true)
                        }
                    }
                    i++
                }
                if (locationAccepted) {
                    savePermissionDenied(this@MainActivity, false)
                    customEvents(this@MainActivity, "permission_done")
                    openVideo()
//                    if (!checkGpsStatus(this@MainActivity))
//                        showSettingsAlert()
                } else {
                    savePermissionDenied(this@MainActivity, true)
                }
            }
        }
    }

    //Kotlin
    val startForResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK &&
                result.getData() != null
            ) {


                resultUri = result.data
                showAd()



            } else
                LogMessage.v("videoTrimResultLauncher data is null");
        }


    private fun showAd() {
        if (mInterstitialAd != null) {
            mInterstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    val intent = Intent(this@MainActivity, PreviewActivity::class.java)
                    intent.putExtra(
                        "uriPath",
                        Uri.parse(TrimVideo.getTrimmedVideoPath(resultUri))?.path
                    )
                    startActivity(intent)

                    Log.d("ShowAd","1")

                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError?) {
                    val intent = Intent(this@MainActivity, PreviewActivity::class.java)
                    intent.putExtra(
                        "uriPath",
                        Uri.parse(TrimVideo.getTrimmedVideoPath(resultUri))?.path
                    )
                    startActivity(intent)
                    Log.d("ShowAd","2")
                }

                override fun onAdShowedFullScreenContent() {
                    mInterstitialAd = null
                }
            }

            mInterstitialAd?.show(this@MainActivity)
        } else {
            val intent = Intent(this@MainActivity, PreviewActivity::class.java)
            intent.putExtra("uriPath", Uri.parse(TrimVideo.getTrimmedVideoPath(resultUri))?.path)
            startActivity(intent)
            Log.d("ShowAd","3")
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_VIDEO_TRIMMER && resultCode == RESULT_OK) {
            val path = data?.data
            val uriPath = path?.let { UriUtils.getPath(this, it) }
            if (uriPath != null) {
                App.selectFileEvents(this, uriPath)

                TrimVideo.activity(path.toString()).start(this, startForResult);
                /*val intent = Intent(this, TrimmingActivity::class.java)
                intent.putExtra(EXTRA_INPUT_URI, uriPath)
                intent.putExtra(VIDEO_TOTAL_DURATION, getMediaDuration(Uri.parse(uriPath)))
                startActivity(intent)*/
            }
        }
    }

    private fun getMediaDuration(uriOfFile: Uri): Int {
        val mp = MediaPlayer.create(this, Uri.fromFile(File(uriOfFile.toString())))
        return mp.duration
    }

    override fun onBackPressed() {
        showingExitAlertDialog()
    }


    private fun showingExitAlertDialog() {
        android.app.AlertDialog.Builder(this)
            .setTitle("Exit")
            .setMessage("Do you want to exit?")
            .setPositiveButton(
                "Cancel"
            ) { dialog: DialogInterface?, which: Int ->
                dialog?.dismiss()
            }
            .setNegativeButton(
                "Exit"
            ) { dialog: DialogInterface?, which: Int -> finishAffinity() }
            .show()
    }


    fun sharingApplication(context: Context) {
        val sendIntent = Intent()
        sendIntent.action = Intent.ACTION_SEND
        sendIntent.putExtra(
            Intent.EXTRA_TEXT,
            "https://play.google.com/store/apps/details?id=" + BuildConfig.APPLICATION_ID
        )
        sendIntent.type = "text/plain"
        context.startActivity(sendIntent)
    }

    fun privacyPolicy(context: Context) {
        val browserIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://docs.google.com/document/d/1YeGAPF-JuRLEDJ9VCRfhV1uLov40w9A7oYV3gs7i2Gk/edit")
        )
        context.startActivity(browserIntent)
    }


}