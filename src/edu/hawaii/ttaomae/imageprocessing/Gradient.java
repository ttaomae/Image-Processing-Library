package edu.hawaii.ttaomae.imageprocessing;

import java.awt.geom.Point2D;

/**
 * Represents the gradient of a single pixel.
 *
 * @author Todd Taomae
 */
public class Gradient
{
    /**
     * Represents quantized direction of the gradient.
     *
     * @author Todd Taomae
     */
    enum Direction
    {
        N_S, E_W, NE_SW, NW_SE
    }

    private Point2D.Double gradient;

    /**
     * Constructs a new gradient with the specified x and y magnitude.
     * 
     * @param x x magnitude
     * @param y y magnitude
     */
    public Gradient(double x, double y)
    {
        this.gradient = new Point2D.Double(x, y);
    }

    /**
     * Returns the gradient in the x direction.
     *
     * @return the gradient in the x direction
     */
    public double getXGradient()
    {
        return this.gradient.getX();
    }

    /**
     * Returns the gradient in the y direction.
     *
     * @return the gradient in the y direction
     */
    public double getYGradient()
    {
        return this.gradient.getY();
    }

    /**
     * Returns the magnitude of the gradient.
     *
     * @return the magnitude of the gradient
     */
    public double getMagnitude()
    {
        double x = getXGradient();
        double y = getYGradient();
        return Math.sqrt(x * x + y * y);
    }

    /**
     * Returns the direction of the gradient in radians, from -pi to pi.
     *
     * @return the direction of the gradient
     */
    public double getDirection()
    {
        return Math.atan2(getYGradient(), getXGradient());
    }

    /**
     * Returns the direction of the gradient rounded to the nearest quantized direction. The
     * quantized directions are each of the eight cardinal and intermediate directions. However, a
     * gradient pointing north is considered the equivalent to a gradient pointing south and
     * similarly for other angles. This reduces the eight directions to four possiblities.
     *
     * @return the rounded direction of the gradient
     */
    public Direction getRoundedDirection()
    {
        double direction = getDirection();
        // if angle is negative make it positive
        // we consider a north-south gradient the same as a south-north
        // adding pi radians will change north-south gradients to
        // south-north and similarly for other angles
        direction = (direction < 0) ? direction + Math.PI : direction;

        double eighthPi = Math.PI / 8.0;

        if (direction > eighthPi && direction <= 3 * eighthPi) {
            return Direction.NW_SE;
        }
        else if (direction > 3 * eighthPi && direction <= 5 * eighthPi) {
            return Direction.N_S;
        }
        else if (direction > 5 * eighthPi && direction <= 7 * eighthPi) {
            return Direction.NE_SW;
        }
        else {
            return Direction.E_W;
        }
    }
}
