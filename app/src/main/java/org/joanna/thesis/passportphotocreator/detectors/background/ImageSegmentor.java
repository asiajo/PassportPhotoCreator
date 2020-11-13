package org.joanna.thesis.passportphotocreator.detectors.background;

import android.app.Activity;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.util.Log;

import org.joanna.thesis.passportphotocreator.utils.ImageUtils;
import org.opencv.core.Mat;
import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

/**
 * Segments images with Tensorflow Lite.
 */
public abstract class ImageSegmentor {

    public static final int PROCESS_IMG_SIZE = 448;

    private static final String TAG = ImageSegmentor.class.getSimpleName();

    private static final int DIM_PIXEL_SIZE = 3;

    /** Options for configuring the Interpreter. */
    private final Interpreter.Options mTfliteOptions =
            new Interpreter.Options();
    /** An instance of the driver class to run model inference with Tfl Lite. */
    protected     Interpreter         mTflite;
    /** A ByteBuffer to hold image data, to be feed into Tfl Lite as inputs. */
    protected     ByteBuffer          mImgData;
    /** Mat holding the image for processing. */
    protected     Mat                 mImage;
    /** Preallocated buffers for storing image data in. */
    private       int[]               mIntValues;
    /** The loaded TensorFlow Lite model. */
    private       MappedByteBuffer    mTfliteModel;

    ImageSegmentor(Activity activity) throws IOException {
        mTfliteModel = loadModelFile(activity);
        mTflite = new Interpreter(mTfliteModel, mTfliteOptions);
        mIntValues = new int[getImageSizeX() * getImageSizeY()];
        mImgData = ByteBuffer.allocateDirect(
                getImageSizeX() * getImageSizeY()
                        * DIM_PIXEL_SIZE
                        * getNumBytesPerChannel());
        mImgData.order(ByteOrder.nativeOrder());
        Log.d(TAG, "Created a Tensorflow Lite Image Segmentor.");
    }

    /**
     * Segments a frame from the preview stream.
     */
    public Mat segmentImgGetBackground(Mat image) {
        if (mTflite == null) {
            Log.e(TAG, "Image segmentor has not been initialized; Skipped.");
        }
        mImage = image;

        Mat resizedMat128 = ImageUtils.resizeMat(image, getImageSizeX(),
                getImageSizeY());
        Bitmap tmp = ImageUtils.getBitmapFromMat(resizedMat128);
        resizedMat128.release();
        Bitmap bmp = tmp.copy(tmp.getConfig(), true);
        ImageUtils.safelyRemoveBitmap(tmp);
        convertBitmapToByteBuffer(bmp);
        ImageUtils.safelyRemoveBitmap(bmp);

        runInference();
        return getBackground();
    }

    /**
     * Returns mask hiding background, in the size of input image (but padded
     * to squere!) and in CV_32F color scheme.
     *
     * @return mask
     */
    public abstract Mat getMaskedBackground();

    /**
     * Returns mask hiding person, in the size of input image (but padded
     * to squere!) and in CV_32F color scheme.
     *
     * @return mask
     */
    public abstract Mat getMaskedPerson();

    /**
     * Closes tflite to release resources.
     */
    public void close() {
        mTflite.close();
        mTflite = null;
        mTfliteModel = null;
    }

    /**
     * Memory-map the model file in Assets.
     */
    private MappedByteBuffer loadModelFile(Activity activity)
            throws IOException {
        AssetFileDescriptor fileDescriptor =
                activity.getAssets().openFd(getModelPath());
        FileInputStream inputStream =
                new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset,
                declaredLength);
    }

    /**
     * Writes Image data into a {@code ByteBuffer}.
     */
    private void convertBitmapToByteBuffer(Bitmap bitmap) {
        if (mImgData == null) {
            return;
        }
        mImgData.rewind();
        bitmap.getPixels(mIntValues, 0, bitmap.getWidth(), 0, 0,
                bitmap.getWidth(), bitmap.getHeight());
        // Convert the image to floating point.
        int pixel = 0;
        for (int i = 0; i < getImageSizeX(); ++i) {
            for (int j = 0; j < getImageSizeY(); ++j) {
                final int val = mIntValues[pixel++];
                addPixelValue(val);
            }
        }
    }


    /**
     * Get the name of the model file stored in Assets.
     *
     * @return
     */
    protected abstract String getModelPath();

    /**
     * Get the image size along the x axis.
     *
     * @return
     */
    protected abstract int getImageSizeX();

    /**
     * Get the image size along the y axis.
     *
     * @return
     */
    protected abstract int getImageSizeY();

    /**
     * Get the number of bytes that is used to store a single color channel
     * value.
     *
     * @return
     */
    protected abstract int getNumBytesPerChannel();

    /**
     * Add pixelValue to byteBuffer.
     *
     * @param pixelValue
     */
    protected abstract void addPixelValue(int pixelValue);

    /**
     * Executes call to the network.
     */
    protected abstract void runInference();

    /**
     * Returns original image with detected person painted black.
     *
     * @return image with person painted black
     */
    protected abstract Mat getBackground();

    /**
     * Returns original image with detected background painted black.
     *
     * @return image with background painted black
     */
    protected abstract Mat getForeground();
}
