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

    private static final int            ARROW_MIN_SIZE = 4;
    private static final int            ARROW_MAX_SIZE = 12;
    private              Canvas         canvas;
    private              double         bbProportionLeft;
    private              double         bbProportionTop;
    private              double         bbProportionWidth;
    private              double         bbProportionHeight;
    private              Rect           faceBoundingBox;
    private volatile     Face           mFace;
    private              GraphicOverlay mOverlay;
    private              Context        mContext;
    private              int            arrowsScale    = ARROW_MIN_SIZE;

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
        this.canvas = canvas;
        Face face = mFace;
        if (face == null) {
            return;
        }
        faceBoundingBox = FaceUtils.getFaceBoundingBox(face, this);
        Rect displayBoundingBox = PPCUtlis.translateY(
                faceBoundingBox,
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
        int dstHalfWidth =
                canvas.getWidth() / 2 * arrowsScale++ / ARROW_MAX_SIZE;
        int dstHalfHeight =
                dstHalfWidth * enlarge.getHeight() / enlarge.getWidth();
        final int centerX = canvas.getWidth() / 2;
        final int centerY = canvas.getHeight() / 2;
        final Rect rectDst = new Rect(
                centerX - dstHalfWidth,
                centerY - dstHalfHeight,
                centerX + dstHalfWidth,
                centerY + dstHalfHeight);
        canvas.drawBitmap(enlarge, rectSrc, rectDst, new Paint());
        if (arrowsScale == ARROW_MAX_SIZE) {
            arrowsScale = ARROW_MIN_SIZE;
        }
    }

    private void setBoundingBoxProportions() {
        double canvasWidth = mOverlay.getWidth();
        double canvasHeight = mOverlay.getOverlayRelativeHeight();
        bbProportionLeft = faceBoundingBox.left / canvasWidth;
        bbProportionTop = faceBoundingBox.top / canvasHeight;
        bbProportionWidth = faceBoundingBox.width() / canvasWidth;
        bbProportionHeight = faceBoundingBox.height() / canvasHeight;
    }

    public Rect getFaceBoundingBox() {
        return faceBoundingBox;
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
        return faceBoundingBox.centerX() / (double) mOverlay.getWidth();
    }

    public double getBbProportionCenterY() {
        return faceBoundingBox.centerY() /
                ((double) mOverlay.getOverlayRelativeHeight());
    }

    public double getBbProportionHeight() {
        return bbProportionHeight;
    }
}