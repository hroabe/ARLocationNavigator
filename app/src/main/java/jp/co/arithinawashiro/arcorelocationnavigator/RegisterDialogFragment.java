package jp.co.arithinawashiro.arcorelocationnavigator;

import android.app.Activity;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import petrov.kristiyan.colorpicker.ColorPicker;

import static android.content.Context.LAYOUT_INFLATER_SERVICE;

public class RegisterDialogFragment extends DialogFragment {

    /** ダイアログのView */
    private View layout;

    /** 選択中の色 */
    private int selectedColor = 0xFF0088FF;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(LAYOUT_INFLATER_SERVICE);
        layout = inflater.inflate(R.layout.fragment_register_dialog, (ViewGroup) getActivity().findViewById(R.id.register_dialog));

        // ダイアログの表示
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(layout);

        // 登録地点の情報を取得する
        Bundle bundle = getArguments();

        double latitude = bundle.getDouble("latitude");
        double longitude = bundle.getDouble("longitude");

        // 緯度経度
        String locStr = latitude  + ", " + longitude;
        TextView textViewLocation =  layout.findViewById(R.id.textView_register_location);
        textViewLocation.setText(locStr);

        // 住所
        String address = bundle.getString("address");
        TextView textViewAddressLocation =  layout.findViewById(R.id.textView_register_address);
        textViewAddressLocation.setText(address);

        // キャンセルボタン
        Button button = layout.findViewById(R.id.button_register_cancel);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getDialog().dismiss();
            }
        });

        // 登録ボタン
        button = layout.findViewById(R.id.button_register_location);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                EditText editText = layout.findViewById(R.id.editText_register_location_name);

                Intent result = new Intent();
                result.putExtra("name",editText.getText().toString());
                result.putExtra("latitude",latitude);
                result.putExtra("longitude",longitude);
                result.putExtra("address",address);

                result.putExtra("color",selectedColor);

                CheckBox checkBox = layout.findViewById(R.id.checkbox_regsiter_location_sea_level);
                result.putExtra("sealevel",checkBox.isChecked());

                // 呼び出し元へ通知
                if (getTargetFragment() != null) {
                    getTargetFragment().onActivityResult(getTargetRequestCode(), Activity.RESULT_OK, result);
                } else {
                    PendingIntent pi = getActivity().createPendingResult(getTargetRequestCode(), result, PendingIntent.FLAG_ONE_SHOT);

                    try {
                        pi.send(Activity.RESULT_OK);
                    } catch (PendingIntent.CanceledException e) {
                        e.printStackTrace();
                    }
                }

                getDialog().dismiss();
            }
        });

        // 色選択ボタン
        button = layout.findViewById(R.id.button_register_color);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ColorPicker picker = new ColorPicker(getContext());
                picker.setColors(
                        Color.parseColor("#FF0088FF"),
                        Color.parseColor("#FF7700FF"),
                        Color.parseColor("#FFFF0088"),
                        Color.parseColor("#FFFF7700"),
                        Color.parseColor("#FF88FF00"),
                        Color.parseColor("#FF00FF77"),
                        Color.parseColor("#FFFFFFFF")
                );
                picker.setTitle(getString(R.string.label_color_picker_dialog_title));

                picker.setDefaultColorButton(Color.parseColor("#FF0088FF"));
                picker.setColumns(4);
                picker.setOnChooseColorListener(
                        new ColorPicker.OnChooseColorListener() {
                            @Override
                            public void onChooseColor(int position, int color) {
                                //色見本に反映
                                TextView tv = layout.findViewById(R.id.textView_register_color);
                                tv.setBackgroundColor(color);

                                if (color == 0xFFFFFFFF){
                                    color = 0x00000000;
                                }
                                selectedColor = color;
                            }

                            @Override
                            public void onCancel() {

                            }
                        }
                );
                picker.setRoundColorButton(true).show();
            }
        });

        return builder.create();
    }

}
