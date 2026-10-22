package com.ripti.cluerush.data;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.ripti.cluerush.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TipRepository {

    public interface Callback {
        void onTip(String quote, String author, boolean online);
    }

    private static final String TAG = "TipRepository";
    private static final String QUOTES_URL = "https://dummyjson.com/quotes/random";
    private static final String ADVICE_URL = "https://api.adviceslip.com/advice";
    private static final int TIMEOUT_MS = 6000;
    private static final ExecutorService IO = Executors.newSingleThreadExecutor();

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    /** Network call background thread e hoy, result main thread e callback e ashe. */
    public void load(Context context, Callback callback) {
        Context app = context.getApplicationContext();
        IO.execute(() -> {
            String quote;
            String author;
            boolean online;
            try {
                String[] tip = fetchTip();
                quote = tip[0];
                author = tip[1];
                online = true;
            } catch (IOException e) {
                Log.w(TAG, "All tip sources failed, using fallback: " + e);
                String[] tips = app.getResources().getStringArray(R.array.fallback_tips);
                quote = tips[new Random().nextInt(tips.length)];
                author = "";
                online = false;
            }
            final String finalQuote = quote;
            final String finalAuthor = author;
            final boolean finalOnline = online;
            mainHandler.post(() -> callback.onTip(finalQuote, finalAuthor, finalOnline));
        });
    }

    /** Prothom API fail korle dwitiyo API try kore. */
    private static String[] fetchTip() throws IOException {
        try {
            JSONObject object = new JSONObject(httpGet(QUOTES_URL));
            return new String[]{object.getString("quote"), object.optString("author", "")};
        } catch (IOException | JSONException e) {
            Log.w(TAG, "Quotes API failed: " + e);
        }
        try {
            JSONObject slip = new JSONObject(httpGet(ADVICE_URL)).getJSONObject("slip");
            return new String[]{slip.getString("advice"), "Advice Slip"};
        } catch (IOException | JSONException e) {
            Log.w(TAG, "Advice API failed: " + e);
            throw new IOException("No tip source reachable");
        }
    }

    private static String httpGet(String address) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(address).openConnection();
        try {
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(TIMEOUT_MS);
            connection.setReadTimeout(TIMEOUT_MS);
            connection.setRequestProperty("Accept", "application/json");
            int code = connection.getResponseCode();
            if (code != HttpURLConnection.HTTP_OK) {
                throw new IOException("HTTP " + code);
            }
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder body = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    body.append(line);
                }
                return body.toString();
            }
        } finally {
            connection.disconnect();
        }
    }
}