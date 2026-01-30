package com.skyrist.pawcontrol;

import static com.skyrist.pawcontrol.GameView.cropTransparentPadding;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class Explosion {
    private static Bitmap[] staticExplosions = new Bitmap[4];

    public int explosionFrame = 0;
    public int explosionX, explosionY;

    // Size scale: ~12% screen width, aspect adjust
    private static final float WIDTH_PERCENT = 0.12f;
    public static int explosionWidth, explosionHeight;

    // --- Timing ---
    private float frameTime = 0f;       // time accumulator
    private static final float FRAME_DURATION = 0.10f; // each frame = 0.1s

    public Explosion(Context context, int x, int y) {
        if (staticExplosions[0] == null) {
            Bitmap bmp0 = cropTransparentPadding(BitmapFactory.decodeResource(context.getResources(), R.drawable.explode0));
            Bitmap bmp1 = cropTransparentPadding(BitmapFactory.decodeResource(context.getResources(), R.drawable.explode1));
            Bitmap bmp2 = cropTransparentPadding(BitmapFactory.decodeResource(context.getResources(), R.drawable.explode2));
            Bitmap bmp3 = cropTransparentPadding(BitmapFactory.decodeResource(context.getResources(), R.drawable.explode3));

            // ---- SIZE: scale by % of screen width ----
            int targetW = Math.max(1, (int) (GameView.dWidth * WIDTH_PERCENT));
            float ar = bmp0.getHeight() / (float) bmp0.getWidth();
            int targetH = Math.max(1, Math.round(targetW * ar));

            explosionWidth  = targetW;
            explosionHeight = targetH;

            staticExplosions[0] = Bitmap.createScaledBitmap(bmp0, targetW, targetH, true);
            staticExplosions[1] = Bitmap.createScaledBitmap(bmp1, targetW, targetH, true);
            staticExplosions[2] = Bitmap.createScaledBitmap(bmp2, targetW, targetH, true);
            staticExplosions[3] = Bitmap.createScaledBitmap(bmp3, targetW, targetH, true);
        }

        explosionX = x;
        explosionY = y;
    }

    // time-based frame advance
    public void update(float dt) {
        frameTime += dt;
        if (frameTime >= FRAME_DURATION) {
            frameTime -= FRAME_DURATION;
            explosionFrame++;
        }
    }

    public Bitmap getExplosion(int frame) {
        return staticExplosions[frame % staticExplosions.length];
    }

    public boolean isFinished() {
        return explosionFrame >= staticExplosions.length;
    }
}
