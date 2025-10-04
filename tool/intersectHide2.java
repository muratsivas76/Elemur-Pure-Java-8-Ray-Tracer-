public double intersectHide2(Ray ray) {
    if (inverseTransform == null) return Double.POSITIVE_INFINITY;

    Ray localRay = ray.transform(inverseTransform);
    Vector3 dir = localRay.getDirection();
    if (Math.abs(dir.z) < Ray.EPSILON) return Double.POSITIVE_INFINITY;

    double t = -localRay.getOrigin().z / dir.z;
    if (t < Ray.EPSILON) return Double.POSITIVE_INFINITY;

    Point3 localHit = localRay.pointAtParameter(t);
    
    double halfWidth = width / 2.0;
    double halfHeight = height / 2.0;
    
    if (isRectangle) {
        if (Math.abs(localHit.x) <= halfWidth && Math.abs(localHit.y) <= halfHeight) {
            return Double.POSITIVE_INFINITY; 
        }
    } else {
        double nx = localHit.x / halfWidth;
        double ny = localHit.y / halfHeight;
        if (nx * nx + ny * ny <= 1.0) {
            return Double.POSITIVE_INFINITY;
        }
    }
    
    return t;
}
