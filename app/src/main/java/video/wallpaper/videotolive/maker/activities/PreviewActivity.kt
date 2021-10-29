package video.wallpaper.videotolive.maker.activities

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.CompoundButton
import androidx.appcompat.app.AppCompatActivity
import com.yqritc.scalablevideoview.ScalableType
import video.wallpaper.videotolive.maker.R
import video.wallpaper.videotolive.maker.apputils.App
import video.wallpaper.videotolive.maker.apputils.Constants.dstFile
import video.wallpaper.videotolive.maker.apputils.SharedPref.checkDoubleTapStartVideo
import video.wallpaper.videotolive.maker.apputils.SharedPref.checkVideoScale
import video.wallpaper.videotolive.maker.apputils.SharedPref.setDoubleTapStartStop
import video.wallpaper.videotolive.maker.apputils.SharedPref.setScaleSet
import video.wallpaper.videotolive.maker.apputils.SharedPref.setVolumeBooleanOn
import video.wallpaper.videotolive.maker.databinding.ActivityPreviewBinding
import video.wallpaper.videotolive.maker.services.VideoWallpaper
import java.io.File

class PreviewActivity : AppCompatActivity() {

    var gestureDetector: GestureDetector? = null

    lateinit var binding: ActivityPreviewBinding
    var uriPath: String? = null
    lateinit var value: File
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        } else {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
        }
        binding = ActivityPreviewBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val file = File(dstFile)
        if (file.exists()) {
            if (file.listFiles().isNotEmpty()) {
                value = file.listFiles()[0]
                Log.d("MyData", value.absolutePath)
            }
        }

        val getIntent = intent
        try {
            binding.viewVideo.let { it ->
                it.visibility = View.VISIBLE
                if (getIntent.hasExtra("uriPath")) {
                    uriPath = getIntent.extras?.getString("uriPath")
                    Log.d("PreviewActivity", uriPath.toString())
                    it.setDataSource(uriPath.toString())
                } else {
                    it.setDataSource(value.toString())
                }
                if (checkVideoScale(this)) {
                    it.setScalableType(ScalableType.FIT_XY)
                } else {
                    it.setScalableType(ScalableType.CENTER_CROP)
                }
                it.prepare {
                    it?.start()
                    it?.isLooping = true
                }
                it.setOnErrorListener { mp, what, extra ->
                    this@PreviewActivity.resources.getColor(R.color.black)
                    false
                }
                it.setOnCompletionListener {
                    it?.start()
                    it?.isLooping = true
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        binding.buttonSetWallpaper.setOnClickListener {
            try {
                if (binding.viewVideo.isPlaying) {
                    binding.viewVideo.stop()
                }

                val extraPath = intent
                if (extraPath.hasExtra("uriPath")) {
                    val uriPath = getIntent.extras?.getString("uriPath")
                    VideoWallpaper.setToWallPaper(this@PreviewActivity, uriPath.toString())
                } else {
                    VideoWallpaper.setToWallPaper(this@PreviewActivity, value.toString())
                }

                finish()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        binding.settingVolume.let {
            it.setOnCheckedChangeListener(checkVolume())
            it.isChecked = checkVolumeSett()
        }

        binding.settingScale.let {
            it.setOnCheckedChangeListener(scaleCheck())
            it.isChecked = checkVideoScale(this@PreviewActivity)
        }

        binding.settingDoubleclickPause.let {
            it.setOnCheckedChangeListener(checkDoubleTap())
            it.isChecked = checkDoubleTapStartVideo(this, true)
        }
        gestureDetector = GestureDetector(this@PreviewActivity, GestureListener())


    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (checkDoubleTapStartVideo(this@PreviewActivity, true)) {
            gestureDetector?.onTouchEvent(event)
        }
        return super.onTouchEvent(event)
    }

    internal inner class checkDoubleTap : CompoundButton.OnCheckedChangeListener {
        override fun onCheckedChanged(compoundButton: CompoundButton, isChecked: Boolean) {
            if (isChecked) {
                setDoubleTapStartStop(this@PreviewActivity, true)
            } else {
                setDoubleTapStartStop(this@PreviewActivity, false)
            }
        }
    }

    internal inner class GestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onDoubleTap(motionEvent: MotionEvent): Boolean {
            try {
                if (binding.viewVideo.isPlaying) {
                    binding.viewVideo.pause()
                } else {
                    binding.viewVideo.start()
                }
            } catch (e: Exception) {
                e.printStackTrace()

            }
            return super.onDoubleTap(motionEvent)
        }
    }

    private fun checkVolumeSett(): Boolean {
        return getSharedPreferences("VideoWallpaper", MODE_PRIVATE).getBoolean(
            "pref_turn_on_audio",
            false
        )
    }

    internal inner class scaleCheck : CompoundButton.OnCheckedChangeListener {
        override fun onCheckedChanged(compoundButton: CompoundButton, check: Boolean) {
            try {
                if (check) {
                    binding.viewVideo.setScalableType(ScalableType.FIT_XY)
                    setScaleSet(context = this@PreviewActivity, true)
                } else {
                    binding.viewVideo.setScalableType(ScalableType.FIT_CENTER)
                    setScaleSet(context = this@PreviewActivity, false)
                }
            } catch (e2: java.lang.Exception) {
                e2.printStackTrace()
            }
        }
    }

    internal inner class checkVolume : CompoundButton.OnCheckedChangeListener {
        override fun onCheckedChanged(compoundButton: CompoundButton, isChecked: Boolean) {
            try {
                if (!isChecked) {
                    binding.viewVideo.setVolume(0.0f, 0.0f)
                    setVolumeBooleanOn(this@PreviewActivity, false)
                } else {
                    binding.viewVideo.setVolume(1.0f, 1.0f)
                    setVolumeBooleanOn(this@PreviewActivity, true)
                }
            } catch (e2: Exception) {
                e2.printStackTrace()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        App.setCurrentScreen(this, "preview_screen")
    }

    override fun onStop() {
        super.onStop()
        binding.viewVideo.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.viewVideo.pause()
        binding.viewVideo.release()
    }
}