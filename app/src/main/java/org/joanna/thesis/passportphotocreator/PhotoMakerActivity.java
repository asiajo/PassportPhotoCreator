package org.joanna.thesis.passportphotocreator;


import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;
import com.google.android.gms.vision.face.LargestFaceFocusingProcessor;
import com.google.android.material.snackbar.Snackbar;

import org.joanna.thesis.passportphotocreator.camera.CameraSource;
import org.joanna.thesis.passportphotocreator.camera.CameraSourcePreview;
import org.joanna.thesis.passportphotocreator.camera.Graphic;
import org.joanna.thesis.passportphotocreator.camera.GraphicOverlay;
import org.joanna.thesis.passportphotocreator.detectors.background.BackgroundVerification;
import org.joanna.thesis.passportphotocreator.detectors.face.FaceTracker;
import org.joanna.thesis.passportphotocreator.detectors.light.ShadowRemover;
import org.joanna.thesis.passportphotocreator.detectors.light.ShadowRemoverPix2Pix;
import org.joanna.thesis.passportphotocreator.detectors.light.ShadowVerification;
import org.joanna.thesis.passportphotocreator.utils.ImageUtils;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.io.IOException;

import static com.google.android.gms.vision.Frame.ROTATION_90;
import static com.google.android.material.snackbar.Snackbar.make;
import static org.joanna.thesis.passportphotocreator.detectors.face.FaceUtils.getFaceBoundingBox;
import static org.joanna.thesis.passportphotocreator.utils.ImageUtils.verifyBoundingBox;

public class PhotoMakerActivity extends Activity
        implements View.OnClickListener {

    public static final int PREVIEW_WIDTH  = 480;
    public static final int PREVIEW_HEIGHT = 640;

    private static final String TAG = PhotoMakerActivity.class.getSimpleName();

    // permission request codes need to be < 256
    private static final int RC_HANDLE_CAMERA_PERM = 2;

    static {
        OpenCVLoader.initDebug();
    }

    private CameraSource            mCameraSource;
    private CameraSourcePreview     mPreview;
    private GraphicOverlay<Graphic> mGraphicOverlay;
    private FaceTracker             mFaceTracker;
    private ScaleGestureDetector    mScaleGestureDetector;
    private BackgroundVerification  mBackgroundVerifier;
    private FaceDetector            mDetector;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.photo_capture);

        mPreview = findViewById(R.id.preview);
        mGraphicOverlay = findViewById(R.id.graphicOverlay);
        mScaleGestureDetector = new ScaleGestureDetector(
                this,
                new ScaleListener());

        mBackgroundVerifier = new BackgroundVerification(this, mGraphicOverlay);

        int rc = ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA);
        if (rc == PackageManager.PERMISSION_GRANTED) {
            createCameraSource();
        } else {
            requestCameraPermission();
        }
        findViewById(R.id.take_photo_button).setOnClickListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        startCameraSource();
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.take_photo_button) {
            takePhoto();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mPreview.stop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mBackgroundVerifier != null) {
            mBackgroundVerifier.close();
        }
        if (mCameraSource != null) {
            mCameraSource.release();
        }
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        return mScaleGestureDetector.onTouchEvent(e) || super.onTouchEvent(e);
    }


    private void requestCameraPermission() {
        final String[] permissions = new String[]{Manifest.permission.CAMERA};
        if (!ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.CAMERA)) {
            ActivityCompat.requestPermissions(this, permissions,
                    RC_HANDLE_CAMERA_PERM);
            return;
        }

        final Activity thisActivity = this;

        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ActivityCompat.requestPermissions(thisActivity, permissions,
                        RC_HANDLE_CAMERA_PERM);
            }
        };
        findViewById(R.id.topLayout).setOnClickListener(listener);
        make(mGraphicOverlay, R.string.permission_camera_rationale,
                Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.ok, listener)
                .show();
    }

    private void createCameraSource() {
        Context context = getApplicationContext();

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        mDetector = new FaceDetector.Builder(context)
                .setLandmarkType(FaceDetector.ALL_LANDMARKS)
                .setClassificationType(FaceDetector.ALL_CLASSIFICATIONS)
                .setProminentFaceOnly(true)
                .build();

        mFaceTracker = new FaceTracker(mGraphicOverlay, context);
        mDetector.setProcessor(
                new LargestFaceFocusingProcessor(mDetector, mFaceTracker));

        if (!mDetector.isOperational()) {
            IntentFilter lowstorageFilter =
                    new IntentFilter(Intent.ACTION_DEVICE_STORAGE_LOW);
            boolean hasLowStorage = registerReceiver(
                    null,
                    lowstorageFilter) != null;

            if (hasLowStorage) {
                Toast.makeText(this, R.string.low_storage_error,
                        Toast.LENGTH_LONG).show();
            }
        }

        CameraSource.Builder builder = new CameraSource
                .Builder(context, mDetector)
                .setFacing(CameraSource.CAMERA_FACING_BACK)
                .setRequestedPreviewSize(PREVIEW_HEIGHT, PREVIEW_WIDTH)
                .setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)
                .setBackgroundVerifier(mBackgroundVerifier)
                .setRequestedFps(15.0f);

        mCameraSource = builder.build();

    }

    private void startCameraSource() throws SecurityException {

        if (mCameraSource != null) {
            try {
                mPreview.start(mCameraSource, mGraphicOverlay);
            } catch (IOException e) {
                Log.e(TAG, "Unable to start camera source.", e);
                mCameraSource.release();
                mCameraSource = null;
            }
        }
    }

    private void takePhoto() {
        if (mCameraSource == null) {
            return;
        }
        final Rect boundingBox = mFaceTracker.getFaceBoundingBox();
        if (boundingBox == null) {
            Toast.makeText(this, R.string.no_face_on_the_picture,
                    Toast.LENGTH_LONG).show();
            return;
        }
        final Activity thisActivity = this;
        mCameraSource.takePicture(null, new CameraSource.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] bytes) {

                try {
                    Mat picture = getFaceMatFromPictureTaken(bytes);
                    if (picture == null) {
                        Toast.makeText(thisActivity, R.string.cannot_make_a_picture,
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    try {
                        final ShadowRemover deshadower =
                                new ShadowRemoverPix2Pix(thisActivity);
                        picture = deshadower.deshadow(picture);
                    } catch (IOException e) {
                        Toast.makeText(thisActivity,
                                R.string.no_face_shadow_removal_error,
                                Toast.LENGTH_SHORT).show();
                    }
                    ImageUtils.saveImage(picture, thisActivity);
                    Toast.makeText(thisActivity, R.string.image_saved,
                            Toast.LENGTH_SHORT).show();
                } catch (IOException e) {
                    Toast.makeText(thisActivity, R.string.image_not_saved,
                            Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                }
            }
        });
    }

    private Mat getFaceMatFromPictureTaken(final byte[] bytes) {
        Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        Frame frame = new Frame.Builder().setBitmap(bmp)
                                         .setRotation(ROTATION_90)
                                         .build();

        // we need to detect face again on the bitmap. In case the face is quite
        // small on the screen and the camera was moved there could be a shift
        // between previously detected face position and actual position on the
        // picture. To be on the safe side we make detection on the final photo.
        SparseArray<Face> faces = mDetector.detect(frame);
        if (faces.size() == 0) {
            ImageUtils.safelyRemoveBitmap(bmp);
            return null;
        }
        Face face = faces.valueAt(0);
        Mat picture = new Mat();
        Utils.bitmapToMat(bmp, picture);
        ImageUtils.safelyRemoveBitmap(bmp);
        picture = ImageUtils.rotateMat(picture);

        Rect faceBoundingBox = getFaceBoundingBox(face);
        if (!verifyBoundingBox(faceBoundingBox, picture.size())) {
            return null;
        }
        picture = picture.submat(faceBoundingBox.top, faceBoundingBox.bottom,
                faceBoundingBox.left, faceBoundingBox.right);
        picture = ImageUtils.resizeMatToFinalSize(picture);
        return picture;
    }

    private class ScaleListener
            implements ScaleGestureDetector.OnScaleGestureListener {

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            return false;
        }

        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            return true;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            mCameraSource.doZoom(detector.getScaleFactor());
        }
    }

}