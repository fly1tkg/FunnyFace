package nagoya.gdg.funnyface;

import android.app.AlertDialog;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;

import java.io.File;

/**
 * Created by tkg on 2014/10/25.
 */
public class FunnyFace {
    private static final String U10MESSAGE = "";
    private static final String U20MESSAGE = "";
    private static final String U30MESSAGE = "";
    private static final String U40MESSAGE = "";

    private Context mCtx;
    private int mScore;

    public FunnyFace(Context ctx, File normalFace, File funnyFace) {
        mCtx = ctx;
        Bitmap faceBlack = BitmapFactory.decodeResource(mCtx.getResources(), R.drawable.face_black);

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 2;

        Bitmap normalOriginal = BitmapFactory.decodeFile(normalFace.getAbsolutePath(), options);
        Bitmap normal = Bitmap.createScaledBitmap(normalOriginal, faceBlack.getWidth(), faceBlack.getHeight(), false);
        normalOriginal.recycle();
        Canvas c = new Canvas(normal);
        c.drawBitmap(faceBlack, 0, 0, null);

        Bitmap funnyOriginal = BitmapFactory.decodeFile(funnyFace.getAbsolutePath(), options);
        Bitmap funny = Bitmap.createScaledBitmap(funnyOriginal, faceBlack.getWidth(), faceBlack.getHeight(), false);
        funnyOriginal.recycle();
        Canvas c1 = new Canvas(funny);
        c1.drawBitmap(faceBlack, 0, 0, null);

        mScore = diffImages(normal, funny);
    }

    public AlertDialog getMessageDialog() {
        return new AlertDialog.Builder(mCtx)
                .setMessage(String.format("あなたの変顔度は%dです!", mScore / 100000))
                .setPositiveButton("OK", null)
                .create();
    }

    private int convertGray(int dotColor) {
        float r = (float) Color.red(dotColor);
        float g = (float) Color.green(dotColor);
        float b = (float) Color.blue(dotColor);

        return (int) (r * 0.3 + g * 0.59 + b * 0.11);
    }

    private int diffImages(Bitmap image1, Bitmap image2) {

        if (image1.getWidth() != image2.getWidth() || image1.getHeight() != image2.getHeight()) {
            return -1;
        }

        int w = image1.getWidth();
        int h = image1.getHeight();

        int diff = 0;

        for (int i = 0; i < w; i++) {
            for (int j = 0; j < h; j++) {
                int dotColor1 = image1.getPixel(i, j);
                int dotColor2 = image2.getPixel(i, j);

                int grayColor1 = convertGray(dotColor1);
                int grayColor2 = convertGray(dotColor2);

                diff += Math.abs(grayColor1 - grayColor2);
            }
        }

        return diff;
    }
}
