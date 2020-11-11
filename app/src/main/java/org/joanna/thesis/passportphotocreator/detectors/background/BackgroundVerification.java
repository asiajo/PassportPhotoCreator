package org.joanna.thesis.passportphotocreator.detectors.background;

import android.app.Activity;
import android.graphics.Bitmap;
import android.widget.Toast;

import org.joanna.thesis.passportphotocreator.PhotoMakerActivity;
import org.joanna.thesis.passportphotocreator.R;
import org.joanna.thesis.passportphotocreator.camera.Graphic;
import org.joanna.thesis.passportphotocreator.camera.GraphicOverlay;
import org.joanna.thesis.passportphotocreator.utils.ImageUtils;
import org.opencv.core.Mat;

import java.io.IOException;

public class BackgroundVerification {

    private static final int                     IMAGE_HEIGHT = 128;
    public               ImageSegmentor          segmentor;
    private              GraphicOverlay<Graphic> mGraphicOverlay;

    public BackgroundVerification(
            final Activity activity,
            final GraphicOverlay<Graphic> graphicOverlay) {
        mGraphicOverlay = graphicOverlay;
        try {
            segmentor = new ImageSegmentorFloatMobileUnet(activity);
            segmentor.setNumThreads(1);
        } catch (IOException e) {
            Toast.makeText(activity, R.string.no_background_verification_error,
                    Toast.LENGTH_LONG).show();
        }

    }

    public void verify(final byte[] data) {

        // TODO: do actual verification
        Mat inputMat = ImageUtils.getMatFromYuvBytes(
                data,
                PhotoMakerActivity.PREVIEW_HEIGHT,
                PhotoMakerActivity.PREVIEW_WIDTH);
        Mat croppedMat = ImageUtils.cropMatToFaceBoundingBox(
                inputMat, mGraphicOverlay);
        inputMat.release();
        if (croppedMat == null) {
            return;
        }
        int imgWidth = (int) Math.ceil(
                IMAGE_HEIGHT * ImageUtils.FINAL_IMAGE_W_TO_H_RATIO);
        Mat resizedMat = ImageUtils.resizeMat(croppedMat, imgWidth);
        croppedMat.release();
        Bitmap imageCropped = ImageUtils.getBitmapFromMat(resizedMat);

        ImageUtils.safelyRemoveBitmap(imageCropped);
        resizedMat.release();
    }

}
