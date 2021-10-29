package video.wallpaper.videotolive.maker.apputils

import android.os.Environment
import java.util.ArrayList

internal object Constants {
    const val REQUEST_GALLERY_PERMISSION = "1003"
    var dstFile: String = (Environment.getExternalStorageDirectory()
        .toString() + "/Video Wallpaper/")
    const val REQUEST_VIDEO_TRIMMER = 1
    internal const val EXTRA_INPUT_URI = "EXTRA_INPUT_URI"
    private val allowedVideoFileExtensions = arrayOf("mkv", "mp4", "3gp", "mov", "mts")
    private val videosMimeTypes = ArrayList<String>(allowedVideoFileExtensions.size)
    const val VIDEO_TOTAL_DURATION = "VIDEO_TOTAL_DURATION"
}