package org.joanna.thesis.passportphotocreator.modifiers.face;

import android.app.Activity;
import android.graphics.Bitmap;
import android.util.Log;

import org.joanna.thesis.passportphotocreator.DetectorTensorflowLite;
import org.joanna.thesis.passportphotocreator.utils.ImageUtils;
import org.joanna.thesis.passportphotocreator.utils.ShadowUtils;
import org.opencv.core.Mat;

import java.io.IOException;

public abstract class ShadowRemover extends DetectorTensorflowLite {

    private static final String TAG = ShadowRemover.class.getSimpleName();
    /** Mat holding the image for processing. */
    protected            Mat    mImage;

    ShadowRemover(final Activity activity) throws IOException {
        super(activity);
        Log.d(TAG, "Created a Tensorflow Lite Shadow Remover.");
    }

    public Mat deshadow(final Mat src) {
        Mat input = prepareInput(src);
        Mat deshadowed = getDeshadowedOverlay(input);
        input.release();
        deshadowed = ImageUtils.resizeMatToFinalSize(deshadowed);
        deshadowed = ShadowUtils.overlayDeshadowed(src, deshadowed);
        return deshadowed;
    }

    private Mat prepareInput(final Mat src) {

        Mat image = ImageUtils.resizeMat(src, getOutputImageWidth(),
                getImageSizeY());
        image = ImageUtils.padMatToSquareBlack(image, getImageSizeY());
        return image;
    }

    private Mat getDeshadowedOverlay(final Mat src) {
        if (mTflite == null) {
            Log.e(TAG, "Shadow remover has not been initialized; Skipped.");
        }
        mImage = src;

        Mat resizedMat = ImageUtils.resizeMat(src, getImageSizeX(),
                getImageSizeY());
        Bitmap tmp = ImageUtils.getBitmapFromMat(resizedMat);
        resizedMat.release();
        Bitmap bmp = tmp.copy(tmp.getConfig(), true);
        ImageUtils.safelyRemoveBitmap(tmp);
        convertBitmapToByteBuffer(bmp);
        ImageUtils.safelyRemoveBitmap(bmp);

        runInference();
        Mat deshadowed = getDeshadowedOverlay();
        deshadowed = ImageUtils.unpadMatFromSquare(
                deshadowed,
                getOutputImageWidth());
        return deshadowed;
    }

    protected abstract Mat getDeshadowedOverlay();

    protected abstract int getOutputImageWidth();
}
