package com.skyrist.pawcontrol;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.util.Random;

public class AdvanceBone {
    private static final float WIDTH_PERCENT = 0.12f;   // 10% screen width
    private static final float SPEED_H_PER_S = 0.30f;   // 30% screen height per sec

    private Bitmap bitmap;
    private float x, y;
    private boolean visible = false;
    private float speedPxPerSec;

    private long lastDropTime = 0;
    private long dropInterval = 10000; // every 15 sec (adjust as needed)

    private int screenWidth, screenHeight;
    private Random random = new Random();

    public AdvanceBone(Context context) {
        Bitmap src = GameView.cropTransparentPadding(
                BitmapFactory.decodeResource(context.getResources(), R.drawable.advance_bone)
        );

        screenWidth = GameView.dWidth;
        screenHeight = GameView.dHeight;

        int targetW = Math.max(1, Math.round(screenWidth * WIDTH_PERCENT));
        float ar = src.getHeight() / (float) src.getWidth();
        int targetH = Math.max(1, Math.round(targetW * ar));
        bitmap = Bitmap.createScaledBitmap(src, targetW, targetH, true);

        speedPxPerSec = SPEED_H_PER_S * screenHeight;
    }

    public void update(float dt, int score) {
        long now = System.currentTimeMillis();

        // Score ke hisaab se drop interval change
        if (score < 500) {
            dropInterval = 20000;
        } else if (score < 1500) {
            dropInterval = 25000;
        } else if (score < 3000) {
            dropInterval = 30000;
        } else {
            dropInterval = 10000;
        }

        if (!visible && now - lastDropTime > dropInterval) {
            visible = true;
            x = random.nextInt(Math.max(1, screenWidth - bitmap.getWidth()));
            y = -bitmap.getHeight();
            lastDropTime = now;
            android.util.Log.d("AdvanceBone", "Spawned advance bome at x = "+x+"y="+y);
        }

        if (visible) {
            y += speedPxPerSec * dt;
            if (y > screenHeight) {
                visible = false;
            }
        }
    }

    public boolean checkCollision(float rx, float ry, float rw, float rh) {
        return visible &&
                x + bitmap.getWidth() >= rx &&
                x <= rx + rw &&
                y + bitmap.getHeight() >= ry &&
                y <= ry + rh;
    }

    public void hide() {
        visible = false;
        lastDropTime = System.currentTimeMillis();
    }

    public boolean isVisible() { return visible; }
    public Bitmap getBitmap()  { return bitmap; }
    public float getX()        { return x; }
    public float getY()        { return y; }
}

