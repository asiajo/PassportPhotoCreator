package org.joanna.thesis.passportphotocreator;


import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import com.google.android.gms.vision.face.FaceDetector;
import com.google.android.gms.vision.face.LargestFaceFocusingProcessor;
import com.google.android.material.snackbar.Snackbar;

import org.joanna.thesis.passportphotocreator.camera.CameraSource;
import org.joanna.thesis.passportphotocreator.camera.CameraSourcePreview;
import org.joanna.thesis.passportphotocreator.camera.Graphic;
import org.joanna.thesis.passportphotocreator.camera.GraphicOverlay;
import org.joanna.thesis.passportphotocreator.processing.Enhancer;
import org.joanna.thesis.passportphotocreator.processing.Verifier;
import org.joanna.thesis.passportphotocreator.processing.background.BackgroundProcessing;
import org.joanna.thesis.passportphotocreator.processing.face.FaceTracker;
import org.joanna.thesis.passportphotocreator.processing.light.enhancement.ShadowRemoverPix2Pix;
import org.joanna.thesis.passportphotocreator.processing.light.verification.ShadowVerification;
import org.joanna.thesis.passportphotocreator.utils.ImageUtils;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.google.android.material.snackbar.Snackbar.make;
import static org.joanna.thesis.passportphotocreator.utils.Utlis.getFaceMatFromPictureTaken;

public class PhotoMakerActivity extends Activity
        implements View.OnClickListener {

    public static final int PREVIEW_WIDTH  = 480;
    public static final int PREVIEW_HEIGHT = 640;

    private static final String TAG = PhotoMakerActivity.class.getSimpleName();

    // permission request codes need to be < 256
    private static final int      RC_HANDLE_CAMERA_PERM = 2;
    private static final String[] PERMISSIONS_CAMERA    =
            {Manifest.permission.CAMERA};

    private static final int      REQUEST_EXTERNAL_STORAGE = 1;
    private static final String[] PERMISSIONS_STORAGE      =
            {Manifest.permission.WRITE_EXTERNAL_STORAGE};


    static {
        OpenCVLoader.initDebug();
    }

    private CameraSource            mCameraSource;
    private CameraSourcePreview     mPreview;
    private GraphicOverlay<Graphic> mGraphicOverlay;
    private FaceTracker             mFaceTracker;
    private ScaleGestureDetector    mScaleGestureDetector;
    private FaceDetector            mDetector;
    private List<Verifier>          mVerifiers;
    private List<Enhancer>          mEnhancers;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.photo_capture);

        // TODO: make it nicer
        mPreview = findViewById(R.id.preview);
        mGraphicOverlay = findViewById(R.id.graphicOverlay);
        mScaleGestureDetector = new ScaleGestureDetector(
                this,
                new ScaleListener());

        mEnhancers = new ArrayList<>();
        try {
            mEnhancers.add(new ShadowRemoverPix2Pix(this));
        } catch (IOException e) {
            Toast.makeText(
                    this,
                    R.string.no_face_shadow_removal_error,
                    Toast.LENGTH_SHORT).show();
        }

        mVerifiers = new ArrayList<>();
        // TODO: add face size on the preview verifier
        mVerifiers.add(new ShadowVerification(this, mGraphicOverlay));
        try {
            BackgroundProcessing mBackgroundProcessor =
                    new BackgroundProcessing(this, mGraphicOverlay);
            mVerifiers.add(mBackgroundProcessor);
            mEnhancers.add(mBackgroundProcessor);
        } catch (IOException e) {
            Toast.makeText(this, R.string.no_background_verification_error,
                    Toast.LENGTH_LONG).show();
        }

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
        for (Enhancer enhancer : mEnhancers) {
            enhancer.close();
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

        if (!ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.CAMERA)) {
            ActivityCompat.requestPermissions(
                    this,
                    PERMISSIONS_CAMERA,
                    RC_HANDLE_CAMERA_PERM);
            return;
        }

        final Activity thisActivity = this;

        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ActivityCompat.requestPermissions(
                        thisActivity,
                        PERMISSIONS_CAMERA,
                        RC_HANDLE_CAMERA_PERM);
            }
        };
        findViewById(R.id.topLayout).setOnClickListener(listener);
        make(mGraphicOverlay, R.string.permission_camera_rationale,
                Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.ok, listener)
                .show();
    }

    private void requestStoragePermissions() {
        int permission = ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE);
        }
    }

    private void createCameraSource() {
        Context context = getApplicationContext();

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        mDetector = new FaceDetector.Builder(context)
                .setLandmarkType(FaceDetector.NO_LANDMARKS)
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
                .setVerifiers(mVerifiers)
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
        requestStoragePermissions();
        final Activity thisActivity = this;
        mCameraSource.takePicture(null, new CameraSource.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] bytes) {
                // TODO: do in background
                // TODO: move to new activity and display preview before saving
                try {
                    Mat picture = getFaceMatFromPictureTaken(bytes, mDetector);
                    if (picture == null) {
                        Toast.makeText(
                                thisActivity,
                                R.string.cannot_make_a_picture,
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    for (Enhancer enhancer : mEnhancers) {
                        picture = enhancer.enhance(picture);
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