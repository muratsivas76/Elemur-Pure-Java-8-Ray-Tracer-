package net.elena.murat.material.pbr;

import java.awt.Color;

import net.elena.murat.math.*;
import net.elena.murat.light.*;
import net.elena.murat.util.ColorUtil;

public class CeramicTilePBRMaterial implements PBRCapableMaterial {
    protected final Color tileColor;
    private final Color groutColor;
    private final double tileSize;
    private final double groutWidth;
    private final double tileRoughness;
    private final double groutRoughness;
    private final double tileSpecular;
    private final double groutSpecular;
    
    // Advanced PBR
    private final double fresnelIntensity;
    private final double normalMicroFacet;
    private final double reflectionSharpness;
    private final double energyConservation;
    
    // Default values (glossy ceramic)
    public static final Color DEFAULT_TILE_COLOR = new Color(245, 245, 255); // Ice white
    public static final Color DEFAULT_GROUT_COLOR = new Color(70, 70, 80);   // Blackish grout
    public static final double DEFAULT_TILE_SIZE = 0.6;
    public static final double DEFAULT_GROUT_WIDTH = 0.03;
    public static final double DEFAULT_TILE_ROUGHNESS = 0.1;  // Very glossy
    public static final double DEFAULT_GROUT_ROUGHNESS = 0.6; // Semi-matte grout
    public static final double DEFAULT_TILE_SPECULAR = 0.5;
    public static final double DEFAULT_GROUT_SPECULAR = 0.3;
    
    // Default advanced PBR values
    public static final double DEFAULT_FRESNEL_INTENSITY = 0.9;
    public static final double DEFAULT_NORMAL_MICRO_FACET = 0.02;
    public static final double DEFAULT_REFLECTION_SHARPNESS = 0.95;
    public static final double DEFAULT_ENERGY_CONSERVATION = 1.0;
    
    public CeramicTilePBRMaterial() {
        this(DEFAULT_TILE_COLOR, DEFAULT_GROUT_COLOR,
             DEFAULT_TILE_SIZE, DEFAULT_GROUT_WIDTH,
             DEFAULT_TILE_ROUGHNESS, DEFAULT_GROUT_ROUGHNESS,
             DEFAULT_TILE_SPECULAR, DEFAULT_GROUT_SPECULAR,
             DEFAULT_FRESNEL_INTENSITY, DEFAULT_NORMAL_MICRO_FACET,
             DEFAULT_REFLECTION_SHARPNESS, DEFAULT_ENERGY_CONSERVATION);
    }
    
    public CeramicTilePBRMaterial(Color tileColor, Color groutColor,
                                 double tileSize, double groutWidth,
                                 double tileRoughness, double groutRoughness) {
        this(tileColor, groutColor, tileSize, groutWidth, 
             tileRoughness, groutRoughness, DEFAULT_TILE_SPECULAR, DEFAULT_GROUT_SPECULAR,
             DEFAULT_FRESNEL_INTENSITY, DEFAULT_NORMAL_MICRO_FACET,
             DEFAULT_REFLECTION_SHARPNESS, DEFAULT_ENERGY_CONSERVATION);
    }
    
    public CeramicTilePBRMaterial(Color tileColor, Color groutColor,
                                 double tileSize, double groutWidth,
                                 double tileRoughness, double groutRoughness,
                                 double tileSpecular, double groutSpecular) {
        this(tileColor, groutColor, tileSize, groutWidth, 
             tileRoughness, groutRoughness, tileSpecular, groutSpecular,
             DEFAULT_FRESNEL_INTENSITY, DEFAULT_NORMAL_MICRO_FACET,
             DEFAULT_REFLECTION_SHARPNESS, DEFAULT_ENERGY_CONSERVATION);
    }
    
    // Full constructor
    public CeramicTilePBRMaterial(Color tileColor, Color groutColor,
                                 double tileSize, double groutWidth,
                                 double tileRoughness, double groutRoughness,
                                 double tileSpecular, double groutSpecular,
                                 double fresnelIntensity, double normalMicroFacet,
                                 double reflectionSharpness, double energyConservation) {
        this.tileColor = ColorUtil.adjustSaturation(tileColor, 1.2f);
        this.groutColor = groutColor;
        this.tileSize = Math.max(0.1, tileSize);
        this.groutWidth = Math.max(0.01, Math.min(0.1, groutWidth));
        this.tileRoughness = clamp(tileRoughness, 0.01, 1.0);
        this.groutRoughness = clamp(groutRoughness, 0.01, 1.0);
        this.tileSpecular = clamp(tileSpecular, 0.0, 1.0);
        this.groutSpecular = clamp(groutSpecular, 0.0, 1.0);
        
        // Advanced
        this.fresnelIntensity = clamp(fresnelIntensity, 0.0, 1.0);
        this.normalMicroFacet = clamp(normalMicroFacet, 0.0, 0.1);
        this.reflectionSharpness = clamp(reflectionSharpness, 0.5, 1.0);
        this.energyConservation = clamp(energyConservation, 0.8, 1.0);
    }
    
    @Override
    public Color getColorAt(Point3 point, Vector3 normal, Light light, Point3 viewerPos) {
        // 1. Tile pattern detection with proper UV mapping
        double u = ((point.x % tileSize) + tileSize) % tileSize / tileSize;
        double v = ((point.z % tileSize) + tileSize) % tileSize / tileSize;
        
        boolean isGroutLocal = (u < groutWidth/tileSize || u > 1.0 - groutWidth/tileSize ||
                               v < groutWidth/tileSize || v > 1.0 - groutWidth/tileSize);
        
        // 2. Select base color and material properties
        Color baseColor = isGroutLocal ? groutColor : tileColor;
        double roughness = isGroutLocal ? groutRoughness : tileRoughness;
        double specularIntensity = isGroutLocal ? groutSpecular : tileSpecular;
        
        // 3. Light direction calculations
        Vector3 lightDir = light.getDirectionTo(point).normalize();
        Vector3 viewDir = new Vector3(point, viewerPos).normalize();
        
        // 4. Fresnel
        double cosTheta = Math.max(0.001, normal.dot(viewDir));
        double fresnel = Math.pow(1.0 - cosTheta, 5.0);
        fresnel = fresnelIntensity * (0.04 + 0.96 * fresnel);
        
        // 5. Mikro roughness
        Vector3 perturbedNormal = applyMicroFacet(normal, point);
        
        // 6. Diffuse component (Lambertian) - Energy conservation
        double NdotL = Math.max(0.0, perturbedNormal.dot(lightDir));
        Color diffuseColor = ColorUtil.multiply(baseColor, (float)(NdotL * energyConservation));
        
        // 7. Specular component - Yeni özelliklerle geliştirilmiş
        Vector3 reflectDir = reflect(lightDir.negate(), perturbedNormal);
        double RdotV = Math.max(0.0, reflectDir.dot(viewDir));
        double specularPower = (1.0 - roughness * reflectionSharpness) * 256.0 + 1.0;
        double specular = specularIntensity * fresnel * Math.pow(RdotV, specularPower);
        
        // 8. Ambient component
        double ambientIntensity = isGroutLocal ? 0.3 : 0.2;
        Color ambientColor = ColorUtil.multiply(baseColor, (float)ambientIntensity);
        
        // 9. Combine all components
        Color finalColor = ColorUtil.add(ambientColor, diffuseColor);
        finalColor = ColorUtil.add(finalColor, 
                                 ColorUtil.multiply(Color.WHITE, (float)specular));
        
        // 10. Apply contrast and gamma correction
        finalColor = ColorUtil.adjustContrast(finalColor, 1.1f);
        return ColorUtil.gammaCorrect(finalColor, 0.8f);
    }
    
    /**
     * Mikro roughness
     */
    private Vector3 applyMicroFacet(Vector3 normal, Point3 point) {
        if (normalMicroFacet <= 0.0) {
            return normal;
        }
        
        // Simple normal perturbasyon
        double noise = Math.sin(point.x * 10.0) * Math.cos(point.z * 10.0) * 0.01;
        Vector3 perturbation = new Vector3(
            noise * normalMicroFacet,
            (1.0 - Math.abs(noise)) * normalMicroFacet,
            noise * normalMicroFacet * 0.5
        );
        
        return normal.add(perturbation).normalize();
    }
    
    private Vector3 reflect(Vector3 incident, Vector3 normal) {
        return incident.subtract(normal.multiply(2.0 * incident.dot(normal)));
    }
    
    // PBR Properties
    @Override 
    public Color getAlbedo() {
        return ColorUtil.blendColors(tileColor, groutColor, 0.7f);
    }

    @Override 
    public double getRoughness() {
        return (tileRoughness + groutRoughness) / 2.0;
    }
    
    @Override 
    public double getMetalness() { 
        return 0.0; 
    }
    
    @Override 
    public MaterialType getMaterialType() { 
        return MaterialType.DIELECTRIC; 
    }
    
    @Override 
    public double getReflectivity() { 
        return 0.4 * fresnelIntensity; // Fresnel adding
    }
    
    @Override 
    public double getIndexOfRefraction() { 
        return 1.52; 
    }
    
    @Override 
    public double getTransparency() { 
        return 0.0; 
    }

    @Override
    public void setObjectTransform(Matrix4 tm) {
        // Optional: Implement if needed for texture mapping
    }
    
    // Helper methods
    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
    
    // Getter metodları
    public Color getTileColor() { return tileColor; }
    public Color getGroutColor() { return groutColor; }
    public double getTileRoughness() { return tileRoughness; }
    public double getGroutRoughness() { return groutRoughness; }
    public double getTileSize() { return tileSize; }
    public double getGroutWidth() { return groutWidth; }
    public double getTileSpecular() { return tileSpecular; }
    public double getGroutSpecular() { return groutSpecular; }
    
    // Getters advanced properties
    public double getFresnelIntensity() { return fresnelIntensity; }
    public double getNormalMicroFacet() { return normalMicroFacet; }
    public double getReflectionSharpness() { return reflectionSharpness; }
    public double getEnergyConservation() { return energyConservation; }
	
}
