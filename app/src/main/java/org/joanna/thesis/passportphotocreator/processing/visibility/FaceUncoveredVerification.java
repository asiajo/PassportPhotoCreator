package org.joanna.thesis.passportphotocreator.processing.visibility;

import android.app.Activity;
import android.graphics.Rect;

import com.google.mlkit.vision.face.Face;

import org.joanna.thesis.passportphotocreator.camera.Graphic;
import org.joanna.thesis.passportphotocreator.camera.GraphicOverlay;
import org.joanna.thesis.passportphotocreator.processing.Action;
import org.joanna.thesis.passportphotocreator.processing.Verifier;
import org.joanna.thesis.passportphotocreator.processing.face.FaceUtils;
import org.joanna.thesis.passportphotocreator.utils.ImageUtils;
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
    public void verify(final byte[] data, final Face face) {

        List<Action> positions = new ArrayList<>();
        if (!FaceUtils.isFacePositionCorrect(face)) {
            mVisibilityGraphic.setBarActions(positions, mContext,
                    VisibilityGraphic.class);
            return;
        }
        mOverlay.add(mVisibilityGraphic);
        Mat image = ImageUtils.getMatFromYuvBytes(
                data,
                PREVIEW_HEIGHT,
                PREVIEW_WIDTH);

        final Rect bbox = face.getBoundingBox();
        int left = bbox.left + bbox.width() / 16;
        int top = bbox.top + bbox.height() / 4;
        int right = bbox.right - bbox.width() / 16;
        int bottom = bbox.bottom + bbox.height() / 8;
        image = ImageUtils.cropMatToBoundingBox(
                image, new Rect(left, top, right, bottom));
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

        int oneThird = src.width() / 3;
        Mat left = src.submat(
                0, src.height(),
                0, oneThird);

        Mat right = src.submat(
                0, src.height(),
                src.width() - oneThird, src.width());
        Core.flip(right, right, 1);

        Mat comparisionResult = new Mat();

        Imgproc.matchTemplate(left, right, comparisionResult, TM_CCORR_NORMED);

        final double epsilon = 0.96;
        final double similarity = comparisionResult.get(0, 0)[0];

        left.release();
        right.release();
        comparisionResult.release();

        return similarity > epsilon;
    }


}
