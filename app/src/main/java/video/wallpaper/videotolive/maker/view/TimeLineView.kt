package video.wallpaper.videotolive.maker.view

import video.wallpaper.videotolive.maker.R
import video.wallpaper.videotolive.maker.videoutils.BackgroundExecutor
import video.wallpaper.videotolive.maker.videoutils.UiThreadExecutor
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.AttributeSet
import android.util.LongSparseArray
import android.view.View
import androidx.annotation.NonNull
import androidx.annotation.RequiresApi

class TimeLineView @JvmOverloads constructor(
    @NonNull context: Context?,
    attrs: AttributeSet?,
    defStyleAttr: Int = 0
) :
    View(context, attrs, defStyleAttr) {
    private var mVideoUri: Uri? = null
    private var mHeightView = 0
    private var mBitmapList: LongSparseArray<Bitmap?>? = null
    private var checkProgress: Boolean = false

    private fun init() {
        mHeightView = context.resources.getDimensionPixelOffset(R.dimen.frames_video_height)
    }

    @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB)
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val minW = paddingLeft + paddingRight + suggestedMinimumWidth
        val w = resolveSizeAndState(minW, widthMeasureSpec, 1)
        val minH = paddingBottom + paddingTop + mHeightView
        val h = resolveSizeAndState(minH, heightMeasureSpec, 1)
        setMeasuredDimension(w, h)
    }

    override fun onSizeChanged(w: Int, h: Int, oldW: Int, oldH: Int) {
        super.onSizeChanged(w, h, oldW, oldH)
        if (w != oldW) {
            getBitmap(w)
        }
    }

    private fun getBitmap(viewWidth: Int) {
        BackgroundExecutor.execute(object : BackgroundExecutor.Task("", 0L, "") {
            @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
            override fun execute() {
                try {
                    val thumbnailList = LongSparseArray<Bitmap?>()
                    val mediaMetadataRetriever = MediaMetadataRetriever()
                    mediaMetadataRetriever.setDataSource(context, mVideoUri)
                    // Retrieve media data
                    val videoLengthInMs =
                        (mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)!!
                            .toInt() * 1000).toLong()
                    // Set thumbnail properties (Thumbs are squares)
                    val thumbWidth = mHeightView
                    val thumbHeight = mHeightView
                    val numThumbs =
                        Math.ceil((viewWidth.toFloat() / thumbWidth).toDouble()).toInt()
                    val interval = videoLengthInMs / numThumbs
                    for (i in 0 until numThumbs) {
                        var bitmap = mediaMetadataRetriever.getFrameAtTime(
                            i * interval,
                            MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                        )
                        // TODO: bitmap might be null here, hence throwing NullPointerException. You were right
                        try {
                            bitmap =
                                Bitmap.createScaledBitmap(bitmap!!, thumbWidth, thumbHeight, false)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        thumbnailList.put(i.toLong(), bitmap)
                    }
                    mediaMetadataRetriever.release()
                    returnBitmaps(thumbnailList)
                } catch (e: Throwable) {
                    Thread.getDefaultUncaughtExceptionHandler()
                        .uncaughtException(Thread.currentThread(), e)
                }
            }

        }
        )
    }

    private fun returnBitmaps(thumbnailList: LongSparseArray<Bitmap?>) {
        UiThreadExecutor.runTask("", Runnable {
            mBitmapList = thumbnailList
            invalidate()
        }, 0L)
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    override fun onDraw(@NonNull canvas: Canvas) {
        super.onDraw(canvas)
        if (mBitmapList != null) {
            canvas.save()
            var x = 0
            for (i in 0 until mBitmapList!!.size()) {
                val bitmap = mBitmapList!![i.toLong()]
                if (bitmap != null) {
                    canvas.drawBitmap(bitmap, x.toFloat(), 0f, null)
                    x = x + bitmap.width
                }
            }
            checkProgress = true
        } else {
            checkProgress = false
        }
    }

    fun setVideo(@NonNull data: Uri?) {
        mVideoUri = data
    }

    init {
        init()
    }
}