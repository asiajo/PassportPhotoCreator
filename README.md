# PassportPhotoCreator

## Description

Application supporting passport photos creation with use of Deep Neural Networks. Application:
- [x] detects face -> ML Kit Face Detection,
- [x] verifies if the head is oriented straight, eyes are open and facial expression is neutral -> ML Kit Face Detection,
- [x] segments person from the background -> Tensorflow lite [munet](https://github.com/tensorflow/examples/tree/master/lite/examples/image_segmentation/android) model
- [x] verifies if background is plain and bright -> OpenCV
- [ ] TBD: verifies if face does not contain shadows -> OpenCv
- [ ] TBD: verifies if eyes are directed towards camera -> Tensorflow lite
- [ ] TBD: After photo capturing attempts to improve face shadows -> OpenCv
- [ ] TBD: After photo capturing attempts to improve background -> Tensorflow lite GAN [pix2pix](https://github.com/affinelayer/pix2pix-tensorflow) model
- [x] Cuts the photo to correct format and saves it to the phone.

## Snapshots

TBD