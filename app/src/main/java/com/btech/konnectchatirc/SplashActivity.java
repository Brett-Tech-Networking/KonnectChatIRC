package com.btech.konnectchatirc;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DELAY = 3000; // 3 seconds delay

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        android.widget.ImageView splashImageView = findViewById(R.id.splashGif);
        
        // Apply ColorMatrix to remove white (keying out white)
        float[] colorMatrix = {
            1f, 0, 0, 0, 0, //red
            0, 1f, 0, 0, 0, //green
            0, 0, 1f, 0, 0, //blue
            -0.33f, -0.33f, -0.33f, 0, 255f //alpha
        };
        android.graphics.ColorMatrixColorFilter filter = new android.graphics.ColorMatrixColorFilter(colorMatrix);
        splashImageView.setColorFilter(filter);

        // Use Glide to load the GIF
        com.bumptech.glide.Glide.with(this)
                .asGif()
                .load(R.drawable.konnecttrans)
                .into(splashImageView);

        // Delay for 3 seconds and then start MainActivity
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(SplashActivity.this, MainActivity.class);
                startActivity(intent);
                finish(); // Close the splash activity
            }
        }, SPLASH_DELAY);
    }
}

