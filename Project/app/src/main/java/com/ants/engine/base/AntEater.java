package com.ants.engine.base;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;

import com.ants.Utils;
import com.ants.engine.AntsEngine;

import java.util.Random;

public class AntEater {
    private AntsEngine mEngine;
    public int x, y;
    private int direction; // -a1: left, a1: right, 0: no movement
    private float speed;

    private long random;
    private static Paint paint = new Paint();


    public AntEater(AntsEngine antsEngine, float movement, int index) {
        mEngine = antsEngine;
        this.speed = mEngine.stepDistance * movement / 3;
        direction = (movement != 0) ? (index % 2) * 2 - 1 : 0;

        Random rand = new Random();
        random = rand.nextLong();

        int antEaterCount = AntsEngine.LEVEL_ANTEATER_COUNT[mEngine.mLevel - 1];
        if (direction == 0) {
            x = (mEngine.mFood.x + mEngine.mAntLine.listAnts.get(0).x) / 2;
            y = (mEngine.mFood.y + mEngine.mAntLine.listAnts.get(0).y) / 2;
        } else {
//            if (direction == a1)
//                x = mEngine.mBmpAntEaters[0].getWidth();
//            else
//                x = mEngine.mSurface.width - mEngine.mBmpAntEaters[0].getWidth();
            x = mEngine.mBmpAntEaters[0].getWidth() + rand.nextInt(mEngine.mSurface.width - mEngine.mBmpAntEaters[0].getWidth() * 2);

            y = mEngine.mBmpAntEaters[0].getHeight() / 2 + (mEngine.mSurface.height - mEngine.mBmpAntEaters[1].getHeight() * 2 - mEngine.mFood.size) * (index + 1) / (antEaterCount + 1);
            if (y + mEngine.mBmpAntEaters[0].getHeight() / 2 >= mEngine.mFood.y - mEngine.mFood.size / 2)
                y += mEngine.mFood.size + mEngine.mBmpAntEaters[0].getHeight();
        }
    }

    public void draw(Canvas canvas) {
        if (mEngine.mState == AntsEngine.STATE_END)
            return;

        long curTime = System.currentTimeMillis();

        paint.setAlpha(255);
        if (mEngine.mState == AntsEngine.STATE_CONFLICT) {
            if (curTime - mEngine.mStateStartTime >= 1000) {
                paint.setAlpha(0);
            } else {
                paint.setAlpha(255 - (int) ((float) (curTime - mEngine.mStateStartTime) * 255 / 1000));
            }
        }

        int bitmapNo = (direction == 0) ? 0 : direction + 1;

        Utils.drawBitmap(canvas,                                // canvas
                        mEngine.mBmpAntEaters[(Math.abs((curTime + random) % 1000) < 500) ? bitmapNo : (bitmapNo + 1)],     // which bitmap?
                        x, y,                                   // position
                        0, paint);                              // angle, paint
    }

    public Rect getBoundRect() {
        int width = mEngine.mBmpAntEaters[0].getWidth();
        int height = mEngine.mBmpAntEaters[0].getHeight();
        return new Rect(x - width / 2, y - height / 2, x + width / 2, y + height / 2);
    }

    public void updateFrame() {
        if (mEngine.mState == AntsEngine.STATE_PLAY || mEngine.mState == AntsEngine.STATE_ENTER) {

            int width = mEngine.mBmpAntEaters[0].getWidth();

            x += direction * speed;
            if (direction == 1) {
                if (x + width / 2 >= mEngine.mSurface.width)
                    direction = -1;
            } else if (direction == -1) {
                if (x - width / 2 <= 0)
                    direction = 1;
            }
        }
    }
}
