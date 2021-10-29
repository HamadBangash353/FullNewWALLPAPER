package video.wallpaper.videotolive.maker.activities

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.WindowManager
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import video.wallpaper.videotolive.maker.R

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)


        val ivSplash = findViewById<ImageView>(R.id.ivSplash)



        Glide
            .with(this)
            .load(R.drawable.splash)
            .centerCrop()
            .into(ivSplash)

        timeToMoveToMainActivity()
    }


    private fun timeToMoveToMainActivity() {
        Handler().postDelayed({
            val mainIntent = Intent(this@SplashActivity, MainActivity::class.java)
            startActivity(mainIntent)
            finish()
        }, 3500)
    }
}