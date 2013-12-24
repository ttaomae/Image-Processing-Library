package edu.hawaii.ttaomae.imageprocessing;

/**
 * Provides edge detection operators as 2-dimensional arrays of doubles.
 *
 * @author Todd Taomae
 */
public class EdgeDetectionOperator
{
    /** Sobel operator for detecting edges in the x direction */
    public static final double[][] SOBEL_X = {{1, 0, -1 },
                                              {2, 0, -2 },
                                              {1, 0, -1 }};

    /** Sobel operator for detecting edges in the y direction */
    public static final double[][] SOBEL_Y = {{ 1,  2,  1},
                                              { 0,  0,  0},
                                              {-1, -2, -1}};

    /** Prewitt operator for detecting edges in the x direction */
    public static final double[][] PREWITT_X =  {{1, 0, -1 },
                                                 {1, 0, -1 },
                                                 {1, 0, -1 }};

    /** Prewitt operator for detecting edges in the y direction */
    public static final double[][] PREWITT_Y = {{ 1,  1,  1},
                                                { 0,  0,  0},
                                                {-1, -1, -1}};
}
