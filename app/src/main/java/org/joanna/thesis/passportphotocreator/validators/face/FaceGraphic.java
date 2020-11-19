package org.joanna.thesis.passportphotocreator.validators.face;

import android.graphics.Canvas;
import android.graphics.Rect;

import com.google.android.gms.vision.face.Face;

import org.joanna.thesis.passportphotocreator.R;
import org.joanna.thesis.passportphotocreator.camera.Graphic;
import org.joanna.thesis.passportphotocreator.camera.GraphicOverlay;
import org.joanna.thesis.passportphotocreator.validators.PhotoValidity;
import org.joanna.thesis.passportphotocreator.utils.FaceUtils;

public class FaceGraphic extends Graphic {

    private Canvas canvas;

    private double        bbProportionLeft;
    private double        bbProportionTop;
    private double        bbProportionWidth;
    private double        bbProportionHeight;
    private Rect          faceBoundingBox;
    private volatile Face mFace;

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

    FaceGraphic(final GraphicOverlay overlay) {
        super(overlay);
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
        setBoundingBoxProportions();
        canvas.drawRect(faceBoundingBox, getmPaint());
        drawActionsToBePerformed(canvas);
    }

    private void setBoundingBoxProportions() {
        double canvasWidth = canvas.getWidth();
        double canvasHeight = canvas.getHeight();
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
        return faceBoundingBox.centerX() / (double) canvas.getWidth();
    }

    public double getBbProportionCenterY() {
        return faceBoundingBox.centerY() / (double) canvas.getHeight();
    }

    public double getBbProportionHeight() {
        return bbProportionHeight;
    }
}