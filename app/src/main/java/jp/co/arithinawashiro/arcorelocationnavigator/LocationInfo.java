package jp.co.arithinawashiro.arcorelocationnavigator;

/**
 * 位置情報を保持するクラス
 */
class LocationInfo{

    LocationInfo(String name,double longitude,double latitude){
        this.name = name;
        this.longitude = longitude;
        this.latitude = latitude;
    }
    private  double longitude;
    private  double latitude;
    private  String  name;

    double getLongitude() {
        return longitude;
    }
    double getLatitude(){
        return latitude;
    }
    String getName() {
        return name;
    }
}
