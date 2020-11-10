package org.joanna.thesis.passportphotocreator.camera;

import android.content.Context;
import android.graphics.Canvas;

import org.joanna.thesis.passportphotocreator.detectors.Action;

import java.util.List;

public abstract class Graphic {

    private GraphicOverlay mOverlay;

    public Graphic(GraphicOverlay overlay) {
        mOverlay = overlay;
    }

    public abstract void draw(final Canvas canvas);

    public abstract void setBarActions(
            final List<Action> positions,
            final Context context);

    public float scaleX(final float horizontal) {
        return horizontal * mOverlay.getWidthScaleFactor();
    }

    public float scaleY(final float vertical) {
        return vertical * mOverlay.getHeightScaleFactor();
    }

    public float translateX(final float x) {
        if (mOverlay.getFacing() == CameraSource.CAMERA_FACING_FRONT) {
            return mOverlay.getWidth() - scaleX(x);
        } else {
            return scaleX(x);
        }
    }

    public float translateY(final float y) {
        return scaleY(y);
    }

    public void postInvalidate() {
        mOverlay.postInvalidate();
    }
}