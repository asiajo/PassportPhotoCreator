package org.joanna.thesis.passportphotocreator;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import org.joanna.thesis.passportphotocreator.camera.CameraSource;
import org.joanna.thesis.passportphotocreator.camera.CameraSourcePreview;
import org.joanna.thesis.passportphotocreator.camera.Graphic;
import org.joanna.thesis.passportphotocreator.camera.GraphicOverlay;
import org.joanna.thesis.passportphotocreator.processing.Verifier;
import org.joanna.thesis.passportphotocreator.processing.background.verification.BackgroundVerifier;
import org.joanna.thesis.passportphotocreator.processing.face.FaceTracker;
import org.joanna.thesis.passportphotocreator.processing.light.verification.ShadowVerification;
import org.joanna.thesis.passportphotocreator.processing.visibility.FaceUncoveredVerification;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
import static com.google.mlkit.vision.face.FaceDetectorOptions.CLASSIFICATION_MODE_ALL;
import static com.google.mlkit.vision.face.FaceDetectorOptions.LANDMARK_MODE_NONE;
import static com.google.mlkit.vision.face.FaceDetectorOptions.PERFORMANCE_MODE_FAST;
import static org.joanna.thesis.passportphotocreator.utils.ImageUtils.getFaceMatFromPictureTaken;
import static org.joanna.thesis.passportphotocreator.utils.PPCUtlis.makeCenteredToast;

public class CameraFragment extends Fragment implements View.OnClickListener {

    public static final int PREVIEW_WIDTH  = 480;
    public static final int PREVIEW_HEIGHT = 640;

    private static final String TAG = PhotoMakerActivity.class.getSimpleName();

    private static final int      REQUEST_EXTERNAL_STORAGE = 1;
    private static final String[] PERMISSIONS_STORAGE      =
            {Manifest.permission.WRITE_EXTERNAL_STORAGE};

    static {
        OpenCVLoader.initDebug();
    }

    private ScaleGestureDetector    mScaleGestureDetector;
    private CameraSource            mCameraSource;
    private CameraSourcePreview     mPreview;
    private GraphicOverlay<Graphic> mGraphicOverlay;
    private FaceTracker             mFaceTracker;
    private List<Verifier>          mVerifiers;
    private PhotoSender             photoSender;
    private FaceDetector            mDetectorVideo;
    private com.google.android.gms.vision.face.FaceDetector mDetectorPhoto;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        photoSender = (PhotoSender) context;
    }

    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_camera, container, false);
    }

    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mPreview = view.findViewById(R.id.preview);
        {
            Button buttonTakePicture = view.findViewById(
                    R.id.take_photo_button);
            buttonTakePicture.setOnClickListener(this);

            mGraphicOverlay = view.findViewById(R.id.graphicOverlay);
            mVerifiers = new ArrayList<>();
            mVerifiers.add(new ShadowVerification(
                    getActivity(), mGraphicOverlay));
            mVerifiers.add(new FaceUncoveredVerification(
                    getActivity(), mGraphicOverlay));
            try {
                mVerifiers.add(new BackgroundVerifier(
                        getActivity(), mGraphicOverlay));
            } catch (IOException e) {
                Toast.makeText(
                        getActivity(),
                        R.string.no_background_verification_error,
                        Toast.LENGTH_LONG).show();
            }
        }
        mScaleGestureDetector = new ScaleGestureDetector(
                getActivity(),
                new CameraFragment.ScaleListener());
        view.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                mScaleGestureDetector.onTouchEvent(motionEvent);
                return true;
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        for (Verifier verifier : mVerifiers) {
            verifier.close();
        }
        if (mCameraSource != null) {
            mCameraSource.release();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        createCameraSource();
        startCameraSource();
    }

    @Override
    public void onPause() {
        super.onPause();
        mPreview.release();
        mFaceTracker.clear();
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
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.take_photo_button:
                try {
                    takePhoto();
                } catch (Exception e) {
                    makeCenteredToast(
                            getActivity(),
                            R.string.picture_making_failed,
                            Toast.LENGTH_LONG).show();
                }
                break;
        }
    }

    private void createCameraSource() {
        Context context = getActivity().getApplicationContext();

        getActivity().setRequestedOrientation(SCREEN_ORIENTATION_PORTRAIT);

        FaceDetectorOptions options =
                new FaceDetectorOptions.Builder()
                        .setPerformanceMode(PERFORMANCE_MODE_FAST)
                        .setContourMode(LANDMARK_MODE_NONE)
                        .setClassificationMode(CLASSIFICATION_MODE_ALL)
                        .build();

        mDetectorPhoto = new com.google.android.gms.vision
                .face.FaceDetector.Builder(context)
                .setProminentFaceOnly(true)
                .setMode(com.google.android.gms.vision
                        .face.FaceDetector.ACCURATE_MODE)
                .build();

        mDetectorVideo = FaceDetection.getClient(options);

        mFaceTracker = new FaceTracker(mGraphicOverlay, context);

        CameraSource.Builder builder = new CameraSource
                .Builder(context, mDetectorVideo)
                .setFacing(CameraSource.CAMERA_FACING_BACK)
                .setRequestedPreviewSize(PREVIEW_HEIGHT, PREVIEW_WIDTH)
                .setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)
                .setVerifiers(mVerifiers)
                .setFaceDetector(mFaceTracker)
                .setRequestedFps(15.0f);

        mCameraSource = builder.build();

    }

    private void requestStoragePermissions() {
        int permission = ActivityCompat.checkSelfPermission(
                getActivity(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    getActivity(),
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE);
        }
    }

    private void takePhoto() {
        if (null == mCameraSource || null == mFaceTracker ||
                cannotMakePicture(null == mFaceTracker.getFaces() ||
                        mFaceTracker.getFaces().size() != 1)) {
            return;
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            requestStoragePermissions();
        }
        Toast toast = makeCenteredToast(getActivity(),
                R.string.wait_for_a_picture, Toast.LENGTH_LONG);
        toast.show();

        mCameraSource.takePicture(null, new CameraSource.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] bytes) {
                toast.cancel();
                Mat picture = getFaceMatFromPictureTaken(bytes, mDetectorPhoto);
                if (picture == null) {
                    Toast.makeText(
                            getActivity(),
                            R.string.cannot_make_a_picture,
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                photoSender.setPhoto(picture);
                mPreview.stop();
                photoSender.displayPreviewFragment();
            }
        });
    }


    private boolean cannotMakePicture(final boolean condition) {
        if (condition) {
            makeCenteredToast(
                    getActivity(),
                    R.string.cannot_make_a_picture,
                    Toast.LENGTH_LONG).show();
        }
        return condition;
    }

    public interface PhotoSender {

        void setPhoto(Mat pict);

        void displayPreviewFragment();
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
