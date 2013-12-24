package edu.hawaii.ttaomae.imageprocessing;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.LUDecomposition;
import org.apache.commons.math3.linear.RealMatrix;

/**
 * A set of {@code PhotometricStereoImage}s.
 *
 * @author Todd Taomae
 */
public class PhotometricStereoImageSet
{
    private List<PhotometricStereoImage> images;
    private final int width;
    private final int height;
    private Vector3D[][] surfaceDescriptors;
    private double[][] albedoMap;
    private Vector3D[][] surfaceNormals;
    private double[][] heightMap;


    /**
     * Constructs a PhotometricStereoImageSet from the specified PhotometricStereoImages.
     *
     * @param images the images from which to make this PhotometricStereoImageSet
     * @throws IllegalArgumentException if the images do not have the same dimensions
     */
    public PhotometricStereoImageSet(PhotometricStereoImage... images) throws IllegalArgumentException
    {
        this.width = images[0].getWidth();
        this.height = images[0].getHeight();

        this.images = new ArrayList<PhotometricStereoImage>();
        for (PhotometricStereoImage image : images) {
            if (image.getWidth() != this.width || image.getHeight() != this.height) {
                throw new IllegalArgumentException("images are not the same size");
            }

            this.images.add(image);
        }

        this.surfaceDescriptors = null;
        this.albedoMap = null;
        this.surfaceNormals = null;
        this.heightMap = null;
    }

    /**
     * Returns the width of the images.
     *
     * @return the width of the images
     */
    public int getWidth()
    {
        return this.width;
    }

    /**
     * Returns the height of the images.
     *
     * @return the height of the images
     */
    public int getHeight()
    {
        return this.height;
    }

    /**
     * Returns the surface descriptors of the image set. The surface description at a given point is
     * given by the pseudo-inverse of matrix S multiplied by matrix I, where S is an Nx3 matrix
     * where each row is the light source direction of each of the N images in this image set and I
     * is an Nx1 matrix where each row is the intensity at the given point for each of the N images
     * in this image set.
     *
     * @return the surface descriptors of the image set
     */
    public Vector3D[][] getSurfaceDescription()
    {
        // if we have already calculated this, immediately return
        if (this.surfaceDescriptors != null) {
            return this.surfaceDescriptors.clone();
        }

        this.surfaceDescriptors = new Vector3D[this.height][this.width];

        for (int y = 0; y < this.height; y++) {
            for (int x = 0; x < this.width; x++) {
                RealMatrix illuminationMatrix = getIlluminationMatrix(x, y);
                RealMatrix intensityMatrix = getIntensityMatrix(x, y);

                // calculate g = (S^T S)^-1 S^T I
                RealMatrix pseudoInverse = pseudoInverse(illuminationMatrix);
                RealMatrix result = pseudoInverse.multiply(intensityMatrix);

                double gx = result.getEntry(0, 0);
                double gy = result.getEntry(1, 0);
                double gz = result.getEntry(2, 0);
                Vector3D surfaceDescriptor = new Vector3D(gx, gy, gz);

                this.surfaceDescriptors[y][x] = surfaceDescriptor;
            }
        }

        return this.surfaceDescriptors.clone();
    }

    /**
     * Returns the albedo map of the image set. The albedo at a given point is given by the
     * magnitude of the surface description at that point.
     *
     * @return the albedo map of the image set
     */
    public double[][] getAlbedoMap()
    {
        // if we have already calculated this, immediately return
        if (this.albedoMap != null) {
            return this.albedoMap.clone();
        }

        this.albedoMap = new double[this.height][this.width];
        Vector3D[][] surfaceDescriptors = getSurfaceDescription();

        for (int y = 0; y < this.height; y++) {
            for (int x = 0; x < this.width; x++) {
                this.albedoMap[y][x] = surfaceDescriptors[y][x].getNorm();
            }
        }

        return this.albedoMap.clone();
    }

    /**
     * Returns the surface normals of the image set. The surface normal at a given point is given by
     * the surface description at that point divided by the albedo at that point.
     *
     * @return the surface normals of the image set
     */
    public Vector3D[][] getSurfaceNormals()
    {
        // if we have already calculated this, immediately return
        if (this.surfaceNormals != null) {
            return this.surfaceNormals.clone();
        }

        this.surfaceNormals = new Vector3D[this.height][this.width];
        Vector3D[][] surfaceDescriptors = getSurfaceDescription();
        double[][] albedos = getAlbedoMap();

        for (int y = 0; y < this.height; y++) {
            for (int x = 0; x < this.width; x++) {

                this.surfaceNormals[y][x] = surfaceDescriptors[y][x]
                        .scalarMultiply(1.0 / albedos[y][x]);
            }
        }

        return this.surfaceNormals.clone();
    }

    /**
     * TODO: fix terminology? Returns the height map of the image set. The height at the top left
     * pixel is assumed to be zero. This is the integration constant. The height at a given point is
     * given by first integrating along the y-axis, then along the x-axis.
     *
     * For example, the height at point (1, 2) is given by finding the height at point (0, 1), (0,
     * 2), then finally (1, 2). To integrate along the y-axis, we use the following formula:
     * z<sub>x,y</sub> = z<sub>x,y-1</sub> - (n<sub>y</sub> / n<sub>z</sub>) where z<sub>x,y</sub>
     * and z<sub>x,y-1</sub> is the height at a given point, and n<sub>y</sub> and n<sub>z</sub> are
     * the y and z components of the surface normal at the given point. To integrate along the
     * x-axis, we use the following, very similar formula: z<sub>x,y</sub> = z<sub>x-1,y</sub> -
     * (n<sub>x</sub> / n<sub>z</sub>)
     *
     * @return the height map of the image set
     */
    public double[][] getHeightMap()
    {
        // if we have already calculated this, immediately return
        if (this.heightMap != null) {
            return this.heightMap.clone();
        }

        this.heightMap = new double[this.height][this.width];
        Vector3D[][] surfaceNormals = this.getSurfaceNormals();

        for (int y = 0; y < this.height; y++) {
            for (int x = 0; x < this.height; x++) {
                // first pixel
                if (x == 0 && y == 0) {
                    // set integration constant to 0
                    this.heightMap[y][x] = 0.0f;
                }
                // left column of image
                else if (x == 0) {
                    // use y partial derivative
                    this.heightMap[y][x] = this.heightMap[y - 1][x]
                                      - (surfaceNormals[y][x].getY() / surfaceNormals[y][x].getZ());
                }
                else {
                    // use x partial derivative
                    this.heightMap[y][x] = this.heightMap[y][x - 1]
                                      - (surfaceNormals[y][x].getX() / surfaceNormals[y][x].getZ());
                }
            }
        }

        return this.heightMap.clone();
    }

    /**
     * Returns the matrix S for the given pixel. S is an Nx3 matrix, where N is the number of images
     * in this image set. Each row contains the (x, y, z) coordinates of the light source origin,
     * for a single image of the set, multiplied by the intensity at given pixel. Multiplying by the
     * intensity will eliminate the contribution from shadow pixels.
     *
     * @param x the x coordinate of the pixel
     * @param y the y coordinate of the pixel
     * @return the matrix S for the given pixel
     */
    private RealMatrix getIlluminationMatrix(int x, int y)
    {
        RealMatrix illuminationMatrix = new Array2DRowRealMatrix(this.images.size(), 3);
        for (int row = 0; row < illuminationMatrix.getRowDimension(); row++) {
            double intensity = this.images.get(row).getIntensity(x, y);
            double[] lightSourceDirection = this.images.get(row).getLightSourceDirection();

            for (int col = 0; col < illuminationMatrix.getColumnDimension(); col++) {
                illuminationMatrix.setEntry(row, col, intensity * lightSourceDirection[col]);
            }
        }

        return illuminationMatrix;
    }

    /**
     * Returns the matrix I for the given pixel. I is an Nx1 matrix, where N is the number of images
     * in this image set. Each row contains the square of the intensity at the given pixel for a
     * single image of the set. The intensity is squared to help deal with shadows.
     *
     * @param x the x coordinate of the pixel
     * @param y the y coordinate of the pixel
     * @return the matrix I for the given pixel
     */
    private RealMatrix getIntensityMatrix(int x, int y)
    {
        RealMatrix intensityMatrix = new Array2DRowRealMatrix(this.images.size(), 1);

        for (int row = 0; row < intensityMatrix.getRowDimension(); row++) {
            double intensity = this.images.get(row).getIntensity(x, y);
            intensityMatrix.setEntry(row, 0, intensity * intensity);
        }

        return intensityMatrix;
    }

    /**
     * Returns the pseudo-inverse of the specified matrix. The pseudo-inverse of a matrix A is
     * defined as ((A^T * A)^-1 * A^T) where '*' indicates a matrix multiplication, '^T' indicates a
     * transposition, and '^-1' indicates the inverse of a matrix.
     *
     * @param m the matrix of which to get the pseudo-inverse
     * @return the pseudo-inverse of the specified matrix
     */
    private RealMatrix pseudoInverse(RealMatrix m)
    {
        RealMatrix mTranspose = m.transpose();
        RealMatrix pseudoInverse = mTranspose.multiply(m);
        pseudoInverse = new LUDecomposition(pseudoInverse).getSolver().getInverse();
        pseudoInverse = pseudoInverse.multiply(mTranspose);

        return pseudoInverse;
    }
}
