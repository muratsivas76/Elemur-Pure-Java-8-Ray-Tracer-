package net.elena.murat.material;

import java.awt.Color;

import net.elena.murat.light.Light;
import net.elena.murat.math.*;
import net.elena.murat.util.ColorUtil;

public class PovDielectricMaterial implements Material {
    private double currentTransparency = 0.7;
	private double transparency = currentTransparency;
    private double currentReflectivity;

    private double ior;
    private double fadeDistance;
    private double fadePower;
    private Color fadeColor;
    private double ambient;
    private double diffuse;
    private double reflection;
    private double specular;
    private double roughness;
    private double phong;
    private double phongSize;
    private Color pigmentColor;
    private double filter;
    private double transmit;

    private Matrix4 objectTransform;

    public PovDielectricMaterial() {
        this.ior = 1.5;
        this.fadeDistance = 2.0;
        this.fadePower = 3;
        this.fadeColor = new Color(0.9f, 0.0f, 0.0f, 0.5f);
        this.ambient = 0.1;
        this.diffuse = 0.1;
        this.reflection = 0.25;
        this.specular = 0.8;
        this.roughness = 0.003;
        this.phong = 0;
        this.phongSize = 40;
        this.pigmentColor = new Color(0.3f, 0.9f, 0.5f, 0.6f);
        this.filter = 0.7;
        this.transmit = 0.7;
        this.objectTransform = Matrix4.identity();
    }

//Original
/***
  @Override
  public Color getColorAt(Point3 point, Vector3 normal, Light light, Point3 viewerPoint) {
    Vector3 lightDir = light.getDirectionTo(point).normalize();
    double diffuseFactor = Math.max(0, normal.dot(lightDir));
    
    // Calculate Fresnel reflection coefficient (for external use or debugging)
    Vector3 viewDir = viewerPoint.subtract(point).normalize();
    double fresnel = Vector3.calculateFresnel(viewDir, normal, 1.0, ior);
    
    this.currentReflectivity = Math.min(0.95, reflection + (fresnel * 0.8));
    this.currentTransparency = Math.max(0.05, transparency * (1.0 - fresnel * 0.2));
    
    // Basic diffuse color
    Color diffuse = ColorUtil.multiplyColor(pigmentColor, diffuseFactor * light.getIntensity());
    
    // Specular component
    Vector3 reflectDir = lightDir.reflect(normal);
    double specularFactor = Math.pow(Math.max(0, viewDir.dot(reflectDir)), 32);
    Color specular = ColorUtil.multiplyColor(light.getColor(), specularFactor * 0.5);
    
    // Combine diffuse and specular with light color
    Color c1 = ColorUtil.multiplyColors(light.getColor(), diffuse);
    Color result = ColorUtil.add(c1, specular);
    
    return ColorUtil.clampColor(result);
  }
*/

@Override
public Color getColorAt(Point3 point, Vector3 normal, Light light, Point3 viewerPoint) {
    Vector3 lightDir = light.getDirectionTo(point).normalize();
    double diffuseFactor = Math.max(0, normal.dot(lightDir));
    
    Vector3 viewDir = viewerPoint.subtract(point).normalize();
    double fresnel = Vector3.calculateFresnel(viewDir, normal, 1.0, ior);
    
    this.currentReflectivity = Math.min(0.95, reflection + (fresnel * 0.8));
	this.currentTransparency = Math.max(0.05, Math.min(1.0, (filter + transmit) * (1.0 - fresnel * 0.3)));    
    
    Color diffuse = ColorUtil.multiplyColor(pigmentColor, diffuseFactor * light.getIntensity());
    Vector3 reflectDir = lightDir.reflect(normal);
    double specularFactor = Math.pow(Math.max(0, viewDir.dot(reflectDir)), 32);
    Color specular = ColorUtil.multiplyColor(light.getColor(), specularFactor * 0.5);
    Color c1 = ColorUtil.multiplyColors(light.getColor(), diffuse);
    Color result = ColorUtil.add(c1, specular);
    
    float[] rgb = result.getRGBColorComponents(null);
    float alpha = (float) Math.max(0.0, Math.min(1.0, currentTransparency));    
    
	return new Color(rgb[0], rgb[1], rgb[2], alpha);
}

public Color getFilterColorInside() {
    float[] rgb = pigmentColor.getRGBColorComponents(null);
    double transparency = Math.max(0.0, Math.min(1.0, currentTransparency));
    
    return new Color(
        (float)Math.max(0.0, Math.min(1.0, rgb[0] * transparency)),
        (float)Math.max(0.0, Math.min(1.0, rgb[1] * transparency)),
        (float)Math.max(0.0, Math.min(1.0, rgb[2] * transparency))
	);
}

public Color getFilterColorOutside() {
    return getFilterColorInside();
}

    private Vector3 reflect(Vector3 incident, Vector3 normal) {
        return incident.subtract(normal.multiply(2 * incident.dot(normal)));
    }

    @Override
    public void setObjectTransform(Matrix4 tm) {
        this.objectTransform = tm;
    }

@Override
public double getTransparency() {
    return currentTransparency;
}

    @Override
    public double getReflectivity() {
        return currentReflectivity;
    }

    @Override
    public double getIndexOfRefraction() {
        return ior;
    }

    public void setIor(double ior) { this.ior = ior; }
    public void setFadeDistance(double fadeDistance) { this.fadeDistance = fadeDistance; }
    public void setFadePower(double fadePower) { this.fadePower = fadePower; }
    public void setFadeColor(Color fadeColor) { this.fadeColor = fadeColor; }
    public void setAmbient(double ambient) { this.ambient = ambient; }
    public void setDiffuse(double diffuse) { this.diffuse = diffuse; }
    public void setReflection(double reflection) { this.reflection = reflection; }
    public void setSpecular(double specular) { this.specular = specular; }
    public void setRoughness(double roughness) { this.roughness = roughness; }
    public void setPhong(double phong) { this.phong = phong; }
    public void setPhongSize(double phongSize) { this.phongSize = phongSize; }
    public void setPigmentColor(Color pigmentColor) { this.pigmentColor = pigmentColor; }
    public void setFilter(double filter) { this.filter = filter; }
    public void setTransmit(double transmit) { this.transmit = transmit; }

    public static PovDielectricMaterial createGlass() {
        PovDielectricMaterial glass = new PovDielectricMaterial();
        glass.setIor(1.5);
        glass.setFilter(0.7);
        glass.setTransmit(0.7);
        glass.setPigmentColor(new Color(0.8f, 0.9f, 1.0f, 0.7f));
        return glass;
    }
	
}
