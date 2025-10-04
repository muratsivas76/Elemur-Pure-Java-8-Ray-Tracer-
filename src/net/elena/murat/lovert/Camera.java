package net.elena.murat.lovert;

import net.elena.murat.math.Point3;
import net.elena.murat.math.Vector3;

public class Camera
extends Object
implements java.io.Serializable {
  
  private Point3 cameraPosition=new Point3 (0, 1, 5);
  private Point3 lookAt=new Point3 (0, 0, -1);
  
  private Vector3 upVector=new Vector3 (0, 1, 0);
  
  private boolean ortographic=false;
  
  private double fov=0.0;
  private double ortographicScale=1.0;
  
  private int maxRecursionDepth=3;
  
  private boolean isReflective=true;
  private boolean isRefractive=true;
  private boolean shadowsEnabled = true;
  
  public Camera () {
    super ();
  }
  
  public String toString () {
    return "Camera Settings...";
  }
  
  
  // Getters
  public Point3 getCameraPosition () {
    return cameraPosition;
  }
  
  public Point3 getLookAt () {
    return lookAt;
  }
  
  public Vector3 getUpVector () {
    return upVector;
  }
  
  public double getFov () {
    return fov;
  }
  
  public int getMaxRecursionDepth () {
    return maxRecursionDepth;
  }
  
  public double getOrthographicScale () {
    return this.ortographicScale;
  }
  
  public boolean isOrthographic () {
    return this.ortographic;
  }
  
  public boolean isReflective () {
    return this.isReflective;
  }
  
  public boolean isRefractive () {
    return this.isRefractive;
  }
  
  public boolean isShadowsEnabled() {
    return shadowsEnabled;
  }
  
  // Setters
  public void setCameraPosition (Point3 pv) {
    this.cameraPosition=pv;
  }
  
  public void setLookAt (Point3 pv) {
    this.lookAt=pv;
  }
  
  public void setUpVector (Vector3 vv) {
    this.upVector=vv;
  }
  
  public void setFov (double dbl) {
    this.fov=dbl;
  }
  
  public void setMaxRecursionDepth (int mr) {
    this.maxRecursionDepth=mr;
  }
  
  public void setOrthographicScale (double scl) {
    this.ortographicScale=scl;
  }
  
  public void setOrthographic (boolean bb) {
    this.ortographic=bb;
  }
  
  public void setReflective (boolean rb) {
    this.isReflective=rb;
  }
  
  public void setRefractive (boolean rb) {
    this.isRefractive=rb;
  }
  
  public void setShadowsEnabled(boolean enabled) {
    this.shadowsEnabled = enabled;
  }
  
}
