package net.elena.murat.material;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import net.elena.murat.math.*;
import net.elena.murat.light.Light;
import net.elena.murat.util.ColorUtil;

public class EmeraldMaterial implements Material {

    private Color baseColor;   
    private double density;   
    private double reflectivity;
    private Matrix4 objectTransform;

    public EmeraldMaterial() {
        this.baseColor = new Color(20, 220, 60); 
        this.density = 0.8;
        this.reflectivity = 0.12;
    }

    public EmeraldMaterial(Color baseColor, double density, double reflectivity) {
        this.baseColor = baseColor;
        this.density = Math.max(0, Math.min(1, density));
        this.reflectivity = Math.max(0, Math.min(1, reflectivity));
    }

    @Override
    public Color getColorAt(Point3 point, Vector3 normal, Light light, Point3 viewerPoint) {
        Color lightColor = light.getColor();
        
        double red = (lightColor.getRed() * baseColor.getRed() / 255.0) * density * 0.3;
        double green = (lightColor.getGreen() * baseColor.getGreen() / 255.0) * density;
        double blue = (lightColor.getBlue() * baseColor.getBlue() / 255.0) * density * 0.4;
        
        int r = ColorUtil.clampColorValue((int) red);
        int g = ColorUtil.clampColorValue((int) green);
        int b = ColorUtil.clampColorValue((int) blue);
        
        return new Color(r, g, b);
    }

    @Override
    public void setObjectTransform(Matrix4 tm) {
        this.objectTransform = tm;
    }

    @Override
    public double getTransparency() {
        return 0.75;
    }

    @Override
    public double getIndexOfRefraction() {
        return 1.58; 
    }

    @Override
    public double getReflectivity() {
        return this.reflectivity;
    }

    public Color getBaseColor() {
        return baseColor;
    }

    public void setBaseColor(Color baseColor) {
        this.baseColor = baseColor;
    }

    public double getDensity() {
        return density;
    }

    public void setDensity(double density) {
        this.density = Math.max(0, Math.min(1, density));
    }

    public double getReflectivityValue() {
        return reflectivity;
    }

    public void setReflectivity(double reflectivity) {
        this.reflectivity = Math.max(0, Math.min(1, reflectivity));
    }
	
}
