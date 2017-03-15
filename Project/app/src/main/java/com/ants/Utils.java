package com.ants;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Picture;
import android.graphics.Point;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import com.larvalabs.svgandroid.SVGParser;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class Utils {
    private static final long NOACTION_PERIOD_GAME = 1000 * 60 * 2; // 2 min
    private static long lastActionTime;

    public static void setLastActionTime() {
        lastActionTime = System.currentTimeMillis();
    }

    public static void checkNoAction(NEInterface neInterface) {
        if (System.currentTimeMillis() - lastActionTime > NOACTION_PERIOD_GAME)
            neInterface.exit();
    }

    public static View.OnTouchListener onTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN)
                v.setAlpha(0.3f);
            else if (event.getAction() == MotionEvent.ACTION_UP)
                v.setAlpha(1f);
            return false;
        }
    };

    public static Picture loadSVGFromAssets(Context context, String filename) {
        try {
            return SVGParser.getSVGFromAsset(context.getAssets(), filename).getPicture();
//            String svgFileData = convertStreamToString(context.getAssets().open(filename));
//            return SVGHelper.useContext(context).open(svgFileData).checkSVGSize().getPicture();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

//    private static String convertStreamToString(InputStream is) throws Exception {
//        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
//        StringBuilder sb = new StringBuilder();
//        String line = null;
//        while ((line = reader.readLine()) != null) {
//            sb.append(line).append("\n");
//        }
//        reader.close();
//        return sb.toString();
//    }

    public static void drawPicture(Canvas canvas, Picture picture, int x, int y, int angle, float scale) {
        Matrix matrix = new Matrix();

        matrix.postTranslate(-picture.getWidth() / 2f, -picture.getHeight() / 2f);
        matrix.postRotate(angle, 0, 0);
        matrix.postScale(scale, scale);
        matrix.postTranslate(x, y);

        canvas.setMatrix(matrix);
        canvas.drawPicture(picture);
        canvas.setMatrix(null);
    }

    public static void drawBitmap(Canvas canvas, Bitmap bmp, int x, int y, int angle, Paint paint) {
        if (paint.getAlpha() == 0)
            return;

        Matrix matrix = new Matrix();
        matrix.postTranslate(-bmp.getWidth() / 2f, -bmp.getHeight() / 2f);
        matrix.postRotate(angle, 0, 0);
        matrix.postTranslate(x, y);

        canvas.drawBitmap(bmp, matrix, paint);
    }

    public static Bitmap createScaledBitmapFromPicture(Picture picture, int width) {
        int height = picture.getHeight() * width / picture.getWidth();
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawPicture(canvas, picture, width / 2, height / 2, 0, (float) width / (float) picture.getWidth());

        return bitmap;
    }

    public static MediaPlayer getMediaPlayer(Context context){

        MediaPlayer mediaplayer = new MediaPlayer();

        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.KITKAT) {
            return mediaplayer;
        }

        try {
            Class<?> cMediaTimeProvider = Class.forName( "android.media.MediaTimeProvider" );
            Class<?> cSubtitleController = Class.forName( "android.media.SubtitleController" );
            Class<?> iSubtitleControllerAnchor = Class.forName( "android.media.SubtitleController$Anchor" );
            Class<?> iSubtitleControllerListener = Class.forName( "android.media.SubtitleController$Listener" );

            Constructor constructor = cSubtitleController.getConstructor(new Class[]{Context.class, cMediaTimeProvider, iSubtitleControllerListener});

            Object subtitleInstance = constructor.newInstance(context, null, null);

            Field f = cSubtitleController.getDeclaredField("mHandler");

            f.setAccessible(true);
            try {
                f.set(subtitleInstance, new Handler());
            }
            catch (IllegalAccessException e) {return mediaplayer;}
            finally {
                f.setAccessible(false);
            }

            Method setsubtitleanchor = mediaplayer.getClass().getMethod("setSubtitleAnchor", cSubtitleController, iSubtitleControllerAnchor);

            setsubtitleanchor.invoke(mediaplayer, subtitleInstance, null);
            //Log.e("", "subtitle is setted :p");
        } catch (Exception e) {}

        return mediaplayer;
    }

    public static Point getDimentionalSize(Context context)
    {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        int realWidth;
        int realHeight;

        if (Build.VERSION.SDK_INT >= 17){
            //new pleasant way to get real metrics
            DisplayMetrics realMetrics = new DisplayMetrics();
            display.getRealMetrics(realMetrics);
            realWidth = realMetrics.widthPixels;
            realHeight = realMetrics.heightPixels;

        } else if (Build.VERSION.SDK_INT >= 14) {
            //reflection for this weird in-between time
            try {
                Method mGetRawH = Display.class.getMethod("getRawHeight");
                Method mGetRawW = Display.class.getMethod("getRawWidth");
                realWidth = (Integer) mGetRawW.invoke(display);
                realHeight = (Integer) mGetRawH.invoke(display);
            } catch (Exception e) {
                //this may not be 100% accurate, but it's all we've got
                realWidth = display.getWidth();
                realHeight = display.getHeight();
                Log.e("Display Info", "Couldn't use reflection to get the real display metrics.");
            }

        } else {
            //This should be close, as lower API devices should not have window navigation bars
            realWidth = display.getWidth();
            realHeight = display.getHeight();
        }

        return new Point(realWidth, realHeight);
    }

//    public static Point getUsableScreenSize(Context context) {
//        Point dimensionSize = getDimentionalSize(context);
//        dimensionSize.y -= getNavigationBarHeight(context);// + getStatusBarHeight(context);
//        return dimensionSize;
//    }
//
//    public static int getNavigationBarHeight(Context context) {
//        Resources resources = context.getResources();
//        int resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android");
//        if (resourceId > 0) {
//            return resources.getDimensionPixelSize(resourceId);
//        }
//        return 0;
//    }

//    public static int getStatusBarHeight(Context context) {
//        Resources resources = context.getResources();
//        int resourceId = resources.getIdentifier("status_bar_height", "dimen", "android");
//        if (resourceId > 0) {
//            return resources.getDimensionPixelSize(resourceId);
//        }
//        return 0;
//    }

//    private boolean isTablet(Context c) {
//        return (c.getResources().getConfiguration().screenLayout
//                & Configuration.SCREENLAYOUT_SIZE_MASK)
//                >= Configuration.SCREENLAYOUT_SIZE_LARGE;
//    }

    public static void makeFullScreen(Activity activity) {
        View decorView = activity.getWindow().getDecorView();
        int uiOptions = 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            uiOptions = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                    | View.SYSTEM_UI_FLAG_FULLSCREEN; // hide status bar
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
                uiOptions |= View.SYSTEM_UI_FLAG_IMMERSIVE;
        }
        decorView.setSystemUiVisibility(uiOptions);
    }
}
