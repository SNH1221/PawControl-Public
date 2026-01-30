package com.skyrist.pawcontrol;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.*;
import android.os.Build;
import android.os.Handler;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.graphics.Paint.Align;

import androidx.core.content.res.ResourcesCompat;

import java.util.ArrayList;

public class GameView extends View {

    // ---------- Virtual / Percentage base (UI only) ----------
    private static final float VIRTUAL_W = 1080f;
    private static final float VIRTUAL_H = 1920f;

    private float sx, sy; // screen-to-virtual scale
    private int vx(float v) { return Math.round(v * sx); }   // virtual X -> px
    private int vy(float v) { return Math.round(v * sy); }   // virtual Y -> px

    Typeface kenneyFont;
    Vibrator vibrator;
    Bitmap background, ground, rabbit, pauseIcon, resumeIcon;
    Rect rectBackground, rectGround, pauseBtnRect;
    Context context;


    Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    Paint healthPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    Paint pauseTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    float TEXT_SIZE_V = 120f;

    int points = 0;
    int life = 3;
    int maxHealth = 3;

    public static int dWidth, dHeight;
    float rabbitX, rabbitY;
    float oldX, oldRabbitX;
    boolean isPaused = false;
    boolean showPauseMenu = false;

    ArrayList<Spike> spikes;
    ArrayList<Explosion> explosions;
    Bone bone;

    float healthBarExtra = 0; // px (scaled)
    float bounceOffset = 0;   // px
    float bounceVelocity = 0; // px/frame
    boolean isBouncing = false;

    // Pause-menu button rects reused for drawing + touch
    private Rect backMenuBtnRect = null;
    private Rect exitBtnRect = null;

    // time-based movement ke liye
    long lastFrameNs = 0L;

    AdvanceBone advanceBone;
    boolean isInvincible = false;
    long invincibleEndTime = 0;



    public GameView(Context context) {
        super(context);
        this.context = context;




        kenneyFont = ResourcesCompat.getFont(context, R.font.kenney_blocks);
        vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);

        Display display = ((Activity) context).getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        dWidth = size.x;
        dHeight = size.y;

        advanceBone = new AdvanceBone(context);

        sx = dWidth / VIRTUAL_W;
        sy = dHeight / VIRTUAL_H;

        // ---- Bitmaps (sizes via percentages/virtual) ----
        background = cropTransparentPadding(BitmapFactory.decodeResource(getResources(), R.drawable.background));
        background = Bitmap.createScaledBitmap(background, dWidth, dHeight, false);

        int groundH = Math.round(dHeight * 0.25f); // 25% height
        ground = cropTransparentPadding(BitmapFactory.decodeResource(getResources(), R.drawable.ground));
        ground = Bitmap.createScaledBitmap(ground, dWidth / 5, groundH, false);

        int rabbitW = Math.round(dWidth * 0.20f);     // ~20% screen width
        int rabbitH = Math.round(dHeight * 0.0833f);  // ~8.33% screen height
        rabbit = cropTransparentPadding(BitmapFactory.decodeResource(getResources(), R.drawable.rabbit));
        rabbit = Bitmap.createScaledBitmap(rabbit, rabbitW, rabbitH, false);

        int pauseSize = Math.round(dWidth / 12f);
        pauseIcon = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.pause), pauseSize, pauseSize, false);
        resumeIcon = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.resume), pauseSize, pauseSize, false);

        rectBackground = new Rect(0, 0, dWidth, dHeight);
        rectGround = new Rect(0, dHeight - groundH, dWidth, dHeight);

        int margin = vx(20);
        pauseBtnRect = new Rect(dWidth - pauseSize - margin, margin, dWidth - margin, margin + pauseSize);



        textPaint.setColor(Color.rgb(255, 165, 0));
        textPaint.setTextSize(vy(TEXT_SIZE_V));
        textPaint.setTextAlign(Align.LEFT);
        textPaint.setTypeface(kenneyFont);

        pauseTextPaint.setColor(Color.WHITE);
        pauseTextPaint.setTextSize(vy(TEXT_SIZE_V));
        pauseTextPaint.setTextAlign(Align.CENTER);
        pauseTextPaint.setTypeface(kenneyFont);

        healthPaint.setColor(Color.GREEN);

        rabbitX = dWidth / 2f - rabbit.getWidth() / 2f;
        rabbitY = rectGround.top - rabbit.getHeight();

        spikes = new ArrayList<>();
        explosions = new ArrayList<>();
        bone = new Bone(context);

        for (int i = 0; i < 3; i++) {
            spikes.add(new Spike(context));
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.drawBitmap(background, null, rectBackground, null);
        canvas.drawBitmap(ground, null, rectGround, null);



        // ---- Delta time (seconds) ----
        long nowNs = System.nanoTime();
        if (lastFrameNs == 0L) lastFrameNs = nowNs;
        float dt = (nowNs - lastFrameNs) / 1_000_000_000f; // seconds

// Lag spikes se bachao (clamp)
        if (dt < 0f)   dt = 0f;
        if (dt > 0.033f) dt = 0.033f; // max ~30 FPS step
        lastFrameNs = nowNs;

        // Invincibility timer
        if (isInvincible && System.currentTimeMillis() > invincibleEndTime) {
            isInvincible = false;
        }

        advanceBone.update(dt, points);


        if (advanceBone.isVisible()) {
            canvas.drawBitmap(advanceBone.getBitmap(), advanceBone.getX(), advanceBone.getY(), null);
            if (advanceBone.checkCollision(rabbitX, rabbitY, rabbit.getWidth(), rabbit.getHeight())) {
                advanceBone.hide();
                isInvincible = true;
                invincibleEndTime = System.currentTimeMillis() + 3000; // 3 sec invincible
            }
        }

        // Rabbit draw with glow if invincible
        if (isInvincible) {
            Paint glow = new Paint(Paint.ANTI_ALIAS_FLAG);
            glow.setColor(Color.YELLOW);
            glow.setAlpha(120);
            float cx = rabbitX + rabbit.getWidth()/2f;
            float cy = rabbitY + rabbit.getHeight()/2f + bounceOffset;
            float radius = rabbit.getWidth();
            canvas.drawCircle(cx, cy, radius, glow);
        }

        canvas.drawBitmap(rabbit, rabbitX, rabbitY + bounceOffset, null);



        if (!isPaused) {
            // --- Spike Logic (unchanged) ---
            for (Spike spike : spikes) {
                canvas.drawBitmap(spike.getSpike(spike.spikeFrame), spike.spikeX, spike.spikeY, null);
                spike.spikeFrame = (spike.spikeFrame + 1) % 3;
                float speedMultiplier = 1.0f;
                if (points >= 1900) {
                    speedMultiplier = 1.35f;
                } else if (points >= 1500) {
                    speedMultiplier = 1.25f;
                } else if (points >= 1000) {
                    speedMultiplier = 1.05f;
                }
                // time-based update (ye method Spike.java me abhi add karwayunga)
                spike.update(dt, speedMultiplier);


                if (spike.spikeY + spike.getSpikeHeight() >= rectGround.top) {
                    points += 10;
                    explosions.add(new Explosion(
                            context,
                            (int) spike.spikeX,
                            (int) (rectGround.top - Explosion.explosionHeight / 2f)   // cast for current constructor
                    ));
                    if (explosions.size() > 10) explosions.remove(0);
                    spike.resetPosition();
                }

                if (!isInvincible && RectF.intersects(
                        new RectF(spike.spikeX, spike.spikeY, spike.spikeX + spike.getSpikeWidth(), spike.spikeY + spike.getSpikeHeight()),
                        new RectF(rabbitX, rabbitY, rabbitX + rabbit.getWidth(), rabbitY + rabbit.getHeight()))
                ){
                    life--;

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator.vibrate(VibrationEffect.createOneShot(150, VibrationEffect.DEFAULT_AMPLITUDE));
                    } else {
                        vibrator.vibrate(150);
                    }
                    spike.resetPosition();
                    if (life == 0) {
                        context.startActivity(new Intent(context, GameOver.class).putExtra("points", points));
                        ((Activity) context).finish();
                    }
                }
            }

            if (points >= 2000 && spikes.size() == 4) {
                spikes.add(new Spike(context));
            } else if (points >= 400 && spikes.size() == 3) {
                spikes.add(new Spike(context));
            }

            // --- Explosion Logic (unchanged) ---
            for (int i = 0; i < explosions.size(); i++) {
                Explosion ex = explosions.get(i);
                canvas.drawBitmap(ex.getExplosion(ex.explosionFrame), ex.explosionX, ex.explosionY, null);
                ex.update(dt);
                if (ex.isFinished()) {
                    explosions.remove(i);
                    i--;
                }
            }

            // --- Bone Logic (unchanged, but bounce scaled) ---
            bone.update(dt, life);
            if (bone.isVisible()) {
                canvas.drawBitmap(bone.getBitmap(), bone.getX(), bone.getY(), null);
                if (bone.checkCollision(rabbitX, rabbitY, rabbit.getWidth(), rabbit.getHeight())) {
                    bone.hide();
                    bounceVelocity = -1000f; // scaled up-thrust
                    isBouncing = true;

                    if (life == maxHealth && maxHealth < 5) {
                        maxHealth++;
                        life++;
                        healthBarExtra += vx(30f); // animate bar extension (scaled)
                        life = maxHealth;
                    } else if (life < maxHealth) {
                        life++;
                    }
                }
            }

            // --- Bounce Animation (scaled gravity) ---
            // Bounce Animation (time-based)
            if (isBouncing) {
                // gravity per second (virtual units)
                final float GRAVITY = 2000f;

                bounceOffset += bounceVelocity * dt;     // position update
                bounceVelocity += GRAVITY * dt;      // velocity update

                if (bounceOffset >= 0) {
                    bounceOffset = 0;
                    isBouncing = false;
                }
            }


            // Animate extra health bar (scaled decay)
            if (healthBarExtra > 0) {
                healthBarExtra -= vx(2f);
                if (healthBarExtra < 0) healthBarExtra = 0;
            }


            // AdvanceBone logic


        }

        // Health Bar Color
        if (life == 2) healthPaint.setColor(Color.YELLOW);
        else if (life == 1) healthPaint.setColor(Color.RED);
        else healthPaint.setColor(Color.GREEN);

        // Health bar geometry via virtual units
        int hbStartX = dWidth - vx(200f);
        int hbStartY = pauseBtnRect.bottom + vy(20f);
        int hbHeight = vy(50f);
        float perLife = vx(60f);

        canvas.drawRect(
                hbStartX,
                hbStartY,
                hbStartX + perLife * life + healthBarExtra,
                hbStartY + hbHeight,
                healthPaint
        );

        // Score
        canvas.drawText(String.valueOf(points), vx(20f), vy(TEXT_SIZE_V), textPaint);

        // ---------- Pause Menu ----------
        if (showPauseMenu) {
            // Dim full screen
            Paint dimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            dimPaint.setColor(Color.argb(220, 0, 0, 0));
            canvas.drawRect(0, 0, dWidth, dHeight, dimPaint);

            // Title
            canvas.drawText("PAUSED", dWidth / 2f, dHeight / 2f - vy(200f), pauseTextPaint);

            // Dynamic buttons from label size
            String backLabel = "BACK TO MENU";
            String exitLabel = "EXIT GAME";

            Paint btnPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            btnPaint.setColor(Color.LTGRAY);

            Paint btnTxt = new Paint(Paint.ANTI_ALIAS_FLAG);
            btnTxt.setColor(Color.BLACK);
            btnTxt.setTextSize(vy(68f));
            btnTxt.setTextAlign(Paint.Align.CENTER);
            btnTxt.setTypeface(kenneyFont);

            Rect backBounds = new Rect();
            Rect exitBounds = new Rect();
            btnTxt.getTextBounds(backLabel, 0, backLabel.length(), backBounds);
            btnTxt.getTextBounds(exitLabel, 0, exitLabel.length(), exitBounds);

            int padX = vx(40f);
            int padY = vy(28f);

            int btnW1 = backBounds.width() + padX * 2;
            int btnH1 = backBounds.height() + padY * 2;

            int btnW2 = exitBounds.width() + padX * 2;
            int btnH2 = exitBounds.height() + padY * 2;

            int cx = dWidth / 2;
            int y1 = dHeight / 2;              // first button center Y baseline
            int y2 = y1 + vy(200f);            // gap to second button

            backMenuBtnRect = new Rect(cx - btnW1 / 2, y1 - btnH1 / 2, cx + btnW1 / 2, y1 + btnH1 / 2);
            exitBtnRect     = new Rect(cx - btnW2 / 2, y2 - btnH2 / 2, cx + btnW2 / 2, y2 + btnH2 / 2);

            canvas.drawRect(backMenuBtnRect, btnPaint);
            canvas.drawRect(exitBtnRect, btnPaint);

            Paint.FontMetrics fm = btnTxt.getFontMetrics();
            float textOffset = (fm.descent - fm.ascent) / 2f - fm.descent;

            canvas.drawText(backLabel, backMenuBtnRect.centerX(), backMenuBtnRect.centerY() + textOffset, btnTxt);
            canvas.drawText(exitLabel,  exitBtnRect.centerX(),     exitBtnRect.centerY()  + textOffset, btnTxt);
        } else {
            // menu hidden -> clear hit rects to avoid stale taps
            backMenuBtnRect = null;
            exitBtnRect = null;
        }

        if (isPaused) {
            // Highlighted resume icon (white base + glow + border)
            float cx = pauseBtnRect.centerX();
            float cy = pauseBtnRect.centerY();


            // Radii tuned to your screenshot
            float baseR   = pauseBtnRect.width() * 1.25f; // light-grey base
            float glowR   = baseR * 1.55f;                // soft outer halo
            float strokeW = pauseBtnRect.width() * 0.10f; // thick black ring

            // Soft outer halo
            Paint halo = new Paint(Paint.ANTI_ALIAS_FLAG);
            halo.setStyle(Paint.Style.FILL);
            halo.setColor(Color.WHITE);
            halo.setAlpha(70); // soft
            canvas.drawCircle(cx, cy, glowR, halo);

            // Light-grey solid base (not pure white, screenshot-like)
            Paint base = new Paint(Paint.ANTI_ALIAS_FLAG);
            base.setStyle(Paint.Style.FILL);
            base.setColor(Color.rgb(230, 230, 230)); // light grey
            base.setAlpha(255);
            canvas.drawCircle(cx, cy, baseR, base);

            // Bold black border
            Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);
            ring.setStyle(Paint.Style.STROKE);
            ring.setStrokeWidth(strokeW);
            ring.setColor(Color.BLACK);
            canvas.drawCircle(cx, cy, baseR, ring);

            // 3) Resume icon on top (full opacity)
            Paint icon = new Paint(Paint.ANTI_ALIAS_FLAG);
            icon.setAlpha(255);
            canvas.drawBitmap(resumeIcon, null, pauseBtnRect, icon);

        } else {
            // Running state: normal pause icon
            canvas.drawBitmap(pauseIcon, null, pauseBtnRect, null);
        }

        postInvalidateOnAnimation();


    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX(), y = event.getY();

        if (pauseBtnRect.contains((int) x, (int) y) && event.getAction() == MotionEvent.ACTION_DOWN) {
            isPaused = !isPaused;
            showPauseMenu = isPaused;
            return true;
        }

        if (showPauseMenu && event.getAction() == MotionEvent.ACTION_DOWN) {
            // Use the rects computed in onDraw()
            if (backMenuBtnRect != null && backMenuBtnRect.contains((int) x, (int) y)) {
                context.startActivity(new Intent(context, MainActivity.class));
                ((Activity) context).finish();
                return true;
            } else if (exitBtnRect != null && exitBtnRect.contains((int) x, (int) y)) {
                ((Activity) context).finishAffinity();
                return true;
            }
        }

        if (!isPaused && y >= rabbitY) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                oldX = x;
                oldRabbitX = rabbitX;
            } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
                float shift = oldX - x;
                rabbitX = Math.max(0, Math.min(oldRabbitX - shift, dWidth - rabbit.getWidth()));
            }
        }
        return true;
    }

    public static Bitmap cropTransparentPadding(Bitmap bitmap) {
        int imgHeight = bitmap.getHeight(), imgWidth = bitmap.getWidth();
        int minX = imgWidth, minY = imgHeight, maxX = -1, maxY = -1;

        int[] pixels = new int[imgWidth * imgHeight];
        bitmap.getPixels(pixels, 0, imgWidth, 0, 0, imgWidth, imgHeight);

        for (int y = 0; y < imgHeight; y++) {
            for (int x = 0; x < imgWidth; x++) {
                if (((pixels[y * imgWidth + x] >> 24) & 0xff) > 0) {
                    if (x < minX) minX = x;
                    if (x > maxX) maxX = x;
                    if (y < minY) minY = y;
                    if (y > maxY) maxY = y;
                }
            }
        }
        return (maxX < minX || maxY < minY) ? bitmap :
                Bitmap.createBitmap(bitmap, minX, minY, maxX - minX + 1, maxY - minY + 1);
    }
}
