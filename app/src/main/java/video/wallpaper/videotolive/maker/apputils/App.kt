package video.wallpaper.videotolive.maker.apputils

import android.content.Context
import android.os.Bundle
import androidx.core.content.ContextCompat
import com.google.firebase.analytics.FirebaseAnalytics

object App {
    fun hasPermissions(context: Context?): Boolean {
        val strArr = arrayOf(
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE"
        )
        if (context == null) {
            return true
        }
        for (checkSelfPermission in strArr) {
            if (ContextCompat.checkSelfPermission(context, checkSelfPermission) != 0) {
                return false
            }
        }
        return true
    }

    fun selectFileEvents(context: Context, eventName: String) {
        val firebaseAnalytics = FirebaseAnalytics.getInstance(context)
        val bundle = Bundle()
        bundle.putString("image_url", eventName)
        bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "Image")
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle)
    }

    fun customEvents(context: Context, eventName: String) {
        val firebaseAnalytics = FirebaseAnalytics.getInstance(context)
        val bundle = Bundle()
        bundle.putString("event", eventName)
        firebaseAnalytics.logEvent(eventName, bundle)
    }

    fun setCurrentScreen(context: Context, screenName: String) {
        val firebaseAnalytics = FirebaseAnalytics.getInstance(context)
        firebaseAnalytics.run {
            val bundle = Bundle()
            bundle.putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
            bundle.putString(
                FirebaseAnalytics.Param.SCREEN_CLASS,
                javaClass.simpleName
            )
            logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle)
        }
    }
}