package com.BD.uavcaster.activity;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.util.JsonWriter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TableRow;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.BD.uavcaster.R;
import com.BD.uavcaster.application.DataForwarding;
import com.BD.uavcaster.application.FileOperation;

import org.json.JSONObject;

import java.io.File;
import java.io.RandomAccessFile;
import java.security.PrivilegedAction;
import java.util.Map;

public class FragmentSetting extends Fragment {
    //file storage path
    private static final String FILEPATH = "/sdcard/UAVCaster/";
    private static final String FILENAME = "configuration.bin";

    private Spinner spinner_input_mode, spinner_input_encryption, spinner_output_mode, spinner_output_protocol;
    private Spinner spinner_input_model, spinner_output_model;
    private Button btn_connect;
    private TableRow tr_outputMode, tr_outputIP, tr_outputPort, tr_outputProtocol;
    private TableRow tr_inputMode, tr_inputEncryption;
    private EditText edit_ip, edit_port;
    private String name;
    private RandomAccessFile mFile;
    private int spinner_inputMode_item = 0, spinner_outputMode_item = 0, spinner_outputProtocol_item = 0;
    private int spinner_outputModel_item = 0, spinner_inputModel_item = 0;
    private int output_protocol_item, output_mode_item;
    private BluetoothAdapter mBluetooth;

    private DataForwarding mDataForwarding;
    private FileOperation mFileOperation;

    public FragmentSetting(String fName, DataForwarding dataForwarding){
        this.name = fName;
        mDataForwarding = dataForwarding;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = LayoutInflater.from(getActivity()).inflate(R.layout.framement_setting,container,false);

        spinner_input_model = (Spinner) view.findViewById(R.id.spinner_input_model);
        spinner_input_mode = (Spinner) view.findViewById(R.id.spinner_input);
        spinner_input_encryption = (Spinner) view.findViewById(R.id.spinner_encryption);
        spinner_output_mode = (Spinner) view.findViewById(R.id.spinner_output);
        spinner_output_protocol = (Spinner) view.findViewById(R.id.spinner_protocol);
        spinner_output_model = (Spinner) view.findViewById(R.id.spinner_output_model);

        edit_ip = (EditText) view.findViewById(R.id.edit_ip);
        edit_port = (EditText) view.findViewById(R.id.edit_port);
        btn_connect = (Button) view.findViewById(R.id.fragment_connect);
        tr_inputMode = (TableRow) view.findViewById(R.id.tableRow_input_mode);
        tr_inputEncryption = (TableRow) view.findViewById(R.id.tableRow_input_encryption);
        tr_outputMode = (TableRow) view.findViewById(R.id.tableRow_output_mode);
        tr_outputIP = (TableRow) view.findViewById(R.id.tableRow_outputIP);
        tr_outputPort = (TableRow) view.findViewById(R.id.tableRow_outputPort);
        tr_outputProtocol = (TableRow) view.findViewById(R.id.tableRow_outputProtocol);

        //read the default configure
        getSettingConfigure();

        spinner_input_model.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                spinner_inputModel_item = position;

                if (spinner_inputModel_item == 2) {
                    tr_inputMode.setVisibility(View.VISIBLE);
                    tr_inputEncryption.setVisibility(View.VISIBLE);
                } else {
                    tr_inputMode.setVisibility(View.GONE);
                    tr_inputEncryption.setVisibility(View.GONE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        spinner_output_model.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                spinner_outputModel_item = position;

                if (spinner_outputModel_item == 2) {
                    spinner_output_mode.setSelection(output_mode_item);

                    tr_outputMode.setVisibility(View.VISIBLE);
                    if(output_mode_item == 0) {
                        //display the IP, port, protocol options
                        tr_outputIP.setVisibility(View.VISIBLE);
                        tr_outputPort.setVisibility(View.VISIBLE);
                        tr_outputProtocol.setVisibility(View.VISIBLE);
                    } else if (output_mode_item == 1) {
                        //display the IP, port, protocol options
                        tr_outputIP.setVisibility(View.GONE);
                        tr_outputPort.setVisibility(View.GONE);
                        tr_outputProtocol.setVisibility(View.VISIBLE);
                    } else if (output_mode_item == 2) {
                        //Hide the IP, port, protocol options
                        tr_outputIP.setVisibility(View.GONE);
                        tr_outputPort.setVisibility(View.GONE);
                        tr_outputProtocol.setVisibility(View.GONE);
                    } else if (output_mode_item == 3) {
                        //Hide the IP, port, protocol options
                        tr_outputIP.setVisibility(View.GONE);
                        tr_outputPort.setVisibility(View.GONE);
                        tr_outputProtocol.setVisibility(View.GONE);
                    }
                } else {
                    tr_outputMode.setVisibility(View.GONE);
                    tr_outputIP.setVisibility(View.GONE);
                    tr_outputPort.setVisibility(View.GONE);
                    tr_outputProtocol.setVisibility(View.GONE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        spinner_input_mode.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                spinner_inputMode_item = position;

                if (spinner_inputMode_item == 1) {
                    String notice = " kindly connect to the bluetooth device ";
                    Toast.makeText(getContext(), notice, Toast.LENGTH_LONG).show();

                    //enter system setting page
                    Intent intent = new Intent(Settings.ACTION_BLUETOOTH_SETTINGS);
                    startActivity(intent);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        spinner_output_mode.setSelection(0, true);
        spinner_output_mode.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                spinner_outputMode_item = position;

                if(spinner_outputMode_item == 0) {
                    //display the IP, port, protocol options
                    tr_outputIP.setVisibility(View.VISIBLE);
                    tr_outputPort.setVisibility(View.VISIBLE);
                    tr_outputProtocol.setVisibility(View.VISIBLE);

                    SpinnerAdapter adapter = null;
                    adapter = ArrayAdapter.createFromResource(getContext(), R.array.output_protocol_list_client, android.R.layout.simple_spinner_dropdown_item);
                    spinner_output_protocol.setAdapter(adapter);
                    spinner_output_protocol.setSelection(output_protocol_item);

                } else if (spinner_outputMode_item == 1) {
                    //display the IP, port, protocol options
                    tr_outputIP.setVisibility(View.VISIBLE);
                    tr_outputPort.setVisibility(View.VISIBLE);
                    tr_outputProtocol.setVisibility(View.VISIBLE);

                    SpinnerAdapter adapter = null;
                    adapter = ArrayAdapter.createFromResource(getContext(), R.array.output_protocol_list_server, android.R.layout.simple_spinner_dropdown_item);
                    spinner_output_protocol.setAdapter(adapter);
                    spinner_output_protocol.setSelection(output_protocol_item);

                    String notice = " Build the Wifi hotspot connection ";
                    Toast.makeText(getContext(), notice, Toast.LENGTH_LONG).show();

                    //enter system setting page
                    Intent intent = new Intent();
                    intent.addCategory(Intent.CATEGORY_DEFAULT);
                    intent.setAction("android.intent.action.MAIN");
                    ComponentName cn = new ComponentName("com.android.settings", "com.android.settings.TetherSettings");
                    intent.setComponent(cn);
                    startActivity(intent);

                } else if (spinner_outputMode_item == 2) {
                    //Hide the IP, port, protocol options
                    tr_outputIP.setVisibility(View.GONE);
                    tr_outputPort.setVisibility(View.GONE);
                    tr_outputProtocol.setVisibility(View.GONE);
                } else if (spinner_outputMode_item == 3) {
                    //Hide the IP, port, protocol options
                    tr_outputIP.setVisibility(View.GONE);
                    tr_outputPort.setVisibility(View.GONE);
                    tr_outputProtocol.setVisibility(View.GONE);

                    String notice = " kindly connect to the bluetooth device ";
                    Toast.makeText(getContext(), notice, Toast.LENGTH_LONG).show();

                    //enter system setting page
                    Intent intent = new Intent(Settings.ACTION_BLUETOOTH_SETTINGS);
                    startActivity(intent);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        spinner_output_protocol.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                spinner_outputProtocol_item = position;

                if (spinner_outputMode_item == 1) {
                    if (spinner_outputProtocol_item == 0 || spinner_outputProtocol_item == 1) {
                        tr_outputIP.setVisibility(View.GONE);
                        tr_outputPort.setVisibility(View.GONE);
                    } else {
                        tr_outputIP.setVisibility(View.VISIBLE);
                        tr_outputPort.setVisibility(View.VISIBLE);
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        //the connect button onclick function
        btn_connect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean status;

                //check the parameter validity
//                if (isParameterValidity() == false) {
//                    String notice = " Incomplete Data, please input the right IP:Port";
//                    Toast.makeText(getContext(), notice,Toast.LENGTH_LONG).show();
//                    return;
//                }

                int input_model = spinner_input_model.getSelectedItemPosition();
                int input_mode = spinner_input_mode.getSelectedItemPosition();
                int input_encryption = spinner_input_encryption.getSelectedItemPosition();
                int output_model = spinner_output_model.getSelectedItemPosition();
                int output_mode = spinner_output_mode.getSelectedItemPosition();
                int output_protocol = spinner_output_protocol.getSelectedItemPosition();
                String output_IP = edit_ip.getText().toString();
                String output_port = edit_port.getText().toString();

                try {
                    JSONObject object = new JSONObject();
                    object.put("InputModel", input_model);
                    object.put("InputMode", input_mode);
                    object.put("InputEncryption", input_encryption);
                    object.put("OutputModel", output_model);
                    object.put("OutputMode", output_mode);
                    object.put("OutputIP", output_IP);
                    object.put("OutputPort", output_port);
                    object.put("OutputProtocol", output_protocol);

                    mFileOperation = new FileOperation(getActivity(), FILEPATH, FILENAME);
                    mFile = mFileOperation.openNewFile();
                    mFileOperation.writeToFileOverwrite(mFile, object.toString().getBytes(), object.toString().length());
                    mFile.close();

                    String notice = " Saved ";
                    Toast.makeText(getContext(), notice, Toast.LENGTH_SHORT).show();

                } catch (Exception e) {
                    Log.e("Setting","error " + e);
                }

            }
        });

        return view;
    }

    private boolean isParameterValidity() {
        if (spinner_outputMode_item == 2 || spinner_inputMode_item == 3) {
            return true;
        }

        if (spinner_outputProtocol_item == 0 || spinner_outputProtocol_item == 1 ||
                spinner_outputProtocol_item == 3 || spinner_outputProtocol_item == 4) {
            if (edit_ip.length() < 7 || edit_port.length() <= 0) {
                return false;
            }
        }

        return true;
    }

    private boolean getSettingConfigure() {
        FileOperation mFileOperation;
        RandomAccessFile mFile;
        Map<String, String> map;
        int input_model, input_mode, output_model, output_mode, input_encryption, output_protocol;
        String output_IP, output_port;

        try {
            mFileOperation = new FileOperation(getActivity(), FILEPATH, FILENAME);
            mFile = mFileOperation.openFile();
            map = mFileOperation.readAllFromFile(mFile);

            JSONObject setting_json = new JSONObject(map.get("content"));
            input_model = setting_json.getInt("InputModel");
            input_mode = setting_json.getInt("InputMode");
            input_encryption = setting_json.getInt("InputEncryption");
            output_model = setting_json.getInt("OutputModel");
            output_mode = setting_json.getInt("OutputMode");
            output_protocol = setting_json.getInt("OutputProtocol");
            output_IP = setting_json.getString("OutputIP");
            output_port = setting_json.getString("OutputPort");

            spinner_input_mode.setSelection(input_mode);
            spinner_input_encryption.setSelection(input_encryption);
            spinner_input_model.setSelection(input_model);
            spinner_output_model.setSelection(output_model);
            spinner_output_mode.setSelection(output_mode);
            spinner_output_protocol.setSelection(output_protocol);
            output_mode_item = output_mode;
            output_protocol_item = output_protocol;
            edit_ip.setText(output_IP);
            edit_port.setText(output_port);

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}







