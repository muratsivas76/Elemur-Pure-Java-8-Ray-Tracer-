package net.elena.murat.material;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import net.elena.murat.math.*;
import net.elena.murat.light.Light;
import net.elena.murat.util.ColorUtil;

public class InvertLightColorMaterial implements Material {
    
    @Override
    public Color getColorAt(Point3 point, Vector3 normal, Light light, Point3 viewerPoint) {
        // Light'ın orijinal rengini al
        Color originalColor = light.getColor();
        
        // Rengi invert et: 255'ten çıkar
        int invertedRed = 255 - originalColor.getRed();
        int invertedGreen = 255 - originalColor.getGreen();
        int invertedBlue = 255 - originalColor.getBlue();
        
        return new Color(invertedRed, invertedGreen, invertedBlue);
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
        return 1.0; // Hava gibi davranır (ışık kırılmaz)
    }
}