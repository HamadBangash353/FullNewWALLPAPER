package video.wallpaper.videotolive.maker.`interface`

import android.net.Uri

interface OnTrimVideoListener {
    fun onTrimStarted()
    fun onFinishedTrimming(uri: Uri?)
    fun cancelAction()
    fun onError(message: String?)
}