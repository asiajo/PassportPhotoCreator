package org.joanna.thesis.passportphotocreator.processing.face;

import android.graphics.Rect;

import com.google.mlkit.vision.face.Face;

import org.joanna.thesis.passportphotocreator.camera.Graphic;
import org.joanna.thesis.passportphotocreator.utils.ImageUtils;

public final class FaceUtils {

    private static final float BB_SCALING = 1.2f;

    private FaceUtils() {
    }

    public static Rect getFaceBoundingBox(
            final Face face,
            final Graphic graphic) {
        double centerX = graphic.scaleX(face.getBoundingBox().centerX());
        double centerY = graphic.scaleX(face.getBoundingBox().centerY());
        double widthWithOffset =
                graphic.scaleX(face.getBoundingBox().width()) * BB_SCALING;

        return getFaceBoundingBox(centerX, centerY, widthWithOffset);
    }

    public static Rect getFaceBoundingBox(final Face face) {
        double centerX = face.getBoundingBox().centerX();
        double centerY = face.getBoundingBox().centerY();
        int widthWithOffset =
                (int) (face.getBoundingBox().width() * BB_SCALING);
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
