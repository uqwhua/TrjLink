package index;

import basic.SimplePoint;

import java.util.Arrays;

public class GeoHash {

    public static final float MIN_LAT = -90;
    public static final float MAX_LAT = 90;
    public static final float MIN_LNG = -180;
    public static final float MAX_LNG = 180;

    /**
     * 1: 2500km; 2: 630km; 3: 78km; 4: 30km
     * 5: 2.4km; 6: 610m; 7: 76m; 8: 19m
     */
    private static final int hashLength = 8; //经纬度转化为geohash长度
    private static final int latLength = 20; //纬度转化为二进制长度
    private static final int lngLength = 20; //经度转化为二进制长度


    private static final char[] CHARS = {'0', '1', '2', '3', '4', '5', '6', '7',
            '8', '9', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'j', 'k', 'm', 'n',
            'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z'};


    public static String getGeoHash(SimplePoint p){
        return getGeoHash_Base32(p.getLatitude(), p.getLongitude());
    }

    // given lat and lng, return its base32 string
    private static String getGeoHash_Base32(double lat, double lng) {

        boolean[] binaryHash = getGeoHash_Binary(lat, lng);
        if (binaryHash == null) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < binaryHash.length; i = i + 5) {
            boolean[] base32 = new boolean[5];
            for (int j = 0; j < 5; j++) {
                base32[j] = binaryHash[i + j];
            }
            char cha = getChar_Base32(base32);
            if (cha == ' ') {
                System.out.println("Error in GeoHash.getGeoHash_Base32: sth wrong.");
                return null;
            }
            sb.append(cha);
        }
        return sb.toString();
    }

    private static boolean[] getGeoHash_Binary(double lat, double lng) {
        boolean[] latArray = getHashArray(lat, MIN_LAT, MAX_LAT, latLength);
        boolean[] lngArray = getHashArray(lng, MIN_LNG, MAX_LNG, lngLength);
        return mergeArray(latArray, lngArray);
    }

    //  value: latitude or longitude
    private static boolean[] getHashArray(double value, double min, double max, int length) {

        if (value < min || value > max) {
            throw new IndexOutOfBoundsException("Error in GeoHash.getHashArray: value is out of range.");
        }
        if (length < 1) {
            throw new IllegalArgumentException("Error in GeoHash.getHashArray: the binary length is illegal.");
        }

        boolean[] result = new boolean[length];
        for (int i = 0; i < length; i++) {
            double mid = (min + max) / 2.0;
            if (value > mid) {
                result[i] = true;
                min = mid;
            } else {
                result[i] = false;
                max = mid;
            }
        }

        return result;
    }

    // mergeArray longitude-array(even) and latitude array(odd)
    private static boolean[] mergeArray(boolean[] latArray, boolean[] lngArray) {
        if (latArray == null || lngArray == null) {
            return null;
        }
        boolean[] result = new boolean[lngArray.length + latArray.length];
        Arrays.fill(result, false);

        for (int i = 0; i < lngArray.length; i++) {
            result[2 * i] = lngArray[i];
        }
        for (int i = 0; i < latArray.length; i++) {
            result[2 * i + 1] = latArray[i];
        }
        return result;
    }

    private static char getChar_Base32(boolean[] base32) {

        if (base32 == null || base32.length != 5) {
            return ' ';
        }
        int num = 0;
        for (boolean bool : base32) {
            num <<= 1;
            if (bool) {
                num += 1;
            }
        }
        return CHARS[num % CHARS.length];
    }

}
