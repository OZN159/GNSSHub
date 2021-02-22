package com.BD.uavcaster;

import android.Manifest;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.BD.uavcaster.activity.FragmentConnection;
import com.BD.uavcaster.activity.FragmentRegistration;
import com.BD.uavcaster.activity.FragmentSetting;
import com.BD.uavcaster.application.DataForwarding;
import com.BD.uavcaster.application.Registration;
import com.BD.uavcaster.application.TimeFunction;
import com.BD.uavcaster.view.FloatingNavigationView;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentTransaction;

public class MainActivity extends AppCompatActivity {
    private final String CONTACT_INFO = "Email: Nander@hitargetgroup.com.cn \n" +
                                        "Website: http://bd.hi-target.com.cn/ \n";

    private FragmentConnection f1;
    private FragmentSetting f2;
    private FragmentRegistration f3;

    private DataForwarding mDataForwarding;
    private FloatingNavigationView mFloatingNavigationView;
    private Registration mRegistration;
    private View alert_view;
    private EditText alert_editText;
    private TextView alert_textView, alert_expireDate, alert_expireState;
    private Button alert_button;
    private ClipboardManager myClipboard;
    private ClipData myClip;
    private networkTimeThread mTimeThread;
    private TimeFunction mTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mDataForwarding = new DataForwarding(getApplicationContext());

        mTimeThread = new networkTimeThread();
        mTimeThread.start();

        mRegistration = new Registration(getApplicationContext(), mTime);
        if (mRegistration.isRegistration() >= 0) {
                Toast.makeText(this, "Registered", Toast.LENGTH_LONG).show();
        } else {
            alert_view = LayoutInflater.from(this).inflate(R.layout.alertdialog_edittext, null);
            alert_editText = (EditText) alert_view.findViewById(R.id.alert_editText);

            alert_textView = (TextView) alert_view.findViewById(R.id.textView_UUID);
            alert_textView.setText(mRegistration.getPsuedoCode());

//            alert_expireDate = (TextView) alert_view.findViewById(R.id.textView_expireDate);
            //wait for the Ntp time to get success
//            while(mTime.getLongNTPTime() == 0) {
//
//            }

            alert_button = (Button) alert_view.findViewById(R.id.button_UUID);
            alert_button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    myClipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                    String text;
                    text = alert_textView.getText().toString();

                    myClip = ClipData.newPlainText("text", text);
                    myClipboard.setPrimaryClip(myClip);

                    Toast.makeText(getApplicationContext(), "Copy", Toast.LENGTH_LONG).show();
                }
            });

             final AlertDialog alert_Dialog = new AlertDialog.Builder(this)
                    .setTitle("Registration")
                    .setView(alert_view)
                    .setMessage("Please register first")
                    .setCancelable(false)
                    .setPositiveButton("Submit", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            String registration_code = alert_editText.getText().toString();
                            if (!mRegistration.setRegistration(registration_code)) {
                                new AlertDialog.Builder(MainActivity.this)
                                        .setTitle("Error")
                                        .setMessage("Registration failed: the Application will be exited.")
                                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialogInterface, int i) {
                                                exitApplication();
                                            }
                                        })
                                        .show();

                            } else {
                                new AlertDialog.Builder(MainActivity.this)
                                        .setTitle("Success")
                                        .setMessage("Registration Success: Please login again.")
                                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialogInterface, int i) {
                                                exitApplication();
                                            }
                                        })
                                        .show();
                            }
                        }
                    })

                    .setNegativeButton("Exit", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            exitApplication();
                        }
                    })
                    .create();
            alert_Dialog.show();
        }

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //fragment initialization
        //initFragment2();
        initFragment1();

        mFloatingNavigationView = (FloatingNavigationView) findViewById(R.id.floating_navigation_view);
        mFloatingNavigationView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mFloatingNavigationView.open();
            }
        });
        mFloatingNavigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.nav_connection:
                        initFragment1();
                        break;
                    case R.id.nav_manage:
                        initFragment2();
                        break;
                    case R.id.nav_slideshow:
                        initFragment3();
                        break;
                    case R.id.nav_share:
                        initShare();
                        break;
                    case R.id.nav_send:
                        Snackbar.make((View) mFloatingNavigationView.getParent(), CONTACT_INFO, Snackbar.LENGTH_LONG).show();
                        break;
                }

                mFloatingNavigationView.close();
                return true;
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (mFloatingNavigationView.isOpened()) {
            mFloatingNavigationView.close();
        } else {
            super.onBackPressed();
        }
    }

    private void initShare() {
        Intent textIntent = new Intent(Intent.ACTION_SEND);
        textIntent.setType("text/plain");
        textIntent.putExtra(Intent.EXTRA_TEXT,
                "This is a software that is a smart tool to be used in UAV and base station. " +
                        "It's compatible with DJI, XAG, Yuneec and others popular UAV companies.\n\n" +
                        "Contact us: Hi-target international group.\n" +
                        "Website: http://bd.hi-target.com.cn\n" +
                        "Technical support: Nander@hitargetgroup.com");
        startActivity(Intent.createChooser(textIntent, "Share"));
    }

    //display the connection fragment
    private void initFragment1(){
        //开启事务，fragment的控制是由事务来实现的
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

        //第一种方式（add），初始化fragment并添加到事务中，如果为null就new一个
        if(f1 == null){
            f1 = new FragmentConnection("Connection", mDataForwarding);
            transaction.add(R.id.main_frame_layout, f1);
        }
        //隐藏所有fragment
        hideFragment(transaction);
        //显示需要显示的fragment
        transaction.show(f1);

        //提交事务
        transaction.commit();
    }

    //display the setting fragment
    private void initFragment2(){
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

        if(f2 == null){
            f2 = new FragmentSetting("Setting", mDataForwarding);
            transaction.add(R.id.main_frame_layout,f2);
        }
        hideFragment(transaction);
        transaction.show(f2);

        transaction.commit();
    }

    //display the setting fragment
    private void initFragment3(){
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

        if(f3 == null){
            f3 = new FragmentRegistration("Registration", mTime, mRegistration);
            transaction.add(R.id.main_frame_layout,f3);
        }
        hideFragment(transaction);
        transaction.show(f3);

        transaction.commit();
    }

    //hide all the fragment
    private void hideFragment(FragmentTransaction transaction){
        if(f1 != null){
            transaction.hide(f1);
        }
        if(f2 != null){
            transaction.hide(f2);
        }
        if(f3 != null){
            transaction.hide(f3);
        }
    }

    //exit APP
    private void exitApplication(){
//        Long exitTime;
//
//        exitTime = System.currentTimeMillis();
//        while (System.currentTimeMillis() - exitTime < 2000);

        finish();
        System.exit(0);
    }

    private class networkTimeThread extends Thread {

        public networkTimeThread() {
            Log.d("networkTimeThread", "create networkTimeThread");
            mTime = new TimeFunction(getApplicationContext());
        }

        public void run() {
            if (!mTime.getCalendarTime()) {
                Looper.prepare();
                AlertDialog alert_Dialog = new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Error")
                        .setMessage("No Time service. Please Check your location time.")
                        .setCancelable(false)
                        .setNegativeButton("Exit", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                exitApplication();
                            }
                        })
                        .create();
                alert_Dialog.show();
                Looper.loop();
            } else if (mTime.getLongCalendarTime() > mRegistration.getLongExpireTime()
            && mRegistration.getLongExpireTime() > 20200101 && mTime.getLongCalendarTime() > 20200101) {
                Looper.prepare();
                alert_view = LayoutInflater.from(MainActivity.this).inflate(R.layout.alertdialog_edittext, null);
                alert_editText = (EditText) alert_view.findViewById(R.id.alert_editText);

                alert_textView = (TextView) alert_view.findViewById(R.id.textView_UUID);
                alert_textView.setText(mRegistration.getPsuedoCode());

                //wait for the Ntp time to get success
                while(mTime.getLongCalendarTime() == 0) {

                }

                alert_button = (Button) alert_view.findViewById(R.id.button_UUID);
                alert_button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        myClipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                        String text;
                        text = alert_textView.getText().toString();

                        myClip = ClipData.newPlainText("text", text);
                        myClipboard.setPrimaryClip(myClip);

                        Toast.makeText(getApplicationContext(), "Copy", Toast.LENGTH_LONG).show();
                    }
                });

                AlertDialog alert_Dialog = new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Registration")
                        .setView(alert_view)
                        .setMessage("")
                        .setCancelable(false)
                        .setPositiveButton("Submit", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                String registration_code = alert_editText.getText().toString();
                                if (!mRegistration.setRegistration(registration_code)) {
                                    new AlertDialog.Builder(MainActivity.this)
                                            .setTitle("Error")
                                            .setMessage("Registration failed: the Application will be exited.")
                                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialogInterface, int i) {
                                                    exitApplication();
                                                }
                                            })
                                            .show();

                                } else {
                                    new AlertDialog.Builder(MainActivity.this)
                                            .setTitle("Success")
                                            .setMessage("Registration Success: Please login again.")
                                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialogInterface, int i) {
                                                    exitApplication();
                                                }
                                            })
                                            .show();
                                }
                            }
                        })

                        .setNegativeButton("Exit", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                exitApplication();
                            }
                        })
                        .create();
                alert_Dialog.show();
                Looper.loop();
            }
        }
    }
}
