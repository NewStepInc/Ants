package com.ants.engine;

import android.graphics.Canvas;
import android.graphics.Picture;
import android.graphics.Rect;

import com.ants.Utils;

import java.util.Random;

public class Food {
    private AntsEngine mEngine;
    public int x, y;
    public float scale;
    public Picture picture;
    public int size;

    public Food(AntsEngine antsEngine) {
        mEngine = antsEngine;
        this.size = (int) (mEngine.sizeFood * mEngine.LEVEL_FOOD_SIZE_PARTIAL[mEngine.mLevel - 1]);

        Random random = new Random(System.currentTimeMillis());
        x = size + random.nextInt(mEngine.mSurface.width - size * 2);
        y = size + random.nextInt(mEngine.mSurface.height - size * 2);

        picture = mEngine.mPicFoods[mEngine.mLevel - 1];
        scale = (float) size / picture.getWidth();
    }

    public void draw(Canvas canvas) {
        if (mEngine.mState == mEngine.STATE_END)
            return;

        long curTime = System.currentTimeMillis();

        int angle = 0;
        if (mEngine.mState == mEngine.STATE_CONFLICT) {
            if (1000 <= curTime - mEngine.mStateStartTime && curTime - mEngine.mStateStartTime < 1500) {
                angle = (int) ((float) (curTime - mEngine.mStateStartTime - 1000) * 360 / 500);
            }
        }

        Utils.drawPicture(canvas, picture, x, y, angle, scale);
    }

    public Rect getBoundRect() {
        int halfSize = size / 2;
        Rect rect = new Rect(x - halfSize, y - halfSize, x + halfSize, y + halfSize);
        return rect;
    }
}
