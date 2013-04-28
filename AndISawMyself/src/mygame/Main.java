package mygame;

import com.jme3.app.DebugKeysAppState;
import com.jme3.app.SimpleApplication;
import com.jme3.app.StatsAppState;
import com.jme3.app.state.ScreenshotAppState;
import com.jme3.asset.plugins.HttpZipLocator;
import com.jme3.asset.plugins.ZipLocator;
import com.jme3.audio.AudioNode;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.shapes.CapsuleCollisionShape;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.collision.shapes.HeightfieldCollisionShape;
import com.jme3.bullet.control.CharacterControl;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.collision.CollisionResults;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Ray;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.post.FilterPostProcessor;
import com.jme3.scene.Geometry;
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
    private WaterFilter water2;
    private AudioNode birds, bubbles;
    Geometry holding;
    Node pickables;
    final BulletAppState bulletAppState = new BulletAppState();

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
    
    private CharacterControl player;

    @Override
    public void simpleInitApp() {
        pickables = new Node("Pickables");
                FilterPostProcessor fpp = new FilterPostProcessor(assetManager);
        water = new WaterFilter(rootNode, lightDir);
        Vector3f firstPuddle = new Vector3f(-4.25f, 10f, 140f);
        water.setCenter(firstPuddle);
        water.setRadius(5);
        water.setMaxAmplitude(6f);
        water.setUseRefraction(false);
        water.setUseRipples(true);
        water.setSpeed(.1f);
        water.setShoreHardness(.000001f);
        water.setWaterTransparency(2f);
        water.setDeepWaterColor(ColorRGBA.DarkGray);
        water.setWaterColor(ColorRGBA.Gray);
        fpp.addFilter(water);
        water2 = new WaterFilter(rootNode, lightDir);
        water2.setCenter(new Vector3f(2500f, 0f, 0f).add(firstPuddle));
        water2.setRadius(1000);
        water2.setWaterHeight(1000);
        water2.setMaxAmplitude(6f);
        fpp.addFilter(water2);
        viewPort.addProcessor(fpp);
        cam.setLocation(new Vector3f(cam.getLocation().x, cam.getLocation().y, cam.getLocation().z));
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
        scene_model = assetManager.loadModel("Scenes/theScene1.j3o");
        CollisionShape sceneShape = CollisionShapeFactory.createMeshShape((Node) scene_model);
        RigidBodyControl sceneControl = new RigidBodyControl(sceneShape, 0);
        scene_model.addControl(sceneControl);
        rootNode.attachChild(scene_model);
        bulletAppState.getPhysicsSpace().add(sceneControl);
        
        Spatial scene_model_2;
        scene_model_2 = assetManager.loadModel("Scenes/theScene2.j3o");
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
            player = new CharacterControl(capsuleShape, 0.5f);
            player.setJumpSpeed(10);
            player.setFallSpeed(10);
            player.setGravity(35);

            //player.setPhysicsLocation(new Vector3f(cam.getLocation().x, 10, cam.getLocation().z));
            player.setPhysicsLocation(new Vector3f(-5, 2, 160));

            bulletAppState.getPhysicsSpace().add(player);

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
        birds = new AudioNode(assetManager, "Sounds/birds.ogg");
        birds.setVolume(3);
        birds.setLooping(true);
        bubbles = new AudioNode(assetManager, "Sounds/bubbles.ogg");
        bubbles.setVolume(3);
        bubbles.setLooping(true);
        birds.play();
        this.initKeys();
    }

    private void initKeys() {
        // You can map one or several inputs to one named action
        this.inputManager.addMapping("Lefts", new KeyTrigger(KeyInput.KEY_A));
        this.inputManager.addMapping("Rights", new KeyTrigger(KeyInput.KEY_D));
        this.inputManager.addMapping("Ups", new KeyTrigger(KeyInput.KEY_W));
        this.inputManager.addMapping("Downs", new KeyTrigger(KeyInput.KEY_S));
        this.inputManager.addMapping("Jumps", new KeyTrigger(KeyInput.KEY_SPACE));
        inputManager.addMapping("PickTarget", new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
    
        this.inputManager.addListener(this.actionListener, "Lefts");
        this.inputManager.addListener(this.actionListener, "Rights");
        this.inputManager.addListener(this.actionListener, "Ups");
        this.inputManager.addListener(this.actionListener, "Downs");
        this.inputManager.addListener(this.actionListener, "Jumps");
        inputManager.addListener(this.actionListener, "PickTarget");
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
                Main.this.player.jump();
            }
            
            else if (name.equals("PickTarget"))
    {
        if(holding == null)
        {
        // Reset results list.
        CollisionResults results = new CollisionResults();
        // Convert screen click to 3d position
        Vector2f click2d = inputManager.getCursorPosition();
        Vector3f click3d = cam.getWorldCoordinates(new Vector2f(click2d.x, click2d.y), 0f).clone();
        //Vector3f dir = cam.getWorldCoordinates(new Vector2f(click2d.x, click2d.y), 1f).subtractLocal(click3d).normalizeLocal();
        Vector3f dir = cam.getDirection().clone().multLocal(20f);
        // Aim the ray from the clicked spot forwards.
        Ray ray = new Ray(click3d, dir);
        // Collect intersections between ray and all nodes in results list.
        rootNode.collideWith(ray, results);
        // (Print the results so we see what is going on:)
        for (int i = 0; i < results.size(); i++) {
          // (For each “hit”, we know distance, impact point, geometry.)
          float dist = results.getCollision(i).getDistance();
          Vector3f pt = results.getCollision(i).getContactPoint();
          String target = results.getCollision(i).getGeometry().getName();
          System.out.println("Selection #" + i + ": " + target + " at " + pt + ", " + dist + " WU away.");
        }
        // Use the results -- we rotate the selected geometry.
        if (results.size() > 0) {
          // The closest result is the target that the player picked:
          holding = results.getClosestCollision().getGeometry();
          // Here comes the action:
          //if (target.getName().equals("cannonball")) {
            //pickables.detachChild(target);
            bulletAppState.getPhysicsSpace().remove(holding);
            //playerNode.attachChild(target);
            holding.setLocalTranslation
                    (player.getPhysicsLocation().x + (cam.getDirection().clone().multLocal(0.6f).x)*10, player.getPhysicsLocation().y + (cam.getDirection().clone().multLocal(0.6f).y)*10, player.getPhysicsLocation().z + (cam.getDirection().clone().multLocal(0.6f).z)*10);
            //target.setLocalTranslation(player.getPhysicsLocation() + cam.getDirection().clone().multLocal(0.6f));
          }
        }
        else
        {
            for(int i = 0; i < holding.getNumControls(); i++)
            {
                holding.removeControl(holding.getControl(i));
            }
            holding.addControl(new RigidBodyControl(CollisionShapeFactory.createBoxShape(holding)));
            bulletAppState.getPhysicsSpace().add(holding);
            holding = null;
        }
    }
            
        }
    };
    private final Vector3f walkDirection = new Vector3f();

    @Override
    public void simpleUpdate(final float tpf) {
        System.out.println(cam.getLocation().x + ", " + cam.getLocation().y + ", " + cam.getLocation().z);
        this.cam.getRotation().toAngles(eyeAngles);
        if(eyeAngles[0]>1.2f){
            eyeAngles[0] = 1.2f;
            this.cam.setRotation(q.fromAngles(eyeAngles));
        }
        else if(eyeAngles[0]<-1.2f){
            eyeAngles[0] = -1.2f;
            this.cam.setRotation(q.fromAngles(eyeAngles));
        }
            
        if(cam.getLocation().y< -5)
        {
            if(cam.getLocation().x<=2300){
                player.setPhysicsLocation(new Vector3f(cam.getLocation().x+2500, 10, cam.getLocation().z+10));
                player.setGravity(7);
                birds.pause();
                bubbles.play();
            }
            else{
                player.setPhysicsLocation(new Vector3f(cam.getLocation().x-2500, 10, cam.getLocation().z+10));
                player.setGravity(35);
                bubbles.pause();
                birds.play();
            }
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
            this.player.setWalkDirection(this.walkDirection.multLocal(playerSpeed));
            this.cam.setLocation(this.player.getPhysicsLocation());
        }
        
        if(holding != null){holding.setLocalTranslation
                    (player.getPhysicsLocation().x + (camDir.x)*20, player.getPhysicsLocation().y + (camDir.y)*20, player.getPhysicsLocation().z + (camDir.z)*20);
    }
    }
}
