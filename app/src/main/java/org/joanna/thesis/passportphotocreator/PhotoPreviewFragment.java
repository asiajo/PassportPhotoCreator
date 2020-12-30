package org.joanna.thesis.passportphotocreator;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import org.joanna.thesis.passportphotocreator.utils.ImageUtils;
import org.opencv.core.Mat;

import java.io.IOException;

import static org.joanna.thesis.passportphotocreator.utils.ImageUtils.getBitmapFromMat;
import static org.joanna.thesis.passportphotocreator.utils.ImageUtils.getMultiplePhotosOnOnePaper;

public class PhotoPreviewFragment extends Fragment
        implements View.OnClickListener, View.OnTouchListener {
    private static final float MAX_ZOOM      = 6.0f;
    private static final float MIN_ZOOM      = 1.0f;
    private static final float INCH          = 2.54f;
    private static final int   DPI_600_CM_10 = (int) (600 * 10 / INCH);
    private static final int   DPI_600_CM_15 = (int) (600 * 15 / INCH);

    private PhotoReceiver photoReceiver;
    private Bitmap        mImage;
    private ImageView     mImageView;
    private Mat           picture;
    private Matrix        matrix         = new Matrix();
    private Matrix        savedMatrix    = new Matrix();
    private TouchAction   mode           = TouchAction.NONE;
    private PointF        start          = new PointF();
    private PointF        mid            = new PointF();
    private float         oldDist        = 1f;
    private float         startingWidth  = 0f;
    private float         startingHeight = 0f;
    private float[]       valuesStart    = new float[9];
    private float[]       values         = new float[9];


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        photoReceiver = (PhotoReceiver) context;
        picture = photoReceiver.getPhoto();
        mImage = getBitmapFromMat(picture);
        startingWidth = mImage.getWidth();
        startingHeight = mImage.getHeight();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(
                R.layout.fragment_photo_preview, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Button buttonSave = view.findViewById(R.id.save_photo);
        buttonSave.setOnClickListener(this);
        Button buttonCancel = view.findViewById(R.id.cancel);
        buttonCancel.setOnClickListener(this);
        Button buttonPictures = (Button) view.findViewById(R.id.many_photos);
        buttonPictures.setOnClickListener(this);

        mImageView = view.findViewById(R.id.view_photo);
        mImageView.setImageBitmap(mImage);
        mImageView.setOnTouchListener(this);
        mImageView.setScaleType(ImageView.ScaleType.MATRIX);
        matrix = mImageView.getImageMatrix();
        savedMatrix = mImageView.getImageMatrix();
        matrix.getValues(values);
    }

    @Override
    public void onClick(final View v) {

        if (v.getId() == R.id.cancel) {
            photoReceiver.displayCameraFragment();
        }
        if (v.getId() == R.id.save_photo || v.getId() == R.id.many_photos) {
            if (v.getId() == R.id.many_photos) {
                mImage = getMultiplePhotosOnOnePaper(DPI_600_CM_10,
                        DPI_600_CM_15, picture);
            }
            try {
                String fileName = ImageUtils.saveImage(mImage, getActivity());
                Toast.makeText(
                        getActivity(), R.string.image_saved,
                        Toast.LENGTH_SHORT).show();
                photoReceiver.finishWithResult(fileName);
            } catch (IOException e) {
                Toast.makeText(
                        getActivity(), R.string.image_not_saved,
                        Toast.LENGTH_LONG).show();
                e.printStackTrace();
            }
        }
    }


    @Override
    public boolean onTouch(View view, MotionEvent event) {
        matrix.getValues(valuesStart);
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                savedMatrix.set(matrix);
                mode = TouchAction.DRAG;
                start.set(event.getX(), event.getY());
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                savedMatrix.set(matrix);
                mode = TouchAction.ZOOM;
                oldDist = spacing(event);
                setMidPoint(mid, event);
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                mode = TouchAction.NONE;
                break;
            case MotionEvent.ACTION_MOVE:
                if (mode == TouchAction.DRAG) {
                    matrix.set(savedMatrix);
                    matrix.postTranslate(
                            event.getX() - start.x,
                            event.getY() - start.y);
                } else if (mode == TouchAction.ZOOM) {
                    matrix.set(savedMatrix);
                    float scale = spacing(event) / oldDist;
                    matrix.postScale(scale, scale, mid.x, mid.y);
                }
                break;
        }
        correctThePlacement(((ImageView) view).getDrawable().getBounds());
        ((ImageView) view).setImageMatrix(matrix);
        return true;
    }

    private void correctThePlacement(Rect bounds) {
        matrix.getValues(values);
        float scaleX = values[Matrix.MSCALE_X];
        float scaleY = values[Matrix.MSCALE_Y];
        float width = (bounds.right - bounds.left) * scaleX;
        float height = (bounds.bottom - bounds.top) * scaleY;
        if (!isZoomWithinRange(scaleX, scaleY)) {
            matrix.setValues(valuesStart);
        }
        matrix.getValues(values);
        alignToRight(values[Matrix.MTRANS_X], width);
        alignToHeight(values[Matrix.MTRANS_Y], height);
        matrix.getValues(values);
        alignToLeft(values[Matrix.MTRANS_X]);
        alignToTop(values[Matrix.MTRANS_Y]);
    }

    private void alignToRight(float theX, float theWidth) {
        if (theX + theWidth <= startingWidth) {
            values[Matrix.MTRANS_X] = startingWidth - theWidth;
            matrix.setValues(values);
        }
    }

    private void alignToHeight(float theY, float theHeight) {
        if (theY + theHeight <= startingHeight) {
            values[Matrix.MTRANS_Y] = startingHeight - theHeight;
            matrix.setValues(values);
        }
    }

    private void alignToLeft(float theX) {
        if (theX > 0) {
            values[Matrix.MTRANS_X] = 0;
            matrix.setValues(values);
        }
    }

    private void alignToTop(float theY) {
        if (theY > 0) {
            values[Matrix.MTRANS_Y] = 0;
            matrix.setValues(values);
        }
    }

    private Boolean isZoomWithinRange(float scaleX, float scaleY) {
        float scaleX0 = valuesStart[Matrix.MSCALE_X];
        float scaleY0 = valuesStart[Matrix.MSCALE_Y];
        if ((scaleX > MAX_ZOOM || scaleY > MAX_ZOOM) &&
                (scaleX > scaleX0 || scaleY > scaleY0)) {
            return false;
        }
        return (!(scaleX < MIN_ZOOM) && !(scaleY < MIN_ZOOM)) ||
                (!(scaleX < scaleX0) && !(scaleY < scaleY0));
    }

    private float spacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }

    private void setMidPoint(PointF point, MotionEvent event) {
        float x = event.getX(0) + event.getX(1);
        float y = event.getY(0) + event.getY(1);
        point.set(x / 2, y / 2);
    }

    private enum TouchAction {
        NONE,
        DRAG,
        ZOOM
    }

    public interface PhotoReceiver {
        Mat getPhoto();

        void displayCameraFragment();

        void finishWithResult(String fileName);
    }
}