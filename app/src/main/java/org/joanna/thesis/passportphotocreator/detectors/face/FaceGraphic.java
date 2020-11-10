package org.joanna.thesis.passportphotocreator.detectors.face;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;

import com.google.android.gms.vision.face.Face;

import org.joanna.thesis.passportphotocreator.R;
import org.joanna.thesis.passportphotocreator.camera.Graphic;
import org.joanna.thesis.passportphotocreator.camera.GraphicOverlay;
import org.joanna.thesis.passportphotocreator.detectors.Action;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FaceGraphic extends Graphic {

    private static final float BOX_STROKE_WIDTH = 5.0f;
    private static final int   VALID_COLOR      = Color.GREEN;
    private static final int   INVALID_COLOR    = Color.RED;

    private Paint  mPaint;
    private Canvas canvas;

    private          double bbProportionLeft;
    private          double bbProportionTop;
    private          double bbProportionWidth;
    private          double bbProportionHeight;
    private          Rect   faceBoundingBox;
    private          PointF facePosition;
    private volatile Face   mFace;

    private List<Bitmap>              headActions;
    private Map<FaceActions, Integer> headActionsMap;

    {
        headActionsMap = new HashMap<>();
        headActionsMap.put(FaceActions.ROTATE_LEFT, R.drawable.arrow_left);
        headActionsMap.put(FaceActions.ROTATE_RIGHT, R.drawable.arrow_right);
        headActionsMap.put(
                FaceActions.STRAIGHTEN_FROM_LEFT,
                R.drawable.arrow_straighten_right);
        headActionsMap.put(
                FaceActions.STRAIGHTEN_FROM_RIGHT,
                R.drawable.arrow_straighten_left);
        headActionsMap.put(FaceActions.FACE_DOWN, R.drawable.arrow_down);
        headActionsMap.put(FaceActions.FACE_UP, R.drawable.arrow_up);
        headActionsMap.put(FaceActions.LEFT_EYE_OPEN, R.drawable.eye);
        headActionsMap.put(FaceActions.RIGHT_EYE_OPEN, R.drawable.eye);
        headActionsMap.put(FaceActions.NEUTRAL_MOUTH, R.drawable.mouth);

    }

    FaceGraphic(final GraphicOverlay overlay) {
        super(overlay);
        mPaint = new Paint();
        mPaint.setColor(INVALID_COLOR);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(BOX_STROKE_WIDTH);
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
        facePosition = face.getPosition();
        faceBoundingBox = FaceUtils.getFaceBoundingBox(face, this);
        setBoundingBoxProportions();
        canvas.drawRect(faceBoundingBox, mPaint);
        drawFaceActionsToBePerformed(canvas);
    }

    private void setBoundingBoxProportions() {
        double canvasWidth = canvas.getWidth();
        double canvasHeight = canvas.getHeight();
        bbProportionLeft = faceBoundingBox.left / canvasWidth;
        bbProportionTop = faceBoundingBox.top / canvasHeight;
        bbProportionWidth = faceBoundingBox.width() / canvasWidth;
        bbProportionHeight = faceBoundingBox.height() / canvasHeight;
    }

    private void drawFaceActionsToBePerformed(final Canvas canvas) {
        // draws actions to be performed to correct face position
        int i = 0;
        int iconSize = canvas.getWidth() / 12;
        int padding = iconSize / 5;
        for (Bitmap headAction : headActions) {
            final Rect rectSrc = new Rect(0, 0, headAction.getWidth(),
                    headAction.getHeight());
            final Rect rectDst = new Rect(
                    (i + 1) * padding + i * iconSize,
                    padding,
                    (i + 1) * padding + (i + 1) * iconSize,
                    padding + iconSize);
            canvas.drawBitmap(headAction, rectSrc, rectDst, mPaint);
            i++;
        }
    }

    @Override
    public void setBarActions(
            final List<Action> positions,
            final Context context) {
        headActions = new ArrayList<>();
        for (Action position : positions) {
            headActions.add(BitmapFactory.decodeResource(
                    context.getResources(),
                    headActionsMap.get(position)));
        }
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

    public double getBbProportionHeight() {
        return bbProportionHeight;
    }

    public PointF getFacePosition() {
        return facePosition;
    }

    void setIsValid(final boolean isValid) {
        mPaint.setColor(isValid ? VALID_COLOR : INVALID_COLOR);
    }
}