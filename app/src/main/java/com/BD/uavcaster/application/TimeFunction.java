package com.BD.uavcaster.application;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.SystemClock;
import android.util.Log;

import androidx.core.content.ContextCompat;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class TimeFunction {
    private Context mContext;
    private SntpClient client;
    private LocationManager mLocationManager;
    private String NTP_Time;
    private String calendar_time;

    public TimeFunction(Context context) {
        mContext = context;
    }

    public boolean getNTPTime() {
        client = new SntpClient();
        // pool.ntp.org
        if (client.requestTime("pool.ntp.org", 15000)
                || client.requestTime("time.google.com", 15000)
                || client.requestTime("time.windows.com", 15000)
                || client.requestTime("cn.pool.ntp.org", 15000)) {
            long now = client.getNtpTime() + System.nanoTime() / 1000
                    - client.getNtpTimeReference();

//            SimpleDateFormat time = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
            SimpleDateFormat time = new SimpleDateFormat("yyyyMMdd");
            time.setTimeZone(TimeZone.getTimeZone("Africa/Abidjan"));
            NTP_Time = time.format(new Date(now));
            Log.d("getNTPTime", NTP_Time);

            return true;
        } else {
            Log.d("getNTPTime", " failed");
            return false;
        }
    }

    public boolean getCalendarTime() {
        final Calendar c = Calendar.getInstance();

        c.setTimeZone(TimeZone.getTimeZone("GMT+8:00"));

        String mYear = String.valueOf(c.get(Calendar.YEAR));//年

        String mMonth = String.valueOf(c.get(Calendar.MONTH) + 1);//月
        mMonth = Integer.parseInt(mMonth) < 10? "0" + mMonth : mMonth;

        String mDay = String.valueOf(c.get(Calendar.DAY_OF_MONTH));//日
        mDay = Integer.parseInt(mDay) < 10? "0" + mDay : mDay;

        String mHour = String.valueOf(c.get(Calendar.HOUR_OF_DAY));//24小时格式    HOUR(12小时格式)

        String mMinute = String.valueOf(c.get(Calendar.MINUTE));//分

        String mSecond = String.valueOf(c.get(Calendar.SECOND));//秒

        calendar_time = mYear + mMonth + mDay;

        Log.d("getCalendarTime", mYear + "-" + mMonth + "-" + mDay + "  " + mHour + ":" + mMinute + ":" + mSecond);

        if (Integer.parseInt(calendar_time) > 20210101) {
            return true;
        } else {
            return false;
        }
    }

    public boolean getLocationTime() {
        try {
            if (ContextCompat.checkSelfPermission(mContext,
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                mLocationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
                boolean gps = mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
                boolean network = mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

                if (gps || network) {
                    long UTC_time = mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER).getTime();

                    SimpleDateFormat time = new SimpleDateFormat("yyyy-MM-dd");
                    time.setTimeZone(TimeZone.getTimeZone("GMT+0"));
                    String ee = time.format(new Date(UTC_time));
                    Log.d("getLocationTime", ee);

                    return true;
                } else {
                    return false;
                }
            } else {
                return false;
            }
        } catch (Exception e) {
            return false;
        }
    }

    public long getLongNTPTime() {
        long mTime;

        if (NTP_Time != null) {
            mTime = Long.parseLong(NTP_Time);
        } else {
            mTime = 0;
        }
        return mTime;
    }

    public String getStringNTPTime() {
        String mTime;

        if (NTP_Time != null) {
            mTime = NTP_Time.substring(0, 4) + "-" + NTP_Time.substring(4, 6) + "-" + NTP_Time.substring(6, 8);
        } else {
            mTime = "Null";
        }
        return mTime;
    }

    public long getLongCalendarTime() {
        long mTime;

        if (calendar_time != null) {
            mTime = Long.parseLong(calendar_time);
        } else {
            mTime = 0;
        }
        return mTime;
    }

    public String getStringCalendarTime() {
        String mTime;

        if (calendar_time != null) {
            mTime = calendar_time.substring(0, 4) + "-" + calendar_time.substring(4, 6) + "-" + calendar_time.substring(6, 8);
        } else {
            mTime = "Null";
        }
        return mTime;
    }
}


class SntpClient {
    private static final int ORIGINATE_TIME_OFFSET = 24;
    private static final int RECEIVE_TIME_OFFSET = 32;
    private static final int TRANSMIT_TIME_OFFSET = 40;
    private static final int NTP_PACKET_SIZE = 48;

    private static final int NTP_PORT = 123;
    private static final int NTP_MODE_CLIENT = 3;
    private static final int NTP_VERSION = 3;

    // Number of seconds between Jan 1, 1900 and Jan 1, 1970
    // 70 years plus 17 leap days
    private static final long OFFSET_1900_TO_1970 = ((365L * 70L) + 17L) * 24L * 60L * 60L;

    // system time computed from NTP server response
    private long mNtpTime;

    // value of SystemClock.elapsedRealtime() corresponding to mNtpTime
    private long mNtpTimeReference;

    // round trip time in milliseconds
    private long mRoundTripTime;


    public boolean requestTime(String host, int timeout) {
        try {
            DatagramSocket socket = new DatagramSocket();
            socket.setSoTimeout(timeout);
            InetAddress address = InetAddress.getByName(host);
            byte[] buffer = new byte[NTP_PACKET_SIZE];
            DatagramPacket request = new DatagramPacket(buffer, buffer.length,
                    address, NTP_PORT);

            // set mode = 3 (client) and version = 3
            // mode is in low 3 bits of first byte
            // version is in bits 3-5 of first byte
            buffer[0] = NTP_MODE_CLIENT | (NTP_VERSION << 3);

            // get current time and write it to the request packet
            long requestTime = System.currentTimeMillis();
            long requestTicks = System.nanoTime() / 1000;
            writeTimeStamp(buffer, TRANSMIT_TIME_OFFSET, requestTime);

            socket.send(request);

            // read the response
            DatagramPacket response = new DatagramPacket(buffer, buffer.length);
            socket.receive(response);
            long responseTicks = System.nanoTime() / 1000;
            long responseTime = requestTime + (responseTicks - requestTicks);
            socket.close();

            // extract the results
            long originateTime = readTimeStamp(buffer, ORIGINATE_TIME_OFFSET);
            long receiveTime = readTimeStamp(buffer, RECEIVE_TIME_OFFSET);
            long transmitTime = readTimeStamp(buffer, TRANSMIT_TIME_OFFSET);
            long roundTripTime = responseTicks - requestTicks
                    - (transmitTime - receiveTime);
            // receiveTime = originateTime + transit + skew
            // responseTime = transmitTime + transit - skew
            // clockOffset = ((receiveTime - originateTime) + (transmitTime -
            // responseTime))/2
            // = ((originateTime + transit + skew - originateTime) +
            // (transmitTime - (transmitTime + transit - skew)))/2
            // = ((transit + skew) + (transmitTime - transmitTime - transit +
            // skew))/2
            // = (transit + skew - transit + skew)/2
            // = (2 * skew)/2 = skew
            long clockOffset = ((receiveTime - originateTime) + (transmitTime - responseTime)) / 2;
            // if (Config.LOGD) Log.d(TAG, "round trip: " + roundTripTime +
            // " ms");
            // if (Config.LOGD) Log.d(TAG, "clock offset: " + clockOffset +
            // " ms");

            // save our results - use the times on this side of the network
            // latency
            // (response rather than request time)
            mNtpTime = responseTime + clockOffset;
            mNtpTimeReference = responseTicks;
            mRoundTripTime = roundTripTime;
        } catch (Exception e) {

            return false;
        }

        return true;
    }


    public long getNtpTime() {
        return mNtpTime;
    }


    public long getNtpTimeReference() {
        return mNtpTimeReference;
    }


    public long getRoundTripTime() {
        return mRoundTripTime;
    }


    private long read32(byte[] buffer, int offset) {
        byte b0 = buffer[offset];
        byte b1 = buffer[offset + 1];
        byte b2 = buffer[offset + 2];
        byte b3 = buffer[offset + 3];

        // convert signed bytes to unsigned values
        int i0 = ((b0 & 0x80) == 0x80 ? (b0 & 0x7F) + 0x80 : b0);
        int i1 = ((b1 & 0x80) == 0x80 ? (b1 & 0x7F) + 0x80 : b1);
        int i2 = ((b2 & 0x80) == 0x80 ? (b2 & 0x7F) + 0x80 : b2);
        int i3 = ((b3 & 0x80) == 0x80 ? (b3 & 0x7F) + 0x80 : b3);

        return ((long) i0 << 24) + ((long) i1 << 16) + ((long) i2 << 8)
                + (long) i3;
    }


    private long readTimeStamp(byte[] buffer, int offset) {
        long seconds = read32(buffer, offset);
        long fraction = read32(buffer, offset + 4);
        return ((seconds - OFFSET_1900_TO_1970) * 1000)
                + ((fraction * 1000L) / 0x100000000L);
    }


    private void writeTimeStamp(byte[] buffer, int offset, long time) {
        long seconds = time / 1000L;
        long milliseconds = time - seconds * 1000L;
        seconds += OFFSET_1900_TO_1970;

        // write seconds in big endian format
        buffer[offset++] = (byte) (seconds >> 24);
        buffer[offset++] = (byte) (seconds >> 16);
        buffer[offset++] = (byte) (seconds >> 8);
        buffer[offset++] = (byte) (seconds >> 0);

        long fraction = milliseconds * 0x100000000L / 1000L;
        // write fraction in big endian format
        buffer[offset++] = (byte) (fraction >> 24);
        buffer[offset++] = (byte) (fraction >> 16);
        buffer[offset++] = (byte) (fraction >> 8);
        // low order bits should be random data
        buffer[offset++] = (byte) (Math.random() * 255.0);
    }
}
//class SntpClient {
//    private static final String TAG = "SntpClient";
//
//    private static final int REFERENCE_TIME_OFFSET = 16;
//    private static final int ORIGINATE_TIME_OFFSET = 24;
//    private static final int RECEIVE_TIME_OFFSET = 32;
//    private static final int TRANSMIT_TIME_OFFSET = 40;
//    private static final int NTP_PACKET_SIZE = 48;
//
//    private static final int NTP_PORT = 123;
//    private static final int NTP_MODE_CLIENT = 3;
//    private static final int NTP_VERSION = 3;
//
//    // Number of seconds between Jan 1, 1900 and Jan 1, 1970
//    // 70 years plus 17 leap days
//    private static final long OFFSET_1900_TO_1970 = ((365L * 70L) + 17L) * 24L * 60L * 60L;
//
//    // system time computed from NTP server response
//    private long mNtpTime;
//
//    // value of SystemClock.elapsedRealtime() corresponding to mNtpTime
//    private long mNtpTimeReference;
//
//    // round trip time in milliseconds
//    private long mRoundTripTime;
//
//    /**
//     * Sends an SNTP request to the given host and processes the response.
//     *
//     * @param host
//     *            host name of the server.
//     * @param timeout
//     *            network timeout in milliseconds.
//     * @return true if the transaction was successful.
//     */
//    public boolean requestTime(String host, int timeout) {
//        DatagramSocket socket = null;
//        try {
//            socket = new DatagramSocket();
//            socket.setSoTimeout(timeout);
//            InetAddress address = InetAddress.getByName(host);
//            byte[] buffer = new byte[NTP_PACKET_SIZE];
//            DatagramPacket request = new DatagramPacket(buffer,
//                    buffer.length, address, NTP_PORT);
//
//            // set mode = 3 (client) and version = 3
//            // mode is in low 3 bits of first byte
//            // version is in bits 3-5 of first byte
//            buffer[0] = NTP_MODE_CLIENT | (NTP_VERSION << 3);
//
//            // get current time and write it to the request packet
//            long requestTime = System.currentTimeMillis();
//            Log.d(TAG, "RequestTime:"+new Date(requestTime));
//            long requestTicks = SystemClock.elapsedRealtime();
//            writeTimeStamp(buffer, TRANSMIT_TIME_OFFSET, requestTime);
//
//            socket.send(request);
//
//            // read the response
//            DatagramPacket response = new DatagramPacket(buffer,
//                    buffer.length);
//            socket.receive(response);
//            long responseTicks = SystemClock.elapsedRealtime();
//            long responseTime = requestTime
//                    + (responseTicks - requestTicks);
//
//            // extract the results
//            long originateTime = readTimeStamp(buffer,
//                    ORIGINATE_TIME_OFFSET);
//            long receiveTime = readTimeStamp(buffer, RECEIVE_TIME_OFFSET);
//            long transmitTime = readTimeStamp(buffer, TRANSMIT_TIME_OFFSET);
//            long roundTripTime = responseTicks - requestTicks
//                    - (transmitTime - receiveTime);
//            // receiveTime = originateTime + transit + skew
//            // responseTime = transmitTime + transit - skew
//            // clockOffset = ((receiveTime - originateTime) + (transmitTime
//            // - responseTime))/2
//            // = ((originateTime + transit + skew - originateTime) +
//            // (transmitTime - (transmitTime + transit - skew)))/2
//            // = ((transit + skew) + (transmitTime - transmitTime - transit
//            // + skew))/2
//            // = (transit + skew - transit + skew)/2
//            // = (2 * skew)/2 = skew
//            long clockOffset = ((receiveTime -  requestTime) + (transmitTime - System.currentTimeMillis())) / 2;
//            // if (false) Log.d(TAG, "round trip: " + roundTripTime +
//            // " ms");
//            // if (false) Log.d(TAG, "clock offset: " + clockOffset +
//            // " ms");
//
//            // save our results - use the times on this side of the network
//            // latency
//            // (response rather than request time)
//            mNtpTime = System.currentTimeMillis() + clockOffset;
//
////              mNtpTime = transmitTime;
//            //mNtpTime = responseTime + clockOffset;
//            mNtpTimeReference = responseTicks;
//            mRoundTripTime = roundTripTime;
//        } catch (Exception e) {
//            if (false)
//                Log.d(TAG, "request time failed:" + e);
//            e.printStackTrace();
//            return false;
//        } finally {
//            if (socket != null) {
//                socket.close();
//            }
//        }
//
//        return true;
//    }
//
//    /**
//     * Returns the time computed from the NTP transaction.
//     *
//     * @return time value computed from NTP server response.
//     */
//    public long getNtpTime() {
//        return mNtpTime;
//    }
//
//    /**
//     * Returns the reference clock value (value of
//     * SystemClock.elapsedRealtime()) corresponding to the NTP time.
//     *
//     * @return reference clock corresponding to the NTP time.
//     */
//    public long getNtpTimeReference() {
//        return mNtpTimeReference;
//    }
//
//    /**
//     * Returns the round trip time of the NTP transaction
//     *
//     * @return round trip time in milliseconds.
//     */
//    public long getRoundTripTime() {
//        return mRoundTripTime;
//    }
//
//    /**
//     * Reads an unsigned 32 bit big endian number from the given offset in
//     * the buffer.
//     */
//    private long read32(byte[] buffer, int offset) {
//        byte b0 = buffer[offset];
//        byte b1 = buffer[offset + 1];
//        byte b2 = buffer[offset + 2];
//        byte b3 = buffer[offset + 3];
//
//        // convert signed bytes to unsigned values
//        int i0 = ((b0 & 0x80) == 0x80 ? (b0 & 0x7F) + 0x80 : b0);
//        int i1 = ((b1 & 0x80) == 0x80 ? (b1 & 0x7F) + 0x80 : b1);
//        int i2 = ((b2 & 0x80) == 0x80 ? (b2 & 0x7F) + 0x80 : b2);
//        int i3 = ((b3 & 0x80) == 0x80 ? (b3 & 0x7F) + 0x80 : b3);
//
//        return ((long) i0 << 24) + ((long) i1 << 16) + ((long) i2 << 8)
//                + (long) i3;
//    }
//
//    /**
//     * Reads the NTP time stamp at the given offset in the buffer and
//     * returns it as a system time (milliseconds since January 1, 1970).
//     */
//    private long readTimeStamp(byte[] buffer, int offset) {
//        long seconds = read32(buffer, offset);
//        long fraction = read32(buffer, offset + 4);
//        return ((seconds - OFFSET_1900_TO_1970) * 1000)
//                + ((fraction * 1000L) / 0x100000000L);
//    }
//
//    /**
//     * Writes system time (milliseconds since January 1, 1970) as an NTP
//     * time stamp at the given offset in the buffer.
//     */
//    private void writeTimeStamp(byte[] buffer, int offset, long time) {
//        long seconds = time / 1000L;
//        long milliseconds = time - seconds * 1000L;
//        seconds += OFFSET_1900_TO_1970;
//
//        // write seconds in big endian format
//        buffer[offset++] = (byte) (seconds >> 24);
//        buffer[offset++] = (byte) (seconds >> 16);
//        buffer[offset++] = (byte) (seconds >> 8);
//        buffer[offset++] = (byte) (seconds >> 0);
//
//        long fraction = milliseconds * 0x100000000L / 1000L;
//        // write fraction in big endian format
//        buffer[offset++] = (byte) (fraction >> 24);
//        buffer[offset++] = (byte) (fraction >> 16);
//        buffer[offset++] = (byte) (fraction >> 8);
//        // low order bits should be random data
//        buffer[offset++] = (byte) (Math.random() * 255.0);
//    }
//}

