package video.wallpaper.videotolive.maker.apputils

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import androidx.core.app.ActivityCompat.finishAffinity

object AppRater {



    private fun showrateUs(mContext: Activity) {
        mContext.startActivity(
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/apps/details?id=" + mContext.packageName)
            )
        )
    }

    fun showRateDialog(mContext:Activity) {
        android.app.AlertDialog.Builder(mContext)
            .setTitle("Rate Us")
            .setMessage("How was your experience with us?")
            .setPositiveButton(
                "Rate Us"
            ) { dialog: DialogInterface?, which: Int ->
                showrateUs(mContext)
                dialog?.dismiss()
            }
            .setNegativeButton(
                "Exit"
            ) { dialog: DialogInterface?, which: Int -> finishAffinity(mContext) }
            .show()
    }
}