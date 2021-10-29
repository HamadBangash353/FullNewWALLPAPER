package video.wallpaper.videotolive.maker.apputils

import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import java.io.File


class MediaScanner : MediaScannerConnection.MediaScannerConnectionClient {


    private var mMs: MediaScannerConnection? = null
    private var mFile: File? = null

    fun SingleMediaScanner(context: Context?, f: File?) {
        mFile = f
        mMs = MediaScannerConnection(context, this)
        mMs!!.connect()
    }

    override fun onMediaScannerConnected() {
        mMs!!.scanFile(mFile?.absolutePath, null)
    }

    override fun onScanCompleted(path: String?, uri: Uri?) {
        mMs!!.disconnect()
    }

}