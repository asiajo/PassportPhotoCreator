package org.joanna.thesis.passportphotocreator.detectors.light;

import android.app.Activity;
import android.content.Context;

import org.joanna.thesis.passportphotocreator.PhotoMakerActivity;
import org.joanna.thesis.passportphotocreator.camera.Graphic;
import org.joanna.thesis.passportphotocreator.camera.GraphicOverlay;
import org.joanna.thesis.passportphotocreator.detectors.Action;
import org.joanna.thesis.passportphotocreator.utils.ImageUtils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

import static org.opencv.imgproc.Imgproc.THRESH_BINARY;

/**
 * Verifies if background is bright and uniform.
 */
public class ShadowVerification {

    private static final String TAG =
            ShadowVerification.class.getSimpleName();

    private GraphicOverlay<Graphic> mOverlay;
    private Graphic                 mShadowGraphic;
    private Context                 mContext;


    public ShadowVerification(
            final Activity activity,
            final GraphicOverlay<Graphic> overlay) {
        mOverlay = overlay;
        mShadowGraphic = new ShadowGraphic(overlay);
        mContext = activity.getApplicationContext();
    }

    /**
     * Performs the verification and sets the graphic overlay respectively.
     *
     * @param data image data under verification
     */
    public void verify(final byte[] data) {

        mOverlay.add(mShadowGraphic);
        List<Action> positions = new ArrayList<>();

        Mat image = ImageUtils.getMatFromYuvBytes(
                data,
                PhotoMakerActivity.PREVIEW_HEIGHT,
                PhotoMakerActivity.PREVIEW_WIDTH);
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

    /**
     * Verifies if there is no side shadow on the face.
     *
     * @param src Mat containing face only.
     * @return information if face is similarly lightened on the right as on
     *         the left side
     */
    private boolean isEvenlyLightened(final Mat src) {

        Mat ycrcb = new Mat();
        Imgproc.cvtColor(src, ycrcb, Imgproc.COLOR_RGB2YCrCb);
        List<Mat> channels = new ArrayList<>();
        Core.split(ycrcb, channels);
        ycrcb.release();

        MatOfDouble mean = new MatOfDouble();
        MatOfDouble std = new MatOfDouble();
        Mat yChannel = channels.get(0);
        Core.meanStdDev(yChannel, mean, std);
        final double yMean = mean.toArray()[0];
        final double yStd = std.toArray()[0];
        final double treshold = yMean - (yStd / 3);
        Mat mask = new Mat();
        Imgproc.threshold(yChannel, mask, treshold, 255., THRESH_BINARY);
        yChannel.release();
        Mat left = mask.submat(
                0, mask.height(),
                0, mask.width() / 2);
        Mat right = mask.submat(
                0, mask.height(),
                mask.width() / 2, mask.width());
        final double meanLeft = Core.mean(left).val[0];
        final double meanRight = Core.mean(right).val[0];
        left.release();
        right.release();
        final int epsilon = 25;
        return !(Math.abs(meanLeft - meanRight) > epsilon);
    }
}
