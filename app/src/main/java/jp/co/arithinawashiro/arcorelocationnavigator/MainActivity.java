package jp.co.arithinawashiro.arcorelocationnavigator;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.annotation.NonNull;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.ar.core.ArCoreApk;
import com.google.ar.core.Config;
import com.google.ar.core.Frame;
import com.google.ar.core.Session;
import com.google.ar.core.TrackingState;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.core.exceptions.UnavailableException;
import com.google.ar.sceneform.ArSceneView;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.rendering.ViewRenderable;
import com.google.maps.ElevationApi;
import com.google.maps.GeoApiContext;
import com.google.maps.PendingResult;
import com.google.maps.model.ElevationResult;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import uk.co.appoly.arcorelocation.LocationMarker;
import uk.co.appoly.arcorelocation.LocationScene;
import uk.co.appoly.arcorelocation.utils.ARLocationPermissionHelper;

import static android.content.res.AssetManager.*;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback  {

    private static GeoApiContext geoApiContext;

    private static final String TAG = MainActivity.class.getSimpleName();

    private ArSceneView arSceneView;
    private LocationScene locationScene;
    private boolean installRequested;

    /* サンプル表示のLocationView */
    private final LocationInfo[] locInfo = {
            new LocationInfo("甲府駅",138.568788,35.666692),
            new LocationInfo("静岡駅",138.388897,34.971759),
    };

    /* サンプル表示のLocationViewその2 from GeoJSON */
    private LocationInfo[] locInfoFromGeoJSON = {
            /* これらは後にJSONの内容に上書きされる．コンストラクタは未実装 */
            new LocationInfo("甲府駅GeoJSON",138.,35.),
            new LocationInfo("静岡駅GeoJSON",138.,34.),
    };
    // GeoJSON形式読み込み用の関数(loadJSONFromAsset, setGeoJSONSamples)
    // ローカルフォルダのJSONを読み，locInfoFromGeoJSONの2要素に書き込む
    private AssetManager assetManager;
    private JSONObject sampleJSONObjects;
    public String loadJSONFromAsset(AssetManager assetManager) {
        String json = null;
        try {
            InputStream is = assetManager.open("JSONSamples/coordsSample.JSON");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer, "UTF-8");
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
        return json;
    }
    void setGeoJSONSamples(LocationInfo[] locInfoFromGeoJSON, JSONObject sampleJSONObjects){
        int i = 0;
        for (LocationInfo info: locInfoFromGeoJSON){
            try {
                info.setLocationFromGeoJSON(sampleJSONObjects.getJSONArray("features").getJSONObject(i));
            } catch (JSONException e) {
                e.printStackTrace();
            }
            i = i+1;
        }
    };

    /* GoogleMapオブジェクト */
    private GoogleMap mMap;

    /* 現在選択中の位置を示すマーカー */
    private Marker marker = null;

    /**
     * マーカーのマップ
     */
    private HashMap<LatLng,Marker> markerMap = new HashMap<>();

    /**
     * Googleマップの準備ができたときに呼ばれる
     * @param googleMap GoogleMapオブジェクトのインスタンス
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;


        mMap.getUiSettings().setAllGesturesEnabled(true);
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);
        mMap.getUiSettings().setMapToolbarEnabled(true);


        // InfoWindowをカスタムする
        mMap.setInfoWindowAdapter(new MyInfoWindowAdapter(this));

        // InfoWindowをクリックしたときのリスナーをセット
        mMap.setOnInfoWindowClickListener(marker -> {

            // 視点を移動する
            LatLng lng = marker.getPosition();
            moveCameraToPosition(lng);

        });

        // タップした時のリスナーをセット
        mMap.setOnMapClickListener(tapLocation -> {

            // Tapされた位置の緯度経度を取得
            LatLng location = new LatLng(tapLocation.latitude, tapLocation.longitude);
            //String str = String.format(Locale.US, "%f, %f", tapLocation.latitude, tapLocation.longitude);

            // マーカーをセットする
            setMarker(location,false);
            //mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 15));
        });

        // サンプルのMarkerを地図とAR空間にに追加
        for (LocationInfo info: locInfo) {
            LatLng lng = new LatLng(info.getLatitude(),info.getLongitude());
            createLocationMarker(lng,
                                 info.getName(),
                     false,
                                true,
                                0xFF0088FF);
        }
        // サンプルのMarkerを地図とAR空間にに追加
        for (LocationInfo info: locInfo) {
            LatLng lng = new LatLng(info.getLatitude(),info.getLongitude());
            createLocationMarker(lng,
                    info.getName(),
                    false,
                    true,
                    0xFF0088FF);
        }
        // GeoJSON生成のサンプルのMarkerを地図とAR空間にに追加
        for (LocationInfo info: locInfoFromGeoJSON) {
            LatLng lng = new LatLng(info.getLatitude(),info.getLongitude());
            createLocationMarker(lng,
                    info.getName(),
                    false,
                    true,
                    0xFF0088FF);
        }
        // 現在地表示ボタンを有効にする
        enableMyLocation();
    }

    /**
     * 指定位置に視点を移動する
     * @param lng 座標
     */
    private void moveCameraToPosition(LatLng lng){

        CameraUpdate center = CameraUpdateFactory.newLatLng(lng);
        CameraUpdate zoom=CameraUpdateFactory.zoomTo(15);
        mMap.moveCamera(center);
        mMap.animateCamera(zoom);

        try{
            Geocoder geocoder = new Geocoder(this);
            List<Address> addresses = geocoder.getFromLocation(lng.latitude,lng.longitude,1);

            if (addresses.size() > 0){
                TextView tv = findViewById(R.id.textView_location);
                tv.setText(addresses.get(0).getAddressLine(0));
            }
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    /***
     * 現在地表示ボタンを有効化する
     */
    private void enableMyLocation(){
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ARLocationPermissionHelper.requestPermission(this);
            return;
        }
        mMap.setMyLocationEnabled(true);
    }

    private final int REQUEST_CODE_REGISTER_DIALOG = 1000;

    /**
     * マーカーの登録ボタンを押下したとき
     */
    public void onClickRegisterLocation(View view){
        if (marker != null){

            // タップした場所を中心にする
            LatLng latLng = marker.getPosition();
            CameraUpdate center = CameraUpdateFactory.newLatLng(latLng);
            mMap.moveCamera(center);

            // 登録用Dialogを表示する
            RegisterDialogFragment dialog = new RegisterDialogFragment();

            // 現在地の情報をFragmentに渡す
            Bundle bundle = new Bundle();

            // 緯度経度
            bundle.putDouble("latitude",latLng.latitude);
            bundle.putDouble("longitude",latLng.longitude);

            // 住所
            TextView textViewLocation = findViewById(R.id.textView_location);
            bundle.putString("address", textViewLocation.getText().toString());

            dialog.setArguments(bundle);
            dialog.setTargetFragment(null,REQUEST_CODE_REGISTER_DIALOG);
            dialog.show(getSupportFragmentManager(),"register");

            // Dialogの実行結果は、onActivityResult()で受け取る
        }
    }

    /**
     * LocationMarkerをマップに追加する
     * @param latLng 緯度経度
     * @param locName 登録する地点名
     * @param isShowInfo InfoWindowを表示状態にするかどうか
     * @param isSeaLevel 標高を登録するかどうか
     * @param color 背景色
     */
    private void createLocationMarker(LatLng latLng,
                                      final String locName,
                                      boolean isShowInfo,
                                      boolean isSeaLevel,
                                      int color){

        // 新規にマーカーを追加する
        MarkerOptions mo =  new MarkerOptions();
        mo.position(latLng);
        mo.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE ));
        mo.title(locName);

        // 標高を取得する
        String elevation = "";
        if (isSeaLevel){
            com.google.maps.model.LatLng lng = new com.google.maps.model.LatLng(latLng.latitude,latLng.longitude);
            PendingResult<ElevationResult> elevationResult = ElevationApi.getByPoint(geoApiContext,lng);
            try{
                ElevationResult result =  elevationResult.await();
                elevation = "" + Math.round(result.elevation);
            }catch (Exception e){
                e.printStackTrace();
            }
        }

        final String elevation_ = elevation;

        String location = String.format(Locale.US, "%f, %f", latLng.latitude,latLng.longitude);
        if (!elevation.equals("")){
            location += "\n" + elevation + " M";
        }
        mo.snippet(location);
        mo.alpha(0.8f);

        Marker newMarker = mMap.addMarker(mo);
        if (isShowInfo){
            newMarker.showInfoWindow();
        }

        // Mapにマーカーを保持しておく
        markerMap.put(latLng,newMarker);

        // LocationMarkerを作成する
        Context c = this;

        // リニアレイアウトの作成
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);

        // Layoutの横・縦幅の指定
        layout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        layout.setAlpha(0.8f);
        layout.setPadding(30, 5, 30, 5);
        layout.setBackgroundColor(color);

        // TextViewの作成
        LinearLayout.LayoutParams textLayoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);

        // 地名表示のTextView
        TextView locationTextView = new TextView(this);
        locationTextView.setTextColor(Color.WHITE);
        locationTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP,10);
        locationTextView.setLayoutParams(textLayoutParams);
        layout.addView(locationTextView);

        // 距離表示のTextView
        TextView distanceTextView = new TextView(this);
        distanceTextView.setTextColor(Color.WHITE);
        distanceTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP,9);
        distanceTextView.setLayoutParams(textLayoutParams);
        layout.addView(distanceTextView);

        // LocationMarkerを作成する
        CompletableFuture cf = ViewRenderable.builder().setView(c,layout).build();

        CompletableFuture.allOf(
                cf)
                .handle(
                        (notUsed, throwable) -> {
                            // When you build a Renderable, Sceneform loads its resources in the background while
                            // returning a CompletableFuture. Call handle(), thenAccept(), or check isDone()
                            // before calling get().

                            if (throwable != null) {
                                return null;
                            }

                            try {
                                ViewRenderable vr = (ViewRenderable)cf.get();
                                LocationInfo locationInfo = new LocationInfo("test",latLng.longitude,latLng.latitude);

                                // LocationMarkerを作成
                                LocationMarker locMaker = new LocationMarker(
                                        latLng.longitude,
                                        latLng.latitude,
                                        getLocationView(vr,locationInfo)
                                );

                                locMaker.setRenderEvent(node -> {
                                    locationTextView.setText(locName);

                                    double distance = node.getDistance();

                                    String dst = "";
                                    if(distance > 1000){
                                        distance = distance / 1000 ;
                                        if (distance > 1000){
                                            dst = String.format(Locale.US, "%.0f", distance) + " kM";
                                        }else{
                                            dst = String.format(Locale.US, "%.2f", distance) + " kM";
                                        }
                                    }else{
                                        dst = String.format(Locale.US, "%.2f", distance) + " M";
                                    }

                                    if (!elevation_.equals("")){
                                        dst += "\n" + elevation_ + " M";
                                    }

                                    distanceTextView.setText(dst);
                                });

                                // シーンにマーカーを追加する
                                if (locationScene != null){
                                    locationScene.mLocationMarkers.add(locMaker);
                                }

                            } catch (InterruptedException | ExecutionException ex) {
                                Log.e( TAG, ex.getMessage());
                            }

                            return null;
                        });

    }

    /**
     * マップの検索が完了したときに呼ばれる
     * @param view view
     */
    public void onMapSearch(View view) {
        EditText locationSearch = findViewById(R.id.editText);
        String location = locationSearch.getText().toString();
        List<Address> addressList = null;

        if (!location.equals("")) {
            Geocoder geocoder = new Geocoder(this);
            try {
                addressList = geocoder.getFromLocationName(location, 1);
            } catch (IOException e) {
                Log.e(TAG,e.getMessage());
                e.printStackTrace();
            }
            // 検索結果が存在すれば
            // 一番最初の検索結果位置に移動する
            if (addressList != null && addressList.size() > 0){

                // 検索結果の位置
                Address address = addressList.get(0);
                LatLng latLng = new LatLng(address.getLatitude(), address.getLongitude());

                // 視点を移動する
                moveCameraToPosition(latLng);

                // マーカーをセット
                setMarker(latLng,true);

            }else{
                // 発見できなかった

                Toast toast = Toast.makeText(this, "Not Found" , Toast.LENGTH_SHORT);
                toast.setGravity(Gravity.CENTER,0,0);
                toast.show();
            }
        }
    }

    /**
     * マップ上のマーカーを変更する
     * @param latLng 緯度経度
     */
    private void setMarker(LatLng latLng,Boolean isShowInfo){

        if(marker == null){

            MarkerOptions mo =  new MarkerOptions();
            mo.position(latLng);
            mo.draggable(true);
            mo.alpha(0.8f);

            marker = mMap.addMarker(mo);

        }else{
            marker.setPosition(latLng);
        }

        String city = "";

        try{
            Geocoder coder = new Geocoder(this);
            // ネットワークアクセスが発生するので、UIスレッド以外で動かすこと
            List<Address> addresses = coder.getFromLocation(
                    latLng.latitude,latLng.longitude, 1);

            if (addresses != null && addresses.size() > 0){
                // 住所を取得する
                TextView tv = findViewById(R.id.textView_location);
                tv.setText(addresses.get(0).getAddressLine(0));

                // 県、郡、市を取得する
                Address address = addresses.get(0);

                if (address.getCountryCode().equals("JP")){
                    // 日本
                    city = address.getAdminArea();
                    if (address.getSubAdminArea() != null) {
                        city += address.getSubAdminArea();
                    }
                    if(address.getLocality() != null){
                        city += address.getLocality();
                    }
                }else{
                    // 日本以外
                    if(address.getLocality() != null){
                        city += address.getLocality();
                    }
                    if (address.getSubAdminArea() != null) {
                        if (!city.equals("")){
                            city += ", ";
                        }
                        city += address.getSubAdminArea();
                    }
                    if (!city.equals("")){
                        city += ", ";
                    }
                    city += address.getAdminArea();
                }

            }
        }catch (Exception e){
            Log.e(TAG,e.getMessage());
        }

        String str = String.format(Locale.US, "%f, %f", latLng.latitude,latLng.longitude);

        marker.setSnippet(str);
        marker.setTitle(city);

        if (isShowInfo){
            marker.showInfoWindow();
        }else{
            marker.hideInfoWindow();
        }

    }

    /**
     * メニューが作成されたときに呼ばれる
     * @param menu 作成されたメニュー
     * @return true
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    /**
     * アプリバーのメニュー選択時に呼ばれる
     * @param  item メニューアイテム
     * @return アイテム選択状態
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_license) {
            new LicenseDialogFragment().show(getSupportFragmentManager(), "license");
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setSupportActionBar(findViewById(R.id.toolbar_main));

        // JSONSamples/coordSample.JSONからサンプルデータを読み込み，
        // locInfoFromGeoJSONに格納
        assetManager = this.getResources().getAssets();
        try {
            sampleJSONObjects = new JSONObject(loadJSONFromAsset(assetManager));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        setGeoJSONSamples(locInfoFromGeoJSON, sampleJSONObjects);

        // バーのタイトルを消去
        ActionBar bar =  getSupportActionBar();
        if (bar != null){
            bar.setDisplayShowTitleEnabled(false);
        }

        geoApiContext = new GeoApiContext.Builder()
                .apiKey(getString(R.string.map_key))
                .build();

        arSceneView = findViewById(R.id.ar_scene_view);

        // Set an update listener on the Scene that will hide the loading message once a Plane is
        // detected.
        arSceneView
                .getScene()
                .addOnUpdateListener(
                        // If our locationScene object hasn't been setup yet, this is a good time to do it
                        // We know that here, the AR components have been initiated.
                        // Now lets create our location markers.
                        // First, a layout
                        // An example "onRender" event, called every frame
                        // Updates the layout with the markers distance
                        //if( locationTextView == null){
                        //    Log.e(TAG,info.getName() + " "  + strId + " " + resourceNo);
                        //}
                        // Adding the marker
                        this::onUpdate);

        // Lastly request CAMERA & fine location permission which is required by ARCore-Location.
        ARLocationPermissionHelper.requestPermission(this);

        MapFragment mapFragment = (MapFragment) getFragmentManager()
                .findFragmentById(R.id.mapFragment);

        mapFragment.getMapAsync(this);

    }

    @SuppressLint("ClickableViewAccessibility")
    private Node getLocationView(ViewRenderable viewRenderable, LocationInfo locInfo ) {
        Node base = new Node();
        base.setRenderable(viewRenderable);

        // Add  listeners etc here
        View eView = viewRenderable.getView();
        eView.setOnTouchListener(
                (v, event) -> {

                    //String loc =  + locInfo.getLatitude() + "," + locInfo.getLongitude() ;
                    //Toast toast = Toast.makeText(
                    //        c, locInfo.getName() + "\n" + loc , Toast.LENGTH_SHORT);

                    //toast.setGravity(Gravity.BOTTOM,0,0);
                    //toast.show();

                    // Mapに表示
                    LatLng latLng = new  LatLng(
                            locInfo.getLatitude(),
                            locInfo.getLongitude());

                    // 視点を移動する
                    moveCameraToPosition(latLng);

                    // InfoWindowを表示する
                    if (markerMap.containsKey(latLng)){
                        Marker m = markerMap.get(latLng);
                        if(m != null){
                            m.showInfoWindow();
                        }
                    }

                    // 現在地のマーカーは消しておく
                    if (marker != null){
                        marker.remove();
                        marker = null;
                    }

                    return false;
                });

        return base;
    }

    /**
     * Make sure we call locationScene.resume();
     */
    @Override
    protected void onResume() {
        super.onResume();

        if (locationScene != null) {
            locationScene.resume();
        }

        if (arSceneView.getSession() == null) {
            // If the session wasn't created yet, don't resume rendering.
            // This can happen if ARCore needs to be updated or permissions are not granted yet.
            try {
                Session session = createArSession(this, installRequested);
                if (session == null) {
                    installRequested = ARLocationPermissionHelper.hasPermission(this);
                    return;
                } else {
                    arSceneView.setupSession(session);
                }
            } catch (UnavailableException e) {
                Log.e( TAG, e.getMessage());
            }
        }

        try {
            arSceneView.resume();
        } catch (CameraNotAvailableException ex) {
            finish();
        }

    }
    /**
     * Make sure we call locationScene.pause();
     */
    @Override
    public void onPause() {
        super.onPause();
        if (locationScene != null) {
            locationScene.pause();
        }
        arSceneView.pause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        arSceneView.destroy();
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] results) {

        if (!ARLocationPermissionHelper.hasPermission(this)) {
            if (!ARLocationPermissionHelper.shouldShowRequestPermissionRationale(this)) {
                // Permission denied with checking "Do not ask again".
                ARLocationPermissionHelper.launchPermissionSettings(this);
            } else {
                Toast.makeText(
                        this, "Camera permission is needed to run this application", Toast.LENGTH_LONG)
                        .show();
            }
            finish();
        }
    }

    public static Session createArSession(Activity activity, boolean installRequested)
            throws UnavailableException {
        Session session = null;
        // if we have the camera permission, create the session
        if (ARLocationPermissionHelper.hasPermission(activity)) {
            switch (ArCoreApk.getInstance().requestInstall(activity, !installRequested)) {
                case INSTALL_REQUESTED:
                    return null;
                case INSTALLED:
                    break;
            }
            session = new Session(activity);
            // IMPORTANT!!!  ArSceneView needs to use the non-blocking update mode.
            Config config = new Config(session);
            config.setUpdateMode(Config.UpdateMode.LATEST_CAMERA_IMAGE);
            session.configure(config);
        }
        return session;
    }

    private void onUpdate(FrameTime frameTime) {

        if (locationScene == null) {
            // If our locationScene object hasn't been setup yet, this is a good time to do it
            // We know that here, the AR components have been initiated.
        locationScene = new LocationScene(this, arSceneView);
        }

        Frame frame = arSceneView.getArFrame();
        if (frame == null) {
            return;
        }

        if (frame.getCamera().getTrackingState() != TrackingState.TRACKING) {
            return;
        }

        if (locationScene != null) {
            locationScene.processFrame(frame);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode){
            case REQUEST_CODE_REGISTER_DIALOG:
                // 地点を登録する

                // Fragmentから取得したデータ
                Bundle bundle = data.getExtras();

                double longitude = bundle.getDouble("longitude");
                double latitude = bundle.getDouble("latitude");
                LatLng latLng = new LatLng(latitude,longitude);
                String name = bundle.getString("name");
                boolean isSeaLevel = bundle.getBoolean("sealevel");
                int color = bundle.getInt("color");

                // 登録処理
                createLocationMarker(latLng,
                                    name,
                        true,
                                    isSeaLevel,
                                    color);

                // タップ時のマーカーは削除する
                marker.remove();
                marker = null;

                break;

             default:
                 break;

        }
    }
}
