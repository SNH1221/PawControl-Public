package com.skyrist.pawcontrol;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.util.Random;

public class Spike {

    // ---- sizing & speed (Option‑2) ----
    // spike width = 12% of screen width (tune kar sakte ho)
    private static final float WIDTH_PERCENT   = 0.18f;
    // base speed = 35% of screen height per second
    private static final float SPEED_H_PER_S   = 0.35f;

    private final Bitmap[] frames = new Bitmap[3];
    public  int    spikeFrame = 0;

    public  float  spikeX, spikeY;          // float for smooth motion
    private float  baseSpeedPxPerSec;       // pixels per second

    private int    spikeW, spikeH;
    private final Random rnd = new Random();

    public Spike(Context context) {
        // load original frames
        Bitmap f0 = BitmapFactory.decodeResource(context.getResources(), R.drawable.spike0);
        Bitmap f1 = BitmapFactory.decodeResource(context.getResources(), R.drawable.spike1);
        Bitmap f2 = BitmapFactory.decodeResource(context.getResources(), R.drawable.spike2);

        // ---- SIZE: scale by % of screen width ----
        int targetW = Math.max(1, (int) (GameView.dWidth * WIDTH_PERCENT));
        float ar = f0.getHeight() / (float) f0.getWidth();
        int targetH = Math.max(1, Math.round(targetW * ar));

        frames[0] = Bitmap.createScaledBitmap(f0, targetW, targetH, true);
        frames[1] = Bitmap.createScaledBitmap(f1, targetW, targetH, true);
        frames[2] = Bitmap.createScaledBitmap(f2, targetW, targetH, true);

        spikeW = targetW;
        spikeH = targetH;

        // ---- SPEED: pixels/second = % of screen height (with slight random variation) ----
        float vary = 0.9f + rnd.nextFloat() * 0.2f; // 0.9x..1.1x variety
        baseSpeedPxPerSec = SPEED_H_PER_S * GameView.dHeight * vary;

        resetPosition();
    }

    // time‑based movement (GameView se dt & multiplier aayega)
    public void update(float dtSeconds, float speedMultiplier) {
        spikeY += baseSpeedPxPerSec * speedMultiplier * dtSeconds;
    }

    public Bitmap getSpike(int frame) { return frames[frame % frames.length]; }
    public int getSpikeWidth()  { return spikeW; }
    public int getSpikeHeight() { return spikeH; }

    public void resetPosition() {
        int safe = Math.max(1, GameView.dWidth - spikeW);
        spikeX = rnd.nextInt(safe);
        // start just above screen with a little random offset
        spikeY = -spikeH - rnd.nextInt(spikeH * 3);
    }
}
