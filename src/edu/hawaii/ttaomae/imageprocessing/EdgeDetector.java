package edu.hawaii.ttaomae.imageprocessing;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * Performs edge detection on grayscale images.
 *
 * @author Todd Taomae
 */
public class EdgeDetector
{
    private final String filename;
    private final int width;
    private final int height;
    private final int[][] grayscale;

    /**
     * Constructs a new EdgeDetector from the image with the specified filename.
     *
     * @param filename name of the image to perform edge detection on
     */
    public EdgeDetector(String filename) throws IOException
    {
        this.filename = filename;

        BufferedImage image = ImageUtils.loadImage(this.filename);
        this.width = image.getWidth();
        this.height = image.getHeight();

        // build array of grayscale values.
        this.grayscale = new int[this.height][this.width];
        for (int y = 0; y < this.height; y++) {
            for (int x = 0; x < this.width; x++) {
                this.grayscale[y][x] = ImageUtils.getGrayscaleValue(image.getRGB(x, y));
            }
        }
    }

    /**
     * Runs Canny's Edge Detection algorithm on the image with the specified filter size and sigma
     * (for blurring) and low and high thresholds (for hysteresis). Returns a BufferedImage for the
     * final edge detected image.
     *
     * @param filterSize size of the Gaussian filter used for blurring
     * @param sigma standard deviation
     * @param lowThreshold low threshold used for hysteresis
     * @param highThreshold high threshold used for hysteresis
     * @return the final edge detected image
     */
    public BufferedImage runCannyEdgeDetection(int filterSize, double sigma,
                               double lowThreshold, double highThreshold)
    {
        // blur
        int[][] blurred = gaussianBlur(filterSize, sigma);
        // edge detection
        Gradient[][] gradientMap = getGradientMap(blurred);
        // non-maximal suppression
        Gradient[][] suppressedGradients = accurateNonMaximalSuppression(gradientMap);
        // hysteresis
        Edge[][] edges = hysteresis(suppressedGradients, lowThreshold, highThreshold);

        return createImageFromEdges(edges);
    }

    /**
     * Returns an array of BufferedImages which are the intermediate and final results of running
     * Canny's Edge Detection. The first image is the blurred image
     *
     * @param filterSize size of the Gaussian filter used for blurring
     * @param sigma standard deviation
     * @param lowThreshold low threshold used for hysteresis
     * @param highThreshold high threshold used for hysteresis
     * @return an array of BufferedImages which are the results of Canny's Edge Detection
     */
    public BufferedImage[] getCannyIntermediateImages(int filterSize, double sigma,
                                                      double lowThreshold, double highThreshold)
    {
        BufferedImage[] results = new BufferedImage[4];
        // blur
        int[][] blurred = gaussianBlur(filterSize, sigma);
        // edge detection
        Gradient[][] gradientMap = getGradientMap(blurred);
        // non-maximal suppression
        Gradient[][] suppressedGradients = accurateNonMaximalSuppression(gradientMap);
        // hysteresis
        Edge[][] edges = hysteresis(suppressedGradients, lowThreshold, highThreshold);

        results[0] = createImageFromIntArray(blurred);
        results[1] = createImageFromGradient(gradientMap);
        results[2] = createImageFromGradient(suppressedGradients);
        results[3] = createImageFromEdges(edges);
        return results;
    }

    /**
     * Returns a normalized 2D Gaussian kernel based on the specified size and sigma.
     *
     * @param size height and width of the kernel
     * @param sigma standard deviation
     * @return a 2D array of doubles representing a Gaussian kernel
     */
    public static double[][] get2DGaussianKernel(int size, double sigma)
    {
        double[][] kernel = new double[size][size];
        double radius = size / 2;
        double twoSigmaSq = 2.0 * sigma * sigma;
        double sum = 0.0;

        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                double x = -radius + j;
                double y = -radius + i;

                double exponent = -(x * x + y * y) / (twoSigmaSq);
                kernel[i][j] = Math.exp(exponent) / (Math.PI * twoSigmaSq);
                sum += kernel[i][j];
            }
        }

        // normalize
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                kernel[i][j] /= sum;
            }
        }

        return kernel;
    }

    /**
     * Performs a Gaussian blur on the image using a kernel of the specified size and sigma. Returns
     * a 2D array of ints which are the grayscale values of the blurred image
     *
     * @param size height and width of the kernel
     * @param sigma standard deviation
     * @return a 2D array of ints representing the grayscale values of the blurred image
     */
    private int[][] gaussianBlur(int filterSize, double sigma)
    {
        int[][] blurred = new int[this.height][this.width];

        double[][] kernel = get2DGaussianKernel(filterSize, sigma);

        for (int y = 0; y < this.height; y++) {
            for (int x = 0; x < this.width; x++) {
                blurred[y][x] = applyFilter(this.grayscale, kernel, x, y);
            }
        }

        return blurred;
    }

    /**
     * Returns the Gradient of each pixel using Sobel's operator.
     *
     * @param blurred the blurred image as a 2D array of ints representing grayscale values
     * @return the gradient of each pixel in the image
     */
    private Gradient[][] getGradientMap(int[][] blurred)
    {
        Gradient[][] gradientMap = new Gradient[this.height][this.width];

        // for each pixel in the image
        for (int y = 0; y < this.height; y++) {
            for (int x = 0; x < this.width; x++) {
                // apply gradient filters
                double xGradient = applyFilter(blurred, EdgeDetectionOperator.SOBEL_X, x, y);
                double yGradient = applyFilter(blurred, EdgeDetectionOperator.SOBEL_Y, x, y);
                gradientMap[y][x] = new Gradient(xGradient, yGradient);
            }
        }

        return gradientMap;
    }

    /**
     * Performs a non-maximal suppression, by comparing a pixels gradient magnitude with an estimate
     * of the gradient magnitude of its neighboring subpixels. Subpixels are one unit away, in the
     * direction (or opposite direction) of the pixel. Bilinear interpolation is used to estimate
     * the gradient magnitude of the subpixel.
     *
     * @param gradientMap the non-suppressed gradients of an image
     * @return the gradient of only maximal edges
     */
    private static Gradient[][] accurateNonMaximalSuppression(Gradient[][] gradientMap)
    {
        int height = gradientMap.length;
        int width = gradientMap[0].length;
        Gradient[][] suppressedGradient = new Gradient[height][width];

        double maxMagnitude = gradientMap[0][0].getMagnitude();
        for (Gradient[] row : gradientMap) {
            for (Gradient g : row) {
                maxMagnitude = Math.max(maxMagnitude, g.getMagnitude());
            }
        }

        // go through each pixel
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                double subpixelMagnitudeA = getSubpixelMagnitude(gradientMap, x, y, true);
                double subpixelMagnitudeB = getSubpixelMagnitude(gradientMap, x, y, false);
                double currentMagnitude = gradientMap[y][x].getMagnitude();

                // maximum
                if (currentMagnitude >= subpixelMagnitudeA && currentMagnitude > subpixelMagnitudeB) {
                    suppressedGradient[y][x] = gradientMap[y][x];
                }
                // non-maximum
                else {
                    suppressedGradient[y][x] = new Gradient(0.0, 0.0);
                }
            }
        }

        return suppressedGradient;
    }


    /**
     * Returns the gradient magnitude of the subpixel which is one unit away from the pixel at (x,
     * y) in the direction (or opposite direction) of its gradient.
     * 
     * @param gradientMap the non-suppressed gradients of an image
     * @param x x coordinate
     * @param y y coordinate
     * @param positive true if you want the magnitude of the subpixel in the positive direction,
     *            false otherwise
     * @return the magnitude at the subpixel that is one unit away from the specified coordinates
     */
    private static double getSubpixelMagnitude(Gradient[][] gradientMap, int x, int y,
                                               boolean positive)
    {
        int height = gradientMap.length;
        int width = gradientMap[0].length;
        double angle = gradientMap[y][x].getDirection();

        // subpixel coordinates
        double xx;
        double yy;

        if (positive) {
            xx = x + Math.cos(angle);
            yy = y + Math.sin(angle);
        }
        else {
            xx = x - Math.cos(angle);
            yy = y - Math.sin(angle);
        }
        // neigboring pixel coordinates
        double x1 = Math.floor(xx);
        double x2 = Math.ceil(xx);
        double y1 = Math.floor(yy);
        double y2 = Math.ceil(yy);

        // check if each coordinate is valid
        boolean x1Valid = x1 >= 0 && x1 < width;
        boolean x2Valid = x2 >= 0 && x2 < width;
        boolean y1Valid = y1 >= 0 && y1 < height;
        boolean y2Valid = y2 >= 0 && y2 < height;

        // bilinear interpolation
        if (x1 != x2 && y1 != y2) {
            // if the coordinates are invalid (i.e. outside the image), use 0

            double magnitude11 = (x1Valid && y1Valid) ? gradientMap[(int)y1][(int)x1].getMagnitude() : 0.0;
            double magnitude12 = (x1Valid && y2Valid) ? gradientMap[(int)y2][(int)x1].getMagnitude() : 0.0;
            double magnitude21 = (x2Valid && y1Valid) ? gradientMap[(int)y1][(int)x2].getMagnitude() : 0.0;
            double magnitude22 = (x2Valid && y2Valid) ? gradientMap[(int)y2][(int)x2].getMagnitude() : 0.0;

            double weight11 = (x2 - xx) * (y2 - yy) * magnitude11;
            double weight12 = (x2 - xx) * (yy - y1) * magnitude12;
            double weight21 = (xx - x1) * (y2 - yy) * magnitude21;
            double weight22 = (xx - x1) * (yy - y1) * magnitude22;


            return weight11 + weight12 + weight21 + weight22;
            }
        // subpixel is same as pixel
        else if (x1 == x2 && y1 == y2) {
            return (x1Valid && y1Valid) ? gradientMap[(int) y1][(int) x1].getMagnitude() : 0.0;
        }
        // x1 == x2 && y1 != y2
        else if (x1 == x2) {
            // linear interpolation
            double magnitude1 = (x1Valid && y1Valid) ? gradientMap[(int)y1][(int)x1].getMagnitude() : 0.0;
            double magnitude2 = (x1Valid && y2Valid) ? gradientMap[(int)y2][(int)x1].getMagnitude() : 0.0;

            double weight1 = (y2 - yy) * magnitude1;
            double weight2 = (yy - y1) * magnitude2;

            return weight1 + weight2;
        }
        // x1 != x2 && y1 == y2
        else {
            // linear interpolation
            double magnitude1 = (x1Valid && y1Valid) ? gradientMap[(int)y1][(int)x1].getMagnitude() : 0.0;
            double magnitude2 = (x2Valid && y1Valid) ? gradientMap[(int)y1][(int)x2].getMagnitude() : 0.0;

            double weight1 = (x2 - xx) * magnitude1;
            double weight2 = (xx - x1) * magnitude2;

            return weight1 + weight2;
        }
    }

    /**
     * Performs a faster non-maximal suppression of the gradient map by quantizing gradient angles
     * to one of four angles (vertical, horizontal, positive/negative diagonal) then comparing with
     * neighboring pixels.
     *
     * @param gradientMap the non-suppressed gradients of an image
     * @return the gradient of only maximal edges
     */
    @SuppressWarnings("unused")
    private static Gradient[][] fastNonMaximalSuppression(Gradient[][] gradientMap)
    {
        int height = gradientMap.length;
        int width = gradientMap[0].length;
        Gradient[][] suppressedGradient = new Gradient[height][width];

        // go through each pixel
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                // neighboring pixel coordinates
                int leftPixelX = x;
                int leftPixelY = y;
                int rightPixelX = x;
                int rightPixelY = y;

                switch (gradientMap[y][x].getRoundedDirection()) {
                    case E_W:
                        leftPixelX--;
                        rightPixelX++;
                        break;
                    case N_S:
                        leftPixelY--;
                        rightPixelY++;
                        break;
                    case NE_SW:
                        leftPixelX++;
                        leftPixelY++;
                        rightPixelX--;
                        rightPixelY--;
                        break;
                    case NW_SE:
                        leftPixelX--;
                        leftPixelY++;
                        rightPixelX++;
                        rightPixelY--;
                        break;
                }

                // check if each coordinate if valid
                boolean leftXValid = leftPixelX >= 0 && leftPixelX < width;
                boolean rightXValid = rightPixelX >= 0 && rightPixelX < width;
                boolean leftYValid = leftPixelY >= 0 && leftPixelY < height;
                boolean rightYValid = rightPixelY >= 0 && rightPixelY < height;

                double currentPixelGradient = gradientMap[y][x].getMagnitude();
                // if a coordinate is invalid, the gradient is 0
                double leftPixelGradient = (!leftXValid || !leftYValid) ? 0
                        : gradientMap[leftPixelY][leftPixelX].getMagnitude();
                double rightPixelGradient = (!rightXValid || !rightYValid) ? 0
                        : gradientMap[rightPixelY][rightPixelX].getMagnitude();


                // maximum
                if (currentPixelGradient >= leftPixelGradient
                    && currentPixelGradient > rightPixelGradient) {
                    suppressedGradient[y][x] = gradientMap[y][x];
                }
                // non-maximum
                else {
                    suppressedGradient[y][x] = new Gradient(0.0, 0.0);
                }
            }
        }

        return suppressedGradient;
    }

    /**
     * Performs hysteresis on the specified suppressed gradients. If the gradient is above the high
     * threshold it is a strong edge. If it is between the low and high threshold, it is a weak edge
     * if it is connected to a strong edge or another weak edge otherwise it is a dropped edge. If
     * it is below the low threshold it is not an edge.
     *
     * @param suppressedGradients the suppressed gradient of an image
     * @param lowThreshold threshold for weak edges
     * @param highThreshold threshold for strong edges
     * @return a 2D array of edges
     */
    private static Edge[][] hysteresis(Gradient[][] suppressedGradients, double lowThreshold,
                                double highThreshold)
    {
        int height = suppressedGradients.length;
        int width = suppressedGradients[0].length;
        // assume all pixel are non-edges
        Edge[][] edges = new Edge[height][width];
        for (Edge[] row : edges) {
            Arrays.fill(row, Edge.NONE);
        }
        double maxMagnitude = suppressedGradients[0][0].getMagnitude();
        for (Gradient[] row : suppressedGradients) {
            for (Gradient g : row) {
                maxMagnitude = Math.max(maxMagnitude, g.getMagnitude());
            }
        }

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                // strong edge
                if (suppressedGradients[y][x].getMagnitude() / maxMagnitude >= highThreshold) {
                    edges[y][x] = Edge.STRONG;
                    List<Point> connectedPixels = findConnectedEdges(suppressedGradients,
                                                                          maxMagnitude, x, y,
                                                                          lowThreshold,
                                                                          highThreshold);
                    // all pixels returned are weak edges
                    for (Point p : connectedPixels) {
                        int xx = (int) p.getX();
                        int yy = (int) p.getY();

                        edges[yy][xx] = Edge.WEAK;
                    }
                }
                // if it hasn't already been labeled as weak, assume it will be dropped
                // it may be converted back later
                else if (suppressedGradients[y][x].getMagnitude() / maxMagnitude >= lowThreshold) {
                    if (edges[y][x] != Edge.WEAK) {
                        edges[y][x] = Edge.DROPPED;
                    }
                }
            }
        }

        return edges;
    }

    /**
     * Returns a list of weak edges which are connected to the point at (x, y), which is assumed to
     * be a strong edge.
     *
     * @param suppressedGradients the suppressed gradient of an image
     * @param maxMagnitude the maximum magnitude of the gradients
     * @param x the x coordinate of the starting pixel
     * @param y the y coordinate of the starting pixel
     * @param lowThreshold threshold for weak edges
     * @param highThreshold threshold for strong edges
     * @return a list of points that are connected to the starting point
     */
    private static List<Point> findConnectedEdges(Gradient[][] suppressedGradients,
                                                       double maxMagnitude, int x, int y,
                                                       double lowThreshold,
                                                       double highThreshold)
    {
        int height = suppressedGradients.length;
        int width = suppressedGradients[0].length;

        // list of connected edges
        List<Point> result = new ArrayList<Point>();
        // queue of points to check
        Queue<Point> queue = new LinkedList<Point>();
        queue.add(new Point(x, y));

        while (queue.size() > 0) {
            Point p = queue.remove();
            for (int i = -1; i <= 1; i++) {
                for (int j = -1; j <= 1; j++) {
                    // neighboring pixel
                    int yy = (int) p.getY() + i;
                    int xx = (int) p.getX() + j;

                    // valid pixel
                    if (xx >= 0 && yy >= 0 && xx < width && yy < height) {
                        Point q = new Point(xx, yy);
                        // results does not already contain point
                        if (!result.contains(q)) {
                            double magnitude = suppressedGradients[yy][xx].getMagnitude()
                                               / maxMagnitude;
                            if (magnitude >= lowThreshold && magnitude < highThreshold
                                && isConnected(suppressedGradients, p, q)) {
                                result.add(q);
                                queue.add(q);
                            }
                        }
                    }
                }
            }
        }

        return result;
    }

    /**
     * Returns true if the pixel at point q is connected to the pixel at point p. The two pixels are
     * connected if point p is adjacent to point q and the rounded gradient at point p is in the
     * direction from point p to q.
     *
     * @param suppressedGradients the suppressed gradient of an image
     * @param p starting point
     * @param q test piont
     * @return true if q is connected to p, false otherwise
     */
    private static boolean isConnected(Gradient[][] suppressedGradients, Point p, Point q)
    {
        int px = (int) p.getX();
        int py = (int) p.getY();
        int qx = (int) q.getX();
        int qy = (int) q.getY();

        // direction of gradient
        int xDiff = 0;
        int yDiff = 0;
        switch(suppressedGradients[py][px].getRoundedDirection()) {
            case N_S:
                yDiff = 1;
                break;
            case E_W:
                xDiff = 1;
                break;
            case NE_SW:
                xDiff = 1;
                yDiff = 1;
                break;
            case NW_SE:
                xDiff = -1;
                yDiff = 1;
                break;
        }

        // possible valid coordinates for connected pixels
        int x1 = px + xDiff;
        int y1 = py + yDiff;
        int x2 = px - xDiff;
        int y2 = py - yDiff;

        return ((qx == x1 && qy == y1) || (qx == x2 && qy == y2));
    }

    /**
     * Returns the grayscale value given by applying the specified filter to the specified image at
     * the specified coordinates. The filter should be normalized such that it sums to 1.
     *
     * @param image a 2D array of ints representing the greyscale values of an image
     * @param filter the filter being applied to the image
     * @param x x coordinate
     * @param y y coordinate
     * @return the value at the given pixel after applying the filter
     */
    private static int applyFilter(int[][] image, double[][] filter, int x, int y)
    {
        int filterRadius = filter.length / 2;
        double result = 0;

        for (int i = -filterRadius; i <= filterRadius; i++) {
            for (int j = -filterRadius; j <= filterRadius; j++) {
                int xx = x + j;
                int yy = y + i;

                // get the grayscale value from the image
                int imagePixel;
                if (xx >= 0 && xx < image[0].length && yy >= 0 && yy < image.length) {
                    imagePixel = image[yy][xx];
                }
                else {
                    imagePixel = 0;
                }

                result += imagePixel * filter[filterRadius + i][filterRadius + j];

            }
        }

        return (int) result;
    }

    /**
     * Creates an image from an int array, assuming that the array contains grayscale values for an
     * image.
     *
     * @param grayscale a 2D array of ints representing a grayscale image
     * @return the image created from the int array
     */
    public static BufferedImage createImageFromIntArray(int[][] grayscale)
    {
        BufferedImage image = new BufferedImage(grayscale[0].length, grayscale.length,
                                                BufferedImage.TYPE_INT_ARGB);

        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                image.setRGB(x, y, ImageUtils.getRgbValue(grayscale[y][x]));
            }
        }

        return image;
    }

    /**
     * Creates an image from an array of Gradients by normalizing the gradients then multiplying by
     * 255.
     *
     * @param gradientMap a 2D array of gradients
     * @return the image created from the gradient map
     */
    public static BufferedImage createImageFromGradient(Gradient[][] gradientMap)
    {
        BufferedImage image = new BufferedImage(gradientMap[0].length, gradientMap.length,
                                                BufferedImage.TYPE_INT_ARGB);
        double[][] gradientMagnitude = new double[image.getHeight()][image.getWidth()];
        double maxMagnitude = 0.0;

        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                // get magnitude
                gradientMagnitude[y][x]
                        = gradientMap[y][x].getMagnitude();
                maxMagnitude = Math.max(gradientMagnitude[y][x], maxMagnitude);
            }
        }

        // create image
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                // normalize, then multiply by 255, then cast
                image.setRGB(x, y, ImageUtils
                        .getRgbValue((int) (255 * (gradientMagnitude[y][x] / maxMagnitude))));
            }

        }

        return image;
    }

    /**
     * Creates an image from a 2D array of Edges. White pixels are strong edges, light gray pixels
     * are weak edges, dark gray pixels are dropped edges, and black pixels are non-edges.
     *
     * @param edges a 2D array of Edges
     * @return the image created from the 2D array of Edges
     */
    public static BufferedImage createImageFromEdges(Edge[][] edges)
    {
        BufferedImage image = new BufferedImage(edges[0].length, edges.length,
                                                BufferedImage.TYPE_INT_ARGB);

        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int grayscale = 0;
                switch (edges[y][x]) {
                    case NONE:
                        grayscale = 0;
                        break;
                    case DROPPED:
                        grayscale = 50;
                        break;
                    case WEAK:
                        grayscale = 127;
                        break;
                    case STRONG:
                        grayscale = 255;
                        break;
                }
                image.setRGB(x, y, ImageUtils.getRgbValue(grayscale));
            }
        }

        return image;
    }

    /**
     * The 'edginess' of a pixel.
     *
     * @author Todd Taomae
     */
    private enum Edge
    {
        NONE, DROPPED, WEAK, STRONG
    }
}
