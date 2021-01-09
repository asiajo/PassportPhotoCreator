package org.joanna.thesis.passportphotocreator.camera;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;

import java.util.HashSet;
import java.util.Set;

public class GraphicOverlay<T extends Graphic> extends View {
    public static final float  TOP_RECT_W_TO_H_RATIO = 8.0f;
    private final       Object mLock                 = new Object();
    private final Set<T> mGraphics = new HashSet<>();
    private             int    mPreviewWidth;
    private             float  mWidthScaleFactor     = 1.0f;
    private             int    mPreviewHeight;
    private             float  mHeightScaleFactor    = 1.0f;
    private       int    mCounter  = 0;

    public GraphicOverlay(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void add(T graphic) {
        synchronized (mLock) {
            mGraphics.add(graphic);
        }
        slowlyPostInvalidate();
    }

    public void remove(T graphic) {
        mCounter++;
        synchronized (mLock) {
            mGraphics.remove(graphic);
        }
        slowlyPostInvalidate();
    }

    public void setCameraInfo(int previewWidth, int previewHeight) {
        synchronized (mLock) {
            mPreviewWidth = previewWidth;
            mPreviewHeight = previewHeight;
        }
        slowlyPostInvalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        synchronized (mLock) {
            if ((mPreviewWidth != 0) && (mPreviewHeight != 0)) {
                mWidthScaleFactor = getWidth() / (float) mPreviewWidth;
                mHeightScaleFactor =
                        getOverlayRelativeHeight() / (float) mPreviewHeight;
            }

            for (Graphic graphic : mGraphics) {
                graphic.draw(canvas);
            }
        }
    }

    public void slowlyPostInvalidate() {
        if (mCounter >= 5) {
            mCounter = 0;
            postInvalidate();
        }
    }

    public float getWidthScaleFactor() {
        return mWidthScaleFactor;
    }

    public float getHeightScaleFactor() {
        return mHeightScaleFactor;
    }

    public int getOverlayRelativeHeight() {
        return getHeight() - (int) (getWidth() / TOP_RECT_W_TO_H_RATIO);
    }
}
