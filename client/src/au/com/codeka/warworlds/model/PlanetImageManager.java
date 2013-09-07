package au.com.codeka.warworlds.model;

import java.util.Locale;
import java.util.Random;

import au.com.codeka.common.Vector3;
import au.com.codeka.common.model.Planet;
import au.com.codeka.common.model.Star;
import au.com.codeka.planetrender.Template;

/**
 * Manages images of planets, generates images on-the-fly and caches them as
 * required.
 */
public class PlanetImageManager extends ImageManager {
    private static PlanetImageManager sInstance = new PlanetImageManager();

    public static PlanetImageManager getInstance() {
        return sInstance;
    }

    /**
     * Gets a \c Sprite for the given planet.
     */
    public Sprite getSprite(Star star, Planet planet) {
        String key = String.format(Locale.ENGLISH, "%s-%d", star.key, planet.index);
        PlanetExtra planetExtra = new PlanetExtra(star, planet);
        Sprite sprite = getSprite(key, 100, planetExtra);
        if (sprite == null) {
            sprite = SpriteManager.i.getSprite("planet."+PlanetType.get(planet.planet_type).getInternalName());

            if (sprite.getNumFrames() > 1) {
                int frameNo = new Random(star.hashCode()).nextInt(sprite.getNumFrames());
                sprite = sprite.extractFrame(frameNo);
                sprite.setScale((float) getPlanetSize(planetExtra) / 10.0f);
            }
        }

        return sprite;
    }

    /**
     * Loads the \c Template for the given \c Planet.
     */
    @Override
    protected Template getTemplate(Object extra) {
        PlanetExtra planetExtra = (PlanetExtra) extra;
        String key = String.format(Locale.ENGLISH, "%s-%d",
                                   planetExtra.star.key,
                                   planetExtra.planet.index);
        return loadTemplate(PlanetType.get(planetExtra.planet.planet_type).getBitmapBasePath(), key);
    }

    /**
     * We want to adjust the sun position so that shadows look correct when the planet is
     * drawn on the SolarSystem page.
     */
    @Override
    protected Vector3 getSunDirection(Object extra) {
        PlanetExtra planetExtra = (PlanetExtra) extra;
        int numPlanets = planetExtra.star.planets.size();
        float angle = (0.5f/(numPlanets + 1));
        angle = (float) ((angle*planetExtra.planet.index*Math.PI) + angle*Math.PI);

        Vector3 sunDirection = Vector3.pool.borrow().reset(0.0, 1.0, -1.0);
        sunDirection.rotateZ(angle);
        sunDirection.scale(200.0);
        return sunDirection;
    }

    /**
     * Gets the size we render the planet as.
     */
    protected double getPlanetSize(Object extra) {
        PlanetExtra planetExtra = (PlanetExtra) extra;
        return ((planetExtra.planet.size - 10.0) / 8.0) + 4.0;
    }

    private static class PlanetExtra {
        public Star star;
        public Planet planet;

        public PlanetExtra(Star star, Planet planet) {
            this.star = star;
            this.planet = planet;
        }
    }

}
