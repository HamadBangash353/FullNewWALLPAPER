package video.wallpaper.videotolive.maker.activities

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.CompoundButton
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.ads.AdRequest
import video.wallpaper.videotolive.maker.R
import video.wallpaper.videotolive.maker.apputils.App
import video.wallpaper.videotolive.maker.apputils.SharedPref.checkDoubleTapStartVideo
import video.wallpaper.videotolive.maker.apputils.SharedPref.checkVideoScale
import video.wallpaper.videotolive.maker.apputils.SharedPref.checkVolume
import video.wallpaper.videotolive.maker.apputils.SharedPref.setDoubleTapStartStop
import video.wallpaper.videotolive.maker.apputils.SharedPref.setScaleSet
import video.wallpaper.videotolive.maker.apputils.SharedPref.setVolumeBooleanOn
import video.wallpaper.videotolive.maker.databinding.ActivitySettingBinding


class SettingActivity : AppCompatActivity() {

    lateinit var binding: ActivitySettingBinding

    private var audioFlag: Int = 0
    private var doubleTapFlag: Int = 0
    private var videoScaleFlag: Int = 0

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
        binding = ActivitySettingBinding.inflate(layoutInflater)
        setContentView(binding.root)


        val videoSize = checkVideoScale(this)

        val adRequest = AdRequest.Builder().build()
        binding.adView.loadAd(adRequest)



        if (videoSize) {
            videoScaleFlag = 1
            binding.videoSizing.text = getString(R.string.fitXY)
            binding.videoScaling.setImageResource(R.drawable.btn_toggle_on)
        } else {
            videoScaleFlag = 0
            binding.videoScaling.setImageResource(R.drawable.btn_toggle_off)
            binding.videoSizing.text = getString(R.string.centerCrop)
        }


        val doubleTapStatus = checkDoubleTapStartVideo(this, true)

        if (doubleTapStatus) {
            doubleTapFlag = 1
            binding.touchEnableStatus.text = getString(R.string.enable)
            binding.doubleTapOnOff.setImageResource(R.drawable.btn_toggle_on)
        } else {
            doubleTapFlag = 0
            binding.doubleTapOnOff.setImageResource(R.drawable.btn_toggle_off)
            binding.touchEnableStatus.text = getString(R.string.disable)
        }

        val audioStatus = checkVolume(this, false)
        if (audioStatus) {
            audioFlag = 1
            binding.audioStatus.text = getString(R.string.enable)
            binding.switchOnOffAudio.setImageResource(R.drawable.btn_toggle_on)
        } else {
            audioFlag = 0
            binding.switchOnOffAudio.setImageResource(R.drawable.btn_toggle_off)
            binding.audioStatus.text = getString(R.string.disable)
        }

        binding.backButtonLayout.setOnClickListener(View.OnClickListener {
            val i = Intent(this, MainActivity::class.java)
            i.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(i)
        })

        binding.switchOnOffAudio.setOnClickListener(View.OnClickListener {
            if (audioFlag == 0) {
                binding.switchOnOffAudio.setImageResource(R.drawable.btn_toggle_on)
                binding.audioStatus.text = getString(R.string.enable)
                setVolumeBooleanOn(this@SettingActivity, true)
                audioFlag = 1
            } else {
                binding.switchOnOffAudio.setImageResource(R.drawable.btn_toggle_off)
                binding.audioStatus.text = getString(R.string.disable)
                setVolumeBooleanOn(this@SettingActivity, false)
                audioFlag = 0
            }
        })


        binding.doubleTapOnOff.setOnClickListener(View.OnClickListener {
            if (doubleTapFlag == 0) {
                binding.doubleTapOnOff.setImageResource(R.drawable.btn_toggle_on)
                binding.touchEnableStatus.text = getString(R.string.enable)
                setDoubleTapStartStop(this@SettingActivity, true)
                doubleTapFlag = 1
            } else {
                binding.doubleTapOnOff.setImageResource(R.drawable.btn_toggle_off)
                binding.touchEnableStatus.text = getString(R.string.disable)
                setDoubleTapStartStop(this@SettingActivity, false)
                doubleTapFlag = 0
            }
        })


        binding.videoScaling.setOnClickListener(View.OnClickListener {
            if (videoScaleFlag == 0) {
                binding.videoScaling.setImageResource(R.drawable.btn_toggle_on)
                binding.videoSizing.text = getString(R.string.fitXY)
                setScaleSet(this@SettingActivity, true)
                videoScaleFlag = 1
            } else {
                binding.videoScaling.setImageResource(R.drawable.btn_toggle_off)
                binding.videoSizing.text = getString(R.string.centerCrop)
                setScaleSet(this@SettingActivity, false)
                videoScaleFlag = 0
            }
        })
    }

    override fun onResume() {
        super.onResume()
        App.setCurrentScreen(this, "setting_screen")
    }


}