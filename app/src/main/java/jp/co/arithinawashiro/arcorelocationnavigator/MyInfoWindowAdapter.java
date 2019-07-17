package jp.co.arithinawashiro.arcorelocationnavigator;

import android.app.Activity;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Marker;

public class MyInfoWindowAdapter implements GoogleMap.InfoWindowAdapter {

    private final View mWindow;

    public MyInfoWindowAdapter(Activity activity){
        mWindow = activity.getLayoutInflater().inflate(R.layout.custom_info_window,null);
    }

    @Override
    public View getInfoWindow(Marker marker) {
        return null;
    }

    @Override
    public View getInfoContents(Marker marker) {
        render(marker,mWindow);
        return mWindow;
    }

    /**
     * InfoWindow を表示する.
     * @param marker {@link Marker}
     * @param view {@link View}
     */
    private void render(Marker marker, View view) {
        TextView title = (TextView) view.findViewById(R.id.textView_info_window_title);
        TextView snippet = (TextView) view.findViewById(R.id.textView_info_window_snippet);
        title.setText(marker.getTitle());
        snippet.setText(marker.getSnippet());
    }

}
