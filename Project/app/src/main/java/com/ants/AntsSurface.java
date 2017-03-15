package com.ants;


import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Point;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.ants.engine.AntsEngine;

public class AntsSurface extends SurfaceView implements SurfaceHolder.Callback {
    public final class DrawThread extends Thread {
        private boolean mRun = true;
        private boolean mPause = false;

        @Override
        public void run() {
            waitForAntsEngine();

            final SurfaceHolder surfaceHolder = getHolder();
            Canvas canvas = null;

            while (mRun) {
                try {
                    while (mRun && mPause) {
                        Thread.sleep(100);
                    }

                    Utils.checkNoAction(neInterface);

                    canvas = surfaceHolder.lockCanvas();

                    if (canvas == null) {
                        break;
                    }

                    synchronized (surfaceHolder) {
                        // background
                        canvas.drawARGB(255, 255, 255, 255);

                        if (antsEngine.mState != antsEngine.STATE_NONE) {
                            antsEngine.updateFrame();
                            antsEngine.draw(canvas);
                        }
                    }

                    Thread.sleep(10);
                } catch (InterruptedException e) {
                } finally {
                    if (canvas != null) {
                        surfaceHolder.unlockCanvasAndPost(canvas);
                    }
                }
            }
        }
        public void stopDrawing() {
            mRun = false;
        }
    }

    public NEInterface neInterface;
    private AntsEngine antsEngine = null;
    private DrawThread drawThread;
    public int width, height;

    public AntsSurface(Context context, AttributeSet attributes) {
        super(context, attributes);

        getHolder().addCallback(this);
        setFocusable(true);

//        Point screenSize = Utils.getUsableScreenSize(context);
        Point screenSize = Utils.getDimentionalSize(context);
        this.width = screenSize.x;
        this.height = screenSize.y;
    }

    public void setInstances(NEInterface neInterface, AntsEngine antsEngine) {
        this.neInterface = neInterface;
        this.antsEngine = antsEngine;
        this.antsEngine.mSurface = this;
    }

    private void waitForAntsEngine() {
        while (antsEngine == null) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public DrawThread getDrawThread() {
        if (drawThread == null) {
            drawThread = new DrawThread();
        }
        return drawThread;
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
//        this.width = Math.max(width, height);
//        this.height = Math.min(width, height);

        if (this.antsEngine.mState == AntsEngine.STATE_NONE)
            this.antsEngine.start();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        getDrawThread().start();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        getDrawThread().stopDrawing();
        while (true) {
            try {
                getDrawThread().join();
                break;
            } catch (InterruptedException e) {
            }
        }
        drawThread = null;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        Utils.setLastActionTime();

        switch (event.getActionMasked()){
            case MotionEvent.ACTION_DOWN:
//            case MotionEvent.ACTION_POINTER_DOWN:
//            case MotionEvent.ACTION_MOVE:
                antsEngine.changeDirection((int)event.getX(), (int)event.getY());
                break;
        }
        return true;
    }
}
