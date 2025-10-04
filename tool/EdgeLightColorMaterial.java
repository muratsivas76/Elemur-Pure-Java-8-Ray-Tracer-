package net.elena.murat.material;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import net.elena.murat.math.*;
import net.elena.murat.light.Light;
import net.elena.murat.util.ColorUtil;

public class EdgeLightColorMaterial implements Material {
    
    private float edgeThreshold = 0.2f; // Kenar hassasiyeti (0-1 arası)
    private Color edgeColor = new Color(30, 30, 30); // Kenar rengi (koyu gri/siyahımsı)
    
    @Override
    public Color getColorAt(Point3 point, Vector3 normal, Light light, Point3 viewerPoint) {
        // Işık yönünü hesapla
        Vector3 lightDirection = light.getPosition().subtract(point).normalize();
        
        // Normal ve ışık yönünün dot product'ını al (cosine değeri)
        float dotProduct = (float) normal.dot(lightDirection);
        
        // Dot product threshold'a göre kenar kontrolü
        if (dotProduct <= edgeThreshold) {
            return edgeColor; // Kenar bölgesi - koyu renk
        } else {
            return light.getColor(); // Normal aydınlatma - light rengi
        }
    }
    
    @Override
    public void setObjectTransform(Matrix4 tm) {
        // Bu material transformasyonla ilgilenmiyor
    }
    
    @Override
    public double getTransparency() {
        return 0.0; // Tamamen opak
    }
    
    @Override
    public double getReflectivity() {
        return 0.0; // Yansıma yok
    }
    
    @Override
    public double getIndexOfRefraction() {
        return 1.0; // Hava gibi davranır
    }
    
    // İsteğe bağlı: threshold ve edge color setter'ları
    public void setEdgeThreshold(float threshold) {
        this.edgeThreshold = Math.max(0.0f, Math.min(1.0f, threshold));
    }
    
    public void setEdgeColor(Color color) {
        this.edgeColor = color;
    }
}