package video.wallpaper.videotolive.maker.apputils;

import android.app.Application;

import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;

import org.jetbrains.annotations.NotNull;

public class MyApplication extends Application {
    private static AppOpenManager appOpenManager;
    @Override
    public void onCreate() {
        super.onCreate();
        MobileAds.initialize(
                this,
                new OnInitializationCompleteListener() {
                    @Override
                    public void onInitializationComplete(@NotNull InitializationStatus initializationStatus) {}
                });


        appOpenManager = new AppOpenManager(this);
    }
}
