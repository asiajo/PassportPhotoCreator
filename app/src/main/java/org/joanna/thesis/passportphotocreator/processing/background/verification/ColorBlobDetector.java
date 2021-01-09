package org.joanna.thesis.passportphotocreator.processing.background.verification;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Based on ColorBlobDetection Sample from OpenCV. */
public class ColorBlobDetector {
    /** Minimum contour area in percent for contours filtering */
    private static double mMinContourArea     = 0.1;
    /** Cache */
    private final  Mat mPyrDownMat            = new Mat();
    private final  Mat mHsvMat                = new Mat();
    private final  Mat mMask                  = new Mat();
    private final  Mat mDilatedMask           = new Mat();
    private final  Mat mHierarchy             = new Mat();
    /** Lower and Upper bounds for range checking in HSV color space */
    private final  Scalar mLowerBound         = new Scalar(0);
    private final  Scalar mUpperBound         = new Scalar(0);
    /** Color radius for range checking in HSV color space */
    private        Scalar mColorRadius        = new Scalar(25, 50, 50, 0);
    private final  Mat mSpectrum              = new Mat();
    private final  List<MatOfPoint> mContours = new ArrayList<>();

    private Scalar mBlobColorRgba = new Scalar(255);
    private int    imgScale       = 1;

    public double getContoursMaxArea() {
        return getContoursMaxArea(mContours);
    }

    public double getContoursTotalArea() {
        return getContoursTotalArea(mContours);
    }

    private double getContoursMaxArea(List<MatOfPoint> contours) {
        // Find max contour area
        double maxArea = 0;
        for (final MatOfPoint wrapper : contours) {
            double area = Imgproc.contourArea(wrapper);
            if (area > maxArea) {
                maxArea = area;
            }
        }
        return maxArea;
    }

    private double getContoursTotalArea(List<MatOfPoint> contours) {
        // Find max contour area
        double totalArea = 0;
        for (final MatOfPoint matOfPoint : contours) {
            totalArea += Imgproc.contourArea(matOfPoint);
        }
        return totalArea;
    }

    public double getContoursMaxPerimeter() {
        // Find max contour area
        double maxPerimeter = 0;
        for (final MatOfPoint wrapper : mContours) {
            double perm = Imgproc.arcLength(
                    new MatOfPoint2f(wrapper.toArray()), true);
            if (perm > maxPerimeter) {
                maxPerimeter = perm;
            }
        }
        return maxPerimeter;
    }

    public void process(Mat in, Point p) {

        if (p == null || (p.x < 0) || (p.y < 0) || (p.x > in.cols()) ||
                (p.y > in.rows())) {
            return;
        }

        Mat src = in.clone();

        Rect colorRect = getColorRect(p);
        Mat colorSample = src.submat(colorRect);
        Imgproc.cvtColor(colorSample, colorSample, Imgproc.COLOR_RGB2HSV_FULL);

        // Calculate average color of the sample
        final Scalar mBlobColorHsv = Core.sumElems(colorSample);
        int pointCount = colorRect.width * colorRect.height;
        for (int i = 0; i < mBlobColorHsv.val.length; i++) {
            mBlobColorHsv.val[i] /= pointCount;
        }

        mBlobColorRgba = convertScalarHsv2Rgba(mBlobColorHsv);

        setHsvColor(mBlobColorHsv);
        colorSample.release();

        downsizeMat(src, mPyrDownMat);
        downsizeMat(mPyrDownMat, mPyrDownMat);

        Imgproc.cvtColor(mPyrDownMat, mHsvMat, Imgproc.COLOR_RGB2HSV_FULL);
        Core.inRange(mHsvMat, mLowerBound, mUpperBound, mMask);
        Imgproc.dilate(mMask, mDilatedMask, new Mat());

        List<MatOfPoint> contours = new ArrayList<>();
        Imgproc.findContours(
                mDilatedMask, contours, mHierarchy, Imgproc.RETR_EXTERNAL,
                Imgproc.CHAIN_APPROX_SIMPLE);

        double maxArea = getContoursMaxArea(contours);
        fillContoursList(contours, maxArea);
    }

    private Rect getColorRect(final Point p) {
        Rect colorRect = new Rect();
        int diameter = 4;
        colorRect.x = (int) (p.x - diameter);
        colorRect.y = (int) (p.y - diameter);
        colorRect.width = diameter * 2;
        colorRect.height = diameter * 2;
        return colorRect;
    }

    private void fillContoursList(
            final List<MatOfPoint> contours, final double maxArea) {
        // Adds found contours to the mContours list, but only if they are above
        // specified threshold size
        final Iterator<MatOfPoint> each;
        mContours.clear();
        each = contours.iterator();
        while (each.hasNext()) {
            MatOfPoint contour = each.next();
            if (Imgproc.contourArea(contour) > mMinContourArea * maxArea) {
                Core.multiply(contour, new Scalar(imgScale, imgScale), contour);
                mContours.add(contour);
            }
        }
    }

    private void downsizeMat(final Mat src, final Mat dst) {
        Imgproc.pyrDown(src, dst);
        imgScale *= 2;
    }

    private Scalar convertScalarHsv2Rgba(Scalar hsvColor) {
        Mat pointMatRgba = new Mat();
        Mat pointMatHsv = new Mat(1, 1, CvType.CV_8UC3, hsvColor);
        Imgproc.cvtColor(
                pointMatHsv, pointMatRgba, Imgproc.COLOR_HSV2RGB_FULL, 4);

        return new Scalar(pointMatRgba.get(0, 0));
    }

    public Scalar getBlobColorRgba() {
        return mBlobColorRgba;
    }

    private void setHsvColor(Scalar hsvColor) {
        double minH = (hsvColor.val[0] >= mColorRadius.val[0]) ?
                hsvColor.val[0] - mColorRadius.val[0] : 0;
        double maxH = (hsvColor.val[0] + mColorRadius.val[0] <= 255) ?
                hsvColor.val[0] + mColorRadius.val[0] : 255;

        mLowerBound.val[0] = minH;
        mUpperBound.val[0] = maxH;

        mLowerBound.val[1] = hsvColor.val[1] - mColorRadius.val[1];
        mUpperBound.val[1] = hsvColor.val[1] + mColorRadius.val[1];

        mLowerBound.val[2] = hsvColor.val[2] - mColorRadius.val[2];
        mUpperBound.val[2] = hsvColor.val[2] + mColorRadius.val[2];

        mLowerBound.val[3] = 0;
        mUpperBound.val[3] = 255;

        Mat spectrumHsv = new Mat(1, (int) (maxH - minH), CvType.CV_8UC3);

        for (int j = 0; j < maxH - minH; j++) {
            byte[] tmp = {(byte) (minH + j), (byte) 255, (byte) 255};
            spectrumHsv.put(0, j, tmp);
        }

        Imgproc.cvtColor(spectrumHsv, mSpectrum, Imgproc.COLOR_HSV2RGB_FULL, 4);
        spectrumHsv.release();
    }
}
