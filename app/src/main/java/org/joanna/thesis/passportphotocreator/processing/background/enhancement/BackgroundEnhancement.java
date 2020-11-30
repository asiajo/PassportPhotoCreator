package org.joanna.thesis.passportphotocreator.processing.background.enhancement;

import android.app.Activity;

import org.joanna.thesis.passportphotocreator.processing.Enhancer;
import org.joanna.thesis.passportphotocreator.processing.background.BackgroundUtils;
import org.joanna.thesis.passportphotocreator.processing.background.ImageSegmentor;
import org.joanna.thesis.passportphotocreator.processing.background.ImageSegmentorFloatMobileUnet;
import org.joanna.thesis.passportphotocreator.processing.background.verification.BackgroundProperties;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;

public class BackgroundEnhancement implements Enhancer {

    private static final String TAG =
            BackgroundEnhancement.class.getSimpleName();

    public  ImageSegmentor       mSegmentor;
    private BackgroundProperties mBackgroundProperties;


    public BackgroundEnhancement(final Activity activity) throws IOException {
        mSegmentor = new ImageSegmentorFloatMobileUnet(activity);
        mBackgroundProperties = new BackgroundProperties();
    }

    @Override
    public boolean verify(final Mat src) {
        Mat background = BackgroundUtils.getBackground(src, mSegmentor);
        return BackgroundUtils.isUniform(
                background, mBackgroundProperties, mSegmentor) < 3;
    }

    @Override
    public Mat enhance(final Mat src) {

        if (mSegmentor == null) {
            return null;
        }
        Mat personMask = BackgroundUtils.getPersonMask(src, mSegmentor);

        Mat background = src.clone();
        Mat person = src.clone();
        Imgproc.blur(background, background, new Size(15, 15));
        background.convertTo(background, -1, 1, 3); //brighten
        person.copyTo(background, personMask);
        person.release();
        personMask.release();

        return background;
    }

    @Override
    public void close() {
        if (mSegmentor != null) {
            mSegmentor.close();
        }
    }
}
