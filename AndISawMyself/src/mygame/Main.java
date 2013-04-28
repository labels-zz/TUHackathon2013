package mygame;
 
import com.jme3.app.*;
import com.jme3.app.SimpleApplication;
import com.jme3.material.Material;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Box;
import com.jme3.math.ColorRGBA;
import com.jme3.renderer.RenderManager;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.system.AppSettings;
import com.jme3.system.JmeSystem;

 
/** Sample 2 - How to use nodes as handles to manipulate objects in the scene.
 * You can rotate, translate, and scale objects by manipulating their parent nodes.
 * The Root Node is special: Only what is attached to the Root Node appears in the scene. */
 
public class Main extends SimpleApplication {
    
    Node[] platforms;
    Geometry player;
    Geometry[] enemies;
    Geometry[] weapons;
    Spatial artifact;
    Node pivot;
 
    public static void main(String[] args){
        Main app = new Main();
        //app.setShowSettings(false);
        app.start();
    }
    
    @Override
    public void start() {
        if (settings == null) {
            setSettings(new AppSettings(true));
            //loadSettings = true;
        }
        settings.setSettingsDialogImage("Interface/Tower_resized.JPG");
        settings.setTitle("TOWER CLIMB");
        super.start();

    }
    
    public Main() {
        super( new StatsAppState(), new DebugKeysAppState() );
    } 
 
    @Override
    public void simpleInitApp() {
 
        /** create a blue box at coordinates (1,-1,1) */
        Box box1 = new Box( Vector3f.ZERO, 1,1,1);
        Geometry blue = new Geometry("Box", box1);
        Material mat1 = new Material(assetManager, 
                "Common/MatDefs/Misc/Unshaded.j3md");
        mat1.setColor("Color", ColorRGBA.Blue);
        blue.setMaterial(mat1);
        blue.move(1,-1,1);
 
        /** create a red box straight above the blue one at (1,3,1) */
        Box box2 = new Box( Vector3f.ZERO, 1,1,1);
        Geometry red = new Geometry("Box", box2);
        Material mat2 = new Material(assetManager, 
                "Common/MatDefs/Misc/Unshaded.j3md");
        mat2.setColor("Color", ColorRGBA.Red);
        red.setMaterial(mat2);
        red.move(1,3,1);
 
        /** Create a pivot node at (0,0,0) and attach it to the root node */
        pivot = new Node("pivot");
        rootNode.attachChild(pivot); // put this node in the scene
 
        /** Attach the two boxes to the *pivot* node. */
        pivot.attachChild(blue);
        pivot.attachChild(red);
        /** Rotate the pivot node: Note that both boxes have rotated! */
        pivot.rotate(.4f,.4f,0f);
    }
    
        @Override
    public void simpleUpdate(float tpf) {
        //TODO: add update code


    }

    @Override
    public void simpleRender(RenderManager rm) {
        //TODO: add render code

    }
}