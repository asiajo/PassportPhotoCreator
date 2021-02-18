package org.joanna.thesis.passportphotocreator.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;

import com.google.android.gms.vision.face.FaceDetector;

import org.opencv.core.Mat;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.joanna.thesis.passportphotocreator.utils.ImageUtils.getBytesFromBitmap;
import static org.joanna.thesis.passportphotocreator.utils.ImageUtils.getFaceMatFromPictureTaken;
import static org.joanna.thesis.passportphotocreator.utils.ImageUtils.safelyRemoveBitmap;

public final class TestUtils {

    private TestUtils() {
    }

    public static Bitmap RotateBitmap(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(
                source, 0, 0, source.getWidth(), source.getHeight(), matrix,
                true);
    }

    public static Bitmap getBitmapFromFile(String fName, Context context)
            throws IOException {
        InputStream stream = context.getResources().getAssets().open(fName);
        return BitmapFactory.decodeStream(new BufferedInputStream(stream));
    }

    public static Mat getFaceMatFromFileIfCorrectSize(
            String fName,
            Context context,
            FaceDetector mDetectorPhotoT) throws IOException {
        
        Bitmap bitmap = getBitmapFromFile(fName, context);
        Bitmap rotated = RotateBitmap(bitmap, 270);
        byte[] bytes = getBytesFromBitmap(rotated);
        safelyRemoveBitmap(rotated);
        return getFaceMatFromPictureTaken(bytes, mDetectorPhotoT);
    }

    public static void encodeYUV420SP(byte[] yuv420sp, int[] argb, int width, int height) {
        final int frameSize = width * height;

        int yIndex = 0;
        int uvIndex = frameSize;

        int a, R, G, B, Y, U, V;
        int index = 0;
        for (int j = 0; j < height; j++) {
            for (int i = 0; i < width; i++) {

                a = (argb[index] & 0xff000000) >> 24; // a is not used obviously
                R = (argb[index] & 0xff0000) >> 16;
                G = (argb[index] & 0xff00) >> 8;
                B = (argb[index] & 0xff) >> 0;

                // well known RGB to YUV algorithm
                Y = ( (  66 * R + 129 * G +  25 * B + 128) >> 8) +  16;
                U = ( ( -38 * R -  74 * G + 112 * B + 128) >> 8) + 128;
                V = ( ( 112 * R -  94 * G -  18 * B + 128) >> 8) + 128;

                // NV21 has a plane of Y and interleaved planes of VU each sampled by a factor of 2
                //    meaning for every 4 Y pixels there are 1 V and 1 U.  Note the sampling is every other
                //    pixel AND every other scanline.
                yuv420sp[yIndex++] = (byte) ((Y < 0) ? 0 : ((Y > 255) ? 255 : Y));
                if (j % 2 == 0 && index % 2 == 0) {
                    yuv420sp[uvIndex++] = (byte)((V<0) ? 0 : ((V > 255) ? 255 : V));
                    yuv420sp[uvIndex++] = (byte)((U<0) ? 0 : ((U > 255) ? 255 : U));
                }

                index ++;
            }
        }
    }

    public static byte[] getYuvBytes(final Bitmap image) {
        final Bitmap bitmap = RotateBitmap(image, 270);
        final int inputWidth = bitmap.getWidth();
        final int inputHeight = bitmap.getHeight();
        int[] argb = new int[inputWidth * inputHeight];
        byte[] yuv = new byte[inputWidth * inputHeight * 3 / 2];
        bitmap.getPixels(argb, 0, inputWidth, 0, 0, inputWidth, inputHeight);
        encodeYUV420SP(yuv, argb, inputWidth, inputHeight);
        ImageUtils.safelyRemoveBitmap(bitmap);
        return yuv;
    }

}
