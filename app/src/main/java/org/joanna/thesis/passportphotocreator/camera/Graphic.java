package org.joanna.thesis.passportphotocreator.camera;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;

import org.joanna.thesis.passportphotocreator.processing.Action;
import org.joanna.thesis.passportphotocreator.processing.PhotoValidity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

public abstract class Graphic {

    public static final float BOX_STROKE_WIDTH = 5.0f;
    public static final int   VALID_COLOR      = Color.GREEN;
    public static final int   WARNING_COLOR    = Color.YELLOW;
    public static final int   INVALID_COLOR    = Color.RED;

    private static final String TAG = Graphic.class.getSimpleName();

    private static Map<BitmapMetaData, Bitmap> mActions =
            new ConcurrentHashMap<>();
    private        Map<Action, BitmapMetaData> mActionsMap = new HashMap<>();
    private        Map<PhotoValidity, Integer> mColorMap = new HashMap<>();
    private        GraphicOverlay              mOverlay;
    private        Paint                       mPaint;

    {
        mColorMap.put(PhotoValidity.VALID, VALID_COLOR);
        mColorMap.put(PhotoValidity.WARNING, WARNING_COLOR);
        mColorMap.put(PhotoValidity.INVALID, INVALID_COLOR);
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

    public void postInvalidate() {
        mOverlay.postInvalidate();
    }

    protected void drawActionsToBePerformed(final Canvas canvas) {
        // draws actions to be performed to get perfect picture
        int i = 0;
        int iconSize = mOverlay.getWidth() / 12;
        int padding = iconSize / 5;
        List<Bitmap> actionBitmaps = new ArrayList<>();

        actionBitmaps.addAll(mActions.values());

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
                    mActionsMap.get(action),
                    BitmapFactory.decodeResource(
                            context.getResources(),
                            mActionsMap.get(action).getId()));
        }

        if (null != mActions) {
            clearActions(aClass);
            mActions.putAll(newActions);
        }
        setValidity();
    }

    protected void clearActions(final Class<? extends Graphic> aClass) {
        for (Iterator<Map.Entry<BitmapMetaData, Bitmap>> it =
             mActions.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<BitmapMetaData, Bitmap> entry = it.next();
            if (entry.getKey().getGraphicClass().equals(aClass)) {
                it.remove();
            }
        }
    }

    public void clearActions() {
        mActions.clear();
    }

    public Map<Action, BitmapMetaData> getActionsMap() {
        return mActionsMap;
    }

    /**
     * Sets the color to the most strict one: red if at least one item is
     * invalid, yellow if there are only warnings and green if all is perfect.
     */
    public void setValidity() {
        if (containsInvalidActions()) {
            mPaint.setColor(mColorMap.get(PhotoValidity.INVALID));
        } else if (containsWarningActions()) {
            mPaint.setColor(mColorMap.get(PhotoValidity.WARNING));
        } else {
            mPaint.setColor(mColorMap.get(PhotoValidity.VALID));
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
             mActions.keySet().iterator(); it.hasNext(); ) {
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
             mActions.keySet().iterator(); it.hasNext(); ) {
            BitmapMetaData metaData = it.next();
            if (metaData.makesPhotoWarning()) {
                return true;
            }
        }
        return false;
    }

    protected GraphicOverlay getGraphicOverlay() {
        return mOverlay;
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