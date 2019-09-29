package jp.co.arithinawashiro.arcorelocationnavigator;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

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

    // GeoJSON形式からnameとcoordinatesを取得
    // nameはproperties内に配置している(assets/JSONSamples/coordSample.JSONに記載)
    void setLocationFromGeoJSON(JSONObject geoJSON) {
        try {
            if (geoJSON.getJSONObject("properties").isNull("name")){
                name = "hasNoName";
            } else {
                name = geoJSON.getJSONObject("properties").getString("name");
            }
            longitude = geoJSON.getJSONObject("geometry").getJSONArray("coordinates").getDouble(0);
            latitude = geoJSON.getJSONObject("geometry").getJSONArray("coordinates").getDouble(1);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
