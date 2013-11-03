package au.com.codeka.warworlds.game.starfield;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import org.andengine.entity.Entity;
import org.andengine.entity.scene.Scene;
import org.andengine.entity.sprite.Sprite;
import org.andengine.input.touch.TouchEvent;
import org.andengine.opengl.font.Font;
import org.andengine.opengl.font.FontFactory;
import org.andengine.opengl.texture.TextureOptions;
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlas;
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlasTextureRegionFactory;
import org.andengine.opengl.texture.atlas.bitmap.BuildableBitmapTextureAtlas;
import org.andengine.opengl.texture.atlas.bitmap.source.IBitmapTextureAtlasSource;
import org.andengine.opengl.texture.atlas.buildable.builder.BlackPawnTextureAtlasBuilder;
import org.andengine.opengl.texture.atlas.buildable.builder.ITextureAtlasBuilder.TextureAtlasBuilderException;
import org.andengine.opengl.texture.region.ITextureRegion;
import org.andengine.opengl.texture.region.TiledTextureRegion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Handler;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import au.com.codeka.common.Pair;
import au.com.codeka.common.Vector2;
import au.com.codeka.common.model.BaseFleet;
import au.com.codeka.common.model.BaseStar;
import au.com.codeka.warworlds.model.BuildManager;
import au.com.codeka.warworlds.model.Empire;
import au.com.codeka.warworlds.model.EmpireManager;
import au.com.codeka.warworlds.model.Fleet;
import au.com.codeka.warworlds.model.MyEmpire;
import au.com.codeka.warworlds.model.Sector;
import au.com.codeka.warworlds.model.SectorManager;
import au.com.codeka.warworlds.model.Star;
import au.com.codeka.warworlds.model.StarManager;

/**
 * \c SurfaceView that displays the starfield. You can scroll around, tap on stars to bring
 * up their details and so on.
 */
public class StarfieldSceneManager extends SectorSceneManager
                                  implements StarManager.StarFetchedHandler,
                                             EmpireManager.EmpireFetchedHandler {
    private static final Logger log = LoggerFactory.getLogger(StarfieldSceneManager.class);
    private ArrayList<OnSelectionChangedListener> mSelectionChangedListeners;
    private BaseStar mHqStar;
    private Handler mHandler;

    private StarEntity mSelectingSprite;
    private SelectionIndicator mSelectionIndicator;
    private boolean mWasDragging;

    private Font mFont;
    private BitmapTextureAtlas mStarTextureAtlas;
    private TiledTextureRegion mNeutronStarTextureRegion;
    private TiledTextureRegion mNormalStarTextureRegion;

    private BitmapTextureAtlas mFleetIconTextureAtlas;
    private TiledTextureRegion mFleetIconTextureRegion;

    private BuildableBitmapTextureAtlas mFleetSpriteTextureAtlas;
    private HashMap<String, ITextureRegion> mFleetSprites;

    private BitmapTextureAtlas mBackgroundGasTextureAtlas;
    private TiledTextureRegion mBackgroundGasTextureRegion;
    private BitmapTextureAtlas mBackgroundStarsTextureAtlas;
    private TiledTextureRegion mBackgroundStarsTextureRegion;
    private ArrayList<Entity> mBackgroundEntities;
    private boolean mIsBackgroundVisible = true;;
    private float mBackgroundZoomAlpha = 1.0f;

    private StarEntity mSelectedStarSprite;

    public StarfieldSceneManager(BaseStarfieldActivity activity) {
        super(activity);
        log.info("Starfield initializing...");

        mSelectionChangedListeners = new ArrayList<OnSelectionChangedListener>();
        mHandler = new Handler();
    }

    @Override
    public void onLoadResources() {
        mStarTextureAtlas = new BitmapTextureAtlas(mActivity.getTextureManager(), 128, 384,
                TextureOptions.BILINEAR_PREMULTIPLYALPHA);
        mNormalStarTextureRegion = BitmapTextureAtlasTextureRegionFactory.createTiledFromAsset(mStarTextureAtlas, mActivity,
                "stars/stars_small.png", 0, 0, 2, 6);
        mNeutronStarTextureRegion = BitmapTextureAtlasTextureRegionFactory.createTiledFromAsset(mStarTextureAtlas, mActivity,
                "stars/stars_small.png", 0, 0, 1, 3);

        mBackgroundGasTextureAtlas = new BitmapTextureAtlas(mActivity.getTextureManager(), 512, 512,
                TextureOptions.BILINEAR_PREMULTIPLYALPHA);
        mBackgroundGasTextureRegion = BitmapTextureAtlasTextureRegionFactory.createTiledFromAsset(mBackgroundGasTextureAtlas,
                mActivity, "decoration/gas.png", 0, 0, 4, 4);
        mBackgroundStarsTextureAtlas = new BitmapTextureAtlas(mActivity.getTextureManager(), 512, 512,
                TextureOptions.BILINEAR_PREMULTIPLYALPHA);
        mBackgroundStarsTextureRegion = BitmapTextureAtlasTextureRegionFactory.createTiledFromAsset(mBackgroundStarsTextureAtlas,
                mActivity, "decoration/starfield.png", 0, 0, 4, 4);

        mFleetIconTextureAtlas = new BitmapTextureAtlas(mActivity.getTextureManager(), 64, 64,
                TextureOptions.BILINEAR_PREMULTIPLYALPHA);
        mFleetIconTextureRegion = BitmapTextureAtlasTextureRegionFactory.createTiledFromAsset(mFleetIconTextureAtlas, 
                mActivity, "img/fleet.png", 0, 0, 1, 1);

        mFleetSpriteTextureAtlas = new BuildableBitmapTextureAtlas(mActivity.getTextureManager(), 256, 256,
                TextureOptions.BILINEAR_PREMULTIPLYALPHA);
        mFleetSprites = new HashMap<String, ITextureRegion>();
        mFleetSprites.put("ship.fighter", BitmapTextureAtlasTextureRegionFactory.createFromAsset(mFleetSpriteTextureAtlas, mActivity, "spritesheets/ship.fighter.png"));
        mFleetSprites.put("ship.scout", BitmapTextureAtlasTextureRegionFactory.createFromAsset(mFleetSpriteTextureAtlas, mActivity, "spritesheets/ship.scout.png"));
        mFleetSprites.put("ship.colony", BitmapTextureAtlasTextureRegionFactory.createFromAsset(mFleetSpriteTextureAtlas, mActivity, "spritesheets/ship.colony.png"));
        mFleetSprites.put("ship.troopcarrier", BitmapTextureAtlasTextureRegionFactory.createFromAsset(mFleetSpriteTextureAtlas, mActivity, "spritesheets/ship.troopcarrier.png"));

        mActivity.getTextureManager().loadTexture(mStarTextureAtlas);
        mActivity.getTextureManager().loadTexture(mBackgroundGasTextureAtlas);
        mActivity.getTextureManager().loadTexture(mBackgroundStarsTextureAtlas);
        mActivity.getTextureManager().loadTexture(mFleetIconTextureAtlas);

        try {
            mFleetSpriteTextureAtlas.build(new BlackPawnTextureAtlasBuilder<IBitmapTextureAtlasSource, BitmapTextureAtlas>(1, 1, 1));
            mFleetSpriteTextureAtlas.load();
        } catch (TextureAtlasBuilderException e) {
            log.error("Error building texture atlas.", e);
        }

        mFont = FontFactory.create(mActivity.getFontManager(), mActivity.getTextureManager(), 256, 256,
                                   Typeface.create(Typeface.DEFAULT, Typeface.NORMAL), 16, true, Color.WHITE);
        mFont.load();

        mSelectionIndicator = new SelectionIndicator(this);
    }

    @Override
    protected void onStart() {
        super.onStart();

        StarManager.getInstance().addStarUpdatedListener(null, this);
        EmpireManager.i.addEmpireUpdatedListener(null, this);

        MyEmpire myEmpire = EmpireManager.i.getEmpire();
        if (myEmpire != null) {
            BaseStar homeStar = myEmpire.getHomeStar();
            int numHqs = BuildManager.getInstance().getTotalBuildingsInEmpire("hq");
            if (numHqs > 0) {
                mHqStar = homeStar;
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        StarManager.getInstance().removeStarUpdatedListener(this);
        EmpireManager.i.removeEmpireUpdatedListener(this);
    }

    public Font getFont() {
        return mFont;
    }

    public ITextureRegion getSpriteTexture(String spriteName) {
        return mFleetSprites.get(spriteName);
    }

    public void addSelectionChangedListener(OnSelectionChangedListener listener) {
        if (!mSelectionChangedListeners.contains(listener)) {
            mSelectionChangedListeners.add(listener);
        }
    }

    public void removeSelectionChangedListener(OnSelectionChangedListener listener) {
        mSelectionChangedListeners.remove(listener);
    }

    protected void fireSelectionChanged(final Star star) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                for(OnSelectionChangedListener listener : mSelectionChangedListeners) {
                    listener.onStarSelected(star);
                }
            }
        });
    }
    protected void fireSelectionChanged(final Fleet fleet) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                for(OnSelectionChangedListener listener : mSelectionChangedListeners) {
                    listener.onFleetSelected(fleet);
                }
            }
        });
    }

    @Override
    public void onEmpireFetched(Empire empire) {
        // if the player's empire changes, it might mean that the location of their HQ has changed,
        // so we'll want to make sure it's still correct.
        MyEmpire myEmpire = EmpireManager.i.getEmpire();
        if (empire.getKey().equals(myEmpire.getKey())) {
            if (mHqStar != null) {
                mHqStar = empire.getHomeStar();
            }
        }
    }

    public Star getSelectedStar() {
        return null;
    }

    public ITextureRegion getFleetIconTextureRegion() {
        return mFleetIconTextureRegion;
    }

    @Override
    protected void refreshScene(Scene scene) {
        final List<Pair<Long, Long>> missingSectors = drawScene(scene);
        if (missingSectors != null) {
            SectorManager.getInstance().requestSectors(missingSectors, false, null);
        }

        refreshSelectionIndicator();
    }

    @Override
    protected void updateZoomFactor(float zoomFactor) {
        super.updateZoomFactor(zoomFactor);

        // we fade out the background between 0.45 and 0.40, it should be totally invisible < 0.40
        // and totally opaque for >= 0.45
        if (zoomFactor < 0.4f && mIsBackgroundVisible) {
            mIsBackgroundVisible = false;
            // we need to make the background as invisible
            mActivity.runOnUpdateThread(new Runnable() {
                @Override
                public void run() {
                    for (Entity entity : mBackgroundEntities) {
                        entity.setVisible(mIsBackgroundVisible);
                    }
                }
            });
        } else if (zoomFactor >= 0.4f && !mIsBackgroundVisible) {
            mIsBackgroundVisible = true;
            // we need to make the background as visible
            mActivity.runOnUpdateThread(new Runnable() {
                @Override
                public void run() {
                    for (Entity entity : mBackgroundEntities) {
                        entity.setVisible(mIsBackgroundVisible);
                    }
                }
            });
        }
        if (zoomFactor >= 0.4f && zoomFactor < 0.45f) {
            // between 0.4 and 0.45 we need to fade the background in
            mBackgroundZoomAlpha = (zoomFactor - 0.4f) * 20.0f; // make it in the range 0...1
            mActivity.runOnUpdateThread(new Runnable() {
                @Override
                public void run() {
                    for (Entity entity : mBackgroundEntities) {
                        entity.setAlpha(mBackgroundZoomAlpha);
                        entity.setColor(mBackgroundZoomAlpha, mBackgroundZoomAlpha, mBackgroundZoomAlpha);
                    }
                }
            });
        }
    }

    private List<Pair<Long, Long>> drawScene(Scene scene) {
        SectorManager sm = SectorManager.getInstance();
        List<Pair<Long, Long>> missingSectors = null;

        mBackgroundEntities = new ArrayList<Entity>();

        for(int y = -mSectorRadius; y <= mSectorRadius; y++) {
            for(int x = -mSectorRadius; x <= mSectorRadius; x++) {
                long sX = mSectorX + x;
                long sY = mSectorY + y;

                Sector sector = sm.getSector(sX, sY);
                if (sector == null) {
                    if (missingSectors == null) {
                        missingSectors = new ArrayList<Pair<Long, Long>>();
                    }
                    missingSectors.add(new Pair<Long, Long>(sX, sY));
                    log.debug(String.format("Missing sector: %d, %d", sX, sY));
                    continue;
                }

                int sx = (int)(x * Sector.SECTOR_SIZE);
                int sy = (int)(y * Sector.SECTOR_SIZE);
                drawBackground(scene, sector, sx, sy);
            }
        }

        for (int y = -mSectorRadius; y <= mSectorRadius; y++) {
            for(int x = -mSectorRadius; x <= mSectorRadius; x++) {
                long sX = mSectorX + x;
                long sY = mSectorY + y;
                Sector sector = sm.getSector(sX, sY);
                if (sector == null) {
                    continue;
                }

                int sx = (int)(x * Sector.SECTOR_SIZE);
                int sy = (int)(y * Sector.SECTOR_SIZE);
                addSector(scene, sx, sy, sector);
            }
        }

        return missingSectors;
    }

    private void drawBackground(Scene scene, Sector sector, int sx, int sy) {
        Random r = new Random(sector.getX() ^ (long)(sector.getY() * 48647563));
        final int STAR_SIZE = 256;
        for (int y = 0; y < Sector.SECTOR_SIZE / STAR_SIZE; y++) {
            for (int x = 0; x < Sector.SECTOR_SIZE / STAR_SIZE; x++) {
                Sprite bgSprite = new Sprite(
                        (float) (sx + (x * STAR_SIZE)),
                        (float) (sy + (y * STAR_SIZE)),
                        STAR_SIZE, STAR_SIZE,
                        mBackgroundStarsTextureRegion.getTextureRegion(r.nextInt(16)),
                        mActivity.getVertexBufferObjectManager());
                setBackgroundEntityZoomFactor(bgSprite);
                scene.attachChild(bgSprite);
                mBackgroundEntities.add(bgSprite);
            }
        }

        final int GAS_SIZE = 512;
        for (int i = 0; i < 10; i++) {
            float x = r.nextInt(Sector.SECTOR_SIZE + (GAS_SIZE / 4)) - (GAS_SIZE / 8);
            float y = r.nextInt(Sector.SECTOR_SIZE + (GAS_SIZE / 4)) - (GAS_SIZE / 8);

            Sprite bgSprite = new Sprite(
                    (sx + x) - (GAS_SIZE / 2.0f),
                    (sy + y) - (GAS_SIZE / 2.0f),
                    GAS_SIZE, GAS_SIZE,
                    mBackgroundGasTextureRegion.getTextureRegion(r.nextInt(14)),
                    mActivity.getVertexBufferObjectManager());
            setBackgroundEntityZoomFactor(bgSprite);
            scene.attachChild(bgSprite);
            mBackgroundEntities.add(bgSprite);
        }
    }

    private void setBackgroundEntityZoomFactor(Sprite bgSprite) {
        if (mBackgroundZoomAlpha <= 0.0f) {
            bgSprite.setVisible(false);
        } else if (mBackgroundZoomAlpha >= 1.0f) {
            // do nothing
        } else {
            bgSprite.setAlpha(mBackgroundZoomAlpha);
            bgSprite.setColor(mBackgroundZoomAlpha, mBackgroundZoomAlpha, mBackgroundZoomAlpha);
        }

    }

    /**
     * Draws a sector, which is a 1024x1024 area of stars.
     */
    private void addSector(Scene scene, int offsetX, int offsetY, Sector sector) {
        for(BaseStar star : sector.getStars()) {
            addStar(scene, (Star) star, offsetX, offsetY);
        }
        for (BaseStar star : sector.getStars()) {
            for (BaseFleet fleet : star.getFleets()) {
                if (fleet.getState() == Fleet.State.MOVING) {
                    addMovingFleet(scene, (Fleet) fleet, (Star) star, offsetX, offsetY);
                }
            }
        }
    }

    /**
     * Draws a single star. Note that we draw all stars first, then the names of stars
     * after.
     */
    private void addStar(Scene scene, Star star, int x, int y) {
        x += star.getOffsetX();
        y += star.getOffsetY();

        ITextureRegion textureRegion = null;
        if (star.getStarType().getInternalName().equals("neutron")) {
            textureRegion = mNeutronStarTextureRegion.getTextureRegion(0);
        } else {
            int offset = 0;
            if (star.getStarType().getInternalName().equals("black-hole")) {
                offset = 4;
            } else if (star.getStarType().getInternalName().equals("blue")) {
                offset = 5;
            } else if (star.getStarType().getInternalName().equals("orange")) {
                offset = 6;
            } else if (star.getStarType().getInternalName().equals("red")) {
                offset = 7;
            } else if (star.getStarType().getInternalName().equals("white")) {
                offset = 8;
            } else if (star.getStarType().getInternalName().equals("yellow")) {
                offset = 9;
            }
            textureRegion = mNormalStarTextureRegion.getTextureRegion(offset);
        }

        StarEntity sprite = new StarEntity(this, star,
                                           (float) x, (float) y,
                                           textureRegion, mActivity.getVertexBufferObjectManager());
        scene.registerTouchArea(sprite.getTouchEntity());
        scene.attachChild(sprite);

        if (mSelectedStarSprite != null && mSelectedStarSprite.getStar().getKey().equals(star.getKey())) {
            mSelectedStarSprite = sprite;
        }
/*
        if (mHqStar != null && star.getKey().equals(mHqStar.getKey())) {
            if (mHqSprite == null) {
                mHqSprite = SpriteManager.i.getSprite("building.hq");
            }

            mMatrix.reset();
            mMatrix.postTranslate(-(mHqSprite.getWidth() / 2.0f), -(mHqSprite.getHeight() / 2.0f));
            mMatrix.postScale(40.0f * imageScale * pixelScale / mHqSprite.getWidth(),
                              40.0f * imageScale * pixelScale / mHqSprite.getHeight());
            mMatrix.postTranslate(x * pixelScale, (y * pixelScale) + sprite.getHeight());
            canvas.save();
            canvas.concat(mMatrix);
            mHqSprite.draw(canvas);
            canvas.restore();

            mHqOverlay.setEnabled(false);
        }

        List<VisibleEntityAttachedOverlay> overlays = mStarAttachedOverlays.get(star.getKey());
        if (overlays != null && !overlays.isEmpty()) {
            int n = overlays.size();
            for (int i = 0; i < n; i++) {
                VisibleEntityAttachedOverlay sao = overlays.get(i);
                sao.setCentre(x * pixelScale, y * pixelScale);
            }
        }

        VisibleEntity ve = new VisibleEntity(new Vector2(x * pixelScale, y * pixelScale), star);
        if (mSelectedEntity != null && mSelectedEntity.star != null && mSelectedEntity.star.getKey().equals(star.getKey())) {
            mSelectedEntity = ve;
        }
        mVisibleEntities.add(ve);
*/
    }

    /**
     * Given a \c Sector, returns the (x, y) coordinates (in view-space) of the origin of this
     * sector.
     */
    private Vector2 getSectorOffset(long sx, long sy) {
        sx -= mSectorX;
        sy -= mSectorY;
        return new Vector2((sx * Sector.SECTOR_SIZE),
                           (sy * Sector.SECTOR_SIZE));
    }

    /**
     * Draw a moving fleet as a line between the source and destination stars, with an icon
     * representing the current location of the fleet.
     */
    private void addMovingFleet(Scene scene, Fleet fleet, Star srcStar, int offsetX, int offsetY) {
        // we'll need to find the destination star
        Star destStar = SectorManager.getInstance().findStar(fleet.getDestinationStarKey());
        if (destStar == null) {
            // the destination star isn't in one of the sectors we have in memory, we'll
            // just ignore this fleet (it's probably flying off the edge of the sector and our
            // little viewport won't see it anyway -- unless you've got a REALLY long-range
            // flight, maybe we can stop that from being possible).
            return;
        }

        Vector2 srcPoint = Vector2.pool.borrow().reset(offsetX, offsetY);
        srcPoint.x += srcStar.getOffsetX();
        srcPoint.y += srcStar.getOffsetY();

        Vector2 destPoint = getSectorOffset(destStar.getSectorX(), destStar.getSectorY());
        destPoint.x += destStar.getOffsetX();
        destPoint.y += destStar.getOffsetY();

        FleetEntity fleetEntity = new FleetEntity(this, srcPoint, destPoint, fleet, mActivity.getVertexBufferObjectManager());
        //scene.registerTouchArea(fleetEntity.getTouchEntity());
        scene.attachChild(fleetEntity);
    }

    @Override
    public boolean onSceneTouchEvent(Scene scene, TouchEvent touchEvent) {
        boolean handled = super.onSceneTouchEvent(scene, touchEvent);

        if (touchEvent.getAction() == TouchEvent.ACTION_DOWN) {
            mWasDragging = false;
        } else if (touchEvent.getAction() == TouchEvent.ACTION_UP) {
            if (!mWasDragging) {
                selectNothing();
                handled = true;
            }
        }

        return handled;
    }

    @Override
    protected GestureDetector.OnGestureListener createGestureListener() {
        return new GestureListener();
    }

    @Override
    protected ScaleGestureDetector.OnScaleGestureListener createScaleGestureListener() {
        return new ScaleGestureListener();
    }

    /** The default gesture listener is just for scrolling around. */
    protected class GestureListener extends SectorSceneManager.GestureListener {
        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
                float distanceY) {
            super.onScroll(e1, e2, distanceX, distanceY);

            // because we've navigating the map, we're no longer in the process of selecting a sprite.
            mSelectingSprite = null;
            mWasDragging = true;
            return true;
        }
    }

    /** The default scale gesture listener scales the view. */
    protected class ScaleGestureListener extends SectorSceneManager.ScaleGestureListener {
        @Override
        public boolean onScale (ScaleGestureDetector detector) {
            super.onScale(detector);

            // because we've navigating the map, we're no longer in the process of selecting a sprite.
            mSelectingSprite = null;
            mWasDragging = true;
            return true;
        }
    }

    /** Gets the sprite we've marked as "being selected". That is, you've tapped down, but not yet tapped up. */
    public StarEntity getSelectingSprite() {
        return mSelectingSprite;
    }

    /** Sets the sprite that we've tapped down on, but not yet tapped up on. */
    public void setSelectingSprite(StarEntity sprite) {
        mSelectingSprite = sprite;
    }

    public void selectStar(String starKey) {
        Star star = SectorManager.getInstance().findStar(starKey);
        selectStar(star);
    }

    public void selectStar(Star star) {
        
    }

    public void selectStar(StarEntity selectedStarSprite) {
        mSelectedStarSprite = selectedStarSprite;
        // mSelectedFleetSprite = null;

        refreshSelectionIndicator();
        fireSelectionChanged(mSelectedStarSprite.getStar());
    }

    /** Deselects the fleet or star you currently have selected. */
    public void selectNothing() {
        if (mSelectedStarSprite == null) {
            return;
        } else {
            mSelectedStarSprite = null;
            refreshSelectionIndicator();
            fireSelectionChanged((Star) null);
        }
    }

    private void refreshSelectionIndicator() {
        if (mSelectionIndicator.getParent() != null) {
            mSelectionIndicator.getParent().detachChild(mSelectionIndicator);
        }

        if (mSelectedStarSprite != null) {
            Star star = mSelectedStarSprite.getStar();
            mSelectionIndicator.setScale(star.getSize());
            mSelectedStarSprite.attachChild(mSelectionIndicator);
        } else {
            // nothing selected, nothing to do
        }
    }

    public void selectFleet(BaseFleet fleet) {
        
    }

    /**
     * When a star is updated, if it's one of ours, then we'll want to redraw to make sure we
     * have the latest data (e.g. it might've been renamed)
     */
    @Override
    public void onStarFetched(Star s) {
        // TODO: redraw();
    }

    public interface OnSelectionChangedListener {
        public abstract void onStarSelected(Star star);
        public abstract void onFleetSelected(Fleet fleet);
    }
}
