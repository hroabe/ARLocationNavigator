package jp.co.arithinawashiro.arcorelocationnavigator;


import android.app.Dialog;
import android.content.Intent;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import static android.content.Context.LAYOUT_INFLATER_SERVICE;

public class LicenseDialogFragment extends DialogFragment {

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        LayoutInflater inflater = (LayoutInflater)getActivity().getSystemService(LAYOUT_INFLATER_SERVICE);
        View layout = inflater.inflate(R.layout.fragment_license, getActivity().findViewById(R.id.fragment_license));

        try{
            // CSVファイルを読み込み
            AssetManager as = getResources().getAssets();
            InputStream is = as.open("license/LICENSE_ARCore-Location");
            BufferedReader br = new BufferedReader(new InputStreamReader(is));

            StringBuilder license = new StringBuilder();
            String line;
            while((line = br.readLine()) != null){
                line = line +"\n";
                license.append(line);
            }

            TextView tv = layout.findViewById(R.id.license_text);
            tv.setText(license);

        }catch (Exception e){
            TextView tv = layout.findViewById(R.id.license_text);
            tv.setText(R.string.error);
        }

        //ダイアログの表示
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(layout);
        builder.setPositiveButton("OK", null);
        return builder.create();
    }

}
