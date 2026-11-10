package com.ripti.cluerush.data;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.LruCache;

import androidx.annotation.NonNull;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Profile picture storage.
 *
 * The photo the player picks is cropped to a small square JPEG and kept in two places:
 *  1. app internal storage (filesDir/profile_{uid}.jpg) for instant, offline loading
 *  2. Realtime Database users/{uid}/photo as a Base64 string, so it follows the
 *     account to another phone (no Firebase Storage / paid plan needed).
 *
 * The photo is read through ContentResolver, so it works with whatever the system
 * photo picker (MediaStore / documents provider) hands back.
 */
public final class ProfilePhotoRepository {

    public interface Callback {
        /** Called on the main thread. bitmap is null when there is no photo or it failed. */
        void onResult(Bitmap bitmap);
    }

    private static final int SIZE = 192;
    private static final int JPEG_QUALITY = 75;

    private static final ExecutorService IO = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    /** In-memory photos of other players (leaderboard / lobby). Cleared when the app closes. */
    private static final LruCache<String, Bitmap> CACHE = new LruCache<>(40);

    /** When each cached photo was downloaded. Old entries are fetched again so a changed photo shows up. */
    private static final LruCache<String, Long> CACHE_TIME = new LruCache<>(40);
    private static final long CACHE_TTL_MS = 20_000L;

    private ProfilePhotoRepository() {
    }

    /** Round drawable for the avatar circles. */
    public static Drawable circle(Context context, Bitmap bitmap) {
        RoundedBitmapDrawable drawable =
                RoundedBitmapDrawableFactory.create(context.getResources(), bitmap);
        drawable.setCircular(true);
        drawable.setAntiAlias(true);
        return drawable;
    }

    /** Saved photo: local file first, otherwise the copy in Realtime Database. */
    public static void load(Context context, String uid, Callback callback) {
        Context app = context.getApplicationContext();
        IO.execute(() -> {
            Bitmap local = BitmapFactory.decodeFile(file(app, uid).getAbsolutePath());
            if (local != null) {
                MAIN.post(() -> callback.onResult(local));
                return;
            }
            MAIN.post(() -> loadFromCloud(app, uid, callback));
        });
    }

    /**
     * Photo of ANY player (leaderboard, lobby). The signed-in player's own photo comes
     * from the local file; everyone else's is read from Realtime Database and kept in
     * memory so scrolling the list does not download it again.
     */
    public static void loadPublic(Context context, String uid, Callback callback) {
        if (uid == null || uid.isEmpty()) {
            callback.onResult(null);
            return;
        }
        FirebaseUser me = FirebaseAuth.getInstance().getCurrentUser();
        if (me != null && uid.equals(me.getUid())) {
            load(context, uid, callback);
            return;
        }
        Bitmap cached = CACHE.get(uid);
        Long cachedAt = CACHE_TIME.get(uid);
        if (cached != null && cachedAt != null
                && System.currentTimeMillis() - cachedAt < CACHE_TTL_MS) {
            callback.onResult(cached);
            return;
        }
        try {
            FirebaseDatabase.getInstance()
                    .getReference("users")
                    .child(uid)
                    .child("photo")
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            Object value = snapshot.getValue();
                            if (!(value instanceof String)) {
                                CACHE.remove(uid);
                                CACHE_TIME.remove(uid);
                                callback.onResult(null);
                                return;
                            }
                            IO.execute(() -> {
                                Bitmap bitmap = null;
                                try {
                                    byte[] bytes = Base64.decode((String) value, Base64.DEFAULT);
                                    bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                                } catch (Exception ignored) {
                                    // Corrupt cloud copy: behave as if there is no photo.
                                }
                                if (bitmap != null) {
                                    CACHE.put(uid, bitmap);
                                    CACHE_TIME.put(uid, System.currentTimeMillis());
                                }
                                Bitmap result = bitmap;
                                MAIN.post(() -> callback.onResult(result));
                            });
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            callback.onResult(null);
                        }
                    });
        } catch (Exception e) {
            callback.onResult(null);
        }
    }

    /** Picks a Uri from the photo picker, crops it square and shrinks it. */
    public static void decode(Context context, Uri uri, Callback callback) {
        Context app = context.getApplicationContext();
        IO.execute(() -> {
            Bitmap bitmap = null;
            try {
                bitmap = squareThumbnail(app, uri);
            } catch (Exception | OutOfMemoryError ignored) {
                // Unreadable or huge file: report no bitmap and let the screen show an error.
            }
            Bitmap result = bitmap;
            MAIN.post(() -> callback.onResult(result));
        });
    }

    /** Stores the already-cropped photo locally and uploads it. */
    public static void save(Context context, String uid, Bitmap bitmap) {
        save(context, uid, bitmap, null);
    }

    /** Same as save, but onFail gets the Firebase error text on the main thread. */
    public static void save(Context context, String uid, Bitmap bitmap,
                            java.util.function.Consumer<String> onFail) {
        Context app = context.getApplicationContext();
        CACHE.put(uid, bitmap);
        IO.execute(() -> {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out);
            byte[] bytes = out.toByteArray();

            try (FileOutputStream stream = new FileOutputStream(file(app, uid))) {
                stream.write(bytes);
            } catch (IOException ignored) {
                // The cloud copy below still keeps the photo.
            }

            try {
                FirebaseDatabase.getInstance()
                        .getReference("users")
                        .child(uid)
                        .child("photo")
                        .setValue(Base64.encodeToString(bytes, Base64.NO_WRAP))
                        .addOnSuccessListener(v -> {
                            if (onFail != null) {
                                MAIN.post(() -> onFail.accept("Server saved your photo"));
                            }
                        })
                        .addOnFailureListener(e -> {
                            android.util.Log.w("ProfilePhoto", "photo upload failed: " + e);
                            if (onFail != null) {
                                MAIN.post(() -> onFail.accept(
                                        "Not synced to server: photo: " + e.getMessage()));
                            }
                        });
            } catch (Exception e) {
                if (onFail != null) {
                    MAIN.post(() -> onFail.accept("Not synced to server: photo: " + e));
                }
                // Offline writes are queued by Firebase; anything else keeps the local copy.
            }
        });
    }

    private static void loadFromCloud(Context app, String uid, Callback callback) {
        try {
            FirebaseDatabase.getInstance()
                    .getReference("users")
                    .child(uid)
                    .child("photo")
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            Object value = snapshot.getValue();
                            if (!(value instanceof String)) {
                                callback.onResult(null);
                                return;
                            }
                            IO.execute(() -> {
                                Bitmap bitmap = null;
                                try {
                                    byte[] bytes = Base64.decode((String) value, Base64.DEFAULT);
                                    bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                                    if (bitmap != null) {
                                        try (FileOutputStream stream =
                                                     new FileOutputStream(file(app, uid))) {
                                            stream.write(bytes);
                                        }
                                    }
                                } catch (Exception ignored) {
                                    // Corrupt cloud copy: behave as if there is no photo.
                                }
                                Bitmap result = bitmap;
                                MAIN.post(() -> callback.onResult(result));
                            });
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            callback.onResult(null);
                        }
                    });
        } catch (Exception e) {
            callback.onResult(null);
        }
    }

    private static File file(Context context, String uid) {
        return new File(context.getFilesDir(), "profile_" + uid + ".jpg");
    }

    private static Bitmap squareThumbnail(Context context, Uri uri) throws IOException {
        Bitmap source;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ImageDecoder.Source decoderSource =
                    ImageDecoder.createSource(context.getContentResolver(), uri);
            source = ImageDecoder.decodeBitmap(decoderSource, (decoder, info, src) -> {
                decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);
                int shortSide = Math.min(info.getSize().getWidth(), info.getSize().getHeight());
                decoder.setTargetSampleSize(Math.max(1, shortSide / (SIZE * 2)));
            });
        } else {
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            try (InputStream in = context.getContentResolver().openInputStream(uri)) {
                BitmapFactory.decodeStream(in, null, bounds);
            }
            BitmapFactory.Options options = new BitmapFactory.Options();
            int shortSide = Math.min(bounds.outWidth, bounds.outHeight);
            options.inSampleSize = Math.max(1, shortSide / (SIZE * 2));
            try (InputStream in = context.getContentResolver().openInputStream(uri)) {
                source = BitmapFactory.decodeStream(in, null, options);
            }
        }

        if (source == null) {
            throw new IOException("Could not decode image");
        }

        int side = Math.min(source.getWidth(), source.getHeight());
        Bitmap square = Bitmap.createBitmap(source,
                (source.getWidth() - side) / 2, (source.getHeight() - side) / 2, side, side);
        return Bitmap.createScaledBitmap(square, SIZE, SIZE, true);
    }
}