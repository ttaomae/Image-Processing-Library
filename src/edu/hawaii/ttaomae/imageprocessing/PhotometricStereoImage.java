package edu.hawaii.ttaomae.imageprocessing;

import java.awt.image.BufferedImage;
import java.io.IOException;

/**
 * A single image of a photometric stereo image set.
 *
 * @author Todd Taomae
 */
public class PhotometricStereoImage
{
    private double[][] intensities;
    private double[] lightSourceDirection;

    /**
     * Constructs a PhotometricStereoImage from the specified filename, with the specified (x, y, z)
     * light source direction.
     * 
     * @param filename name of image to load
     * @param xLight the x direction of light
     * @param yLight the y direction of light
     * @param zLight the z direction of light
     * @throws IOException if the image could not be loaded
     */
    public PhotometricStereoImage(String filename,
                                  double xLight, double yLight, double zLight) throws IOException
    {
        BufferedImage image = ImageUtils.loadImage(filename);
        this.intensities = new double[image.getHeight()][image.getWidth()];

        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                double intensity = ImageUtils.getGrayscaleValue(image.getRGB(x, y)) / 255.0;
                this.intensities[y][x] = intensity;
            }
        }

        this.lightSourceDirection = new double[] { xLight, yLight, zLight };
    }

    /**
     * Returns the width of the image.
     *
     * @return the width of the image
     */
    public int getWidth()
    {
        return this.intensities[0].length;
    }

    /**
     * Returns the height of the image.
     *
     * @return the height of the image
     */
    public int getHeight()
    {
        return this.intensities.length;
    }

    /**
     * Returns the normalized intensity of the image at the specified pixel.
     *
     * @param x the x coordinate of the pixel from which to get the intensity
     * @param y the y coordinate of the pixel from which to get the intensity
     * @return the intensity at the specified pixel
     */
    public double getIntensity(int x, int y)
    {
        return this.intensities[y][x];
    }

    /**
     * Returns the light source direction as an array of doubles, where the first element is the x
     * direction, the second element is the y direction, and the third element is the z direction.
     *
     * @return the light source direction as an array of doubles
     */
    public double[] getLightSourceDirection()
    {
        return this.lightSourceDirection.clone();
    }
}
