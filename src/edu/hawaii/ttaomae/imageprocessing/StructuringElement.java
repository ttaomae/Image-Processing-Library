package edu.hawaii.ttaomae.imageprocessing;

/**
 * Provides several disk structuring elements as 2-dimensional arrays of doubles. A disk structuring
 * element of radius n, has a height and width of 2n - 1.
 *
 * @author Todd Taomae
 */
public class StructuringElement
{
    /** Disk structuring element with radius 4. */
    public static final int[][] DISK_FOUR = {{0,0,1,1,1,0,0},
                                             {0,1,1,1,1,1,0},
                                             {1,1,1,1,1,1,1},
                                             {1,1,1,1,1,1,1},
                                             {1,1,1,1,1,1,1},
                                             {0,1,1,1,1,1,0},
                                             {0,0,1,1,1,0,0}};

    /** Disk structuring element with radius 5. */
    public static final int[][] DISK_FIVE = {{0,0,0,1,1,1,0,0,0},
                                             {0,1,1,1,1,1,1,1,0},
                                             {0,1,1,1,1,1,1,1,0},
                                             {1,1,1,1,1,1,1,1,1},
                                             {1,1,1,1,1,1,1,1,1},
                                             {1,1,1,1,1,1,1,1,1},
                                             {0,1,1,1,1,1,1,1,0},
                                             {0,1,1,1,1,1,1,1,0},
                                             {0,0,0,1,1,1,0,0,0}};

    /** Disk structuring element with radius 6. */
    public static final int[][] DISK_SIX = {{0,0,0,1,1,1,1,1,0,0,0},
                                            {0,0,1,1,1,1,1,1,1,0,0},
                                            {0,1,1,1,1,1,1,1,1,1,0},
                                            {1,1,1,1,1,1,1,1,1,1,1},
                                            {1,1,1,1,1,1,1,1,1,1,1},
                                            {1,1,1,1,1,1,1,1,1,1,1},
                                            {1,1,1,1,1,1,1,1,1,1,1},
                                            {1,1,1,1,1,1,1,1,1,1,1},
                                            {0,1,1,1,1,1,1,1,1,1,0},
                                            {0,0,1,1,1,1,1,1,1,0,0},
                                            {0,0,0,1,1,1,1,1,0,0,0}};
}
