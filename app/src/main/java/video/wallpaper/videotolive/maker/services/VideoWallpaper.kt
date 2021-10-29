package video.wallpaper.videotolive.maker.services

import android.app.WallpaperManager
import android.content.*
import android.media.MediaPlayer
import android.service.wallpaper.WallpaperService
import android.text.TextUtils
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.SurfaceHolder
import video.wallpaper.videotolive.maker.apputils.SharedPref
import java.io.IOException

class VideoWallpaper : WallpaperService() {

    override fun onCreateEngine(): Engine {
        return VideoWallPagerEngine()
    }

    var gestureDetector: GestureDetector? = null
    var touchenabled: Boolean? = null
    var videoScaleCheck: Boolean? = null
    var checkVolume: Boolean? = null

    inner class VideoWallPagerEngine : Engine() {

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            gestureDetector = GestureDetector(this@VideoWallpaper, GestureListener())
            touchenabled = SharedPref.checkDoubleTapStartVideo(this@VideoWallpaper, true)
            videoScaleCheck = SharedPref.checkVideoScale(this@VideoWallpaper)
            checkVolume = SharedPref.checkVolume(this@VideoWallpaper, false)
        }

        override fun onVisibilityChanged(visible: Boolean) {
            if (visible) {
                Log.d(TAG, "onVisibilityChanged: Visible")
                if (mMediaPlayer != null) {
                    mMediaPlayer!!.start()
                }
            } else {
                if (mMediaPlayer != null) {
                    Log.d(TAG, "onVisibilityChanged: InVisible")
                    mMediaPlayer!!.pause()
                }
            }
        }

        internal inner class GestureListener : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(motionEvent: MotionEvent): Boolean {
                if (mMediaPlayer != null) {
                    if (mMediaPlayer?.isPlaying!!) {
                        mMediaPlayer?.pause()
                        mMediaPlayer?.setVolume(
                            1f,
                            1f
                        )
                    } else {
                        mMediaPlayer?.start()
                        mMediaPlayer?.setVolume(
                            1f,
                            1f
                        )
                    }
                }
                return super.onDoubleTap(motionEvent)
            }
        }

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)
            try {
                if (TextUtils.isEmpty(sVideoPath)) {
                    throw NullPointerException("videoPath must not be null ")
                } else {
                    if (mMediaPlayer != null) {
                        if (mMediaPlayer!!.isPlaying) {
                            mMediaPlayer!!.pause()
                            mMediaPlayer?.setVolume(
                                0f,
                                0f
                            )
                        }
                    }
                    mMediaPlayer = MediaPlayer()
                    mMediaPlayer!!.setSurface(holder.surface)
                    try {
                        mMediaPlayer!!.setDataSource(sVideoPath)
                        mMediaPlayer!!.isLooping = true
                        if (checkVolume == true) {
                            mMediaPlayer?.setVolume(
                                1.0f,
                                1.0f
                            )
                        } else mMediaPlayer?.setVolume(
                            0f,
                            0f
                        )

                        if (videoScaleCheck == true) {
                            mMediaPlayer?.setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT)
                        } else {
                            mMediaPlayer?.setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING)
                        }
                        mMediaPlayer!!.prepare()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            super.onSurfaceDestroyed(holder)
            try {
                if (mMediaPlayer != null) {
                    mMediaPlayer!!.pause()
//                    mMediaPlayer!!.release()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        override fun onTouchEvent(event: MotionEvent?) {
            if (touchenabled == true) {
                gestureDetector?.onTouchEvent(event)
            } else {
                Log.d("LogD", event.toString())
            }
            super.onTouchEvent(event)
        }
    }

    companion object {
        private val TAG = VideoWallpaper::class.java.name
        private var mMediaPlayer: MediaPlayer? = null
        var sVideoPath: String? = null

        fun setToWallPaper(context: Context, videoPath: String) {
            try {
                WallpaperManager.getInstance(context).clear()


            } catch (e: IOException) {
                e.printStackTrace()
            }

            sVideoPath = videoPath
            val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER)
            intent.putExtra(
                WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, ComponentName(
                    context,
                    VideoWallpaper::class.java
                )
            )
            context.startActivity(intent)
        }
    }
}
