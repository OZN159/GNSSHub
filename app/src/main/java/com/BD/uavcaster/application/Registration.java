package com.BD.uavcaster.application;

import android.content.Context;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.io.RandomAccessFile;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;

public class Registration {
    //file storage path
    private static final String FILEPATH = "/sdcard/UAVCaster/";
    private static final String FILENAME_REGISTRATION = "registration.bin";

    private Context mContext;
    private TimeFunction mTime;
    private String IMEI;
    private String PsuedoID;
    private String registration_code_file;
    private int[] expire_time = new int[3];
    private int checkupCode;

    private String decode_registration, decode_expireTime;

    public Registration(Context context, TimeFunction time) {
        mContext = context;
        mTime = time;
    }

    //verify the registration code if correct
    public int isRegistration() {
        getUniquePsuedoID();
        String encrypted_code = encodeRegistrationCode(PsuedoID);
        String code = getRegistrationCodeFromFile();
//        String registration_code = code.substring(0,5) + code.substring(8,13) + code.substring(16,22);
//        String expire_code = code.substring(5,8) + code.substring(13,16) + code.substring(22,24);

        if (code == null) {
            return  -1;
        }

        spiltRegistrationCode(code);

        int checkup = 0;
        for (int i = 0; i < decode_expireTime.length(); i++) {
            checkup += Integer.parseInt(decode_expireTime.substring(i,i+1));
        }

        //check the NTP time if valid
//        if (mTime.getLongCalendarTime() < 20000101) {
//            return -1;
//        }

        if (decode_registration.equals(encrypted_code)
                && (Long.parseLong(decode_expireTime) > mTime.getLongCalendarTime())
                && (checkupCode == checkup)) {
            return 1;
        } else {
            return -2;
        }
    }

    //registration function
    public boolean setRegistration(String code) {
        getUniquePsuedoID();
        String encrypted_code = encodeRegistrationCode(PsuedoID);

        spiltRegistrationCode(code);

        int checkup = 0;
        for (int i = 0; i < decode_expireTime.length(); i++) {
            checkup += Integer.parseInt(decode_expireTime.substring(i,i+1));
        }

        //compare the current date and expire date.
        if ((Long.parseLong(decode_expireTime) <= mTime.getLongCalendarTime()) || (checkupCode != checkup)) {
            String notice = " Wrong registration time ";
            Toast.makeText(mContext, notice, Toast.LENGTH_LONG).show();

            return false;
        }

        //check if the registration code is correct
        if (encrypted_code.equals(decode_registration)) {
            writeCodeToFile(code);

            String notice = " Registration success ";
            Toast.makeText(mContext, notice, Toast.LENGTH_LONG).show();

            return true;
        } else {
            String notice = " Registration failed ";
            Toast.makeText(mContext, notice, Toast.LENGTH_LONG).show();

            return false;
        }
    }

    //return the IMEI code
    public String getIMEICode() {
        return IMEI;
    }

    //return the Psuedo code
    public String getPsuedoCode() {
        return PsuedoID;
    }

    //return the expire time
    public String getExpireTime() {
        if(decode_expireTime != null) {
            return decode_expireTime.substring(0,4) + "-" + decode_expireTime.substring(4,6) + "-" + decode_expireTime.substring(6,8);
        } else {
            return null;
        }
    }

    //return the expire time(Long)
    public Long getLongExpireTime() {
        if(decode_expireTime != null) {
            return Long.parseLong(decode_expireTime);
        } else {
            return Long.parseLong("20000101");
        }
    }

    //open the registration file and get the code
    private String getRegistrationCodeFromFile() {
        FileOperation mFileOperation;
        RandomAccessFile mFile;
        Map<String, String> map;

        mFileOperation = new FileOperation(mContext, FILEPATH, FILENAME_REGISTRATION);
        mFile = mFileOperation.openFile();
        if (mFile != null) {
            map = mFileOperation.readOneLineFromFile(mFile);
            return  map.get("content");
        }

        return null;
    }

    private boolean writeCodeToFile(String code) {
        FileOperation mFileOperation;
        RandomAccessFile mFile;
        Map<String, String> map;

        mFileOperation = new FileOperation(mContext, FILEPATH, FILENAME_REGISTRATION);
        mFile = mFileOperation.openNewFile();
        if (mFile != null) {
            mFileOperation.writeToFileOverwrite(mFile, code.getBytes(), code.length());
        }

        return true;
    }

    //encode
    private String encodeUUID(String code) {
        String encrypted_code = "";

        byte[] code_byte = PsuedoID.replace("-", "").getBytes();
        for (int i =0; i < (code_byte.length/2); i++) {
            //add 1 from the raw code
            code_byte[i] = (byte)((code_byte[i] + code_byte[code_byte.length-i-1]) / 2);
        }
        encrypted_code = new String(code_byte,0, (code_byte.length/2));

        return encrypted_code;
    }

    private String encodeRegistrationCode(String code) {
        final int secret_argument = 6;

        String encrypted_code = "";
        int buf;

        byte[] code_byte = code.getBytes();
        for (int i =0; i < (code_byte.length/2); i++) {
            //The first 4 digits, add the first digits and the last digits.
            buf = (code_byte[i] + code_byte[code_byte.length-i-1]) / 2;
            //add the XOR operation on the latest code with the secret_code
            code_byte[i] = (byte) (buf^secret_argument);

            //The last 4 digits, add the first digits and the second digits.
            buf = (code_byte[i] + code_byte[i+1]) / 2;
            //add the XOR operation on the latest code with the secret_code
            code_byte[(code_byte.length / 2) + i] = (byte) (buf^secret_argument);
        }

        //convert all the characters to "a-z" or "A-Z"
        for (int i =0; i < code_byte.length; i++) {
            if (code_byte[i] < 65) {
                while(code_byte[i] < 65) { code_byte[i] += 25; }
            } else if (code_byte[i] > 90 && code_byte[i] < 97) {
               code_byte[i] += 10;
            } else if (code_byte[i] > 122) {
                while(code_byte[i] > 122) { code_byte[i] -= 25; }
            }
        }

        encrypted_code = new String(code_byte);

        return encrypted_code;
    }

    private String decodeRegistrationCode(String code) {
        final int secret_argument = 6;
        final int secret_argument_time = 26;

        checkupCode = Integer.parseInt(code.substring(0, 3), 16) ^ secret_argument_time;
        expire_time[0] = Integer.parseInt(code.substring(3, 6), 16) ^ secret_argument;
        expire_time[1] = Integer.parseInt(code.substring(6, 7), 16) ^ secret_argument;
        expire_time[2] = Integer.parseInt(code.substring(7, 9), 16) ^ secret_argument;

        String time = "" + expire_time[0] + String.format("%02d", expire_time[1]) + String.format("%02d", expire_time[2]);

        Log.d("decodeRegistrationCode",  checkupCode + "-" + time);

        return time;
    }

    //get the IMEI code, Mac code, phone model......
//    private boolean getMachineCode() {
//        try {
//            TelephonyManager telephonyManager = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
//            IMEI = telephonyManager.getDeviceId();
//
//            if (IMEI == null) {
//                IMEI = "";
//            }
//            return true;
//        } catch (Exception e) {
//            e.printStackTrace();
//            return false;
//        }
//    }

    //get the Psuedo ID
    private boolean getUniquePsuedoID() {
        String serial = null;

        Log.d("zz", "Build.BOARD: "+Build.BOARD);
        Log.d("zz", "Build.BOOTLOADER: "+Build.BOOTLOADER);
        Log.d("zz", "Build.BRAND: "+Build.BRAND);//设备牌子
        Log.d("zz", "Build.DEVICE: "+Build.DEVICE);//设备名
        Log.d("zz", "Build.DISPLAY: "+Build.DISPLAY);//显示设备号
        Log.d("zz", "Build.FINGERPRINT: "+Build.FINGERPRINT);//设备指纹
        Log.d("zz", "Build.HARDWARE: "+Build.HARDWARE);
        Log.d("zz", "Build.HOST: "+Build.HOST);
        Log.d("zz", "Build.ID: "+Build.ID);//设备硬件id
        Log.d("zz", "Build.MANUFACTURER: "+Build.MANUFACTURER);//厂商
        Log.d("zz", "Build.MODEL: "+Build.MODEL);//设备型号
        Log.d("zz", "Build.PRODUCT: "+Build.PRODUCT);//产品名，和DEVICE一样
        Log.d("zz", "Build.SERIAL: "+Build.SERIAL);//设备序列号
        Log.d("zz", "Build.TAGS: "+Build.TAGS);
        Log.d("zz", "Build.TYPE: "+Build.TYPE);
        Log.d("zz", "Build.UNKNOWN: "+Build.UNKNOWN);
        Log.d("zz", "Build.USER: "+Build.USER);
        Log.d("zz", "Build.CPU_ABI: "+Build.CPU_ABI);
        Log.d("zz", "Build.CPU_ABI2: "+Build.CPU_ABI2);
        Log.d("zz", "Build.RADIO: "+Build.RADIO);
        Log.d("zz", "Build.TIME: "+Build.TIME);//出厂时间
        Log.d("zz", "Build.VERSION.CODENAME: "+Build.VERSION.CODENAME);
        Log.d("zz", "Build.VERSION.INCREMENTAL: "+Build.VERSION.INCREMENTAL);//不详，重要
        Log.d("zz", "Build.VERSION.RELEASE: "+Build.VERSION.RELEASE);//系统版本号
        Log.d("zz", "Build.VERSION.SDK: "+Build.VERSION.SDK);//api级数
        Log.d("zz", "Build.VERSION.SDK_INT: "+Build.VERSION.SDK_INT);//api级数，int型返回

        String m_szDevIDShort = + Build.TIME + Build.ID + Build.CPU_ABI + Build.DEVICE + Build.HOST; //13 位

        try {
            serial = android.os.Build.class.getField("SERIAL").get(null).toString();

            PsuedoID = new UUID(m_szDevIDShort.hashCode(), serial.hashCode()).toString();
            Log.d("UUID", PsuedoID);
            PsuedoID = encodeUUID(PsuedoID);

            return true;
        } catch (Exception exception) {
            //serial需要一个初始化
            serial = "serial";
            PsuedoID = new UUID(m_szDevIDShort.hashCode(), serial.hashCode()).toString();
            PsuedoID = encodeUUID(PsuedoID);
            return false;
        }
    }

    private boolean spiltRegistrationCode(String code) {
        if (code.length() < 24) {
            Log.d("spiltRegistrationCode", "code is too short - " + code.length());
            return false;
        }

        decode_registration = code.substring(0,5) + code.substring(8,13) + code.substring(16,22);
        String expireTime = code.substring(5,8) + code.substring(13,16) + code.substring(22,25);

        decode_expireTime = decodeRegistrationCode(expireTime);

        Log.d("spiltRegistrationCode", decode_registration + "+" + expireTime);
        return true;
    }
}
