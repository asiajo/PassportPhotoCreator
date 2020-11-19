# PassportPhotoCreator

## Description

Application supporting passport photos creation with use of Deep Neural Networks. Application:
- [x] detects face -> ML Kit Face Detection,
- [x] verifies if the head is oriented straight, eyes are open and facial expression is neutral -> ML Kit Face Detection,
- [x] segments person from the background -> Tensorflow lite [munet](https://github.com/tensorflow/examples/tree/master/lite/examples/image_segmentation/android) model
- [x] verifies if background is plain and bright -> OpenCV
- [x] verifies if face does not contain shadows -> OpenCv
- [ ] TBD: verifies if eye pupils are directed towards camera -> Tensorflow lite
- [ ] TBD: verifies if there are no objects partially covering face -> OpenCV
- [x] After photo capturing attempts to improve face shadows -> Tensorflow lite GAN [pix2pix](https://github.com/affinelayer/pix2pix-tensorflow) model. See below!
- [ ] TBD: After photo capturing attempts to improve background -> OpenCv
- [x] Cuts the photo to correct format and saves it to the phone.

For enabling shadow removal please download a shadow removal model from [here](https://www.dropbox.com/s/lib09rdp7ku35o1/pix2pix.tflite?dl=0) and place it in assets folder.
Usage of the application without this model is possible, but shadows will not be corrected.
This model was taught using [pix2pix](https://github.com/affinelayer/pix2pix-tensorflow).
Training data was generated with help of [DPR](https://github.com/zhhoper/DPR) and
[face-seg](https://github.com/kampta/face-seg). Link to the code for data generation to follow. (TODO!)

## Snapshots

TBD