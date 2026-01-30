package com.skyrist.pawcontrol;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.util.Random;

public class Bone {
    // ---- Size & Speed configs (percentage based) ----
    // bone width ≈ 1/12 of screen width (same feel har device par)
    private static final float WIDTH_PERCENT = 1f / 10f;   // ~0.0833
    // fall speed ≈ 32% of screen height per second (time-based)
    private static final float SPEED_H_PER_S = 0.32f;

    private Bitmap bitmap;
    private float x, y;                 // float for smooth motion
    private boolean visible = false;

    // pixels per second (computed from screen height)
    private float speedPxPerSec;

    // spawn timing (life based, same as before)
    private long lastDropTime = 0;
    private long dropInterval = 4500; // default 4.5s

    private final int screenWidth, screenHeight;
    private final Random random = new Random();

    public Bone(Context context) {
        // load & crop
        Bitmap src = GameView.cropTransparentPadding(
                BitmapFactory.decodeResource(context.getResources(), R.drawable.bone)
        );

        screenWidth = GameView.dWidth;
        screenHeight = GameView.dHeight;

        // ---- SIZE: scale by width percent, keep aspect ratio ----
        int targetW = Math.max(1, Math.round(screenWidth * WIDTH_PERCENT));
        float ar = src.getHeight() / (float) src.getWidth();
        int targetH = Math.max(1, Math.round(targetW * ar));
        bitmap = Bitmap.createScaledBitmap(src, targetW, targetH, true);

        // ---- SPEED: pixels/sec from % of screen height ----
        speedPxPerSec = SPEED_H_PER_S * screenHeight;
    }

    // ---- TIME-BASED UPDATE ----
    // dt = seconds since last frame (GameView se aayega), life for interval logic
    public void update(float dt, int life) {
        long now = System.currentTimeMillis();

        // interval by health (same logic as before)
        if (life == 3)      dropInterval = 10500;
        else if (life == 2) dropInterval =  5500;
        else if (life == 1) dropInterval =  2500;
        else                dropInterval = 11500;

        // spawn if not visible and interval elapsed
        if (!visible && now - lastDropTime > dropInterval) {
            visible = true;
            x = random.nextInt(Math.max(1, screenWidth - bitmap.getWidth()));
            y = -bitmap.getHeight(); // start just above screen
            lastDropTime = now;
        }

        // fall (time-based)
        if (visible) {
            y += speedPxPerSec * dt;

            if (y > screenHeight) {
                visible = false;       // off-screen -> hide
                // NOTE: lastDropTime ko reset nahi kar rahe; interval naturally chalega
            }
        }
    }

    // AABB collision (unchanged)
    public boolean checkCollision(float rx, float ry, float rw, float rh) {
        return visible &&
                x + bitmap.getWidth()  >= rx &&
                x                      <= rx + rw &&
                y + bitmap.getHeight() >= ry &&
                y                      <= ry + rh;
    }

    public void hide() {
        visible = false;
        lastDropTime = System.currentTimeMillis();
    }

    public boolean isVisible() { return visible; }
    public Bitmap getBitmap()  { return bitmap;  }
    public float  getX()       { return x;       }
    public float  getY()       { return y;       }
}
