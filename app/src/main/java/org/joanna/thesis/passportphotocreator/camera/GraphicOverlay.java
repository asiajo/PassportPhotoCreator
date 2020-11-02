package org.joanna.thesis.passportphotocreator.camera;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

import com.google.android.gms.vision.CameraSource;

import java.util.HashSet;
import java.util.Set;

public class GraphicOverlay<T extends Graphic> extends View {
    private final Object mLock             = new Object();
    private       int    mPreviewWidth;
    private       float  mWidthScaleFactor = 1.0f;

    private int   mPreviewHeight;
    private float mHeightScaleFactor = 1.0f;

    private int    mFacing   = CameraSource.CAMERA_FACING_BACK;
    private Set<T> mGraphics = new HashSet<>();
    private int    counter   = 0;

    public GraphicOverlay(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void add(T graphic) {
        synchronized (mLock) {
            mGraphics.add(graphic);
        }
        slowlyPostinvalidate();
    }

    public void remove(T graphic) {
        counter++;
        synchronized (mLock) {
            mGraphics.remove(graphic);
        }
        slowlyPostinvalidate();
    }

    public void setCameraInfo(int previewWidth, int previewHeight, int facing) {
        synchronized (mLock) {
            mPreviewWidth = previewWidth;
            mPreviewHeight = previewHeight;
            mFacing = facing;
        }
        slowlyPostinvalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        System.out.println("On draw called.");
        super.onDraw(canvas);

        Rect r = new Rect(0, 0, canvas.getWidth(),
                canvas.getWidth() / 8);
        Paint whiteFill = new Paint();
        whiteFill.setStyle(Paint.Style.FILL);
        whiteFill.setColor(Color.WHITE);
        canvas.drawRect(r, whiteFill);

        synchronized (mLock) {
            if ((mPreviewWidth != 0) && (mPreviewHeight != 0)) {
                mWidthScaleFactor =
                        (float) canvas.getWidth() / (float) mPreviewWidth;
                mHeightScaleFactor =
                        (float) canvas.getHeight() / (float) mPreviewHeight;
            }

            for (Graphic graphic : mGraphics) {
                graphic.draw(canvas);
            }
        }
    }

    public void slowlyPostinvalidate() {
        if (counter >= 5) {
            counter = 0;
            postInvalidate();
        }
    }

    public float getWidthScaleFactor() {
        return mWidthScaleFactor;
    }

    public float getHeightScaleFactor() {
        return mHeightScaleFactor;
    }

    public int getFacing() {
        return mFacing;
    }

    public Set<T> getmGraphics() {
        return mGraphics;
    }
}
