package org.joanna.thesis.passportphotocreator.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
        InputStream inputStream =
                context.getResources().getAssets().open(fName);
        BufferedInputStream bufferedInputStream =
                new BufferedInputStream(inputStream);
        return BitmapFactory.decodeStream(bufferedInputStream);
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
}
