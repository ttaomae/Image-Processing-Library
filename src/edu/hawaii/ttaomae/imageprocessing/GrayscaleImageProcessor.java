package edu.hawaii.ttaomae.imageprocessing;

import java.awt.image.BufferedImage;
import java.io.IOException;

/**
 * Provides methods to process greyscale images.
 *
 * @author Todd
 */
public class GrayscaleImageProcessor
{
    private BufferedImage image;

    /**
     * Constructs a new GrayscaleImageProcessor from the specified image.
     *
     * @param image the image to construct the BinaryImageProcessor from
     */
    public GrayscaleImageProcessor(BufferedImage image)
    {
        this.image = image;
    }

    /**
     * Constructs a new GrayscaleImageProcessor from the image with the specified file name.
     *
     * @param filename the name of the image to load
     * @throws IOException if the file image could not be opened
     */
    public GrayscaleImageProcessor(String filename) throws IOException
    {
        this.image = ImageUtils.loadImage(filename);
    }

    /**
     * Returns an int array which represents the histogram for the image.
     *
     * @return the histogram for the image
     */
    public int[] getHistogram()
    {
        int[] histogram = new int[256];

        for (int x = 0; x < this.image.getWidth(); x++) {
            for (int y = 0; y < this.image.getHeight(); y++) {
                int argb = this.image.getRGB(x, y);

                histogram[ImageUtils.getGrayscaleValue(argb)]++;
            }
        }

        return histogram;
    }

    /**
     * Returns the binary threshold of the image using Otsu's method.
     *
     * @return the binary threshold of the image
     */
    public int getOtsuThreshold()
    {
        int threshold = 0;
        double maxVariance = 0.0f;
        int[] histogram = getHistogram();

        // calculate probability of each value in the histogram
        double[] probabilities = new double[histogram.length];
        int totalPixels = this.image.getWidth() * this.image.getHeight();

        for (int i = 0; i < probabilities.length; i++) {
            probabilities[i] = (double) histogram[i] / (double) totalPixels;
        }

        // total weighted sum
        double totalSum = 0.0f;
        for (int i = 0; i < probabilities.length; i++) {
            totalSum += i * probabilities[i];
        }
        // weighted sum for group 1
        double groupSum1 = 0.0f;


        // group probabilities
        double groupProbability1 = 0.0f;
        double groupProbability2 = 1.0f;


        // check each value for maximum variance
        for (int t = 0; t < 256; t++) {
            groupProbability1 += probabilities[t];

            // if probability is 0, ignore and get next value
            if (groupProbability1 == 0.0f) {
                continue;
            }

            groupProbability2 = 1.0f - groupProbability1;


            groupSum1 += t * probabilities[t];

            double groupMean1 = groupSum1 / groupProbability1;
            double groupMean2 = (totalSum - groupSum1) / groupProbability2;

            // calculate between group variance
            double variance = groupProbability1 * groupProbability2
                              * Math.pow(groupMean1 - groupMean2, 2);

            // check for new maximum
            if (variance > maxVariance) {
                maxVariance = variance;
                threshold = t;
            }
        }

        return threshold;
    }

    /**
     * Returns a binarized version of the image using the specified threshold.
     * 
     * @param threshold threshold between background and foreground pixels
     * @return a binarized version of the image
     */
    public BufferedImage getBinaryImage(int threshold)
    {
        BufferedImage binaryImage = new BufferedImage(this.image.getWidth(),
                                                      this.image.getHeight(),
                                                      this.image.getType());

        for (int y = 0; y < binaryImage.getHeight(); y++) {
            for (int x = 0; x < binaryImage.getWidth(); x++) {
                int argb = this.image.getRGB(x, y);

                if (ImageUtils.getGrayscaleValue(argb) > threshold) {
                    binaryImage.setRGB(x, y, BinaryImageProcessor.WHITE);
                }
                else {
                    binaryImage.setRGB(x, y, BinaryImageProcessor.BLACK);
                }
            }
        }

        return binaryImage;
    }

    /**
     * Returns a binarized version of the image using Otsu's threshold.
     *
     * @return a binarized version of the image
     */
    public BufferedImage getBinaryImage()
    {
        return getBinaryImage(getOtsuThreshold());
    }
}
