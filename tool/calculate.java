public interface UVCalculator {
  float[] calculateUV(Point3D localPoint);
}

public class BillboardUVCalculator implements UVCalculator {
  private float width, height;
  
  public BillboardUVCalculator(float width, float height) {
    this.width = width;
    this.height = height;
  }
  
  @Override
  public float[] calculateUV(Point3D localPoint) {
    float u = (localPoint.x / width) + 0.5f;
    float v = (localPoint.y / height) + 0.5f;
    return new float[]{u, v};
  }
}

public class SphereUVCalculator implements UVCalculator {
  @Override
  public float[] calculateUV(Point3D localPoint) {
    // Küre için UV hesaplama
    Vec3 normalized = localPoint.normalize();
    float phi = (float) Math.atan2(normalized.z, normalized.x);
    float theta = (float) Math.asin(normalized.y);
    
    float u = 1 - (phi + (float)Math.PI) / (2 * (float)Math.PI);
    float v = (theta + (float)Math.PI/2) / (float)Math.PI;
    
    return new float[]{u, v};
  }
}