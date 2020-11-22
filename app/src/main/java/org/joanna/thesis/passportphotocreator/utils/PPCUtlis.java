package org.joanna.thesis.passportphotocreator.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.util.Log;
import android.util.SparseArray;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;

import org.opencv.android.Utils;
import org.opencv.core.Mat;

import static com.google.android.gms.vision.Frame.ROTATION_90;
import static org.joanna.thesis.passportphotocreator.processing.face.FaceUtils.getFaceBoundingBox;
import static org.joanna.thesis.passportphotocreator.utils.ImageUtils.verifyBoundingBox;

public final class PPCUtlis {

    public static Rect multiplyRect(
            final int bigToSmallImgScale, final Rect faceBoundingBoxSmall) {
        return new Rect(
                faceBoundingBoxSmall.left * bigToSmallImgScale,
                faceBoundingBoxSmall.top * bigToSmallImgScale,
                faceBoundingBoxSmall.right * bigToSmallImgScale,
                faceBoundingBoxSmall.bottom * bigToSmallImgScale);
    }

    public static org.opencv.core.Rect AndroidRectToOpenCVRect(
            final Rect faceBoundingBoxBig) {

        return new org.opencv.core.Rect(
                faceBoundingBoxBig.left, faceBoundingBoxBig.top,
                faceBoundingBoxBig.width(), faceBoundingBoxBig.height());
    }

    private static Face detectAndGetFace(
            final int bigToSmallImgScale,
            final Bitmap bigImage,
            final FaceDetector detector) {

        Bitmap smallImage = Bitmap.createScaledBitmap(
                bigImage,
                bigImage.getWidth() / bigToSmallImgScale,
                bigImage.getHeight() / bigToSmallImgScale,
                false);

        Frame frame = new Frame.Builder().setBitmap(smallImage)
                                         .setRotation(ROTATION_90)
                                         .build();

        SparseArray<Face> faces = detector.detect(frame);
        ImageUtils.safelyRemoveBitmap(smallImage);
        if (faces.size() == 0) {
            return null;
        }
        return faces.valueAt(0);
    }

    public static Mat getFaceMatFromPictureTaken(
            final byte[] bytes,
            final FaceDetector detector) {

        final int tmpImgScale = 8;
        Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        // we need to detect face again on the bitmap. In case the face is quite
        // small on the screen and the camera was moved there could be a shift
        // between previously detected face position and actual position on the
        // picture. To be on the safe side we make detection on the final photo.
        Face face = detectAndGetFace(tmpImgScale, bmp, detector);
        if (null == face) {
            Log.w(
                    "Photo Taken",
                    "Did not find any face on the image data. Picture taking " +
                            "will fail.");
            ImageUtils.safelyRemoveBitmap(bmp);
            return null;
        }
        Mat picture = new Mat();
        Utils.bitmapToMat(bmp, picture);
        ImageUtils.safelyRemoveBitmap(bmp);
        picture = ImageUtils.rotateMat(picture);

        Rect faceBbSmall = getFaceBoundingBox(face);
        Rect faceBoundingBox = multiplyRect(tmpImgScale, faceBbSmall);
        if (!verifyBoundingBox(faceBoundingBox, picture.size())) {
            Log.w(
                    "Photo Taken",
                    "Picture does not fit entirely within visible camera " +
                            "region. Picture taking will fail.");
            return null;
        }
        picture = picture.submat(AndroidRectToOpenCVRect(faceBoundingBox));
        picture = ImageUtils.resizeMatToFinalSize(picture);
        return picture;
    }

    public static Rect translateY(final Rect faceBoundingBox, final float v) {
        return new Rect(
                faceBoundingBox.left,
                (int) (faceBoundingBox.top + v),
                faceBoundingBox.right,
                (int) (faceBoundingBox.bottom + v)
        );
    }

    private void Utils() {
    }
}
