# Image Processing Library
This is a small library for image processing. It currently contains the following features.

* Grayscale image processing
    * Histogram generation
    * Binary image generation using Otsu's method or a specified threshold
* Binary image processing
    * Inversion
    * Erosion
    * Counting connected components (4-connected neighborhood)
* Edge detection
    * Canny's edge detection
* Photometric stereo

## Building
To compile the project, navigate to the root directory of the project and run the following command:

```
javac -cp lib/commons-math3-3.2.jar src/edu/hawaii/ttaomae/imageprocessing/*.java
```

## Usage and Documentation
Each class and method is documented in the source code. In the future I hope to include simple examples of how to use each feature.

## Future Features
The following is a list of features which I plan to implement:

* Binary image processing
    * Dilation
    * Opening
    * Closing
* Edge detection
    * Prewitt and Sobel operators