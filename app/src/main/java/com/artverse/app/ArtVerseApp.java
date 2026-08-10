package com.artverse.app;

import android.app.Application;

import com.artverse.app.utils.FirebaseUtil;

/**
 * Runs before any activity, so it is the safe place to point Firebase at the
 * local emulator (a no-op unless DEMO_MODE is enabled).
 */
public class ArtVerseApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        FirebaseUtil.configureEmulatorsIfDemo();
    }
}
