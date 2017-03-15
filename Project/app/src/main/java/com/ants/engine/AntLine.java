package com.ants.engine;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;

import com.ants.engine.base.Ant;
import com.ants.engine.base.AntTurn;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class AntLine {
    public static final int CONFLICT_NONE = -1;
    public static final int CONFLICT_FOOD = 0;
    public static final int CONFLICT_ITSELF = 1;
    public static final int CONFLICT_ANTEATER = 2;

    private AntsEngine mEngine;

    public List<Ant> listAnts = new ArrayList<>();
    public List<AntTurn> listTurns = new ArrayList<>();

    private static Paint paint = new Paint();
    private static Random random = new Random(System.currentTimeMillis());

    public AntLine(AntsEngine antsEngine) {
        mEngine = antsEngine;

        int startX, startY;
        int angle;
        if (mEngine.mFood.x < mEngine.mSurface.width / 2) {
            startX = random.nextInt(mEngine.mSurface.width / 5) + mEngine.mSurface.width * 3 / 4;
            if (mEngine.mFood.y < mEngine.mSurface.height / 2) {
                angle = 0;
                startY = mEngine.mSurface.height;
            } else {
                angle = 180;
                startY = 0;
            }
        } else {
            startX = mEngine.mSurface.width - (random.nextInt(mEngine.mSurface.width / 5) + mEngine.mSurface.width * 3 / 4);
            if (mEngine.mFood.y < mEngine.mSurface.height / 2) {
                angle = 0;
                startY = mEngine.mSurface.height;
            } else {
                angle = 180;
                startY = 0;
            }
        }

        for (int i = 0; i < AntsEngine.LEVEL_ANT_COUNT[mEngine.mLevel - 1]; i++) {
            Ant ant = new Ant(mEngine);
            ant.setParams(angle, startX, startY - mEngine.sizeAnt * i * (angle - 90) / 90);
            listAnts.add(ant);
        }

    }

    public void changeDirection(int x, int y) {
        Ant antFirst = listAnts.get(0);
        int offsetX = antFirst.x - x;
        int offsetY = antFirst.y - y;
//        int dist = (int) Math.sqrt(offsetX * offsetX + offsetY * offsetY);
//        if (dist > mEngine.sizeAnt * 3)
//            return;

        int antHalfLength = mEngine.sizeAnt / 2;
        if (Math.abs(offsetX) > Math.abs(offsetY)) {
            if (antFirst.angle % 180 == 90)
                return;

            if ((offsetX < 0 && antFirst.x >= mEngine.mSurface.width - antHalfLength) ||
                (offsetX >= 0 && antFirst.x <= antHalfLength))
                return;
            addTurn(antFirst.angle, (offsetX < 0) ? 90 : 270, antFirst.x, antFirst.y);
        } else {
            if (antFirst.angle % 180 == 0)
                return;

            if ((offsetY < 0 && antFirst.y >= mEngine.mSurface.height - antHalfLength) ||
                (offsetY >= 0 && antFirst.y <= antHalfLength))
                return;
            addTurn(antFirst.angle, (offsetY < 0) ? 180 : 0, antFirst.x, antFirst.y);
        }
    }

    private void addTurn(int angleFrom, int angleTo, int x, int y) {
        AntTurn antTurn = new AntTurn();
        antTurn.x = x;
        antTurn.y = y;
        antTurn.angleFrom = angleFrom;
        antTurn.angleTo = angleTo;
        antTurn.lastIndex = -1;
        listTurns.add(0, antTurn);
    }

    public void draw(Canvas canvas) {
        paint.setAlpha(255);

        long curTime = System.currentTimeMillis();
        if (mEngine.mState == AntsEngine.STATE_CONFLICT && (mEngine.mLevel < AntsEngine.LEVEL_LIMIT || mEngine.mSubState != CONFLICT_FOOD)) {
            if (curTime - mEngine.mStateStartTime >= 1000) {
                paint.setAlpha(0);
            } else {
                paint.setAlpha(255 - (int) ((float) (curTime - mEngine.mStateStartTime) * 255 / 1000));
            }
        }

        for (int i = 0; i < listAnts.size(); i++) {
            listAnts.get(i).draw(canvas, i % AntsEngine.ANT_LIMIT, paint);
        }
    }

    public int updateFrame() {
        if (mEngine.mState == AntsEngine.STATE_PLAY) {
            for (int i = 0; i < listAnts.size(); i++) {
                Ant ant = listAnts.get(i);

                for (int j = 0; j < listTurns.size(); j++) {
                    AntTurn antTurn = listTurns.get(j);
                    if (antTurn.lastIndex != i-1 || antTurn.angleFrom != ant.angle)
                        continue;

                    int offsetX = ant.x - antTurn.x;
                    int offsetY = ant.y - antTurn.y;
                    if (Math.abs(offsetX) >= ant.speed || Math.abs(offsetY) >= ant.speed)
                        continue;

                    if ((antTurn.angleFrom == 0 && offsetY <= 0) ||
                        (antTurn.angleFrom == 90 && offsetX <= 0) ||
                        (antTurn.angleFrom == 180 && offsetY >= 0) ||
                        (antTurn.angleFrom == 270 && offsetX >= 0)) {

                        ant.setParams(antTurn.angleTo, antTurn.x, antTurn.y);
                        if (i < listAnts.size() - 1)
                            antTurn.lastIndex = i;
                        else {
                            listTurns.remove(j);
                            j--;
                        }

                    }

                }

                ant.x += Math.sin(Math.toRadians(ant.angle)) * ant.speed;
                ant.y -= Math.cos(Math.toRadians(ant.angle)) * ant.speed;

            }
        } else if (mEngine.mState == AntsEngine.STATE_CONFLICT && mEngine.mSubState == CONFLICT_FOOD) {
            //scattering
            for (int i = 0; i < listAnts.size(); i++) {
                Ant ant = listAnts.get(i);
                ant.x += Math.sin(Math.toRadians(ant.angle)) * ant.speed * 1.5;
                ant.y -= Math.cos(Math.toRadians(ant.angle)) * ant.speed * 1.5;
            }
        } else if (mEngine.mState == AntsEngine.STATE_END) {
            // 1s waiting, 5s rounding
            long timeDiff = System.currentTimeMillis() - mEngine.mStateStartTime - waitPeriod;
            if (timeDiff - heartPeriod * 2 - waitPeriod * 5 > 0) {
                mEngine.mSurface.neInterface.exit();
                return CONFLICT_NONE;
            }

            for (int i = 0; i < listAnts.size(); i++) {
                long diff = timeDiff - heartPeriod * i / listAnts.size();

                Ant ant = listAnts.get(i);
                ant.x = getHeartX(diff);
                ant.y = getHeartY(diff);

                int nextX = getHeartX(diff + 100);
                int nextY = getHeartY(diff + 100);
                ant.angle = (int) Math.toDegrees(Math.atan2(nextY - ant.y, nextX - ant.x) + Math.PI / 2);
            }
        }


        if (mEngine.mState == AntsEngine.STATE_PLAY)
            return check();

        return CONFLICT_NONE;
    }

    private int check() {
        Ant antFirst = listAnts.get(0);


        // check conflict with sides
        int antHalfLength = mEngine.sizeAnt / 2;
        if ((antFirst.angle == 0 && antFirst.y - antHalfLength < 0) || (antFirst.angle == 180 && antFirst.y + antHalfLength > mEngine.mSurface.height)) {

            int angleTo;
            if (antFirst.x <= antHalfLength)
                angleTo = 90;
            else if (antFirst.x >= mEngine.mSurface.width - antHalfLength)
                angleTo = 270;
            else
                angleTo = random.nextBoolean() ? 90 : 270;
            addTurn(antFirst.angle, angleTo, antFirst.x, antFirst.y);

        } else if ((antFirst.angle == 90 && antFirst.x + antHalfLength > mEngine.mSurface.width) || (antFirst.angle == 270 && antFirst.x - antHalfLength < 0)) {

            int angleTo;
            if (antFirst.y <= antHalfLength)
                angleTo = 180;
            else if (antFirst.y >= mEngine.mSurface.height - antHalfLength)
                angleTo = 0;
            else
                angleTo = random.nextBoolean() ? 0 : 180;
            addTurn(antFirst.angle, angleTo, antFirst.x, antFirst.y);

        }

        // check conflict with itself
        for (int i = 2; i < listAnts.size(); i++) {
            if (antFirst.isConflict(listAnts.get(i).getBoundRect()))
                return CONFLICT_ITSELF;
        }

        // check conflict with anteater
        for (int i = 0; i < listAnts.size(); i++) {
            for (int j = 0; j < mEngine.mAntEaters.listAntEaters.size(); j++)
            if (listAnts.get(i).isConflict(mEngine.mAntEaters.listAntEaters.get(j).getBoundRect()))
                return CONFLICT_ANTEATER;
        }

        // check conflict with food
        if (antFirst.isConflict(mEngine.mFood.getBoundRect())) {
            for (int i = 0; i < listAnts.size(); i++) {
                Ant ant = listAnts.get(i);
                ant.angle = random.nextInt(360);
            }
            return CONFLICT_FOOD;
        }

        return CONFLICT_NONE;
    }

    public boolean isOnScreen() {
        Rect rect = new Rect(0, 0, mEngine.mSurface.width, mEngine.mSurface.height);
        for (int i = 0; i < listAnts.size(); i++) {
            Ant ant = listAnts.get(i);
            if (ant.isConflict(rect))
                return true;
        }

        return false;
    }

    private static long heartPeriod = 8 * 1000;
    private static long waitPeriod = 300;
    private int getHeartX(long diff) {
        int x;
        if (diff < 0) {
            x = mEngine.mSurface.width / 2;
        } else if (diff <= heartPeriod) {
            double radian = -Math.PI - (Math.PI * 2) * diff / heartPeriod;
            x = mEngine.mSurface.width / 2 - (int) (16 * Math.pow(Math.sin(radian), 3) * mEngine.sizeAnt / 4);
        } else {
            x = mEngine.mSurface.width / 2;
        }
        return x;
    }

    private int getHeartY(long diff) {
        int y;
        if (diff < 0) {
            y = (int) (getHeartY(0) - (mEngine.mSurface.height - getHeartY(0)) * diff / waitPeriod);
        } else if (diff <= heartPeriod) {
            double radian = -Math.PI - (Math.PI * 2) * diff / heartPeriod;
            y = mEngine.mSurface.height / 2 - (int) ((13 * Math.cos(radian) - 5 * Math.cos(2 * radian) - 2 * Math.cos(3 * radian) - Math.cos(4 * radian)) * mEngine.sizeAnt / 4);
        } else {
            y = (int) (getHeartY(0) - (mEngine.mSurface.height - getHeartY(0)) * (heartPeriod - diff) / waitPeriod);
        }
        return y;
    }
}
