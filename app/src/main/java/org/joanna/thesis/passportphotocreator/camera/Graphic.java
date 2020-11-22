package org.joanna.thesis.passportphotocreator.camera;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.Log;

import org.joanna.thesis.passportphotocreator.processing.Action;
import org.joanna.thesis.passportphotocreator.processing.PhotoValidity;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.joanna.thesis.passportphotocreator.camera.GraphicOverlay.TOP_RECT_W_TO_H_RATIO;

public abstract class Graphic {

    public static final float BOX_STROKE_WIDTH = 5.0f;
    public static final int   VALID_COLOR      = Color.GREEN;
    public static final int   WARNING_COLOR    = Color.YELLOW;
    public static final int   INVALID_COLOR    = Color.RED;

    private static final String TAG = Graphic.class.getSimpleName();

    private static Map<BitmapMetaData, Bitmap> actions = new TreeMap<>();
    private        Map<Action, BitmapMetaData> actionsMap = new HashMap<>();
    private        Map<PhotoValidity, Integer> colorMap = new HashMap<>();
    private        GraphicOverlay              mOverlay;
    private        Paint                       mPaint;

    {
        colorMap.put(PhotoValidity.VALID, VALID_COLOR);
        colorMap.put(PhotoValidity.WARNING, WARNING_COLOR);
        colorMap.put(PhotoValidity.INVALID, INVALID_COLOR);
    }

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
        int iconSize = mOverlay.getWidth() / 12;
        int padding = iconSize / 5;
        List<Bitmap> actionBitmaps = new ArrayList<>();

        try {
            actionBitmaps.addAll(actions.values());
        } catch (ConcurrentModificationException e) {
            Log.e(TAG, "Exception happened: " + e.getMessage());
        }

        for (Bitmap action : actionBitmaps) {
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
        Map<BitmapMetaData, Bitmap> newActions = new TreeMap<>();
        for (Action action : positions) {
            newActions.put(
                    actionsMap.get(action),
                    BitmapFactory.decodeResource(
                            context.getResources(),
                            actionsMap.get(action).getId()));
        }
        try {
            clearActions(aClass);
            actions.putAll(newActions);
        } catch (ConcurrentModificationException e) {
            Log.e(TAG, "Exception happened: " + e.getMessage());
        }
        setValidity();
    }

    protected void clearActions(final Class<? extends Graphic> aClass) {
        try {
            for (Iterator<Map.Entry<BitmapMetaData, Bitmap>> it =
                 actions.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry<BitmapMetaData, Bitmap> entry = it.next();
                if (entry.getKey().getGraphicClass().equals(aClass)) {
                    it.remove();
                }
            }
        } catch (ConcurrentModificationException e) {
            Log.e(TAG, "Exception happened: " + e.getMessage());
        }
    }

    public void clearActions() {
        actions.clear();
    }

    public Map<Action, BitmapMetaData> getActionsMap() {
        return actionsMap;
    }

    /**
     * Sets the color to the most strict one: red if at least one item is
     * invalid, yellow if there are only warnings and green if all is perfect.
     */
    public void setValidity() {
        if (containsInvalidActions()) {
            mPaint.setColor(colorMap.get(PhotoValidity.INVALID));
        } else if (containsWarningActions()) {
            mPaint.setColor(colorMap.get(PhotoValidity.WARNING));
        } else {
            mPaint.setColor(colorMap.get(PhotoValidity.VALID));
        }
    }

    public Paint getmPaint() {
        return mPaint;
    }

    /**
     * For a good photo face have to be perfectly positioned. Verifies if there
     * are crucial actions to be performed to get a correct photo.
     *
     * @return true if at least one action would make a photo invalid, false
     *         otherwise
     */
    private boolean containsInvalidActions() {
        for (Iterator<BitmapMetaData> it =
             actions.keySet().iterator(); it.hasNext(); ) {
            BitmapMetaData metaData = it.next();
            if (metaData.makesPhotoInvalid()) {
                return true;
            }
        }
        return false;
    }

    /**
     * There are actions that are classified as warnings - for example if
     * background seems to contain edges. Those will be enhanced on the final
     * photo, so they do not block photo creation. Verifies if such actions are
     * present in the action list.
     *
     * @return true if at least one action is warning action, false otherwise.
     */
    private boolean containsWarningActions() {
        for (Iterator<BitmapMetaData> it =
             actions.keySet().iterator(); it.hasNext(); ) {
            BitmapMetaData metaData = it.next();
            if (metaData.makesPhotoWarning()) {
                return true;
            }
        }
        return false;
    }

    public class BitmapMetaData implements Comparable<BitmapMetaData> {
        private Class<? extends Graphic> mClass;
        private int                      mId;
        private PhotoValidity            mValidity;

        public BitmapMetaData(
                final Class<? extends Graphic> aClass,
                final int id,
                final PhotoValidity validity) {
            mClass = aClass;
            mId = id;
            mValidity = validity;
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

        public boolean makesPhotoInvalid() {
            return mValidity.equals(PhotoValidity.INVALID);
        }

        public boolean makesPhotoWarning() {
            return mValidity.equals(PhotoValidity.WARNING);
        }
    }
}