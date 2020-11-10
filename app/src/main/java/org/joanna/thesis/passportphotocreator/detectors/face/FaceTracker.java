package org.joanna.thesis.passportphotocreator.detectors.face;

import android.content.Context;
import android.graphics.Rect;

import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.face.Face;

import org.joanna.thesis.passportphotocreator.camera.Graphic;
import org.joanna.thesis.passportphotocreator.camera.GraphicOverlay;
import org.joanna.thesis.passportphotocreator.detectors.Action;

import java.util.ArrayList;
import java.util.List;

public class FaceTracker extends Tracker<Face> {

    private static final String TAG = FaceTracker.class.getSimpleName();

    private static final double NEUTRAL_FACE_THRESHOLD = 0.4;
    private static final double EYES_OPEN_THRESHOLD    = 0.7;
    private static final double ROTATION_THRESHOLD     = 4;
    private Face                        mFace = null;
    private GraphicOverlay<Graphic> mOverlay;
    private FaceGraphic                 mFaceGraphic;
    private Context                     mContext;

    public FaceTracker(
            final GraphicOverlay<Graphic> mOverlay,
            final Context context) {
        super();
        this.mContext = context;
        this.mOverlay = mOverlay;
        mFaceGraphic = new FaceGraphic(mOverlay);
    }

    @Override
    public void onUpdate(
            final Detector.Detections<Face> detectionResults,
            final Face face) {
        // TODO: refactor
        mOverlay.add(mFaceGraphic);
        mFace = face;
        List<Action> positions = new ArrayList<>();
        if (face.getEulerY() < -ROTATION_THRESHOLD) {
            positions.add(FaceActions.ROTATE_LEFT);
        } else if (face.getEulerY() > ROTATION_THRESHOLD) {
            positions.add(FaceActions.ROTATE_RIGHT);
        }
        if (face.getEulerX() < -ROTATION_THRESHOLD) {
            positions.add(FaceActions.FACE_UP);
        } else if (face.getEulerX() > ROTATION_THRESHOLD) {
            positions.add(FaceActions.FACE_DOWN);
        }
        if (face.getEulerZ() < -ROTATION_THRESHOLD) {
            positions.add(FaceActions.STRAIGHTEN_FROM_LEFT);
        } else if (face.getEulerZ() > ROTATION_THRESHOLD) {
            positions.add(FaceActions.STRAIGHTEN_FROM_RIGHT);
        }
        if (face.getIsLeftEyeOpenProbability() < EYES_OPEN_THRESHOLD) {
            positions.add(FaceActions.LEFT_EYE_OPEN);
        }
        if (face.getIsRightEyeOpenProbability() < EYES_OPEN_THRESHOLD) {
            positions.add(FaceActions.RIGHT_EYE_OPEN);
        }
        if (face.getIsSmilingProbability() > NEUTRAL_FACE_THRESHOLD) {
            positions.add(FaceActions.NEUTRAL_MOUTH);
        }

        mFaceGraphic.setIsValid(positions.size() == 0);
        mFaceGraphic.setBarActions(positions, mContext);
        mFaceGraphic.updateFace(face);
    }

    @Override
    public void onMissing(final Detector.Detections<Face> detectionResults) {
        mOverlay.remove(mFaceGraphic);
    }

    @Override
    public void onDone() {
        mOverlay.remove(mFaceGraphic);
    }

    public Rect getFaceBoundingBox() {
        if (mFace == null) {
            return null;
        }
        return FaceUtils.getFaceBoundingBox(mFace, mFaceGraphic);
    }
}
