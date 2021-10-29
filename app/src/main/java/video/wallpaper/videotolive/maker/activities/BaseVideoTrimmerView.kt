package video.wallpaper.videotolive.maker.activities

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Message
import android.util.AttributeSet
import android.util.Log
import android.view.*
import android.view.GestureDetector.SimpleOnGestureListener
import android.widget.*
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.annotation.NonNull
import androidx.annotation.RequiresApi
import video.wallpaper.videotolive.maker.R
import video.wallpaper.videotolive.maker.`interface`.OnBaseVideoListener
import video.wallpaper.videotolive.maker.`interface`.OnProgressVideoListener
import video.wallpaper.videotolive.maker.`interface`.OnRangeSeekBarListener
import video.wallpaper.videotolive.maker.`interface`.OnTrimVideoListener
import video.wallpaper.videotolive.maker.videoutils.BackgroundExecutor
import video.wallpaper.videotolive.maker.videoutils.TrimVideoUtils
import video.wallpaper.videotolive.maker.videoutils.TrimVideoUtils.stringForTime
import video.wallpaper.videotolive.maker.videoutils.UiThreadExecutor
import video.wallpaper.videotolive.maker.view.ProgressBarView
import video.wallpaper.videotolive.maker.view.RangeSeekBarVIews
import video.wallpaper.videotolive.maker.view.Thumb
import video.wallpaper.videotolive.maker.view.TimeLineView
import java.io.File
import java.lang.ref.WeakReference
import java.util.*

class BaseVideoTrimmerView @JvmOverloads constructor(
    @NonNull context: Context,
    attrs: AttributeSet?,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    private var mHolderTopView: SeekBar? = null
    private var mRangeSeekBarView: RangeSeekBarVIews? = null
    private var mLinearVideo: RelativeLayout? = null
    private var mTimeInfoContainer: View? = null
    private var mVideoView: VideoView? = null
    private var mPlayView: ImageView? = null
    private var mTextSize: TextView? = null
    private var mTextTimeFrame: TextView? = null
    private var mTextTime: TextView? = null
    private var mTimeLineView: TimeLineView? = null
    private var mVideoProgressIndicator: ProgressBarView? = null
    private var mSrc: Uri? = null
    private var mMaxDuration = 0
    private var mListeners: MutableList<OnProgressVideoListener?>? = null
    private var mOnTrimVideoListener: OnTrimVideoListener? = null
    private var mOnBaseVideoListener: OnBaseVideoListener? = null
    private var thumbnailProgress: ProgressBar? = null
    private var mDuration = 0
    private var mTimeVideo = 0
    private var mStartPosition = 0
    private var mEndPosition = 0
    private var mOriginSizeFile: Long = 0
    private var mResetSeekBar = true
    val dstFile2: String = (Environment.getExternalStorageDirectory()
        .toString() + "/Video Wallpaper/")

    private val mMessageHandler: MessageHandler =
        MessageHandler(this)

    private fun init(context: Context) {
        LayoutInflater.from(context).inflate(R.layout.view_time_line, this, true)
        mHolderTopView = findViewById<View>(R.id.handlerTop) as SeekBar
        mVideoProgressIndicator = findViewById<View>(R.id.timeVideoView) as ProgressBarView
        thumbnailProgress = findViewById<View>(R.id.thumbnailProgress) as ProgressBar
        mRangeSeekBarView = findViewById<View>(R.id.timeLineBar) as RangeSeekBarVIews
        mLinearVideo = findViewById<View>(R.id.layout_surface_view) as RelativeLayout
        mVideoView = findViewById<View>(R.id.video_loader) as VideoView
        mPlayView = findViewById<View>(R.id.icon_video_play) as ImageView
        mTimeInfoContainer = findViewById(R.id.timeText)
        mTextSize = findViewById<View>(R.id.textSize) as TextView
        mTextTimeFrame = findViewById<View>(R.id.textTimeSelection) as TextView
        mTextTime = findViewById<View>(R.id.textTime) as TextView
        mTimeLineView = findViewById<View>(R.id.timeLineView) as TimeLineView
        setUpListeners()
        setUpMargins()


    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setUpListeners() {
        mListeners = ArrayList<OnProgressVideoListener?>()
        mListeners!!.add(object : OnProgressVideoListener {
            override fun updateProgress(time: Int, max: Int, scale: Float) {
                updateVideoProgress(time)
            }
        })
        mListeners!!.add(mVideoProgressIndicator)

        findViewById<View>(R.id.btSkip)
            .setOnClickListener {
                onSkip()
            }

        findViewById<View>(R.id.btSave)
            .setOnClickListener { onSaveClicked() }
        val gestureDetector = GestureDetector(
            context,
            object : SimpleOnGestureListener() {
                override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                    onClickVideoPlayPause()
                    return true
                }
            }
        )
        mVideoView!!.setOnErrorListener { mediaPlayer, what, extra ->
            if (mOnTrimVideoListener != null)
                mOnTrimVideoListener!!.onError("Something went wrong reason : $what")

            false
        }

        mVideoView!!.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            true
        }

        mRangeSeekBarView?.addOnRangeSeekBarListener(mVideoProgressIndicator!!)
        mRangeSeekBarView?.addOnRangeSeekBarListener(object : OnRangeSeekBarListener {
            override fun onCreate(rangeSeekBarView: RangeSeekBarVIews?, index: Int, value: Float) {}
            override fun onSeek(rangeSeekBarView: RangeSeekBarVIews?, index: Int, value: Float) {
                onSeekThumbs(index, value)
            }

            override fun onSeekStart(
                rangeSeekBarView: RangeSeekBarVIews?,
                index: Int,
                value: Float
            ) {
            }

            override fun onSeekStop(
                rangeSeekBarView: RangeSeekBarVIews?,
                index: Int,
                value: Float
            ) {
                onStopSeekThumbs()
            }
        })
        mHolderTopView!!.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                onPlayerIndicatorSeekChanged(progress, fromUser)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                onPlayerIndicatorSeekStart()
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                onPlayerIndicatorSeekStop(seekBar)
            }
        })
        mVideoView!!.setOnPreparedListener { mp -> onVideoPrepared(mp) }
        mVideoView!!.setOnCompletionListener { onVideoCompleted() }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    private fun setUpMargins() {
        val marge: Int = mRangeSeekBarView?.thumbs?.get(0)?.widthBitmap!!
//        val widthSeek = mHolderTopView!!.thumb.minimumWidth / 2
        val widthSeek = mHolderTopView!!.thumb.minimumWidth / 4
        var lp = mHolderTopView!!.layoutParams as RelativeLayout.LayoutParams
        lp.setMargins(marge - widthSeek, 0, marge - widthSeek, 0)
        mHolderTopView!!.layoutParams = lp
        lp = mTimeLineView?.layoutParams as RelativeLayout.LayoutParams
        lp.setMargins(marge, 0, marge, 0)
        mTimeLineView?.layoutParams = lp
        lp = mVideoProgressIndicator?.layoutParams as RelativeLayout.LayoutParams
        lp.setMargins(marge, 0, marge, 0)
        mVideoProgressIndicator?.layoutParams = lp
    }

    @RequiresApi(api = Build.VERSION_CODES.GINGERBREAD_MR1)
    private fun onSaveClicked() {
        if (mStartPosition <= 0 && mEndPosition >= mDuration) {
            if (mOnTrimVideoListener != null) mOnTrimVideoListener!!.onFinishedTrimming(mSrc)
        } else {
            mPlayView!!.visibility = VISIBLE
            mVideoView!!.pause()
            val mediaMetadataRetriever = MediaMetadataRetriever()
            mediaMetadataRetriever.setDataSource(context, mSrc)
            val METADATA_KEY_DURATION =
                mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)!!
                    .toLong()
//            val file = File(mSrc.toString())
            if (mTimeVideo < MIN_TIME_FRAME) {
                if (METADATA_KEY_DURATION - mEndPosition > MIN_TIME_FRAME - mTimeVideo) {
                    mEndPosition += MIN_TIME_FRAME - mTimeVideo
                } else if (mStartPosition > MIN_TIME_FRAME - mTimeVideo) {
                    mStartPosition -= MIN_TIME_FRAME - mTimeVideo
                }
            }
            Log.d("BASEVideoView", mSrc.toString())
            val fileUri = File(mSrc?.path)

            //notify that video trimming started
            if (mOnTrimVideoListener != null) mOnTrimVideoListener!!.onTrimStarted()
            BackgroundExecutor.execute(
                object : BackgroundExecutor.Task(null, 0L, null) {
                    override fun execute() {
                        try {
                            TrimVideoUtils.startTrim(
                                fileUri,
                                dstFile2,
                                mStartPosition.toLong(),
                                mEndPosition.toLong(),
                                mOnTrimVideoListener!!
                            )
                        } catch (e: Throwable) {
                            Thread.getDefaultUncaughtExceptionHandler()
                                .uncaughtException(Thread.currentThread(), e)
                        }
                    }
                }
            )
        }
    }

    private fun onClickVideoPlayPause() {
        if (mVideoView!!.isPlaying) {
            mPlayView!!.visibility = VISIBLE
            mMessageHandler.removeMessages(SHOW_PROGRESS)
            mVideoView!!.pause()
        } else {
            mPlayView!!.visibility = GONE
            if (mResetSeekBar) {
                mResetSeekBar = false
                mVideoView!!.seekTo(mStartPosition)
            }
            mMessageHandler.sendEmptyMessage(SHOW_PROGRESS)
            mVideoView!!.start()
        }
    }

    private fun onSkip() {
        val intent = Intent(context, PreviewActivity::class.java)
        intent.putExtra("uriPath", mSrc?.path)
        context.startActivity(intent)
    }

    private fun onCancelClicked() {
        mVideoView!!.stopPlayback()
        if (mOnTrimVideoListener != null) {
            mOnTrimVideoListener!!.cancelAction()
        }
    }

    /**
     * Sets the path where the trimmed video will be saved
     * Ex: /storage/emulated/0/MyAppFolder/
     *
     * @param finalPath the full path
     */
    /*    private var destinationPath: String?
        private get() {
            if (mFinalPath == null) {
                val folder = Environment.getExternalStorageDirectory()
                mFinalPath = folder.path + File.separator
                Log.d(
                    TAG,
                    "Using default path $mFinalPath"
                )
            }
            return mFinalPath
        }
        set(finalPath) {
            mFinalPath = finalPath
            Log.d(
                TAG,
                "Setting custom path $mFinalPath"
            )
        }*/

    private fun onPlayerIndicatorSeekChanged(progress: Int, fromUser: Boolean) {
        var duration = (mDuration * progress / 1000L).toInt()
        if (fromUser) {
            if (duration < mStartPosition) {
                setProgressBarPosition(mStartPosition)
                duration = mStartPosition
            } else if (duration > mEndPosition) {
                setProgressBarPosition(mEndPosition)
                duration = mEndPosition
            }
            setTimeVideo(duration)
        }
    }

    private fun onPlayerIndicatorSeekStart() {
        mMessageHandler.removeMessages(SHOW_PROGRESS)
        mVideoView!!.pause()
        mPlayView!!.visibility = VISIBLE
        notifyProgressUpdate(false)
    }

    private fun onPlayerIndicatorSeekStop(@NonNull seekBar: SeekBar) {
        mMessageHandler.removeMessages(SHOW_PROGRESS)
        mVideoView!!.pause()
        mPlayView!!.visibility = VISIBLE
        val duration = (mDuration * seekBar.progress / 1000L).toInt()
        mVideoView!!.seekTo(duration)
        setTimeVideo(duration)
        notifyProgressUpdate(false)
    }

    private fun onVideoPrepared(@NonNull mp: MediaPlayer) {
        // Adjust the size of the video
        // so it fits on the screen
        val videoWidth = mp.videoWidth
        val videoHeight = mp.videoHeight
        val videoProportion = videoWidth.toFloat() / videoHeight.toFloat()
        val screenWidth = mLinearVideo!!.width
        val screenHeight = mLinearVideo!!.height
        val screenProportion = screenWidth.toFloat() / screenHeight.toFloat()
        val lp = mVideoView!!.layoutParams
        if (videoProportion > screenProportion) {
            lp.width = screenWidth
            lp.height = (screenWidth.toFloat() / videoProportion).toInt()
        } else {
            lp.width = (videoProportion * screenHeight.toFloat()).toInt()
            lp.height = screenHeight
        }
        mVideoView!!.layoutParams = lp
        mPlayView!!.visibility = VISIBLE
        mDuration = mVideoView!!.duration
        setSeekBarPosition()
        setTimeFrames()
        setTimeVideo(0)
        if (mOnBaseVideoListener != null) {
            mOnBaseVideoListener?.onVideoPrepared()
        }
    }

    private fun setSeekBarPosition() {
        if (mDuration >= mMaxDuration) {
            mStartPosition = mDuration / 2 - mMaxDuration / 2
            mEndPosition = mDuration / 2 + mMaxDuration / 2
            mRangeSeekBarView?.setThumbValue(0, (mStartPosition * 100 / mDuration).toFloat())
            mRangeSeekBarView?.setThumbValue(1, (mEndPosition * 100 / mDuration).toFloat())
        } else {
            mStartPosition = 0
            mEndPosition = mDuration
        }
        setProgressBarPosition(mStartPosition)
        mVideoView!!.seekTo(mStartPosition)
        mTimeVideo = mDuration
        mRangeSeekBarView?.initMaxWidth()
    }

    private fun setTimeFrames() {
        val seconds = context.getString(R.string.short_seconds)
        mTextTimeFrame!!.text = java.lang.String.format(
            "%s %s - %s %s",
            stringForTime(mStartPosition),
            seconds,
            stringForTime(mEndPosition),
            seconds
        )
    }

    private fun setTimeVideo(position: Int) {
        val seconds = context.getString(R.string.short_seconds)
        mTextTime!!.text = java.lang.String.format("%s %s", stringForTime(position), seconds)
    }

    private fun onSeekThumbs(index: Int, value: Float) {
        when (index) {
            Thumb.LEFT -> {
                mStartPosition = (mDuration * value / 100L).toInt()
                mVideoView!!.seekTo(mStartPosition)
            }
            Thumb.RIGHT -> {
                mEndPosition = (mDuration * value / 100L).toInt()
            }
        }
        setProgressBarPosition(mStartPosition)
        setTimeFrames()
        mTimeVideo = mEndPosition - mStartPosition
    }

    private fun onStopSeekThumbs() {
        mMessageHandler.removeMessages(SHOW_PROGRESS)
        mVideoView!!.pause()
        mPlayView!!.visibility = VISIBLE
    }

    private fun onVideoCompleted() {
        mVideoView!!.seekTo(mStartPosition)
    }

    private fun notifyProgressUpdate(all: Boolean) {
        if (mDuration == 0) return
        val position = mVideoView!!.currentPosition
        if (all) {
            for (item in mListeners!!) {
                item?.updateProgress(position, mDuration, (position * 100 / mDuration).toFloat())
            }
        } else {
            mListeners!![1]?.updateProgress(
                position, mDuration,
                (position * 100 / mDuration).toFloat()
            )
        }
    }

    private fun updateVideoProgress(time: Int) {
        if (mVideoView == null) {
            return
        }
        if (time >= mEndPosition) {
            mMessageHandler.removeMessages(SHOW_PROGRESS)
            mVideoView!!.pause()
            mPlayView!!.visibility = VISIBLE
            mResetSeekBar = true
            return
        }
        if (mHolderTopView != null) {
            // use long to avoid overflow
            setProgressBarPosition(time)
        }
        setTimeVideo(time)
    }

    private fun setProgressBarPosition(position: Int) {
        if (mDuration > 0) {
            val pos = 1000L * position / mDuration
            mHolderTopView!!.progress = pos.toInt()
        }
    }

    /**
     * Set video information visibility.
     * For now this is for debugging
     *
     * @param visible whether or not the videoInformation will be visible
     */
    fun setVideoInformationVisibility(visible: Boolean) {
        mTimeInfoContainer!!.visibility = if (visible) VISIBLE else GONE
    }

    /**
     * Listener for events such as trimming operation success and cancel
     *
     * @param onTrimVideoListener interface for events
     */
    fun setOnTrimVideoListener(onTrimVideoListener: OnTrimVideoListener?) {
        mOnTrimVideoListener = onTrimVideoListener
    }

    /**
     * Listener for some [VideoView] events
     *
     * @param onBaseVideoListener interface for events
     */
    fun setOnBaseVideoListener(onBaseVideoListener: OnBaseVideoListener?) {
        mOnBaseVideoListener = onBaseVideoListener
    }

    /**
     * Cancel all current operations
     */
    fun destroy() {
        BackgroundExecutor.cancelAll("", true)
        UiThreadExecutor.cancelAllTask("")
    }

    /**
     * Set the maximum duration of the trimmed video.
     * The trimmer interface wont allow the user to set duration longer than maxDuration
     *
     * @param maxDuration the maximum duration of the trimmed video in seconds
     */
    fun setMaxDuration(maxDuration: Int) {
//        mMaxDuration = maxDuration * 1000;
        mMaxDuration = maxDuration
    }

    /**
     * Sets the uri of the video to be trimmer
     *
     * @param videoURI Uri of the video
     */
    fun setVideoURI(videoURI: Uri?) {
        mSrc = videoURI
        if (mOriginSizeFile == 0L) {
            val file = File(mSrc!!.path)
            mOriginSizeFile = file.length()
            val fileSizeInKB = mOriginSizeFile / 1024
            if (fileSizeInKB > 1000) {
                val fileSizeInMB = fileSizeInKB / 1024
                mTextSize!!.text =
                    String.format("%s %s", fileSizeInMB, context.getString(R.string.megabyte))
            } else {
                mTextSize!!.text =
                    String.format("%s %s", fileSizeInKB, context.getString(R.string.kilobyte))
            }
        }
//        val finalUri =
//            FileProvider.getUriForFile(context,
//                context.getString(R.string.app_provider),
//                File(mSrc.toString()))

        mVideoView!!.setVideoURI(mSrc)
        mVideoView!!.requestFocus()
        mTimeLineView?.setVideo(mSrc)
//        mTimeLineView?.progress(thumbnailProgress!!)
    }

    private class MessageHandler internal constructor(view: BaseVideoTrimmerView) :
        Handler() {
        @NonNull
        private val mView: WeakReference<BaseVideoTrimmerView> =
            WeakReference<BaseVideoTrimmerView>(view)

        override fun handleMessage(msg: Message) {
            val view: BaseVideoTrimmerView? = mView.get()
            if (view?.mVideoView == null) {
                return
            }
            view.notifyProgressUpdate(true)
            if (view.mVideoView!!.isPlaying) {
                sendEmptyMessageDelayed(0, 10)
            }
        }

    }

    companion object {
        private val TAG: String =
            BaseVideoTrimmerView::class.java.simpleName
        private const val MIN_TIME_FRAME = 1000
        private const val SHOW_PROGRESS = 2
    }

    init {
        init(context)
    }
}