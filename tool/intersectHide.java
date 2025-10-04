public double intersectHide(Ray ray) {
    if (inverseTransform == null || texture == null) return Double.POSITIVE_INFINITY;

    Ray localRay = ray.transform(inverseTransform);
    Vector3 dir = localRay.getDirection();
    if (Math.abs(dir.z) < Ray.EPSILON) return Double.POSITIVE_INFINITY;

    double t = -localRay.getOrigin().z / dir.z;
    if (t < Ray.EPSILON) return Double.POSITIVE_INFINITY;

    Point3 localHit = localRay.pointAtParameter(t);
    
    double halfWidth = width / 2.0;
    double halfHeight = height / 2.0;
    
    if (Math.abs(localHit.x) > halfWidth || Math.abs(localHit.y) > halfHeight) {
        return Double.POSITIVE_INFINITY;
    }

    double u = (localHit.x + 1.0) * 0.5;
    double v = 1.0 - ((localHit.y + 1.0) * 0.5);

    double texU = u * texture.getWidth();
    double texV = v * texture.getHeight();
    
    int x0 = (int) Math.floor(texU);
    int y0 = (int) Math.floor(texV);
    int x1 = x0 + 1;
    int y1 = y0 + 1;
    
    x0 = Math.max(0, Math.min(texture.getWidth() - 1, x0));
    y0 = Math.max(0, Math.min(texture.getHeight() - 1, y0));
    x1 = Math.max(0, Math.min(texture.getWidth() - 1, x1));
    y1 = Math.max(0, Math.min(texture.getHeight() - 1, y1));
    
    double fracU = texU - x0;
    double fracV = texV - y0;
    
    int a00 = (texture.getRGB(x0, y0) >> 24) & 0xFF;
    int a01 = (texture.getRGB(x0, y1) >> 24) & 0xFF;
    int a10 = (texture.getRGB(x1, y0) >> 24) & 0xFF;
    int a11 = (texture.getRGB(x1, y1) >> 24) & 0xFF;
    
    // Bilinear interpolation for alpha
    double alpha00 = a00 * (1 - fracU) + a10 * fracU;
    double alpha01 = a01 * (1 - fracU) + a11 * fracU;
    double finalAlpha = alpha00 * (1 - fracV) + alpha01 * fracV;
    
    if (finalAlpha < 0.1) {
        return Double.POSITIVE_INFINITY;
    }

    return t;
}
