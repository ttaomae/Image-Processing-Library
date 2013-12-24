package edu.hawaii.ttaomae.imageprocessing;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;


/**
 * Provides methods to process binary images.
 *
 * @author Todd Taomae
 */
public class BinaryImageProcessor
{
    /** The ARGB value for black. */
    public static final int BLACK = 0xff000000;
    /** The ARGB value for white. */
    public static final int WHITE = 0xffffffff;


    private BufferedImage image;

    /**
     * Constructs a new BinaryImageProcessor from the specified image.
     *
     * @param image the image to construct the BinaryImageProcessor from
     */
    public BinaryImageProcessor(BufferedImage image)
    {
        this.image = image;
    }

    /**
     * Constructs a new BinaryImageProcessor from the image with the specified file name.
     *
     * @param filename the name of the image to load
     * @throws IOException if the file image could not be opened
     */
    public BinaryImageProcessor(String filename) throws IOException
    {
        this.image = ImageUtils.loadImage(filename);
    }

    /**
     * Returns an inverted version of the image.
     *
     * @return an inverted version of the image
     */
    public BufferedImage invert()
    {
        BufferedImage invertedImage = new BufferedImage(this.image.getWidth(),
                                                        this.image.getHeight(),
                                                        this.image.getType());

        for (int y = 0; y < this.image.getHeight(); y++) {
            for (int x = 0; x < this.image.getWidth(); x++) {
                if (ImageUtils.getBinaryValue(this.image.getRGB(x, y)) == 0) {
                    invertedImage.setRGB(x, y, WHITE);
                }
                else {
                    invertedImage.setRGB(x, y, BLACK);
                }
            }
        }

        return invertedImage;
    }

    /**
     * Erodes the image using the specified structuring element.
     *
     * @param structuringElement the structuring element used to erode the image
     * @return the eroded image
     */
    public BufferedImage erode(int[][] structuringElement)
    {
        BufferedImage erodedImage = new BufferedImage(this.image.getWidth(),
                                                      this.image.getHeight(),
                                                      this.image.getType());

        for (int y = 0; y < this.image.getHeight(); y++) {
            for (int x = 0; x < this.image.getWidth(); x++) {
                int radius = (structuringElement.length / 2) + 1;

                // if the mask falls outside the image
                if (y - (radius - 1) < 0 || x - (radius - 1) < 0
                    || y + (radius - 1) > this.image.getWidth() - 1
                    || x + (radius - 1) > this.image.getHeight() - 1) {
                    // set to black
                    erodedImage.setRGB(y, x, BLACK);
                }

                else {
                    // set pixel to white by default
                    erodedImage.setRGB(y, x, WHITE);

                    testMask: for (int i = -(radius - 1); i <= radius - 1; i++) {
                        for (int j = -(radius - 1); j <= radius - 1; j++) {
                            int argb = this.image.getRGB(y + i, x + j);
                            int binaryValue = ImageUtils.getBinaryValue(argb);

                            // if the mask doesn't match binary image
                            if (structuringElement[radius - 1 + i][radius - 1 + j] == 1
                                && binaryValue == 0) {
                                // set pixel to black
                                erodedImage.setRGB(y, x, BLACK);
                                break testMask;
                            }
                        }
                    }
                }
            }
        }

        return erodedImage;
    }


    /**
     * Returns the number of connected components in this image using four-neighbor connectivity.
     *
     * @return the number of connected components in this image
     */
    public int countConnectedComponents()
    {
        int height = this.image.getHeight();
        int width = this.image.getWidth();

        int[][] labels = new int[height][width];
        int nextLabel = 1;
        // the value at a given index is the parent of the label equal to the index
        ArrayList<Integer> parents = new ArrayList<Integer>();
        // add a value for index 0, since there is no label 0
        parents.add(0);

        // go through each pixel
        for (int y = 0; y < labels.length; y++) {
            for (int x = 0; x < labels[y].length; x++) {
                int binaryValue = ImageUtils.getBinaryValue(this.image.getRGB(x, y));

                // only consider foreground pixels
                if (binaryValue == 1) {
                    // label of the pixel above or to the left
                    // zero if there is no such pixel
                    int northLabel = (y - 1 >= 0) ? labels[y - 1][x] : 0;
                    int westLabel = (x - 1 >= 0) ? labels[y][x - 1] : 0;

                    int higherLabel = Math.max(northLabel, westLabel);
                    int lowerLabel = Math.min(northLabel, westLabel);

                    // if both pixels are not labeled
                    if (higherLabel == 0) {
                        // use new label
                        labels[y][x] = nextLabel;
                        // parent of new label is 0
                        parents.add(0);
                        nextLabel++;
                    }

                    // higher label must be non-zero
                    // if only one pixel is labeled
                    else if (lowerLabel == 0) {
                        labels[y][x] = higherLabel;
                    }

                    // lower label must be non-zero
                    // so both pixels are labeled
                    else {
                        // choose the lower of two labels
                        labels[y][x] = lowerLabel;
                        // if the labels are different
                        if (higherLabel != lowerLabel) {
                            // set lower label as parent of higher label
                            parents.set(higherLabel, lowerLabel);
                        }
                    }
                }
            }
        }

        int totalComponents = 0;
        // count labels
        for (int i = 1; i < parents.size(); i++) {
            // if the parent is not 0 it is part of a different set
            // System.out.println(i + ": " + parents.get(i));
            if (parents.get(i) == 0) {
                totalComponents++;
            }
        }

        return totalComponents;
    }
}
