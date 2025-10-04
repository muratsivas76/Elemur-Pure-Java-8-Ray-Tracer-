package net.elena.murat.material;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import net.elena.murat.math.*;
import net.elena.murat.light.Light;
import net.elena.murat.util.ColorUtil;

public class ObsidianMaterial implements Material {

    private double edgeSharpness; 
    private double reflectivity;
    private Matrix4 objectTransform;

    public ObsidianMaterial() {
        this.edgeSharpness = 0.3; 
        this.reflectivity = 0.04;  
    }

    public ObsidianMaterial(double edgeSharpness, double reflectivity) {
        this.edgeSharpness = Math.max(0, Math.min(1, edgeSharpness));
        this.reflectivity = Math.max(0, Math.min(0.1, reflectivity)); 
    }

    @Override
    public Color getColorAt(Point3 point, Vector3 normal, Light light, Point3 viewerPoint) {
        Color baseColor = Color.BLACK;
        
        Vector3 viewDir = new Vector3(point, viewerPoint).normalize();
        double dotProduct = normal.dot(viewDir);
        
        if (edgeSharpness > 0) {
            double edgeFactor = Math.pow(1.0 - Math.abs(dotProduct), 2.0) * edgeSharpness;
            
            int edgeValue = (int) (30 * edgeFactor);
            edgeValue = clamp(edgeValue);
            
            return new Color(edgeValue, edgeValue, edgeValue);
        }
        
        return baseColor;
    }

    private int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }

    @Override
    public void setObjectTransform(Matrix4 tm) {
        this.objectTransform = tm;
    }

    @Override
    public double getTransparency() {
        return 0.02; 
    }

    @Override
    public double getIndexOfRefraction() {
        return 1.48;
    }

    @Override
    public double getReflectivity() {
        return this.reflectivity;
    }

    public double getEdgeSharpness() {
        return edgeSharpness;
    }

    public void setEdgeSharpness(double edgeSharpness) {
        this.edgeSharpness = Math.max(0, Math.min(1, edgeSharpness));
    }

    public double getReflectivityValue() {
        return reflectivity;
    }

    public void setReflectivity(double reflectivity) {
        this.reflectivity = Math.max(0, Math.min(0.1, reflectivity));
    }
	
}
