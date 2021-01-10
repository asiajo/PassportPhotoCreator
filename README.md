# PassportPhotoCreator

## Description

Application supporting passport photos creation with use of Deep Neural Networks. Application:
- [x] detects face -> ML Kit Face Detection,
- [x] verifies if the head is oriented straight, eyes are open and facial expression is neutral -> ML Kit Face Detection,
- [x] segments person from the background -> TensorFlow lite [munet](https://github.com/tensorflow/examples/tree/master/lite/examples/image_segmentation/android) model,
- [x] verifies if background is plain and bright -> OpenCV,
- [x] verifies if face does not contain shadows -> TensorFlow, OpenCv,
- [ ] TBD: verifies if eye pupils are directed towards camera,
- [x] verifies if there are no objects partially covering face -> OpenCV,
- [x] after photo capturing attempts to improve face shadows -> TensorFlow lite GAN [pix2pix](https://github.com/affinelayer/pix2pix-tensorflow) model. See below!
- [x] after photo capturing attempts to improve background -> OpenCv,
- [x] cuts the photo to correct format and saves it to the phone,
- [x] if chosen, taken picture is saved 8 times on 15x10 cm paper for convinient printing.

For enabling shadow removal please download a shadow removal model from [here](https://www.dropbox.com/s/lib09rdp7ku35o1/pix2pix.tflite?dl=0) and place it in assets folder.
Usage of the application without this model is possible, but shadows will not be corrected.
Shadow removal model was taught using [pix2pix](https://github.com/affinelayer/pix2pix-tensorflow).
Training data was generated with help of [DPR](https://github.com/zhhoper/DPR) and
[face-seg](https://github.com/kampta/face-seg). Link to the code for data generation to follow.



## Snapshots

<p align="center">
  <br>Consecutive previous of the application<br><br>
  <img src="https://user-images.githubusercontent.com/25400249/104119465-afa00400-532f-11eb-88ca-22aa300f8672.jpg" width="200"/>
  <img src="https://user-images.githubusercontent.com/25400249/104119163-060c4300-532e-11eb-9ac7-faaeca6c8b3c.jpg" width="200"/>
  <img src="https://user-images.githubusercontent.com/25400249/104119503-06a5d900-5330-11eb-87d6-b2b07e9ec465.jpg" width="200"/>
</p>
