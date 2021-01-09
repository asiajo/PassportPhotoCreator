package org.joanna.thesis.passportphotocreator.processing.light;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

import static org.opencv.imgproc.Imgproc.THRESH_BINARY;

public final class ShadowUtils {

    private ShadowUtils() {
    }

    public static Mat overlayDeshadowed(final Mat src, final Mat overlay) {

        Mat image = new Mat();
        Imgproc.cvtColor(src, image, Imgproc.COLOR_RGBA2RGB);
        Imgproc.cvtColor(overlay, overlay, Imgproc.COLOR_RGBA2RGB);

        Mat deshadowed = new Mat();
        Mat white = new Mat(image.rows(), image.cols(), CvType.CV_8UC3, Scalar
                .all(255));
        Core.absdiff(image, overlay, deshadowed);
        Core.subtract(white, deshadowed, deshadowed);
        Imgproc.medianBlur(deshadowed, deshadowed, 21);
        Core.absdiff(image, deshadowed, deshadowed);
        Core.subtract(white, deshadowed, deshadowed);

        image.release();
        white.release();
        return deshadowed;
    }

    /**
     * Verifies if there is no side shadow on the face.
     *
     * @param src Mat containing face only.
     * @return information if face is similarly lightened on the right as on
     *         the left side
     */
    public static boolean isEvenlyLightened(final Mat src) {

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
        final double threshold = yMean - (yStd / 3);
        Mat mask = new Mat();
        Imgproc.threshold(yChannel, mask, threshold, 255., THRESH_BINARY);
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
