package org.joanna.thesis.passportphotocreator.processing.light.verification;

import android.app.Activity;

import org.joanna.thesis.passportphotocreator.camera.Graphic;
import org.joanna.thesis.passportphotocreator.camera.GraphicOverlay;
import org.joanna.thesis.passportphotocreator.utils.ImageUtils;
import org.joanna.thesis.passportphotocreator.processing.Action;
import org.joanna.thesis.passportphotocreator.processing.Verifier;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

import static org.joanna.thesis.passportphotocreator.PhotoMakerActivity.PREVIEW_HEIGHT;
import static org.joanna.thesis.passportphotocreator.PhotoMakerActivity.PREVIEW_WIDTH;
import static org.joanna.thesis.passportphotocreator.processing.light.ShadowUtils.isEvenlyLightened;
import static org.opencv.imgproc.Imgproc.THRESH_BINARY;

/**
 * Verifies if face does not contain side shadows.
 */
public class ShadowVerification extends Verifier {

    private static final String TAG =
            ShadowVerification.class.getSimpleName();

    protected Graphic mShadowGraphic;

    public ShadowVerification(
            final Activity activity,
            final GraphicOverlay<Graphic> overlay) {
        super(activity, overlay);
        mShadowGraphic = new ShadowGraphic(overlay);
    }

    @Override
    public void verify(final byte[] data) {

        mOverlay.add(mShadowGraphic);
        List<Action> positions = new ArrayList<>();

        Mat image = ImageUtils.getMatFromYuvBytes(
                data,
                PREVIEW_HEIGHT,
                PREVIEW_WIDTH);
        image = ImageUtils.cropMatToGetFaceOnly(image, mOverlay);
        if (null == image) {
            return;
        }
        if (!isEvenlyLightened(image)) {
            positions.add(ShadowActions.NOT_UNIFORM);
        }
        mShadowGraphic.setBarActions(positions, mContext,
                ShadowGraphic.class);

        image.release();
    }

}
