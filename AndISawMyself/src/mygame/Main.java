package mygame;

import com.jme3.app.DebugKeysAppState;
import com.jme3.app.SimpleApplication;
import com.jme3.app.StatsAppState;
import com.jme3.app.state.ScreenshotAppState;
import com.jme3.asset.plugins.HttpZipLocator;
import com.jme3.asset.plugins.ZipLocator;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.shapes.CapsuleCollisionShape;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.collision.shapes.HeightfieldCollisionShape;
import com.jme3.bullet.control.CharacterControl;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.post.FilterPostProcessor;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.system.AppSettings;
import com.jme3.terrain.Terrain;
import com.jme3.terrain.geomipmap.TerrainGrid;
import com.jme3.terrain.geomipmap.TerrainGridListener;
import com.jme3.terrain.geomipmap.TerrainGridLodControl;
import com.jme3.terrain.geomipmap.TerrainLodControl;
import com.jme3.terrain.geomipmap.TerrainQuad;
import com.jme3.terrain.geomipmap.grid.AssetTileLoader;
import com.jme3.terrain.geomipmap.lodcalc.DistanceLodCalculator;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture.WrapMode;
import com.jme3.water.WaterFilter;
import java.io.File;

public class Main extends SimpleApplication {

    private Material mat_terrain;
    private TerrainGrid terrain;
    private float grassScale = 64;
    private float dirtScale = 16;
    private float rockScale = 128;
    private boolean usePhysics = true;
    private boolean physicsAdded = false;
    private float[] eyeAngles;
    private final float playerSpeed = 0.3f;
    private Quaternion q;
    private Vector3f lightDir = new Vector3f(-4.9236743f, -1.27054665f, 5.896916f);
    private WaterFilter water;

    public static void main(final String[] args) {
        Main app = new Main();
        app.setDisplayFps(false);
        app.setDisplayStatView(false);
        app.start();
    }
    
    @Override
    public void start() {
        if (settings == null) {
            setSettings(new AppSettings(true));
            //loadSettings = true;
        }
        settings.setSettingsDialogImage("Interface/portrait-reflection-03.JPG");
        settings.setTitle("And I Saw Myself");
        super.start();

    }
    
    //public Main() {
     //   super( new StatsAppState(), new DebugKeysAppState() );
    //} 
    
    private CharacterControl player3;

    @Override
    public void simpleInitApp() {
        cam.setLocation(new Vector3f(cam.getLocation().x + 2500, cam.getLocation().y, cam.getLocation().z));
        FilterPostProcessor fpp = new FilterPostProcessor(assetManager);
        water = new WaterFilter(rootNode, lightDir);
        water.setCenter(new Vector3f(-4.3673544f, 1.401453f, -1.4846063f));
        water.setRadius(5);
        water.setMaxAmplitude(2f);
        water.setRefractionStrength(0.2f);
        water.setUseRipples(true);
        water.setSpeed(.1f);
        fpp.addFilter(water);
        viewPort.addProcessor(fpp);
        File file = new File("TerrainGridTestData.zip");
        if (!file.exists()) {
            assetManager.registerLocator("http://jmonkeyengine.googlecode.com/files/TerrainGridTestData.zip", HttpZipLocator.class);
        } else {
            assetManager.registerLocator("TerrainGridTestData.zip", ZipLocator.class);
        }
        eyeAngles = new float[3];
        q = new Quaternion(eyeAngles);
        ScreenshotAppState state = new ScreenshotAppState();
        this.stateManager.attach(state);
        
        final BulletAppState bulletAppState = new BulletAppState();

        // TERRAIN TEXTURE material
        this.mat_terrain = new Material(this.assetManager, "Common/MatDefs/Terrain/HeightBasedTerrain.j3md");

        // Parameters to material:
        // regionXColorMap: X = 1..4 the texture that should be appliad to state X
        // regionX: a Vector3f containing the following information:
        //      regionX.x: the start height of the region
        //      regionX.y: the end height of the region
        //      regionX.z: the texture scale for the region
        //  it might not be the most elegant way for storing these 3 values, but it packs the data nicely :)
        // slopeColorMap: the texture to be used for cliffs, and steep mountain sites
        // slopeTileFactor: the texture scale for slopes
        // terrainSize: the total size of the terrain (used for scaling the texture)
        // GRASS texture
        Texture grass = this.assetManager.loadTexture("Textures/Terrain/splat/grass.jpg");
        grass.setWrap(WrapMode.Repeat);
        this.mat_terrain.setTexture("region1ColorMap", grass);
        this.mat_terrain.setVector3("region1", new Vector3f(88, 200, this.grassScale));

        // DIRT texture
        Texture dirt = this.assetManager.loadTexture("Textures/Terrain/splat/dirt.jpg");
        dirt.setWrap(WrapMode.Repeat);
        this.mat_terrain.setTexture("region2ColorMap", dirt);
        this.mat_terrain.setVector3("region2", new Vector3f(0, 90, this.dirtScale));

        // ROCK texture
        Texture rock = this.assetManager.loadTexture("Textures/Terrain/Rock2/rock.jpg");
        rock.setWrap(WrapMode.Repeat);
        this.mat_terrain.setTexture("region3ColorMap", rock);
        this.mat_terrain.setVector3("region3", new Vector3f(198, 260, this.rockScale));

        this.mat_terrain.setTexture("region4ColorMap", rock);
        this.mat_terrain.setVector3("region4", new Vector3f(198, 260, this.rockScale));

        this.mat_terrain.setTexture("slopeColorMap", rock);
        this.mat_terrain.setFloat("slopeTileFactor", 32);

        this.mat_terrain.setFloat("terrainSize", 129);
//quad.getHeightMap(), terrain.getLocalScale()), 0
        AssetTileLoader grid = new AssetTileLoader(assetManager, "testgrid", "TerrainGrid");
        this.terrain = new TerrainGrid("terrain", 65, 257, grid);
        
        this.terrain.setMaterial(this.mat_terrain);
        this.terrain.setLocalTranslation(0, 0, 0);
        this.terrain.setLocalScale(2f, 1f, 2f);
//        try {
//            BinaryExporter.getInstance().save(terrain, new File("/Users/normenhansen/Documents/Code/jme3/engine/src/test-data/TerrainGrid/"
//                    + "TerrainGrid.j3o"));
//        } catch (IOException ex) {
//            Logger.getLogger(TerrainFractalGridTest.class.getName()).log(Level.SEVERE, null, ex);
//        }
        
        
        
        stateManager.attach(bulletAppState);
        //OUR TERRAIN
        Spatial scene_model;
        scene_model = assetManager.loadModel("Scenes/testScene.j3o");
        CollisionShape sceneShape = CollisionShapeFactory.createMeshShape((Node) scene_model);
        RigidBodyControl sceneControl = new RigidBodyControl(sceneShape, 0);
        scene_model.addControl(sceneControl);
        rootNode.attachChild(scene_model);
        bulletAppState.getPhysicsSpace().add(sceneControl);
        
        Spatial scene_model_2;
        scene_model_2 = assetManager.loadModel("Scenes/testScene2.j3o");
        scene_model_2.setLocalTranslation(2500, 0, 0);
        CollisionShape sceneShape2 = CollisionShapeFactory.createMeshShape((Node) scene_model_2);
        RigidBodyControl sceneControl2 = new RigidBodyControl (sceneShape2, 0);
        scene_model_2.addControl(sceneControl2);

        rootNode.attachChild(scene_model_2);
        bulletAppState.getPhysicsSpace().add(sceneControl2);

        this.getCamera().setLocation(new Vector3f(0, 256, 0));

        this.viewPort.setBackgroundColor(new ColorRGBA(0.7f, 0.8f, 1f, 1f));

        if (usePhysics) {
            CapsuleCollisionShape capsuleShape = new CapsuleCollisionShape(0.5f, 1.8f, 1);
            player3 = new CharacterControl(capsuleShape, 0.5f);
            player3.setJumpSpeed(10);
            player3.setFallSpeed(10);
            player3.setGravity(20);

            player3.setPhysicsLocation(new Vector3f(cam.getLocation().x, 256, cam.getLocation().z));

            bulletAppState.getPhysicsSpace().add(player3);

            terrain.addListener(new TerrainGridListener() {

                public void gridMoved(Vector3f newCenter) {
                }

                public void tileAttached(Vector3f cell, TerrainQuad quad) {
                    while(quad.getControl(RigidBodyControl.class)!=null){
                        quad.removeControl(RigidBodyControl.class);
                    }
                    quad.addControl(new RigidBodyControl(new HeightfieldCollisionShape(quad.getHeightMap(), terrain.getLocalScale()), 0));
                    bulletAppState.getPhysicsSpace().add(quad);
                }

                public void tileDetached(Vector3f cell, TerrainQuad quad) {
                    bulletAppState.getPhysicsSpace().remove(quad);
                    quad.removeControl(RigidBodyControl.class);
                }

            });
        }
        
        this.initKeys();
    }

    private void initKeys() {
        // You can map one or several inputs to one named action
        this.inputManager.addMapping("Lefts", new KeyTrigger(KeyInput.KEY_A));
        this.inputManager.addMapping("Rights", new KeyTrigger(KeyInput.KEY_D));
        this.inputManager.addMapping("Ups", new KeyTrigger(KeyInput.KEY_W));
        this.inputManager.addMapping("Downs", new KeyTrigger(KeyInput.KEY_S));
        this.inputManager.addMapping("Jumps", new KeyTrigger(KeyInput.KEY_SPACE));
        this.inputManager.addListener(this.actionListener, "Lefts");
        this.inputManager.addListener(this.actionListener, "Rights");
        this.inputManager.addListener(this.actionListener, "Ups");
        this.inputManager.addListener(this.actionListener, "Downs");
        this.inputManager.addListener(this.actionListener, "Jumps");
    }
    private boolean left;
    private boolean right;
    private boolean up;
    private boolean down;
    private final ActionListener actionListener = new ActionListener() {

        @Override
        public void onAction(final String name, final boolean keyPressed, final float tpf) {
            if (name.equals("Lefts")) {
                if (keyPressed) {
                    Main.this.left = true;
                } else {
                    Main.this.left = false;
                }
            } else if (name.equals("Rights")) {
                if (keyPressed) {
                    Main.this.right = true;
                } else {
                    Main.this.right = false;
                }
            } else if (name.equals("Ups")) {
                if (keyPressed) {
                    Main.this.up = true;
                } else {
                    Main.this.up = false;
                }
            } else if (name.equals("Downs")) {
                if (keyPressed) {
                    Main.this.down = true;
                } else {
                    Main.this.down = false;
                }
            } else if (name.equals("Jumps")) {
                Main.this.player3.jump();
            }
        }
    };
    private final Vector3f walkDirection = new Vector3f();

    @Override
    public void simpleUpdate(final float tpf) {
        this.cam.getRotation().toAngles(eyeAngles);
        if(eyeAngles[0]>1.2f){
            eyeAngles[0] = 1.2f;
            this.cam.setRotation(q.fromAngles(eyeAngles));
        }
        else if(eyeAngles[0]<-1.2f){
            eyeAngles[0] = -1.2f;
            this.cam.setRotation(q.fromAngles(eyeAngles));
        }
            

        
        Vector3f camDir = new Vector3f(this.cam.getDirection().clone().multLocal(0.6f).x,0,this.cam.getDirection().clone().multLocal(0.6f).z);
        Vector3f camLeft = this.cam.getLeft().clone().multLocal(0.4f);
        this.walkDirection.set(0, 0, 0);
        if (this.left) {
            this.walkDirection.addLocal(camLeft);
 
        }
        if (this.right) {
            this.walkDirection.addLocal(camLeft.negate());
        }
        if (this.up) {
            this.walkDirection.addLocal(camDir);
        }
        if (this.down) {
            this.walkDirection.addLocal(camDir.negate());
        }

        if (usePhysics) {
            this.player3.setWalkDirection(this.walkDirection.multLocal(playerSpeed));
            this.cam.setLocation(this.player3.getPhysicsLocation());
        }
    }
}
