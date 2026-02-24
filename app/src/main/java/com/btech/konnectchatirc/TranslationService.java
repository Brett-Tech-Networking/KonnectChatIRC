package com.btech.konnectchatirc;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONArray;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

/**
 * Service to handle message translation using free web endpoints.
 */
public class TranslationService {

    private static final String TAG = "TranslationService";

    public interface TranslationCallback {
        void onTranslationComplete(String translatedText);
        void onTranslationError(Exception e);
    }

    /**
     * Translates text to English using a public translation endpoint.
     */
    public static void translate(final String text, final TranslationCallback callback) {
        new Thread(() -> {
            try {
                // Using Google Translate's free web endpoint
                String urlString = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl=en&dt=t&q=" 
                                   + URLEncoder.encode(text, "UTF-8");
                
                URL url = new URL(urlString);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("User-Agent", "Mozilla/5.0");

                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                // Format of response is [[["translated text","original text",null,null,1]...],...]
                JSONArray jsonArray = new JSONArray(response.toString());
                JSONArray innerArray = jsonArray.getJSONArray(0);
                
                StringBuilder result = new StringBuilder();
                for (int i = 0; i < innerArray.length(); i++) {
                    result.append(innerArray.getJSONArray(i).getString(0));
                }

                String translatedText = result.toString();
                
                new Handler(Looper.getMainLooper()).post(() -> callback.onTranslationComplete(translatedText));
            } catch (Exception e) {
                Log.e(TAG, "Translation failed", e);
                new Handler(Looper.getMainLooper()).post(() -> callback.onTranslationError(e));
            }
        }).start();
    }
}
