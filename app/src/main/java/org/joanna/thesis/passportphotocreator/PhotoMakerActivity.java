package org.joanna.thesis.passportphotocreator;


import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import com.google.android.gms.vision.face.FaceDetector;
import com.google.android.gms.vision.face.LargestFaceFocusingProcessor;
import com.google.android.material.snackbar.Snackbar;

import org.joanna.thesis.passportphotocreator.camera.CameraSource;
import org.joanna.thesis.passportphotocreator.camera.CameraSourcePreview;
import org.joanna.thesis.passportphotocreator.camera.GraphicOverlay;
import org.joanna.thesis.passportphotocreator.detectors.face.FaceGraphic;
import org.joanna.thesis.passportphotocreator.detectors.face.FaceTracker;

import java.io.IOException;

import static com.google.android.material.snackbar.Snackbar.make;

public class PhotoMakerActivity extends Activity
        implements View.OnClickListener {


    private static final String TAG                   =
            PhotoMakerActivity.class.getSimpleName();
    // permission request codes need to be < 256
    private static final int    RC_HANDLE_CAMERA_PERM = 2;

    private CameraSource                mCameraSource;
    private CameraSourcePreview         mPreview;
    private GraphicOverlay<FaceGraphic> mGraphicOverlay;
    private FaceTracker                 faceTracker;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.photo_capture);

        mPreview = findViewById(R.id.preview);
        mGraphicOverlay = findViewById(R.id.graphicOverlay);

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

    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

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
        FaceDetector detector = new FaceDetector.Builder(context)
                .setLandmarkType(FaceDetector.ALL_LANDMARKS)
                .setClassificationType(FaceDetector.ALL_CLASSIFICATIONS)
                .setProminentFaceOnly(true)
                .build();

        faceTracker = new FaceTracker(mGraphicOverlay, context);
        detector.setProcessor(
                new LargestFaceFocusingProcessor(detector, faceTracker));

        if (!detector.isOperational()) {
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
                .Builder(context, detector)
                .setFacing(CameraSource.CAMERA_FACING_BACK)
                .setRequestedPreviewSize(640, 480)
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

    @Override
    protected void onPause() {
        super.onPause();
    }

}
