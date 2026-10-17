package com.ripti.cluerush.util;

import android.content.Context;

import org.osmdroid.config.Configuration;
import org.osmdroid.config.IConfigurationProvider;

import java.io.File;

public final class MapConfig {

    private static boolean initialised;

    private MapConfig() {
    }

    public static synchronized void init(Context context) {
        if (initialised) {
            return;
        }
        Context app = context.getApplicationContext();
        IConfigurationProvider config = Configuration.getInstance();
        config.load(app, app.getSharedPreferences("osmdroid", Context.MODE_PRIVATE));
        config.setUserAgentValue(app.getPackageName());

        File base = new File(app.getCacheDir(), "osmdroid");
        File tiles = new File(base, "tiles");
        tiles.mkdirs();
        config.setOsmdroidBasePath(base);
        config.setOsmdroidTileCache(tiles);
        initialised = true;
    }
}