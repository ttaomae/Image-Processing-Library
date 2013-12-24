package edu.hawaii.ttaomae.imageprocessing;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

/**
 * Provides static helper methods to process grayscale images.
 *
 * @author Todd Taomae
 */
public class ImageUtils
{
    /**
     * Loads an image with the specified filename and returns it as a BufferedImage.
     *
     * @param filename name of image to load
     * @throws IOException if there was an error loading the image
     */
    public static BufferedImage loadImage(String filename) throws IOException
    {
        return ImageIO.read(new File(filename));
    }

    /**
     * Saves the specified BufferedImage with the specified filename using the extension to
     * determine the file type. The extension is assumed to be the substring following the last
     * period (.) in the filename. If there is no extension, "png" is used by default.
     *
     * @param image image to save
     * @param filename filename to save as
     * @throws IOException if there was an error saving the image
     */
    public static void saveImage(BufferedImage image, String filename) throws IOException
    {
        String extension = getFileExtension(filename);
        extension = (!extension.isEmpty()) ? extension : "png";

        ImageIO.write(image, extension, new File(filename));

    }

    /**
     * Returns the grayscale value of the specified ARGB value. The ARGB value is assumed to be a
     * grayscale value (i.e. red, green, and blue components are the same) so the value of the blue
     * component is extracted and returned.
     *
     * @param argb the ARGB value to find the grayscale value of
     * @return the grayscale value
     */
    public static int getGrayscaleValue(int argb)
    {
        // R, G, and B values should be the same for grayscale image
        // keep only B value
        return argb & 0x000000ff;
    }

    /**
     * Returns the ARGB value of the specified grayscale value. Assumes that the grayscale value is
     * between 0 and 255.
     *
     * @param grayscale the grayscale value to find the argb value of
     * @return the argb value
     * @throws IllegalArgumentException if the grayscale value is not between 0 and 255, inclusive
     */
    public static int getRgbValue(int grayscale) throws IllegalArgumentException
    {
        if (grayscale < 0 || grayscale > 255) {
            throw new IllegalArgumentException("grayscale value must be between 0 and 255.");
        }
        return new Color(grayscale, grayscale, grayscale).getRGB();
    }

    /**
     * Returns the extension of the specified filename. The extension is assumed to be everything
     * after the last period in the filename. If there are no periods in the filename, then an empty
     * string is returned.
     * 
     * @param filename the filename to find the extension of
     * @return the extension of the specified filename
     */
    public static String getFileExtension(String filename)
    {
        int indexOfLastDot = filename.lastIndexOf('.');
        return (indexOfLastDot != -1) ? filename.substring(indexOfLastDot + 1) : "";
    }

    /**
     * Returns the name part of the specified filename. The name part is assumed to be everything
     * before the last period in the filename. If there are no periods in the filename, then the
     * entire filename is returned.
     * 
     * @param filename the filename to find the name part of
     * @return the name part of the given filename
     */
    public static String getFileNamePart(String filename)
    {
        int indexOfLastDot = filename.lastIndexOf('.');
        return (indexOfLastDot != -1) ? filename.substring(0, indexOfLastDot) : filename;
    }

    /**
     * Returns 1 if the value is white and 0 otherwise.
     */
    public static int getBinaryValue(int argb)
    {
        return (argb & 0x000000ff) == 0 ? 0 : 1;
    }
}