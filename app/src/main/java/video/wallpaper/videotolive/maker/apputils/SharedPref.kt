package video.wallpaper.videotolive.maker.apputils

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity

object SharedPref {

    fun checkDoubleTapStartVideo(context: Context, default: Boolean): Boolean {
        return context.getSharedPreferences(
            "VideoWallpaper",
            AppCompatActivity.MODE_PRIVATE
        ).getBoolean("pref_double_tap_start_stop_video", default)
    }

    fun checkVolume(context: Context, default: Boolean): Boolean {
        return context.getSharedPreferences("VideoWallpaper", AppCompatActivity.MODE_PRIVATE)
            .getBoolean(
                "pref_turn_on_audio",
                default
            )
    }

    fun setDoubleTapStartStop(context: Context, check: Boolean) {
        val edit =
            context.getSharedPreferences("VideoWallpaper", AppCompatActivity.MODE_PRIVATE).edit()
        edit.putBoolean("pref_double_tap_start_stop_video", check)
        edit.apply()
    }

    fun setScaleSet(context: Context, fitXY: Boolean) {
        val edit =
            context.getSharedPreferences("VideoWallpaper", AppCompatActivity.MODE_PRIVATE).edit()
        edit.putBoolean("pref_video_scale", fitXY)
        edit.apply()
    }

    fun saveNeverPermission(context: Context, bbb: Boolean?) {
        val prefs = context.getSharedPreferences("VideoWallpaper", Context.MODE_PRIVATE)
        val edit: SharedPreferences.Editor = prefs.edit()
        edit.putBoolean("NeverPermission", bbb!!)
        edit.apply()
    }

    fun savePermissionDenied(context: Context, bbb: Boolean?) {
        val prefs = context.getSharedPreferences("VideoWallpaper", Context.MODE_PRIVATE)
        val edit: SharedPreferences.Editor = prefs.edit()
        edit.putBoolean("permissionDenied", bbb!!)
        edit.apply()
    }

    fun setVolumeBooleanOn(context: Context, isChecked: Boolean) {
        val edit =
            context.getSharedPreferences("VideoWallpaper", AppCompatActivity.MODE_PRIVATE).edit()
        edit.putBoolean("pref_turn_on_audio", isChecked)
        edit.apply()
    }

    fun checkVideoScale(context: Context): Boolean {
        return context.getSharedPreferences("VideoWallpaper", AppCompatActivity.MODE_PRIVATE)
            .getBoolean(
                "pref_video_scale",
                true
            )
    }

//    private fun startPause(view: View, context: Context) {
//        val switchCompat = view.findViewById<View>(R.id.setting_doubleclick_pause) as SwitchCompat
//        switchCompat.isEnabled = true
//        switchCompat.isChecked = true
//    }
}