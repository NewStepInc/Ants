package com.ants.engine.base;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Picture;
import android.graphics.Rect;

import com.ants.Utils;
import com.ants.engine.AntsEngine;

public class Ant {

    private AntsEngine mEngine = null;

    public int angle;
    public int x, y;
    public int speed;

    public Ant(AntsEngine antsEngine) {
        mEngine = antsEngine;
        speed = (int) (AntsEngine.LEVEL_SPEED[mEngine.mLevel - 1] * mEngine.stepDistance);
    }

    public void setParams(int angle, int x, int y) {
        this.angle = angle;
        this.x = x;
        this.y = y;
    }

    public void draw(Canvas canvas, int n, Paint paint) {
        long curTime = System.currentTimeMillis();
        int index = (curTime % 200 < 100) ? 0 : 1;
        int angle = this.angle + 90;
        if (angle % 90 != 0) {
            Picture picture = mEngine.mPicAnts[n * 2 + index];
            Utils.drawPicture(canvas, picture, x, y, angle, (float) mEngine.sizeAnt / picture.getWidth());
        } else {
            Utils.drawBitmap(canvas, mEngine.mBmpAnts[n * 2 + index], x, y, angle, paint);
        }
    }

    public Rect getBoundRect() {
        int halfSize = mEngine.sizeAnt / 2;
        Rect rect;

        if (angle % 180 == 0)
            rect = new Rect(x - halfSize / 2, y - halfSize, x + halfSize / 2, y + halfSize);
        else
            rect = new Rect(x - halfSize, y - halfSize / 2, x + halfSize, y + halfSize / 2);

        return rect;
    }

    public boolean isConflict(Rect rect) {
        return getBoundRect().intersect(rect);
    }
}
