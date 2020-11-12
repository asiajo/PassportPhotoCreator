package org.joanna.thesis.passportphotocreator.camera;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;

import org.joanna.thesis.passportphotocreator.detectors.Action;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public abstract class Graphic {

    public static final float BOX_STROKE_WIDTH              = 5.0f;
    public static final int VALID_COLOR                     = Color.GREEN;
    public static final int INVALID_COLOR                   = Color.RED;
    private static      Map<BitmapMetaData, Bitmap> actions = new TreeMap<>();
    private Map<Action, Integer> actionsMap = new HashMap<>();
    private GraphicOverlay       mOverlay;
    private Paint                mPaint;

    public Graphic(GraphicOverlay overlay) {
        mOverlay = overlay;
        mPaint = new Paint();
        mPaint.setColor(INVALID_COLOR);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(BOX_STROKE_WIDTH);
    }

    public abstract void draw(final Canvas canvas);

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

    protected void drawActionsToBePerformed(final Canvas canvas) {
        // draws actions to be performed to get perfect picture
        int i = 0;
        int iconSize = canvas.getWidth() / 12;
        int padding = iconSize / 5;
        for (Bitmap action : actions.values()) {
            final Rect rectSrc = new Rect(0, 0, action.getWidth(),
                    action.getHeight());
            final Rect rectDst = new Rect(
                    (i + 1) * padding + i * iconSize,
                    padding,
                    (i + 1) * padding + (i + 1) * iconSize,
                    padding + iconSize);
            canvas.drawBitmap(action, rectSrc, rectDst, new Paint());
            i++;
        }
    }

    public void setBarActions(
            final List<Action> positions,
            final Context context,
            final Class<? extends Graphic> aClass) {
        clearActions(getClass());
        for (Action position : positions) {
            actions.put(
                    new BitmapMetaData(aClass, actionsMap.get(position)),
                    BitmapFactory.decodeResource(
                            context.getResources(),
                            actionsMap.get(position)));
        }
        setIsValid(actions.isEmpty());
    }

    protected void clearActions(final Class<? extends Graphic> aClass) {
        for (Iterator<Map.Entry<BitmapMetaData, Bitmap>> it =
             actions.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<BitmapMetaData, Bitmap> entry = it.next();
            if (entry.getKey().getGraphicClass().equals(aClass)) {
                it.remove();
            }
        }
    }

    public Map<Action, Integer> getActionsMap() {
        return actionsMap;
    }

    public void setIsValid(final boolean isValid) {
        mPaint.setColor(isValid ? VALID_COLOR : INVALID_COLOR);
    }

    public Paint getmPaint() {
        return mPaint;
    }

    public class BitmapMetaData implements Comparable<BitmapMetaData> {
        private Class<? extends Graphic> mClass;
        private int                      mId;

        public BitmapMetaData(
                final Class<? extends Graphic> aClass,
                final int id) {
            mClass = aClass;
            mId = id;
        }

        @Override
        public int compareTo(final BitmapMetaData o) {
            return this.getId().compareTo(o.getId());
        }

        @Override
        public String toString() {
            return "Bitmap [id=" + mId + "], class: [" + mClass + "]";
        }

        public Class<? extends Graphic> getGraphicClass() {
            return mClass;
        }

        public Integer getId() {
            return mId;
        }
    }
}