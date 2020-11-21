package org.joanna.thesis.passportphotocreator.validators.visibility;

import android.app.Activity;

import org.joanna.thesis.passportphotocreator.camera.Graphic;
import org.joanna.thesis.passportphotocreator.camera.GraphicOverlay;
import org.joanna.thesis.passportphotocreator.utils.ImageUtils;
import org.joanna.thesis.passportphotocreator.validators.Action;
import org.joanna.thesis.passportphotocreator.validators.Verifier;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

import static org.joanna.thesis.passportphotocreator.PhotoMakerActivity.PREVIEW_HEIGHT;
import static org.joanna.thesis.passportphotocreator.PhotoMakerActivity.PREVIEW_WIDTH;
import static org.opencv.imgproc.Imgproc.TM_CCORR_NORMED;

/**
 * Verifies if there are no objects covering face.
 */
public class FaceUncoveredVerification extends Verifier {

    private static final String TAG =
            FaceUncoveredVerification.class.getSimpleName();

    private Graphic mVisibilityGraphic;

    public FaceUncoveredVerification(
            final Activity activity,
            final GraphicOverlay<Graphic> overlay) {
        super(activity, overlay);
        mVisibilityGraphic = new VisibilityGraphic(overlay);
    }

    @Override
    public void verify(final byte[] data) {

        mOverlay.add(mVisibilityGraphic);
        List<Action> positions = new ArrayList<>();

        Mat image = ImageUtils.getMatFromYuvBytes(
                data,
                PREVIEW_HEIGHT,
                PREVIEW_WIDTH);

        image = ImageUtils.cropMatToFaceBoundingBox(image, mOverlay);
        if (null == image) {
            return;
        }
        if (!isSimilar(image)) {
            positions.add(VisibilityActions.HIDDEN);
        }
        mVisibilityGraphic.setBarActions(positions, mContext,
                VisibilityGraphic.class);
        image.release();
    }

    private boolean isSimilar(final Mat src) {

        // cut away top part of the hair
        Mat image = src.submat(
                src.height() / 4, src.height(),
                0, src.width());

        int halfWidth = image.width() / 2;
        Mat left = image.submat(
                0, image.height(),
                0, halfWidth);

        Mat right = image.submat(
                0, image.height(),
                halfWidth, halfWidth * 2);
        image.release();
        Core.flip(right, right, 1);

        Mat comparisionResult = new Mat();

        Imgproc.matchTemplate(left, right, comparisionResult, TM_CCORR_NORMED);

        final double epsilon = 0.95;
        final double similarity = comparisionResult.get(0, 0)[0];

        left.release();
        right.release();
        comparisionResult.release();

        return similarity > epsilon;
    }


}
