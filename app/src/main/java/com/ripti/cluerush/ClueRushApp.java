package com.ripti.cluerush;

import android.app.Application;

import androidx.appcompat.app.AppCompatDelegate;

public class ClueRushApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        // Phone er mode jai hok, app shobsomoy dark look e thakbe
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
    }
}