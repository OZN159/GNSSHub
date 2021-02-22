package com.BD.uavcaster.activity;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.BD.uavcaster.MainActivity;
import com.BD.uavcaster.R;
import com.BD.uavcaster.application.Registration;
import com.BD.uavcaster.application.TimeFunction;

public class FragmentRegistration extends Fragment {
    private String name;

    private TextView textView_NTP, textView_expire;
    private Button button_register;
    private TimeFunction mTime;
    private Registration mRegistration;

    public FragmentRegistration(String fName, TimeFunction time, Registration registration){
        this.name = fName;
        mTime = time;
        mRegistration = registration;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = LayoutInflater.from(getActivity()).inflate(R.layout.framement_registration,container,false);

        textView_NTP = (TextView) view.findViewById(R.id.textview_NTP);
        textView_expire = (TextView) view.findViewById(R.id.textview_expireDate);
        button_register = (Button) view.findViewById(R.id.button_register);

        textView_NTP.setText(mTime.getStringCalendarTime());
        textView_expire.setText(mRegistration.getExpireTime());

        button_register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                View alert_view = LayoutInflater.from(getActivity()).inflate(R.layout.alertdialog_edittext, null);

                final EditText alert_editText = (EditText) alert_view.findViewById(R.id.alert_editText);

                final TextView alert_textView = (TextView) alert_view.findViewById(R.id.textView_UUID);
                alert_textView.setText(mRegistration.getPsuedoCode());

                Button alert_button = (Button) alert_view.findViewById(R.id.button_UUID);
                alert_button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        ClipboardManager myClipboard = (ClipboardManager)getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
                        String text;
                        text = alert_textView.getText().toString();

                        ClipData myClip = ClipData.newPlainText("text", text);
                        myClipboard.setPrimaryClip(myClip);

                        Toast.makeText(getActivity(), "Copy", Toast.LENGTH_LONG).show();
                    }
                });

                final AlertDialog alert_Dialog = new AlertDialog.Builder(getActivity())
                        .setTitle("Registration")
                        .setView(alert_view)
                        .setMessage("Please input your registration code")
                        .setCancelable(true)
                        .setPositiveButton("Submit", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                String registration_code = alert_editText.getText().toString();
                                if (!mRegistration.setRegistration(registration_code)) {
                                    Toast.makeText(getActivity(), "Registered", Toast.LENGTH_LONG).show();
                                } else {
                                    Toast.makeText(getActivity(), "Failed", Toast.LENGTH_LONG).show();
                                }
                            }
                        })
                        .create();
                alert_Dialog.show();
            }
        });

        return view;
    }
}







