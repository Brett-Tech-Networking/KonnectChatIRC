package com.btech.konnectchatirc;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class FallingItemsView extends View {

    private final List<FallingItem> items = new ArrayList<>();
    private final Random random = new Random();
    private Bitmap itemBitmap;
    private boolean isAnimating = false;
    private long lastFrameTime = 0;
    private final Paint paint = new Paint();

    public FallingItemsView(Context context) {
        super(context);
        init();
    }

    public FallingItemsView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public FallingItemsView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        // Load the placeholder drawable
        Drawable drawable = ContextCompat.getDrawable(getContext(), R.drawable.ic_fun_item);
        if (drawable != null) {
            drawable = (DrawableCompat.wrap(drawable)).mutate();
            // Set a size for the bitmap (e.g., 64x64 dp converted to pixels)
            int size = (int) (64 * getResources().getDisplayMetrics().density);
            Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            drawable.draw(canvas);
            itemBitmap = bitmap;
        }
    }

    public void startAnimation() {
        if (!isAnimating) {
            isAnimating = true;
            lastFrameTime = System.currentTimeMillis();
            postInvalidate();
        }
    }

    public void stopAnimation() {
        isAnimating = false;
        items.clear();
        postInvalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (!isAnimating || itemBitmap == null) return;

        long currentTime = System.currentTimeMillis();
        float deltaTime = (currentTime - lastFrameTime) / 1000f; // Seconds
        lastFrameTime = currentTime;

        if (random.nextFloat() < 0.02f) { // Adjusted spawn rate
            int width = getWidth();
            if (width > 0) {
                items.add(new FallingItem(random.nextInt(width), -itemBitmap.getHeight()));
            }
        }

        // Update and draw items
        Iterator<FallingItem> iterator = items.iterator();
        while (iterator.hasNext()) {
            FallingItem item = iterator.next();
            item.y += item.speed * deltaTime; // Removed * 100 factor
            item.rotation += item.rotationSpeed * deltaTime;

            if (item.y > getHeight()) {
                iterator.remove();
            } else {
                canvas.save();
                canvas.translate(item.x, item.y);
                canvas.rotate(item.rotation, itemBitmap.getWidth() / 2f, itemBitmap.getHeight() / 2f);
                canvas.drawBitmap(itemBitmap, 0, 0, paint);
                canvas.restore();
            }
        }

        if (isAnimating) {
            postInvalidateOnAnimation();
        }
    }

    @Override
    public boolean onTouchEvent(android.view.MotionEvent event) {
        return false; // Pass touches through to the underlying views
    }

    private class FallingItem {
        float x, y;
        float speed;
        float rotation;
        float rotationSpeed;

        FallingItem(float x, float y) {
            this.x = x;
            this.y = y;
            this.speed = 100f + random.nextFloat() * 200f; // Slower speed: 100-300 pixels/sec
            this.rotation = random.nextFloat() * 360f;
            this.rotationSpeed = (random.nextFloat() - 0.5f) * 100f; // Slower rotation
        }
    }
}
