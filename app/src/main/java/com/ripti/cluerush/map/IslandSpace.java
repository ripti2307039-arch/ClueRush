package com.ripti.cluerush.map;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.ripti.cluerush.game.WalkMask;
import com.ripti.cluerush.model.Arena;
import com.ripti.cluerush.util.GeoMath;

import java.io.IOException;
import java.io.InputStream;

public final class IslandSpace {

    public static final String ARENA_ID = "island";
    public static final double SIZE_METERS = 1000.0;

    private static final String TAG = "IslandSpace";
    private static final String MAP_ASSET = "island/island_map.jpg";
    private static final String MASK_ASSET = "island/island_mask.png";
    private static final double METERS_PER_DEGREE = GeoMath.METERS_PER_DEGREE_LATITUDE;

    private static Bitmap mapBitmap;
    private static WalkMask walkMask;

    private IslandSpace() {
    }

    public static boolean isIsland(Arena arena) {
        return arena != null && ARENA_ID.equals(arena.getId());
    }

    public static double xMeters(double longitude) {
        return longitude * METERS_PER_DEGREE;
    }

    public static double yMeters(double latitude) {
        return latitude * METERS_PER_DEGREE;
    }

    public static double longitudeOf(double xMeters) {
        return xMeters / METERS_PER_DEGREE;
    }

    public static double latitudeOf(double yMeters) {
        return yMeters / METERS_PER_DEGREE;
    }

    public static synchronized Bitmap mapBitmap(Context context) {
        if (mapBitmap == null) {
            try (InputStream in = context.getApplicationContext().getAssets().open(MAP_ASSET)) {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inPreferredConfig = Bitmap.Config.RGB_565;
                mapBitmap = BitmapFactory.decodeStream(in, null, options);
            } catch (IOException e) {
                Log.e(TAG, "Could not load the island map", e);
            }
        }
        return mapBitmap;
    }

    public static synchronized WalkMask walkMask(Context context) {
        if (walkMask == null) {
            try (InputStream in = context.getApplicationContext().getAssets().open(MASK_ASSET)) {
                Bitmap bitmap = BitmapFactory.decodeStream(in);
                if (bitmap != null) {
                    int w = bitmap.getWidth();
                    int h = bitmap.getHeight();
                    int[] pixels = new int[w * h];
                    bitmap.getPixels(pixels, 0, w, 0, 0, w, h);
                    bitmap.recycle();
                    boolean[] open = new boolean[w * h];
                    for (int i = 0; i < pixels.length; i++) {
                        open[i] = (pixels[i] & 0xFF) > 127;
                    }
                    walkMask = new GridMask(open, w, h);
                }
            } catch (IOException e) {
                Log.e(TAG, "Could not load the island mask", e);
            }
        }
        return walkMask;
    }

    private static final class GridMask implements WalkMask {

        private final boolean[] open;
        private final int width;
        private final int height;

        GridMask(boolean[] open, int width, int height) {
            this.open = open;
            this.width = width;
            this.height = height;
        }

        @Override
        public boolean isWalkable(double latitude, double longitude) {
            double x = longitude * METERS_PER_DEGREE;
            double y = latitude * METERS_PER_DEGREE;
            int px = (int) Math.floor((x / SIZE_METERS + 0.5) * width);
            int py = (int) Math.floor((0.5 - y / SIZE_METERS) * height);
            if (px < 0 || py < 0 || px >= width || py >= height) {
                return false;
            }
            return open[py * width + px];
        }
    }
}