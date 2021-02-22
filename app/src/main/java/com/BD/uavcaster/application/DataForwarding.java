package com.BD.uavcaster.application;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Build;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.BD.uavcaster.MainActivity;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.RecursiveTask;
import java.util.regex.Pattern;

import it.sauronsoftware.base64.Base64;

public class DataForwarding {
    private volatile boolean STATE_CONNECTED = true;
    private volatile boolean STATE_THREAD = true;

    //Ntrip protocol connection parameters
    private static final String NTRIP_IP = "202.96.185.34";
    private static final int NTRIP_PORT = 2101;
    private static final String NTRIP_SOURCE = "1135095026";
    private static final String NTRIP_USER_NAME = "11350101";
    private static final String NTRIP_PWD = "zhdgps";

    //GGA items
    private static final int GGA_LATITUDE = 0;
    private static final int GGA_LONGITUDE = 1;
    private static final int GGA_STATE = 2;
    private static final int GGA_TIME = 3;
    private static final int GGA_SV = 4;

    // 该应用的唯一UUID
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private Context mContext;
    private Intent intent_rawData, intent_nmea, intent_stopThread, intent_stopConnection;

    private BluetoothAdapter mBluetooth;
    private BluetoothSocket bt_socket;
    private ServerSocket tcpServer_socket;
    private Socket tcpClient_socket;
    private DataForwardingThread mDataThread;

    private LocationManager mLocationManager;
    private boolean hasAddTestProvider = false;
    private double latitude, longitude, altitude;

    private byte[] NMEA_buf = new byte[1024];
    private byte[] RAW_buf = new byte[10240];
    private byte[] RTCM_buf = new byte[1024];

    private int NMEA_buf_cnt, RAW_buf_cnt, RTCM_buf_cnt;
    private String NMEA_stream, RTCM_stream;

    public DataForwarding(Context context) {
        mContext = context;

        //get the BT adapter
        mBluetooth = BluetoothAdapter.getDefaultAdapter();

        //register the raw data update signal
        intent_rawData = new Intent("com.Gnss.CUSTOM_INTENT_RAW");
        intent_rawData.setComponent(new ComponentName("com.BD.uavcaster", "com.BD.uavcaster.rawReceiver"));
        //register the NMEA data update signal
        intent_nmea = new Intent("com.Gnss.CUSTOM_INTENT_NMEA");
        intent_nmea.setComponent(new ComponentName("com.BD.uavcaster", "com.BD.uavcaster.NMEAReceiver"));
        //register the stop thread signal
        intent_stopThread = new Intent("com.Gnss.CUSTOM_INTENT_STOP_THREAD");
        intent_stopThread.setComponent(new ComponentName("com.BD.uavcaster", "com.BD.uavcaster.stopThread"));
        //register the stop Connection signal
        intent_stopConnection = new Intent("com.Gnss.CUSTOM_INTENT_STOP_CONNECTION");
        intent_stopConnection.setComponent(new ComponentName("com.BD.uavcaster", "com.BD.uavcaster.stopConnection"));
    }


    /*********************************************Public Function******************************************/
    public boolean getBluetoothConnected() {
        byte[] bytes = new byte[0];

        //get the BluetoothAdapter class object
        Class<BluetoothAdapter> bluetoothAdapterClass = BluetoothAdapter.class;
        try {
            //use the reflection mechanism to get the BT connected devices
            Method method = bluetoothAdapterClass.getDeclaredMethod("getConnectionState", (Class[]) null);
            method.setAccessible(true);
            int state = (int) method.invoke(mBluetooth, (Object[]) null);

            //get the bound/mated device list
            Log.i("BLUETOOTH","BluetoothAdapter.STATE_CONNECTED");
            Set<BluetoothDevice> devices = mBluetooth.getBondedDevices();
            Log.i("BLUETOOTH","devices:" + devices.size());

            if(state == BluetoothAdapter.STATE_CONNECTED){
                //the connected device already exists
                for(BluetoothDevice device : devices){
                    //check the device if it is connected
                    Method isConnectedMethod = BluetoothDevice.class.getDeclaredMethod("isConnected", (Class[]) null);
                    method.setAccessible(true);
                    boolean isConnected = (boolean) isConnectedMethod.invoke(device, (Object[]) null);

                    if(isConnected){
                        boolean status = getBTSocket(device);
                        return status;
                    }
                }
            } else {
                //the connected device doesn't exist
                return false;

            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        return false;
    }

    public boolean getTCPServerSocket() {
        ServerSocket mServerSocket;

        //create a TCP server socket
        try {
            mServerSocket = new ServerSocket(10001);
            tcpServer_socket = mServerSocket;

            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean getCORSConnected() {
        Socket mSocket;

        while(true) {
            try {
//            SocketChannel socket_channel = SocketChannel.open();
//            socket_channel.connect(new InetSocketAddress(NTRIP_IP, NTRIP_PORT));
//            socket_channel.configureBlocking(false);  // non-block
//            socket_channel.setOption(StandardSocketOptions.SO_KEEPALIVE, Boolean.TRUE)
//                    .setOption(StandardSocketOptions.SO_RCVBUF, 10240)
//                    .setOption(StandardSocketOptions.SO_SNDBUF, 10240);
//            tcpClient_socket = socket_channel;

                mSocket = new Socket();
                SocketAddress address = new InetSocketAddress(NTRIP_IP, NTRIP_PORT);
                mSocket.connect(address, 1000*10);
                tcpClient_socket = mSocket;

                break;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return true;
    }

    public boolean getBTSocket(BluetoothDevice device) {
        try {
            //Bluetooth connect
            String bt_address = device.getName();
            bt_socket = device.createRfcommSocketToServiceRecord(MY_UUID);
            //cancel the display status for faster running operation
            mBluetooth.cancelDiscovery();
            bt_socket.connect();

            String notice = " Bluetooth connected: " + bt_address;
            Toast.makeText(mContext, notice, Toast.LENGTH_LONG).show();
            return true;
        } catch (Exception e) {
            //close and recover the BT service
            stopBTSocket();

            if(mDataThread != null) {
                mDataThread.cancel();
            }

            String notice = " Bluetooth connect failed: " + e;
            Toast.makeText(mContext, notice, Toast.LENGTH_LONG).show();
            e.printStackTrace();
            return false;
        }
    }

    public boolean getBTSocketFromMac(String address) {
        try {
            BluetoothDevice device = mBluetooth.getRemoteDevice(address);
            //Bluetooth connect
            String bt_address = device.getName();
            bt_socket = device.createRfcommSocketToServiceRecord(MY_UUID);
            //cancel the display status for faster running operation
            mBluetooth.cancelDiscovery();
            bt_socket.connect();

            String notice = " Bluetooth connected: " + bt_address;
            Toast.makeText(mContext, notice, Toast.LENGTH_LONG).show();
            return true;
        } catch (Exception e) {
            //close the BT socket
            stopBTSocket();

            if(mDataThread != null) {
                mDataThread.cancel();
            }

            String notice = " Bluetooth connect failed: " + e;
            Toast.makeText(mContext, notice, Toast.LENGTH_LONG).show();
            e.printStackTrace();
            return false;
        }
    }

    public boolean stopBTSocket() {
        //close and recover the BT service
        try {
            if (bt_socket != null) { bt_socket.close(); bt_socket = null; }
            return true;
        } catch (IOException e1) {
            e1.printStackTrace();
            return false;
        }
    }

    public void getConnection(int inputMode, int inputEncryption, int outMode, int outputProtocol) {
        mDataThread = new DataForwardingThread(inputMode, inputEncryption, outMode, outputProtocol);
        mDataThread.start();
    }

    public String[] analysisNMEA() {
        String[] GGA_item = new String[20];

        for(int i=0; i < 20; i++) {
            GGA_item[i] = "";
        }

        if (NMEA_stream != null && NMEA_stream.contains("GGA") && NMEA_buf_cnt > 50) {
            GGA_item = NMEA_stream.substring(0, NMEA_buf_cnt).split(",");

            if(GGA_item.length > 9) {
                //get the lat, lon, alt
                latitude = (GGA_item[2].length() > 6 && isDouble(GGA_item[2]))? Double.parseDouble(GGA_item[2]):0.0;
                longitude = (GGA_item[4].length() > 6 && isDouble(GGA_item[4]))? Double.parseDouble(GGA_item[4]):0.0;
                altitude = (GGA_item[9].length() > 4 && isDouble(GGA_item[9]))? Double.parseDouble(GGA_item[9]):0.0;

                latitude = Math.floor(latitude / 100) + (((latitude / 100) -  Math.floor(latitude / 100)) * 100) / 60;
                longitude = Math.floor(longitude / 100) + (((longitude / 100) -  Math.floor(longitude / 100)) * 100) / 60;

                String notice = latitude + " , " + longitude;
                //Toast.makeText(mContext, notice, Toast.LENGTH_SHORT).show();

                //deal with the issue of S/N , E/W
                latitude = (GGA_item[3].contains("S"))? -latitude:latitude;
                longitude = (GGA_item[5].contains("W"))? -longitude:longitude;
            }

            return GGA_item;
        } else {
            return GGA_item;
        }
    }

    public boolean isBluetoothEnable() {
        if (mBluetooth != null) {
            return  mBluetooth.isEnabled();
        } else {
            return false;
        }
    }

    public void stopConnection(){
        mDataThread.cancel();
        STATE_THREAD = false;

        try {
            if (tcpServer_socket != null) { tcpServer_socket.close(); tcpServer_socket = null; }
            if (bt_socket != null) { bt_socket.close(); bt_socket = null; }
        } catch (Exception e) {
            e.printStackTrace();
        }

        mDataThread = null;
    }

    public byte[] getNMEABuff() {
        return NMEA_buf;
    }

    public int getRAWBuffCnt() {
        return RAW_buf_cnt;
    }

    public int getRTCMBuffCnt() {
        return RTCM_buf_cnt;
    }

    public int getNMEABuffCnt() {
        return NMEA_buf_cnt;
    }

    public byte[] getRawBuff() {
        return RAW_buf;
    }

    public byte[] getRTCMBuff() {
        return RTCM_buf;
    }

    public boolean initLocation() {
        mLocationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);

        //check the GPS service if it is opened
//        if (!mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
//            return false;
//        }

        Criteria criteria = new Criteria();
        criteria.setAccuracy( Criteria.ACCURACY_FINE );
        String provider = mLocationManager.getBestProvider( criteria, true );

        if ( provider == null ) {
            return false;
        }

        try {
            //如果未开启模拟位置服务，则添加模拟位置服务
            mLocationManager.addTestProvider(LocationManager.GPS_PROVIDER, false, false, false, false, true, true, true, 0, 5);
            mLocationManager.setTestProviderEnabled(LocationManager.GPS_PROVIDER, true);

        } catch (Exception e) {
            return false;
        }
        return true;
    }


    public boolean isMockLocationAvailable() {
        boolean canMockPosition = (Settings.Secure.getInt(mContext.getContentResolver(), Settings.Secure.ALLOW_MOCK_LOCATION, 0) != 0)
                || Build.VERSION.SDK_INT > 22;
        if (hasAddTestProvider == false) {
            try {
                mLocationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
                String providerStr = LocationManager.GPS_PROVIDER;
                LocationProvider provider = mLocationManager.getProvider(providerStr);
                if (provider != null) {
                    mLocationManager.addTestProvider(
                            provider.getName()
                            , provider.requiresNetwork()
                            , provider.requiresSatellite()
                            , provider.requiresCell()
                            , provider.hasMonetaryCost()
                            , provider.supportsAltitude()
                            , provider.supportsSpeed()
                            , provider.supportsBearing()
                            , provider.getPowerRequirement()
                            , provider.getAccuracy());
                } else {
                    mLocationManager.addTestProvider(
                            providerStr
                            , true, true, false, false, true, true, true
                            , Criteria.POWER_HIGH, Criteria.ACCURACY_FINE);
                }
                mLocationManager.setTestProviderEnabled(providerStr, true);

                // 模拟位置可用
                hasAddTestProvider = true;
            } catch (SecurityException e) {
                hasAddTestProvider = false;
            }
        }
        return hasAddTestProvider;
    }

    public void stopMockLocation() {
        if (mLocationManager != null) {
            try {
                mLocationManager.removeTestProvider(LocationManager.GPS_PROVIDER);
            } catch (Exception e) {
                Log.e("GPS", e.toString());
            }
        }
    }

    public void separateRTCMData(OutputStream output_stream_w, InputStream input_stream_r, byte[] buf, int buf_cnt) {
        int rtcm_cnt = 0;
        int complement_buf_cnt;

        RTCM_buf = null;
        RTCM_buf = new byte[1024];
        for (int i = 0; i < buf_cnt; i++) {
            if (buf[i] == '$' && buf[i+1] == '$' && buf[i+2] == 'G' && buf[i+3] == 'I') {
                //RTCM Data length
                int a= buf[i+4] & 0xff;
                int b= buf[i+5] & 0xff;
                rtcm_cnt = a + (b << 8) - 1;

                if (rtcm_cnt > (buf_cnt - i - 6)) {
                    String notice = rtcm_cnt + ", " + buf_cnt;
                    Log.d("separateRTCMData: ", notice);

                    //Extract RTCM data
                    System.arraycopy(buf, i+7, RTCM_buf, 0, buf_cnt - i - 7);
                    //
                    try {
                        complement_buf_cnt = input_stream_r.read(RTCM_buf, buf_cnt - i - 7, (rtcm_cnt - 1) - (buf_cnt - i - 7));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    RTCM_buf_cnt = rtcm_cnt;
                } else {
                    //Extract RTCM data
                    System.arraycopy(buf, i+7, RTCM_buf, 0, rtcm_cnt);
                    RTCM_buf_cnt = rtcm_cnt;
                }

                // print the data
                Log.d("DFThread(RTCM_buf_cnt)", Integer.toString(RTCM_buf_cnt));
                Log.d("DFThread(Input)", byte2hex(RTCM_buf, RTCM_buf_cnt));

                //try to forward the data
                if (RTCM_buf_cnt > 0 && RAW_buf_cnt > 0) {
                    try {
                        output_stream_w.write(RTCM_buf, 0, RTCM_buf_cnt);
                        output_stream_w.flush();
                        RTCM_buf_cnt = 0;                   } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    /**************************************************Private Function*************************************/
    private double lat = 22.98;
    private double lon = 113.37;

    private boolean isDouble(String data) {
        String reg = "^[0-9]+(.[0-9]+)?$";

        if(data.matches(reg)) {
            return true;
        } else {
            return false;
        }
    }

    private boolean isInteger(String data) {
        Pattern pattern = Pattern.compile("^[-\\+]?[\\d]*$");
        return pattern.matcher(data).matches();
    }

    //private class RunnableMockLocation implements Runnable {
    public void RunnableMockLocation() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(100);

//                        if (hasAddTestProvider == false) {
//                            continue;
//                        }

                        try {
                            // 模拟位置（addTestProvider成功的前提下）
                            String providerStr = LocationManager.GPS_PROVIDER;
                            Location mockLocation = new Location(providerStr);

                            if (latitude != 0.0) {
                                mockLocation.setLatitude(latitude);
                            } //纬度
                            if (longitude != 0.0) {
                                mockLocation.setLongitude(longitude);
                            } //经度
                            if (altitude != 0.0) {
                                mockLocation.setAltitude(altitude);
                            }  // 高程（米）

                            //test
//                            lat = lat + 10e-6;
//                            lon = lon + 10e-6;
//                            mockLocation.setLatitude(lat);
//                            mockLocation.setLongitude(lon);
//                            mockLocation.setAltitude(22);

                            mockLocation.setBearing(180);  // 方向（度）
                            mockLocation.setSpeed(1);  //速度（米/秒）
                            mockLocation.setAccuracy(0.1f);  // 精度（米）
                            mockLocation.setTime(System.currentTimeMillis());  // 本地时间
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                                mockLocation.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
                            }
                            mLocationManager.setTestProviderLocation(providerStr, mockLocation);
                        } catch (Exception e) {
                            // 防止用户在软件运行过程中关闭模拟位置或选择其他应用
                            stopMockLocation();
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    //ntrip client login request
    private boolean ntripCilentRequest(InputStream input_stream, OutputStream ouput_stream) {
        int buf_cnt;
        byte[] buffer = new byte[1024];
        String user_and_pwd = NTRIP_USER_NAME + ":" + NTRIP_PWD;

        Base64 encoder = new Base64();
        user_and_pwd = encoder.encode(user_and_pwd);

        //user_and_pwd = Base64.getEncoder().encodeToString(user_and_pwd.getBytes());

        String ntrip_client_header = "GET /"+ NTRIP_SOURCE + " HTTP/1.0\r\n" +
                                    "User-Agent: NTRIP Hi-Target-VNet/4.0.0\r\n" +
                                    "Accept: */*\r\n" +
                                    "Connection: close\r\n" +
                                    "Authorization: Basic " + user_and_pwd + "\r\n\r\n";

        //try to send the ntrip client header
        while (true) {
            try {
                ouput_stream.write(ntrip_client_header.getBytes());
                ouput_stream.flush();
                Thread.sleep(500);

                buf_cnt = input_stream.read(buffer);
                String ntrip_respond = new String(buffer);
                if (ntrip_respond.contains("ICY 200 OK")) {
                    break;
                }

            } catch (Exception e) {
                e.printStackTrace();
                return  false;
            }
        }

        return true;
    }

    //ntrip caster login verification
    private boolean ntripCasterVerification(InputStream input_stream, OutputStream ouput_stream) {
        int buf_cnt;
        byte[] buffer = new byte[1024];
        String[] buf, buf1, buf2;
        String source="", user="", pwd="";

        String ntrip_caster_response = "ICY 200 OK\r\n";

        try {
            while(input_stream.available() < 30) {
                Thread.sleep(100);
            }

            buf_cnt = input_stream.read(buffer, 0, 1024);
            String ntrip_request = new String(buffer);
            Log.d("ntripCasterVerification", ntrip_request);
            //parse out source, user and pwd
            buf = ntrip_request.split("\r\n");

            for(int i =0; i < buf.length; i++) {
                //parse out the source
                if (buf[i].contains("GET")) {
                    buf1 = buf[i].split("/");
                    buf2 = buf1[1].split(" ");
                    source = buf2[0];
                }
                //parse out the source
                if (buf[i].contains("Authorization")) {
                    buf1 = buf[i].split(" ");
                    String encode_buf = buf1[2];

                    //decode base64
                    Base64 encoder = new Base64();
                    String decode_buf = encoder.decode(encode_buf);

                    //String decode_buf = new String(Base64.getDecoder().decode(encode_buf), StandardCharsets.UTF_8);
                    buf1 = decode_buf.split(":");
                    user = buf1[0];
                    pwd = buf1[1];
                }
            }

            if (pwd.equals("zhdgps")) {
                ouput_stream.write(ntrip_caster_response.getBytes(), 0, ntrip_caster_response.length());
                Log.d("ntripCasterVerification", ntrip_caster_response);
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return  false;
        }
    }

    private boolean getUSBHostSocket() {
        //get the usb service
        UsbManager mUSBManger = (UsbManager)mContext.getSystemService(Context.USB_SERVICE);
        //get all the usb connected list
        HashMap<String, UsbDevice> USB_device_list = mUSBManger.getDeviceList();
        Iterator mInterator = USB_device_list.values().iterator();

        return true;
    }

    private static String byte2hex(byte [] buffer, int buffer_cnt){
        String h = "";

        for(int i = 0; i < buffer_cnt; i++){
            String temp = Integer.toHexString(buffer[i] & 0xFF);
            if(temp.length() == 1){
                temp = "0" + temp;
            }
            h = h + " 0x"+ temp;
        }

        return h;

    }

    //this function normally used to receive the data.
    private class DataForwardingThread extends Thread {
        private Socket mSocket;
        private InputStream input_stream_r, output_stream_r;
        private OutputStream input_stream_w, output_stream_w;

        private int input_mode, input_encryption, output_mode, output_protocol;

        public DataForwardingThread(int inputMode, int inputEncryption, int outputMode, int outputProtocol) {
            Log.d("DataForwardingThread", "create DataForwardingThread");
            input_mode = inputMode;
            output_mode = outputMode;
            output_protocol = outputProtocol;
            input_encryption = inputEncryption;
        }

        public void run()
        {
            String str;
            int err_cnt = 0;

            Log.i("DataForwardingThread", "BEGIN mConnectedThread");
            int bytes;

            STATE_THREAD = true;

            while (STATE_THREAD) {
                try {
                    STATE_CONNECTED = true;
                    err_cnt = 0;
                    //output mode
                    //GSM mode
                    if (output_mode == 0) {

                    //WIFI hotspot mode
                    } else if (output_mode == 1) {
                        switch (output_protocol) {
                            case 0:
                                //accept: waiting for connecting
                                try {
                                    mSocket = tcpServer_socket.accept();
                                    Log.i("createNtripCasterSocket", "Accepted");
                                    //try to get the IO from the TCP server
                                    output_stream_r = mSocket.getInputStream();
                                    output_stream_w = mSocket.getOutputStream();
                                    output_stream_w.flush();

                                    //ntrip login in verification
                                    while (!ntripCasterVerification(output_stream_r, output_stream_w)) {
//                                  String notice = "Ntrip Caster: login failed ";
//                                  Toast.makeText(mContext, notice, Toast.LENGTH_LONG).show();

                                        mSocket.close();
                                        output_stream_r = null;
                                        output_stream_w = null;

                                        mSocket = tcpServer_socket.accept();

                                        //try to get the IO from the TCP server
                                        output_stream_r = mSocket.getInputStream();
                                        output_stream_w = mSocket.getOutputStream();
                                    }

                                } catch (IOException e) {
                                    e.printStackTrace();
                                    Log.e("DataForwardingThread", "temp sockets not created", e);
                                }
                                break;

                            case 1:
                                //accept: waiting for connecting
                                try {
                                    mSocket = tcpServer_socket.accept();
                                    Log.i("createTCPServerSocket", "Accepted");
                                    //try to get the IO from the TCP server
                                    output_stream_r = mSocket.getInputStream();
                                    output_stream_w = mSocket.getOutputStream();
                                    output_stream_w.flush();

                                } catch (IOException e) {
                                    e.printStackTrace();
                                    Log.e("DataForwardingThread", "temp sockets not created", e);
                                }
                                break;
                        }
                        // internal cable
                    } else if (output_mode == 2) {

                        //Bluetooth
                    } else if (output_mode == 3) {
                        try {
                            //try to get the IO from the BT
                            while (bt_socket == null) {
                                try {
                                    Thread.sleep(1000);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                            output_stream_r = bt_socket.getInputStream();
                            output_stream_w = bt_socket.getOutputStream();
                            output_stream_w.flush();

                        } catch (IOException e) {
                            e.printStackTrace();
                            Log.e("DataForwardingThread", "temp sockets not created", e);
                        }
                    }

                    //input mode
                    //CORS
                    if (input_mode == 0) {
                        try {
                            getCORSConnected();
                            //try to get the IO from the tcp client
                            input_stream_r = tcpClient_socket.getInputStream();
                            input_stream_w = tcpClient_socket.getOutputStream();
                            input_stream_w.flush();

                            //ntrip client login request
                            ntripCilentRequest(input_stream_r, input_stream_w);

                        } catch (IOException e) {
                            e.printStackTrace();
                            Log.e("DataForwardingThread", "temp sockets not created", e);
                        }
                        // Bluetooth
                    } else if (input_mode == 1) {
                        try {
                            //try to get the IO from the BT
                            while (bt_socket == null) {
                                try {
                                    Thread.sleep(1000);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                            input_stream_r = bt_socket.getInputStream();
                            input_stream_w = bt_socket.getOutputStream();
                            input_stream_w.flush();

                        } catch (IOException e) {
                            e.printStackTrace();
                            Log.e("DataForwardingThread", "temp sockets not created", e);
                        }
                    }

                    while (STATE_CONNECTED) {
                        try {
                            //run this function if input section has data
                            if (input_stream_r != null && input_stream_r.available() > 50){
                                //sleep 100ms, waiting the whole package arrive
                                try {
                                    Thread.sleep(100);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }

                                RAW_buf = null;
                                RAW_buf = new byte[10240];
                                RAW_buf_cnt = input_stream_r.read(RAW_buf);
                                LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent_rawData);

                                // print the data
                                //Log.d("DFThread(RAW_buf)", byte2hex(RAW_buf, RAW_buf_cnt));

                                // separate the RTCM data from ZHD GI data
                                if (input_encryption == 1) {
                                    separateRTCMData(output_stream_w, input_stream_r, RAW_buf, RAW_buf_cnt);
                                } else {
                                    // print the data
                                    RTCM_stream = new String(RAW_buf);
                                    //Log.d("DFThread(Input)", byte2hex(RAW_buf, RAW_buf_cnt));
                                    //Log.d("DFThread(Input)", RTCM_stream.substring(0, RAW_buf_cnt));

                                    //try to forward the data
                                    if (RAW_buf_cnt > 0) {
                                        try {
                                            output_stream_w.write(RAW_buf, 0, RAW_buf_cnt);
                                            output_stream_w.flush();
                                            RAW_buf_cnt = 0;
                                            err_cnt = 0;
                                        } catch (Exception e) {
                                            err_cnt++;
                                            e.printStackTrace();
                                        }
                                    }
                                }

                            }

                            //run this function if output section has data
                            if (output_stream_r != null && output_stream_r.available() > 50){
                                //sleep 100ms, waiting the whole package arrive
                                try {
                                    Thread.sleep(100);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                NMEA_buf_cnt = output_stream_r.read(NMEA_buf);
                                LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent_nmea);

                                // print the BT data
                                NMEA_stream = new String(NMEA_buf);
                                Log.d("DF(Output)", NMEA_stream.substring(0, NMEA_buf_cnt));

                                //try to forward the data
                                if (NMEA_buf_cnt > 0) {
                                    try {
                                        input_stream_w.write(NMEA_buf, 0, NMEA_buf_cnt);
                                        input_stream_w.flush();
                                        err_cnt = 0;
                                    } catch (Exception e) {
                                        Looper.prepare();
                                        String notice = "The software is kitted from server, please reconnect again!";
                                        Toast.makeText(mContext, notice, Toast.LENGTH_LONG).show();
                                        Looper.loop();
                                        err_cnt++;
                                        e.printStackTrace();
                                    }
                                }
                            }

                            //break out when error happen out of 30 secs
                            if (err_cnt > 30){
                                Log.e("DataForwardingThread", "err_cnt overrun");
                                if(mDataThread != null) {
                                    mDataThread.cancel();
                                }
                                LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent_stopThread);
                                break;
                            } else if (err_cnt > 10 && err_cnt < 30) {
                                LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent_stopConnection);
                            }
                        } catch (IOException e) {
                            Log.e("DataForwardingThread", "disconnected", e);
                            if(mDataThread != null) {
                                mDataThread.cancel();
                            }
                            LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent_stopThread);
                            break;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        public void cancel() {
            try {
                STATE_CONNECTED = false;
                STATE_THREAD = false;
                Thread.sleep(1000);

                if (tcpClient_socket != null) { tcpClient_socket.close(); tcpClient_socket = null; }
                if (mSocket != null) { mSocket.close(); mSocket = null; }

                input_stream_r = null;
                input_stream_w = null;
                output_stream_r = null;
                output_stream_w = null;

                Log.i("DataForwardingThread", "close() of connect socket, Thread end");
            } catch (Exception e) {
                Log.e("DataForwardingThread", "close() of connect socket failed", e);
            }
        }
    }


}
