package com.swift.tap;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Build;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import java.util.concurrent.atomic.AtomicInteger;

public class TapView extends SurfaceView implements SurfaceHolder.Callback, Runnable {
    private volatile long count = 0;
    private volatile int cps = 0, best = 0;
    private final AtomicInteger tmp = new AtomicInteger(0);
    private volatile boolean running = false;
    private Thread thread;
    private final SurfaceHolder holder;
    private long winStart = 0;
    private long lastCount = -1;
    private int lastCps = -1;
    private final Paint circle = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint big = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint small = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint reset = new Paint(Paint.ANTI_ALIAS_FLAG);

    public TapView(Context c) {
        super(c);
        holder = getHolder();
        holder.addCallback(this);
        circle.setColor(0xFF00E5FF);
        big.setColor(0xFFFFFFFF);
        big.setTextAlign(Paint.Align.CENTER);
        small.setColor(0xFFFF4FA3);
        small.setTextAlign(Paint.Align.CENTER);
        reset.setColor(0xFF888899);
        reset.setTextAlign(Paint.Align.CENTER);
    }

    @Override
    public void surfaceCreated(SurfaceHolder h) {
        lastCount = -1;
        running = true;
        winStart = System.currentTimeMillis();
        thread = new Thread(this);
        thread.start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder h, int f, int w, int hh) {
        lastCount = -1;
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder h) {
        running = false;
        try {
            if (thread != null) thread.join();
        } catch (InterruptedException ignored) {
        }
    }

    @Override
    public void run() {
        while (running) {
            long now = System.currentTimeMillis();
            if (now - winStart >= 1000) {
                int t = tmp.getAndSet(0);
                cps = t;
                if (t > best) best = t;
                winStart = now;
            }
            long cNow = count;
            int pNow = cps;
            if (cNow != lastCount || pNow != lastCps) {
                Canvas c = null;
                boolean ok = false;
                try {
                    c = Build.VERSION.SDK_INT >= 26 ? holder.lockHardwareCanvas() : holder.lockCanvas();
                    if (c != null) {
                        drawFrame(c, cNow, pNow);
                        ok = true;
                    }
                } catch (Exception ignored) {
                } finally {
                    if (c != null) {
                        try {
                            holder.unlockCanvasAndPost(c);
                        } catch (Exception ignored) {
                            ok = false;
                        }
                    }
                }
                if (ok) {
                    lastCount = cNow;
                    lastCps = pNow;
                }
            }
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    private void drawFrame(Canvas c, long n, int p) {
        int w = c.getWidth(), h = c.getHeight();
        c.drawColor(0xFF0B0B14);
        c.drawCircle(w / 2f, h * 0.55f, w * 0.38f, circle);
        big.setTextSize(w * 0.16f);
        c.drawText(String.valueOf(n), w / 2f, h * 0.17f, big);
        small.setTextSize(w * 0.06f);
        c.drawText("Taps/sec: " + p + "   Best: " + best, w / 2f, h * 0.24f, small);
        reset.setTextSize(w * 0.05f);
        c.drawText("RESET", w / 2f, h * 0.95f, reset);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        int a = e.getActionMasked();
        if (a == MotionEvent.ACTION_DOWN) {
            requestUnbufferedDispatch(e);
        }
        if (a == MotionEvent.ACTION_DOWN || a == MotionEvent.ACTION_POINTER_DOWN) {
            float y = e.getY(e.getActionIndex());
            if (y > getHeight() * 0.88f) {
                count = 0;
                cps = 0;
                best = 0;
                tmp.set(0);
            } else {
                count = count + 1;
                tmp.incrementAndGet();
            }
        }
        return true;
    }
}
