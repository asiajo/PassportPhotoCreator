package org.joanna.thesis.passportphotocreator.processing.face;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;

import com.google.android.gms.vision.face.Face;

import org.joanna.thesis.passportphotocreator.R;
import org.joanna.thesis.passportphotocreator.camera.Graphic;
import org.joanna.thesis.passportphotocreator.camera.GraphicOverlay;
import org.joanna.thesis.passportphotocreator.processing.PhotoValidity;
import org.joanna.thesis.passportphotocreator.utils.PPCUtlis;

import static org.joanna.thesis.passportphotocreator.camera.GraphicOverlay.TOP_RECT_W_TO_H_RATIO;

public class FaceGraphic extends Graphic {

    private static final int            ARROW_MIN_SIZE = 12;
    private static final int            ARROW_MAX_SIZE = 24;
    private              double         bbProportionLeft;
    private              double         bbProportionTop;
    private              double         bbProportionWidth;
    private              double         bbProportionHeight;
    private              Rect           mFaceBoundingBox;
    private volatile     Face           mFace;
    private              GraphicOverlay mOverlay;
    private              Context        mContext;
    private              int            mArrowsScale   = ARROW_MIN_SIZE;

    {
        getActionsMap().put(
                FaceActions.ROTATE_LEFT,
                new BitmapMetaData(FaceGraphic.class, R.drawable.arrow_left,
                        PhotoValidity.INVALID));
        getActionsMap().put(
                FaceActions.ROTATE_RIGHT,
                new BitmapMetaData(FaceGraphic.class, R.drawable.arrow_right,
                        PhotoValidity.INVALID));
        getActionsMap().put(
                FaceActions.STRAIGHTEN_FROM_LEFT,
                new BitmapMetaData(
                        FaceGraphic.class, R.drawable.arrow_straighten_right,
                        PhotoValidity.INVALID));
        getActionsMap().put(
                FaceActions.STRAIGHTEN_FROM_RIGHT,
                new BitmapMetaData(
                        FaceGraphic.class, R.drawable.arrow_straighten_left,
                        PhotoValidity.INVALID));
        getActionsMap().put(
                FaceActions.FACE_DOWN,
                new BitmapMetaData(FaceGraphic.class, R.drawable.arrow_down,
                        PhotoValidity.INVALID));
        getActionsMap().put(
                FaceActions.FACE_UP,
                new BitmapMetaData(FaceGraphic.class, R.drawable.arrow_up,
                        PhotoValidity.INVALID));
        getActionsMap().put(
                FaceActions.LEFT_EYE_OPEN,
                new BitmapMetaData(
                        FaceGraphic.class, R.drawable.eye,
                        PhotoValidity.INVALID));
        getActionsMap().put(
                FaceActions.RIGHT_EYE_OPEN,
                new BitmapMetaData(
                        FaceGraphic.class, R.drawable.eye,
                        PhotoValidity.INVALID));
        getActionsMap().put(
                FaceActions.NEUTRAL_MOUTH,
                new BitmapMetaData(
                        FaceGraphic.class, R.drawable.mouth,
                        PhotoValidity.INVALID));
    }

    FaceGraphic(final GraphicOverlay overlay, final Context context) {
        super(overlay);
        mOverlay = overlay;
        mContext = context;
    }

    void updateFace(final Face face) {
        mFace = face;
        postInvalidate();
    }

    @Override
    public void draw(final Canvas canvas) {
        Face face = mFace;
        if (face == null) {
            return;
        }
        mFaceBoundingBox = FaceUtils.getFaceBoundingBox(face, this);
        Rect displayBoundingBox = PPCUtlis.translateY(
                mFaceBoundingBox,
                mOverlay.getWidth() / TOP_RECT_W_TO_H_RATIO);
        setBoundingBoxProportions();
        canvas.drawRect(displayBoundingBox, getmPaint());
        drawActionsToBePerformed(canvas);
        if (isFaceTooSmall()) {
            drawEnlargingInfo(canvas);
        }
    }

    private boolean isFaceTooSmall() {
        return bbProportionWidth < 0.5;
    }

    private void drawEnlargingInfo(final Canvas canvas) {
        Bitmap enlarge = BitmapFactory.decodeResource(
                mContext.getResources(),
                R.drawable.enlarge);
        final Rect rectSrc = new Rect(0, 0, enlarge.getWidth(),
                enlarge.getHeight());
        int dstHalfWidth = (int) (mFaceBoundingBox.width() * mArrowsScale++ /
                ARROW_MAX_SIZE);
        int dstHalfHeight =
                dstHalfWidth * enlarge.getHeight() / enlarge.getWidth();
        final int centerX = mFaceBoundingBox.centerX();
        final int centerY = (int) (mFaceBoundingBox.centerY() +
                mOverlay.getWidth() / TOP_RECT_W_TO_H_RATIO);
        final Rect rectDst = new Rect(
                centerX - dstHalfWidth,
                centerY - dstHalfHeight,
                centerX + dstHalfWidth,
                centerY + dstHalfHeight);
        canvas.drawBitmap(enlarge, rectSrc, rectDst, new Paint());
        if (mArrowsScale == ARROW_MAX_SIZE) {
            mArrowsScale = ARROW_MIN_SIZE;
        }
    }

    private void setBoundingBoxProportions() {
        double canvasWidth = mOverlay.getWidth();
        double canvasHeight = mOverlay.getOverlayRelativeHeight();
        bbProportionLeft = mFaceBoundingBox.left / canvasWidth;
        bbProportionTop = mFaceBoundingBox.top / canvasHeight;
        bbProportionWidth = mFaceBoundingBox.width() / canvasWidth;
        bbProportionHeight = mFaceBoundingBox.height() / canvasHeight;
    }

    public Rect getFaceBoundingBox() {
        return mFaceBoundingBox;
    }

    public double getBbProportionLeft() {
        return bbProportionLeft;
    }

    public double getBbProportionTop() {
        return bbProportionTop;
    }

    public double getBbProportionWidth() {
        return bbProportionWidth;
    }

    public double getBbProportionCenterX() {
        return mFaceBoundingBox.centerX() / (double) mOverlay.getWidth();
    }

    public double getBbProportionCenterY() {
        return mFaceBoundingBox.centerY() /
                ((double) mOverlay.getOverlayRelativeHeight());
    }

    public double getBbProportionHeight() {
        return bbProportionHeight;
    }
}