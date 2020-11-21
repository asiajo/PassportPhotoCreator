package org.joanna.thesis.passportphotocreator.processing.face;

import android.graphics.Rect;

import com.google.android.gms.vision.face.Face;

import org.joanna.thesis.passportphotocreator.camera.Graphic;
import org.joanna.thesis.passportphotocreator.utils.ImageUtils;

public final class FaceUtils {

    private static final float BB_SCALING = 1.2f;

    private FaceUtils() {
    }

    public static Rect getFaceBoundingBox(
            final Face face,
            final Graphic graphic) {
        // getPosition returns left top corner of the face bounding box
        double centerX =
                graphic.translateX(face.getPosition().x + face.getWidth() / 2);
        double centerY =
                graphic.translateY(face.getPosition().y + face.getHeight() / 2);
        double widthWithOffset = graphic.scaleX(face.getWidth()) * BB_SCALING;

        return getFaceBoundingBox(centerX, centerY, widthWithOffset);
    }

    public static Rect getFaceBoundingBox(final Face face) {
        // getPosition returns left top corner of the face bounding box
        int centerX = (int) (face.getPosition().x + face.getWidth() / 2.);
        int centerY = (int) (face.getPosition().y + face.getHeight() / 2.);
        int widthWithOffset = (int) (face.getWidth() * BB_SCALING);

        return getFaceBoundingBox(centerX, centerY, widthWithOffset);
    }

    private static Rect getFaceBoundingBox(
            final double centerX, final double centerY,
            final double widthWithOffset) {

        double heightWithOffset =
                widthWithOffset / ImageUtils.FINAL_IMAGE_W_TO_H_RATIO;

        // find location of bounding box edges
        final int RatioDivisor = 16; // for changing y position of BB
        final float upperEdgeProportion = RatioDivisor / 2.0f + 1;
        final float upperEdgeRatio = upperEdgeProportion / RatioDivisor;
        final float lowerEdgeRatio = 1 - upperEdgeRatio;
        final float half = 0.5f;

        int left = (int) (centerX - widthWithOffset * half);
        int top = (int) (centerY - heightWithOffset * upperEdgeRatio);
        int right = (int) (centerX + widthWithOffset * half);
        int bottom = (int) (centerY + heightWithOffset * lowerEdgeRatio);

        return new Rect(left, top, right, bottom);
    }

}
