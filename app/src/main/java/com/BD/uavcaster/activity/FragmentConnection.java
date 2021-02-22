package com.BD.uavcaster.activity;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.provider.Settings;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.BD.uavcaster.R;
import com.BD.uavcaster.application.DataForwarding;
import com.BD.uavcaster.application.FileOperation;
import com.BD.uavcaster.view.BubbleViscosity;
import com.dd.CircularProgressButton;

import org.json.JSONObject;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Map;

/**
 * Created by Administrator on 2016/7/8.
 */
public class FragmentConnection extends Fragment {
    // Intent请求代码
    private static final int REQUEST_CONNECT_DEVICE = 1;

    //file storage path
    private static final String FILEPATH = "/sdcard/UAVCaster/";
    private static final String FILENAME_CONFIGURATION = "configuration.bin";
    private static final String FILENAME_NMEA = "NMEA.txt";

    private String name;
    private TextView tv_state, tv_age, tv_lat, tv_lon, tv_alt, tv_time, tv_tip;
    private Button btn_gga;
    private DataForwarding mDataForwarding;
    private FileOperation mFileOperation;
    private byte[] nmea_buf = new byte[1024];
    private LocalBroadcastManager broadcastManager;
    private RandomAccessFile mFile_nmea = null;
    private Button btn_connect;
    private BubbleViscosity btn_process;
    private RandomAccessFile mFile;

    public FragmentConnection(String fName, DataForwarding dataForwarding){
        this.name = fName;
        this.mDataForwarding = dataForwarding;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = LayoutInflater.from(getActivity()).inflate(R.layout.framement_connection, container,false);

        tv_state = (TextView) view.findViewById(R.id.textview_state);
        tv_age = (TextView) view.findViewById(R.id.textview_age);
        tv_lat = (TextView) view.findViewById(R.id.textview_latitude);
        tv_lon = (TextView) view.findViewById(R.id.textview_longitude);
        tv_alt = (TextView) view.findViewById(R.id.textview_altitude);
        tv_time = (TextView) view.findViewById(R.id.textview_time);
        tv_tip = (TextView) view.findViewById(R.id.textview_tip);

        btn_gga = (Button) view.findViewById(R.id.fragment_gga);
        btn_connect = (Button) view.findViewById(R.id.btn_connect);

        btn_process = (BubbleViscosity) view.findViewById(R.id.btn_process);
        btn_process.setVisibility(View.GONE);

        btn_gga.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    if (btn_gga.getText().equals("POSITION LOGGER")) {
                        btn_gga.setText("position data logging......");

                        //create the NMEA data collecting file
                        mFileOperation = new FileOperation(getActivity(), FILEPATH, FILENAME_NMEA);
                        mFile_nmea = mFileOperation.openSuffixFile();
                    } else {
                        if (mFile_nmea != null) { mFile_nmea.close(); }
                        btn_gga.setText("POSITION LOGGER");

                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        btn_connect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Map<String, String> map;
                int input_model, input_encryption, input_mode, output_model, output_mode, output_protocol;
                String output_IP, output_port;
                boolean status;

                try {
                    if (btn_connect.getText().equals("Connect")) {
                        btn_connect.setText("Disconnect (Data Forwarding .....)");
                        tv_tip.setText("The screen will keep on while connecting.\r\n");

                        //keep the screen light
                        getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

                        mFileOperation = new FileOperation(getActivity(), FILEPATH, FILENAME_CONFIGURATION);
                        mFile = mFileOperation.openFile();
                        map = mFileOperation.readAllFromFile(mFile);

                        JSONObject setting_json = new JSONObject(map.get("content"));
                        input_model = setting_json.getInt("InputModel");
                        input_mode = setting_json.getInt("InputMode");
                        input_encryption = setting_json.getInt("InputEncryption");
                        output_model = setting_json.getInt("OutputModel");
                        output_mode = setting_json.getInt("OutputMode");
                        output_protocol = setting_json.getInt("OutputProtocol");

                        //input_model
                        if (input_model == 0) { //V100
                            input_mode = 1; //BT
                            input_encryption = 1; //HT
                        } else if (input_model == 1) { //Inno1
                            input_mode = 1; //BT
                            input_encryption = 1; //HT
                        }

                        //output_model
                        if (output_model == 0) { // DJI
                            output_mode = 1; //WIFI hotspot
                            output_protocol = 0; // Ntrip caster
                        } else if (output_model == 1) { // XAG
                            output_mode = 1; //WIFI hotspot
                            output_protocol = 0; //Ntrip caster
                        }

                        switch (input_mode) {
                            //CORS connection
                            case 0:
                                //connect to ZHD ZNetCaster
                                //status = mDataForwarding.getCORSConnected();
                                break;
                            //bluetooth connection
                            case 1:
                                //manually built a BT connection
//                                status = mDataForwarding.getBluetoothConnected();
//                                if (status == false) {
                                    if (!mDataForwarding.isBluetoothEnable()) {
                                        btn_process.setVisibility(View.GONE);
                                        tv_tip.setVisibility(View.GONE);
                                        btn_connect.setText("Connect");

                                        String notice = " kindly open the bluetooth service. ";
                                        Toast.makeText(getContext(), notice, Toast.LENGTH_LONG).show();
                                        return;
                                    }

                                    Intent serverIntent = new Intent(getActivity(), BTDeviceListActivity.class);
                                    startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
//                                }
                                break;
                        }

                        switch (output_mode) {
                            //GSM or Wifi connection
                            case 0:
                            case 1:
                                switch (output_protocol) {
                                    //0-Ntrip Caster
                                    //1-TCP Server
                                    case 0:
                                    case 1:
                                        //build a TCP Server connection
                                        status = mDataForwarding.getTCPServerSocket();
                                        break;
                                }
                                break;
                            //internal cable connection
                            case 2:

                                break;
                            //Bluetooth
                            case 3:
                                //manually built a BT connection
//                                status = mDataForwarding.getBluetoothConnected();
//                                if (status == false) {
                                    if (!mDataForwarding.isBluetoothEnable()) {
                                        btn_process.setVisibility(View.GONE);
                                        tv_tip.setVisibility(View.GONE);
                                        btn_connect.setText("Connect");

                                        String notice = " kindly open the bluetooth service. ";
                                        Toast.makeText(getContext(), notice, Toast.LENGTH_LONG).show();
                                        return;
                                    }

                                    Intent serverIntent = new Intent(getActivity(), BTDeviceListActivity.class);
                                    startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
//                                }
                                break;
                        }

                        //create a independent thread to process the BT data forwarded.
                        mDataForwarding.getConnection(input_mode, input_encryption, output_mode, output_protocol);

                        btn_process.setVisibility(View.VISIBLE);
                        tv_tip.setVisibility(View.VISIBLE);

                    } else {
                        mDataForwarding.stopConnection();

                        btn_process.setVisibility(View.GONE);
                        tv_tip.setVisibility(View.GONE);
                        tv_state.setText("");
                        tv_lat.setText("");
                        tv_lon.setText("");
                        tv_age.setText("");
                        tv_alt.setText("");
                        tv_time.setText("");
                        btn_connect.setText("Connect");

                        getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        //register the NMEA broadcastManager
        broadcastManager = LocalBroadcastManager.getInstance(getContext());
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("com.Gnss.CUSTOM_INTENT_NMEA");
        broadcastManager.registerReceiver(NMEAReceiver, intentFilter);
        //register the stop thread broadcastManager
        intentFilter = new IntentFilter();
        intentFilter.addAction("com.Gnss.CUSTOM_INTENT_STOP_THREAD");
        broadcastManager.registerReceiver(stopThread, intentFilter);
        //register the stop Conection broadcastManager
        intentFilter = new IntentFilter();
        intentFilter.addAction("com.Gnss.CUSTOM_INTENT_STOP_CONNECTION");
        broadcastManager.registerReceiver(stopConnection, intentFilter);

        return view;
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();

        try {
            //取消注册广播侦听器
            broadcastManager.unregisterReceiver(NMEAReceiver);
            broadcastManager.unregisterReceiver(stopThread);
            broadcastManager.unregisterReceiver(stopConnection);

            if (mFile_nmea != null) { mFile_nmea.close(); }

            //stop all the service
            mDataForwarding.stopConnection();
            btn_process.setVisibility(View.GONE);
            tv_tip.setVisibility(View.GONE);
            tv_state.setText("");
            tv_lat.setText("");
            tv_lon.setText("");
            tv_age.setText("");
            tv_alt.setText("");
            tv_time.setText("");
            btn_connect.setText("Connect");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private final BroadcastReceiver stopThread = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent) {
            //stop all the service
            btn_process.setVisibility(View.GONE);
            tv_tip.setVisibility(View.GONE);
            tv_state.setText("");
            tv_lat.setText("");
            tv_lon.setText("");
            tv_age.setText("");
            tv_alt.setText("");
            tv_time.setText("");
            btn_connect.setText("Connect");
        }
    };

    private final BroadcastReceiver stopConnection = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent) {
            //stop all the display
            tv_state.setText(" ");
            tv_lat.setText(" ");
            tv_lon.setText(" ");
            tv_age.setText(" ");
            tv_alt.setText(" ");
            tv_time.setText(" ");

            btn_process.setColor("#FFFFFF");
            btn_process.setText("0 KB/S");
        }
    };

    private final BroadcastReceiver NMEAReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent) {
            String[] gga_items;

            nmea_buf = null;
            nmea_buf = mDataForwarding.getNMEABuff();
            int buf_cnt = mDataForwarding.getNMEABuffCnt();
            int RTCM_cnt = mDataForwarding.getRAWBuffCnt();
            gga_items = mDataForwarding.analysisNMEA();

            String RTCM_cnt_text = Double.toString(RTCM_cnt / 1000.0);
            btn_process.setText(RTCM_cnt_text + " KB/S");

            if (gga_items.length > 14) {
                //1: Auto; 2:Locking; 3:DGPS; 4:Fixed; 5:Float;
                String state = (gga_items != null && gga_items[6] != null)?gga_items[6]:"1";
                if (state.equals("1")) {
                    tv_state.setText("None");
                } else if (state.equals("2")) {
                    tv_state.setText("Auto");
                } else if (state.equals("3")) {
                    tv_state.setText("DGPS");
                } else if (state.equals("4")) {
                    tv_state.setText("Fixed");
                } else if (state.equals("5")) {
                    tv_state.setText("Float");
                } else {
                    tv_state.setText("None");
                }
                //Latitude
                if (gga_items != null && gga_items[2].length() > 10) {
                    tv_lat.setText(gga_items[2].substring(0, 10) + gga_items[3]);
                } else if (gga_items != null) {
                    tv_lat.setText(gga_items[2] + gga_items[3]);
                } else {
                    tv_lat.setText("");
                }
                //Longitude
                if (gga_items != null && gga_items[4].length() > 10) {
                    tv_lon.setText(gga_items[4].substring(0, 10) + gga_items[5]);
                } else if (gga_items != null) {
                    tv_lon.setText(gga_items[4] + gga_items[5]);
                } else {
                    tv_lon.setText("");
                }

                tv_age.setText((gga_items != null && gga_items[13] != null)?gga_items[13]:"");
                tv_alt.setText((gga_items != null && gga_items[9] != null)?gga_items[9]:"");
                tv_time.setText((gga_items != null && gga_items[1] != null)?gga_items[1]:"");

                if (state.equals("1")) {
                    btn_process.setColor("#FF6347");
                } else if (state.equals("2") || state.equals("3")|| state.equals("5")) {
                    btn_process.setColor("#FFA500");
                } else if (state.equals("4")) {
                    btn_process.setColor("#25DA29");
                } else {
                    btn_process.setColor("#FF6347");
                }
            }

            //write the NMEA data to File if mFile_nmea has initialized
            if (mFile_nmea != null) {
                mFileOperation.writeToFileAppend(mFile_nmea, nmea_buf, buf_cnt);
            }
        }
    };

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch(requestCode) {
            case REQUEST_CONNECT_DEVICE:
                // DeviceListActivity返回时要连接的设备
                if (resultCode == Activity.RESULT_OK) {
                    // 获取设备的MAC地址
                    String address = data.getExtras().getString(
                            BTDeviceListActivity.EXTRA_DEVICE_ADDRESS);

                    boolean status = mDataForwarding.getBTSocketFromMac(address);

                    if (status == false) {
                        //stop all the service
                        mDataForwarding.stopConnection();
                        btn_process.setVisibility(View.GONE);
                        tv_tip.setVisibility(View.GONE);
                        tv_state.setText("");
                        tv_lat.setText("");
                        tv_lon.setText("");
                        tv_age.setText("");
                        tv_alt.setText("");
                        tv_time.setText("");
                        btn_connect.setText("Connect");
                    }
                } else {
                    btn_process.setVisibility(View.GONE);
                    tv_tip.setVisibility(View.GONE);
                    tv_state.setText("");
                    tv_lat.setText("");
                    tv_lon.setText("");
                    tv_age.setText("");
                    tv_alt.setText("");
                    tv_time.setText("");
                    btn_connect.setText("Connect");
                }
                break;
        }
    }
}
