package com.ants.engine;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Picture;
import android.media.MediaPlayer;

import com.ants.AntsSurface;
import com.ants.BuildConfig;
import com.ants.Utils;

import java.io.IOException;

public class AntsEngine {

    private static final String TAG = "AntsEngine";

    private static final boolean DEBUG = BuildConfig.DEBUG;

    public static final int STATE_NONE = -1;
    public static final int STATE_ENTER = 0;
    public static final int STATE_PLAY = 1;
    public static final int STATE_CONFLICT = 2;
    public static final int STATE_END = 3;

//    private static final int[] ANT_COLORS = {0x000000, 0xFF0000, 0x0090CA, 0XF58C1E, 0x37972D,
//                                             0x6B4646, 0x6699FF, 0xFF00A4, 0xD4C735, 0xC85734,
//                                             0x33CC33, 0x6666CC, 0xF58C1E, 0xFf0000, 0x0D90CA,
//                                             0xF58C1E, 0x37972D, 0x6B4646, 0x808080};
    public static final int ANT_LIMIT = 19;

    public static final int LEVEL_LIMIT = 10; // level 1, 3 are ignored. So 2->1. 5->3
    public static final float LEVEL_SPEED[] = {1.3f, 1.3f, 1.3f, 1.3f, 1.6f, 1.6f, 1.9f, 1.9f, 2.2f, 2.2f}; // steps per frame
    public static final int LEVEL_ANT_COUNT[] = {5, 7, 9, 11, 13, 15, 17, 19, 21, 23};
    public static final int LEVEL_ANTEATER_COUNT[] = {0, 1, 0, 2, 2, 3, 3, 4, 4, 5};
//    public static final boolean LEVEL_ANTEATER_MOVEMENT[] = {false, false, true, true, true, true, true, true, true, true};
    public static final float LEVEL_ANTEATER_MOVEMENT[] = {0, 1, 1, 1.2f, 1.3f, 1.2f, 1.2f, 1.2f, 1.2f, 1};
//    public static final int LEVEL_FOOD_SIZE_PARTIAL[] = {1, 2, 4, 4, 4, 4, 2, 2, 4, 4};
    public static final float LEVEL_FOOD_SIZE_PARTIAL[] = {0.5f, 0.4f, 0.4f, 0.4f, 0.4f, 0.4f, 0.5f, 0.5f, 0.5f, 0.5f};

    // Resources
    public Context mContext = null;
    public AntsSurface mSurface = null;
    public Picture[] mPicAnts = new Picture[ANT_LIMIT * 2];
    public Bitmap[] mBmpAnts = new Bitmap[ANT_LIMIT * 2];
    public Picture[] mPicAntEaters = new Picture[2];
    public Bitmap[] mBmpAntEaters = new Bitmap[4];
    public Picture[] mPicFoods = new Picture[LEVEL_LIMIT];

    // Game Preferences
    public int mLevel;
    public int mState = STATE_NONE, mSubState;
    public long mStateStartTime;

    public AntLine     mAntLine;
    public AntEaters   mAntEaters;
    public Food        mFood;

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Implementation
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////


    public AntsEngine(Context context) {
        mContext = context;

        initResources();
    }

    private void initResources() {
        // ant
        for (int i = 0; i < ANT_LIMIT * 2; i++)
            mPicAnts[i] = Utils.loadSVGFromAssets(mContext, "ant/" + (i + 1) + ".svg");

        // and eater
        mPicAntEaters[0] = Utils.loadSVGFromAssets(mContext, "anteater/1.svg");
        mPicAntEaters[1] = Utils.loadSVGFromAssets(mContext, "anteater/2.svg");

        // food
        for (int i = 0; i < LEVEL_LIMIT; i++) {
            mPicFoods[i] = Utils.loadSVGFromAssets(mContext, "food/" + (i + 1) + ".svg");
        }
    }

    public int stepDistance;
    public int sizeAnt;
    public int sizeAntEater;
    public int sizeFood;
    public void initStaticVariables() {
        stepDistance = mSurface.width / 300;
        sizeAnt = mSurface.height / 10;
        sizeAntEater = sizeAnt * 3;
        sizeFood = sizeAnt * 2;


        mBmpAntEaters[0] = Utils.createScaledBitmapFromPicture(mPicAntEaters[0], sizeAntEater);
        mBmpAntEaters[1] = Utils.createScaledBitmapFromPicture(mPicAntEaters[1], sizeAntEater);
        Matrix matrix = new Matrix();
        matrix.preScale(-1, 1);
        mBmpAntEaters[2] = Bitmap.createBitmap(mBmpAntEaters[0], 0, 0, mBmpAntEaters[0].getWidth(), mBmpAntEaters[0].getHeight(), matrix, false);
        mBmpAntEaters[3] = Bitmap.createBitmap(mBmpAntEaters[1], 0, 0, mBmpAntEaters[1].getWidth(), mBmpAntEaters[1].getHeight(), matrix, false);

        for (int i = 0; i < ANT_LIMIT * 2; i++)
            mBmpAnts[i] = Utils.createScaledBitmapFromPicture(mPicAnts[i], sizeAnt);
    }

    public void start() {
        initStaticVariables();
        initLevel(1);
    }

    private void initLevel(int level) {
        // level
        if (level == 1 || level == 3)
            level++;
        mLevel = level;

        // do not change this sequence
        mFood = new Food(this);
        mAntLine = new AntLine(this);
        mAntEaters = new AntEaters(this);


        // state
        setState(STATE_ENTER);
    }

    public void setState(int state) {
        mState = state;
        mSubState = AntLine.CONFLICT_NONE;
        mStateStartTime = System.currentTimeMillis();
    }

    public void updateFrame() {
        if (mState == STATE_END) {
            mAntLine.updateFrame();
            return;
        }

        if (mState == STATE_ENTER) {
            mAntEaters.updateFrame();
            if (System.currentTimeMillis() - mStateStartTime < 1000)    // 1s
                return;

            setState(STATE_PLAY);
        }

        if (mState == STATE_CONFLICT) {
            long curTime = System.currentTimeMillis();
            switch (mSubState) {
                case AntLine.CONFLICT_ITSELF:
                case AntLine.CONFLICT_ANTEATER:
                    if (curTime - mStateStartTime >= 1000)                  // disappear
                        initLevel(mLevel);                      // restart level
                    break;
                case AntLine.CONFLICT_FOOD:
//                    if (mLevel == LEVEL_LIMIT)  // scattering
                    mAntLine.updateFrame();
                    if (!mAntLine.isOnScreen()) {                // disappear/scatter -> rotate
                        if (mLevel == LEVEL_LIMIT)
                            setState(STATE_END);                // game end
                        else
                            initLevel(mLevel + 1);              // next level
                    }
                    break;
            }
        } else {
            mAntEaters.updateFrame();
            int ret = mAntLine.updateFrame();
            if (ret != AntLine.CONFLICT_NONE) {
                setState(STATE_CONFLICT);
                mSubState = ret;

                if (mSubState == AntLine.CONFLICT_ANTEATER)
                    playSound("anteater");
                else if (mSubState == AntLine.CONFLICT_FOOD)
                    playSound("food");
            }
        }
    }

    public void draw(Canvas canvas) {
        mAntLine.draw(canvas);
        mFood.draw(canvas);
        mAntEaters.draw(canvas);
    }

    public void changeDirection(int x, int y) {
        if (mState == STATE_PLAY) {
            mAntLine.changeDirection(x, y);
        }
    }

    MediaPlayer mp = null;
    public void playSound(String filename) {
        if (mp != null) {
            mp.reset();
            mp.release();
        }

        mp = Utils.getMediaPlayer(mContext);
        try {
            int soundID = mContext.getResources().getIdentifier(filename, "raw", mContext.getPackageName());
            AssetFileDescriptor afd = mContext.getResources().openRawResourceFd(soundID);
            if (afd == null) return;
            mp.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            afd.close();
            mp.setLooping(false);
            mp.prepare();
            mp.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
