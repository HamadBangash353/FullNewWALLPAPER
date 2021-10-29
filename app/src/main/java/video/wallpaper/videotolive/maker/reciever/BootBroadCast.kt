package video.wallpaper.videotolive.maker.reciever

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import video.wallpaper.videotolive.maker.services.VideoWallpaper

class BootBroadCast : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        try {

            val service = Intent(context, VideoWallpaper::class.java)
            context.startService(service)

            when (intent.action) {
                Intent.ACTION_DATE_CHANGED -> {
                }
                Intent.ACTION_BOOT_COMPLETED -> {
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }
}