package index.rtree;

import main.Main;
import signatures.PointSeq;
import signatures.Sequential;
import signatures.Spatiotemporal;
import org.apache.commons.lang3.tuple.Pair;
import basic.SimplePoint;

import java.util.Set;

import static basic.SimplePoint.getDistance;


public class Rectangle implements Cloneable  {

    // these two points may don't have pointID
    public SimplePoint bottomLeft;    // on the map, this point has minimum longitude and latitude of this rectangle
    public SimplePoint topRight;                  // this point has maximum longitude and latitude
    private static final int dimension = 2;

    public Rectangle(){
        this.bottomLeft = null;
        this.topRight = null;
    }

    /** initialize a rectangle from a TF-IDF vector */
    public Rectangle(Set<Integer> keyIdSet){
        boolean sequential = Sequential.id2qgram != null;
        boolean spatiotemporal = Spatiotemporal.id2dimension != null;
        boolean st_grid = Spatiotemporal.gridST != null;

        // just for spatial
        boolean isFirstPoint = true;
        for (int keyID : keyIdSet) {
            Rectangle rec = null;
            if(sequential) {
                PointSeq seq = Sequential.id2qgram.get(keyID);
                rec = seq.getRectangle();
            }
            else if (spatiotemporal) {
                if(st_grid) { // grid-based spatiotemporal signatures
                    Pair<Integer, Integer> dim = Spatiotemporal.id2dimension.get(keyID);
                    rec = Spatiotemporal.gridST.getRectangleByGridID(dim.getLeft());    // gridID
                }
                else {
                    Pair<Integer, Integer> dim = Spatiotemporal.id2dimension.get(keyID);
                    SimplePoint point = Main.NNid2lnglat.get(dim.getLeft());
                    rec = new Rectangle(point, point);
                }
            }
            else {  // point-based
                SimplePoint point = Main.NNid2lnglat.get(keyID);
                rec = new Rectangle(point, point);
            }

            if (isFirstPoint) {
                this.bottomLeft = new SimplePoint(rec.bottomLeft);
                this.topRight = new SimplePoint(rec.topRight);
                isFirstPoint = false;
            }
            else {
                this.enlargeRectangle(rec);
            }

        } // end of scanning all points
    }

    public Rectangle(double lng_min, double lng_max, double lat_min, double lat_max){
        assert lng_min <= lng_max && lat_min <= lat_max;
        bottomLeft = new SimplePoint(lng_min, lat_min);
        topRight = new SimplePoint(lng_max, lat_max);
    }

    // just construct a rectangle according to two points' coordinates
    public Rectangle(SimplePoint _bottom, SimplePoint _top)  {
        if (_bottom == null || _top == null)  {
            throw new IllegalArgumentException("Error in Constructor of Rectangle : Points cannot be null.");
        }

        if(_bottom.getLongitude() > _top.getLongitude() || _bottom.getLatitude() > _top.getLatitude()){
            throw new IllegalArgumentException("Error in Constructor of Rectangle : First bottomLeft, then topRight.");
        }

        bottomLeft = new SimplePoint(_bottom);
        topRight = new SimplePoint(_top);
    }

    public Rectangle(Rectangle mbr) {
        this.bottomLeft = new SimplePoint(mbr.bottomLeft);
        this.topRight = new SimplePoint(mbr.topRight);
    }

    private SimplePoint getBottomLeft() {
        return this.bottomLeft;
    }

    private SimplePoint getTopRight() {
        return this.topRight;
    }

    public float getCoord_BottomLeft(int dimIdx){
        return dimIdx == 0 ? bottomLeft.getLongitude() : bottomLeft.getLatitude();
    }

    public float getCoord_TopRight(int dimIdx){
        return dimIdx == 0 ? topRight.getLongitude() : topRight.getLatitude();
    }

    public void enlargeRectangle(Rectangle newRectangle) {

        // this rectangle is enclosed by the given new one
        if(newRectangle.canEnclose(this)){
            this.bottomLeft = new SimplePoint(newRectangle.bottomLeft);
            this.topRight = new SimplePoint(newRectangle.topRight);
        }
        else if(!this.canEnclose(newRectangle)){
            // store two rectangles' min and max upperBound for each dimension
            float[] _bottomLeft = new float[dimension];
            float[] _topRight = new float[dimension];

            for (int i = 0; i < dimension; i++) {
                _bottomLeft[i] = Math.min(this.getCoord_BottomLeft(i), newRectangle.getCoord_BottomLeft(i));
                _topRight[i] = Math.max(this.getCoord_TopRight(i), newRectangle.getCoord_TopRight(i));
            }

            this.bottomLeft = new SimplePoint(_bottomLeft[0], _bottomLeft[1]);
            this.topRight = new SimplePoint(_topRight[0], _topRight[1]);
        }
    }


    /**
     * @return a minimum bounding rectangle which can cover two rectangles
     */
    public Rectangle getUnionRectangle(Rectangle rectangle){
        Rectangle union = new Rectangle(this);
        union.enlargeRectangle(rectangle);
        return union;
    }

    public double getIntersectArea(Rectangle rectangle) {
        if(!this.isIntersection(rectangle)){
            return 0;
        }
        if(this.canEnclose(rectangle)){
            return rectangle.computeArea();
        }
        if(rectangle.canEnclose(this)){
            return this.computeArea();
        }

        Rectangle intersect = new Rectangle();
        // store two rectangles' min and max upperBound for each dimension
        float[] _bottomLeft = new float[dimension];
        float[] _topRight = new float[dimension];

        for (int i = 0; i < dimension; i++) {
            _bottomLeft[i] = Math.max(this.getCoord_BottomLeft(i), rectangle.getCoord_BottomLeft(i));
            _topRight[i] = Math.min(this.getCoord_TopRight(i), rectangle.getCoord_TopRight(i));
        }

        intersect.bottomLeft = new SimplePoint(_bottomLeft[0], _bottomLeft[1]);
        intersect.topRight = new SimplePoint(_topRight[0], _topRight[1]);
        return intersect.computeArea();
    }

    /** whether two rectangles have intersection */
    public boolean isIntersection(final Rectangle rectangle) {

        for (int i = 0; i < dimension; i++) {
            if (this.getCoord_BottomLeft(i) > rectangle.getCoord_TopRight(i)
                    || this.getCoord_TopRight(i) < rectangle.getCoord_BottomLeft(i)) {
                return false;
            }
        }
        return true;
    }

    /** whether the parameter-rectangle is enclosed by this rectangle */
    private boolean canEnclose(final Rectangle rectangle) {
        // as long as one coordinate crosses the boundary, it is impossible to be enclosed
        for (int i = 0; i < dimension; i++) {
            if (this.getCoord_BottomLeft(i) > rectangle.getCoord_BottomLeft(i)
                    ||  this.getCoord_TopRight(i) < rectangle.getCoord_TopRight(i)) {
                return false;
            }
        }
        return true;
    }

    // approximate area, not very accurate, unit: km2
    public double computeArea() {
        SimplePoint bottomRight = new SimplePoint(topRight.getLongitude(), bottomLeft.getLatitude());
        SimplePoint topLeft = new SimplePoint(bottomLeft.getLongitude(), topRight.getLatitude());

        return getDistance(topLeft, topRight) * getDistance(topRight, bottomRight);
    }

    public SimplePoint getCenterPoint() {
        float lng = (bottomLeft.getLongitude() + topRight.getLongitude()) / 2;
        float lat = (bottomLeft.getLatitude() + topRight.getLatitude()) / 2;
        return new SimplePoint(lng, lat);
    }

    @Override
    protected Object clone() {
        SimplePoint p1 = new SimplePoint(this.bottomLeft);
        SimplePoint p2 = new SimplePoint(this.topRight);
        return new Rectangle(p1, p2);
    }

    @Override
    public String toString() {
        return "Bottom Left:" + bottomLeft + ", Top Right:" + topRight;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Rectangle rectangle) {
            return bottomLeft.equals(rectangle.getBottomLeft()) && topRight.equals(rectangle.getTopRight());
        }
        return false;
    }
}