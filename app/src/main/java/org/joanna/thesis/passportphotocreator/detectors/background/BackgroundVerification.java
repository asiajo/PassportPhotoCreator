package org.joanna.thesis.passportphotocreator.detectors.background;

import android.app.Activity;
import android.content.Context;
import android.widget.Toast;

import org.joanna.thesis.passportphotocreator.PhotoMakerActivity;
import org.joanna.thesis.passportphotocreator.R;
import org.joanna.thesis.passportphotocreator.camera.Graphic;
import org.joanna.thesis.passportphotocreator.camera.GraphicOverlay;
import org.joanna.thesis.passportphotocreator.detectors.Action;
import org.joanna.thesis.passportphotocreator.utils.ImageUtils;
import org.opencv.core.Mat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class BackgroundVerification {

    public  ImageSegmentor          segmentor;
    private GraphicOverlay<Graphic> mOverlay;
    private Graphic                 mBackgroundGraphic;
    private Context                 mContext;

    public BackgroundVerification(
            final Activity activity,
            final GraphicOverlay<Graphic> overlay) {
        mOverlay = overlay;
        mBackgroundGraphic = new BackgroundGraphic(overlay);
        mContext = activity.getApplicationContext();
        try {
            segmentor = new ImageSegmentorFloatMobileUnet(activity);
        } catch (IOException e) {
            Toast.makeText(activity, R.string.no_background_verification_error,
                    Toast.LENGTH_LONG).show();
        }

    }

    public void verify(final byte[] data) {

        if (segmentor == null) {
            return;
        }

        mOverlay.add(mBackgroundGraphic);

        // TODO: do actual verification
        Mat background = getBackground(data);
        if (null == background) {
            return;
        }
        List<Action> positions = new ArrayList<>();
        if (!BackgroundUtils.isBright(background)) {
            positions.add(BackgroundActions.TOO_DARK);
        }
        if (!BackgroundUtils.isUniform(background)) {
            positions.add(BackgroundActions.NOT_UNIFORM);
        }

        mBackgroundGraphic.setBarActions(positions, mContext,
                BackgroundGraphic.class);
        background.release();
    }

    public void close() {
        if (segmentor != null) {
            segmentor.close();
        }
    }

    /**
     * Calls the tflite model for image segmentation to retrieve background
     * behind the person present on the image.
     *
     * @param data image frame from the camera in yuv bytes format
     * @return If person is detected on the image returs Mat with the
     *         background, null otherwise.
     */
    private Mat getBackground(final byte[] data) {
        Mat image = ImageUtils.getMatFromYuvBytes(
                data,
                PhotoMakerActivity.PREVIEW_HEIGHT,
                PhotoMakerActivity.PREVIEW_WIDTH);
        image = ImageUtils.cropMatToFaceBoundingBox(
                image, mOverlay);
        if (image == null) {
            return null;
        }
        int imgWidth = (int) Math.ceil(
                ImageSegmentor.PROCESS_IMG_SIZE
                        * ImageUtils.FINAL_IMAGE_W_TO_H_RATIO);
        image = ImageUtils.resizeMat(image, imgWidth);
        image = ImageUtils.padMatToSquare(
                image,
                ImageSegmentor.PROCESS_IMG_SIZE);

        image = segmentor.segmentImgGetBackground(image);

        image = ImageUtils.unpadMatFromSquare(image, imgWidth);
        return image;
    }

}
