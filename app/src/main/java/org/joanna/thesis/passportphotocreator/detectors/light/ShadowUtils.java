package org.joanna.thesis.passportphotocreator.detectors.light;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

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
}
