package basic;

public class ComplexPoint extends SimplePoint{

    long exactTimestamp = -1;    // unit: second

    public ComplexPoint(float lng, float lat, long t) {
        super(lng, lat);
        exactTimestamp = t;
    }

    public ComplexPoint(float lng, float lat, long t, float dlng, float dlat, long dt) {
        super(lng, lat);
        exactTimestamp = t;
    }

    public ComplexPoint(SimplePoint point) {
        super(point.longitude, point.latitude);
    }

    public long get_exactTime() {
        return exactTimestamp;
    }

    public void set_time(long time) {
        exactTimestamp = time;
    }
}
