package org.joanna.thesis.passportphotocreator;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
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

public class PhotoPreviewFragment extends Fragment
        implements View.OnClickListener {

    private Bitmap mImage;
    private PhotoReceiver photoReceiver;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        photoReceiver = (PhotoReceiver) context;
        Mat picture = photoReceiver.getPhoto();
        mImage = getBitmapFromMat(picture);
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

        ImageView mImageView = view.findViewById(R.id.view_photo);
        mImageView.setImageBitmap(mImage);
        mImageView.setScaleType(ImageView.ScaleType.MATRIX);
    }

    @Override
    public void onClick(final View v) {
        if (v.getId() == R.id.cancel) {
            photoReceiver.displayCameraFragment();

        }
        if (v.getId() == R.id.save_photo) {
            try {
                String fileName = ImageUtils.saveImage(mImage, getActivity());
                photoReceiver.sendFileName(fileName);
                Toast.makeText(
                        getActivity(), R.string.image_saved,
                        Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                Toast.makeText(
                        getActivity(), R.string.image_not_saved,
                        Toast.LENGTH_LONG).show();
                e.printStackTrace();
            }
        }
    }

    public interface PhotoReceiver {

        Mat getPhoto();

        void displayCameraFragment();

        void sendFileName(String fileName);
    }
}