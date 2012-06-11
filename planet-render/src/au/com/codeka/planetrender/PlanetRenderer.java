package au.com.codeka.planetrender;

/**
 * This is actually a very simple ray-tracing engine. The simplicity comes from the fact that
 * we assume there's only one object in the scene (the planet) and one light source (the sun).
 */
public class PlanetRenderer {

    private double mPlanetRadius;
    private Vector3 mPlanetOrigin;
    private double mAmbient;
    private Vector3 mSunOrigin;

    public PlanetRenderer() {
        mPlanetRadius = 10.0;
        mPlanetOrigin = new Vector3(0.0, 0.0, 30.0);
        mAmbient = 0.1;
        mSunOrigin = new Vector3(100.0, 100.0, -150.0);
    }

    /**
     * Renders a planet into the given \c Image.
     */
    public void render(Image img) {
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                double nx = ((double) x / (double) img.getWidth()) - 0.5;
                double ny = ((double) y / (double) img.getHeight()) - 0.5;
                Colour c = getPixelColour(nx, ny);
                img.setPixelColour(x, y, c);
            }
        }
    }

    /**
     * Computes the colour of the pixel at (x,y) where each coordinate is defined to
     * be in the range (-0.5, +0.5).
     * 
     * @param x The x-coordinate, between -0.5 and +0.5.
     * @param y The y-coordinate, between -0.5 and +0.5.
     * @return The colour at the given pixel.
     */
    private Colour getPixelColour(double x, double y) {
        Colour c = new Colour();

        // should already be normalized, but just to be sure...
        Vector3 ray = new Vector3(x, -y, 1.0).normalized();

        Vector3 intersection = raytrace(ray);
        if (intersection != null) {
            // we intersected with the planet. Now we need to work out the colour at this point
            // on the planet.

            double intensity = lightSphere(intersection);
            c.setAlpha(1.0);
            c.setRed(intensity);
        }
        return c;
    }

    /**
     * Calculates light intensity from the sun.
     * 
     * @param intersection Point where the ray we're currently tracing intersects with the planet.
     */
    private double lightSphere(Vector3 intersection) {
        Vector3 surfaceNormal = Vector3.subtract(intersection, mPlanetOrigin).normalized();

        double intensity = diffuse(surfaceNormal, intersection);
        return Math.max(mAmbient, Math.min(1.0, intensity));
    }

    /**
     * Calculates diffuse lighting at the given point with the given normal.
     *
     * @param normal Normal at the point we're calculating diffuse lighting for.
     * @param point The point at which we're calculating diffuse lighting.
     * @return The diffuse factor of lighting.
     */
    private double diffuse(Vector3 normal, Vector3 point) {
        Vector3 directionToLight = Vector3.subtract(mSunOrigin, point).normalized();
        return Vector3.dot(normal, directionToLight);
    }

    /**
     * Traces a ray along the given direction. We assume the origin is (0,0,0) (i.e. the eye).
     * 
     * @param direction The direction of the ray we're going to trace.
     * @return A \c Vector3 representing the point in space where we intersect with the planet,
     * or \c null if there's no intersection.
     */
    private Vector3 raytrace(Vector3 direction) {
        // intsection of a sphere and a line
        final double a = Vector3.dot(direction, direction);
        final double b = -2.0 * Vector3.dot(mPlanetOrigin, direction);
        final double c = Vector3.dot(mPlanetOrigin, mPlanetOrigin) - (mPlanetRadius * mPlanetRadius);
        final double d = (b * b) - (4.0 * a * c);

        if (d > 0.0) {
            double sign = (c < -0.00001) ? 1.0 : -1.0;
            double distance = (-b + (sign * Math.sqrt(d))) / (2.0 * a);
            return direction.scaled(distance);
        } else {
            return null;
        }
    }

}
