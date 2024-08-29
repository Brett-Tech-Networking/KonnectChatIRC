package com.btech.konnectchatirc;

import android.content.Context;
import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.engine.bitmap_recycle.LruBitmapPool;
import com.bumptech.glide.module.AppGlideModule;
import com.bumptech.glide.request.RequestOptions;

@GlideModule
public final class MyAppGlideModule extends AppGlideModule {
    @Override
    public void applyOptions(Context context, GlideBuilder builder) {
        // Set options globally for Glide
        builder.setDefaultRequestOptions(
                new RequestOptions()
                        .format(DecodeFormat.PREFER_RGB_565) // Use a non-hardware format
                        .disallowHardwareConfig() // Explicitly disallow hardware bitmaps
        );
        // Optional: Adjust bitmap pool size if needed
        builder.setBitmapPool(new LruBitmapPool(20 * 1024 * 1024)); // 20MB pool size
    }
}
