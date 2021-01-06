package org.joanna.thesis.passportphotocreator.processing.face;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;

import com.google.mlkit.vision.face.Face;

import org.joanna.thesis.passportphotocreator.R;
import org.joanna.thesis.passportphotocreator.camera.Graphic;
import org.joanna.thesis.passportphotocreator.camera.GraphicOverlay;
import org.joanna.thesis.passportphotocreator.processing.PhotoValidity;
import org.joanna.thesis.passportphotocreator.utils.PPCUtlis;

import java.util.ArrayList;
import java.util.List;

import static org.joanna.thesis.passportphotocreator.camera.GraphicOverlay.TOP_RECT_W_TO_H_RATIO;

public class FaceGraphic extends Graphic {

    private static final int        ARROW_MIN_SIZE = 12;
    private static final int        ARROW_MAX_SIZE = 24;
    private              double     bbProportionWidth;
    private              List<Rect> mFaceBoundingBoxes;
    private volatile     List<Face> mFaces;
    private              Context    mContext;
    private              int        mArrowsScale   = ARROW_MIN_SIZE;

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
        getActionsMap().put(
                FaceActions.TOO_MANY_FACES,
                new BitmapMetaData(
                        FaceGraphic.class, R.drawable.too_many_faces,
                        PhotoValidity.INVALID));
    }

    FaceGraphic(final GraphicOverlay overlay, final Context context) {
        super(overlay);
        mContext = context;
        mFaceBoundingBoxes = new ArrayList<>();
    }

    public void updateFaces(final List<Face> faces) {
        mFaces = faces;
        postInvalidate();
    }

    @Override
    public void draw(final Canvas canvas) {
        if (null == mFaces || mFaces.size() == 0) {
            return;
        }
        int i = 0;
        mFaceBoundingBoxes.clear();
        for (Face face : mFaces) {
            mFaceBoundingBoxes.add(i, FaceUtils.getFaceBoundingBox(face, this));
            Rect displayBoundingBox = PPCUtlis.translateY(
                    mFaceBoundingBoxes.get(i),
                    getGraphicOverlay().getWidth() / TOP_RECT_W_TO_H_RATIO);
            canvas.drawRect(displayBoundingBox, getmPaint());
        }
        drawActionsToBePerformed(canvas);
        if (mFaces.size() == 1) {
            setFirstBoundingBoxProportions();
            if (isFaceTooSmall()) {
                drawEnlargingInfo(canvas);
            }
        }
    }

    private boolean isFaceTooSmall() {
        return bbProportionWidth < 0.5;
    }

    private void drawEnlargingInfo(final Canvas canvas) {
        Rect bbox = mFaceBoundingBoxes.get(0);
        Bitmap enlarge = BitmapFactory.decodeResource(
                mContext.getResources(),
                R.drawable.enlarge);
        final Rect rectSrc = new Rect(0, 0, enlarge.getWidth(),
                enlarge.getHeight());
        int dstHalfWidth = bbox.width() * mArrowsScale++ /
                ARROW_MAX_SIZE;
        int dstHalfHeight =
                dstHalfWidth * enlarge.getHeight() / enlarge.getWidth();
        final int centerX = bbox.centerX();
        final int centerY = (int) (bbox.centerY() +
                getGraphicOverlay().getWidth() / TOP_RECT_W_TO_H_RATIO);
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

    private void setFirstBoundingBoxProportions() {
        double canvasWidth = getGraphicOverlay().getWidth();
        bbProportionWidth = mFaceBoundingBoxes.get(0).width() / canvasWidth;
    }
}