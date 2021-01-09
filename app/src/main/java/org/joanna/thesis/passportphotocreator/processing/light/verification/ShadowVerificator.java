package org.joanna.thesis.passportphotocreator.processing.light.verification;

import android.app.Activity;
import android.graphics.Bitmap;
import android.util.Log;

import org.joanna.thesis.passportphotocreator.DetectorTensorflowLite;
import org.joanna.thesis.passportphotocreator.utils.ImageUtils;
import org.opencv.core.Mat;

import java.io.IOException;

/**
 * Classifies images with Tensorflow Lite.
 */
public abstract class ShadowVerificator extends DetectorTensorflowLite {

    private static final String TAG = ShadowVerificator.class.getSimpleName();

    /** Mat holding the image for processing. */
    protected Mat mImage;

    ShadowVerificator(final Activity activity) throws IOException {
        super(activity);
        Log.d(TAG, "Created a Tensorflow Lite Shadow classificator.");
    }

    /**
     * Classifies received image.
     */
    public void classify(Mat image) {
        if (mTflite == null) {
            Log.e(TAG, "Shadow Verificator has not been initialized; Skipped.");
        }
        mImage = image;

        Mat resizedMat = ImageUtils.resizeMat(image, getImageSizeX(),
                getImageSizeY());
        Bitmap tmp = ImageUtils.getBitmapFromMat(resizedMat);
        resizedMat.release();
        Bitmap bmp = tmp.copy(tmp.getConfig(), true);
        ImageUtils.safelyRemoveBitmap(tmp);
        convertBitmapToByteBuffer(bmp);
        ImageUtils.safelyRemoveBitmap(bmp);

        runInference();
    }

    /**
     * @return mask
     */
    public abstract EvenlyLightened isEvenlyLightened();

    public enum EvenlyLightened {
        EVENLY,
        NOT_SURE,
        SHADOW
    }

}
