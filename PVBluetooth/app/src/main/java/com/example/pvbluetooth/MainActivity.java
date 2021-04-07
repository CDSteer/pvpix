package com.example.pvbluetooth;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
    private String USERNAME = "Beth";
    private String BASE_URL_GET = "https://pvpix.herokuapp.com/";
    private String BASE_URL_POST = "https://pvpix.herokuapp.com/post";

    BluetoothAdapter bluetoothAdapter;

    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;
    private static final int PERMISSION_REQUEST_FINE_LOCATION = 1;

    private Context context = this;

    BluetoothGattCharacteristic m_characteristicTX;
    BluetoothConect mService;


    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //add button listener
        this.registerButtons();
        findViewById(R.id.send).setOnClickListener(sendClickListener);


        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (bluetoothAdapter == null) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        } else {
//            bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
        }

        // Make sure we have access coarse location enabled, if not, prompt the user to enable it
        if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("This app needs location access");
            builder.setMessage("Please grant location access so this app can detect peripherals.");
            builder.setPositiveButton(android.R.string.ok, null);
            builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
                }
            });
            builder.show();
        }

        // Make sure we have access coarse location enabled, if not, prompt the user to enable it
        if (this.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("This app needs location access");
            builder.setMessage("Please grant location access so this app can detect peripherals.");
            builder.setPositiveButton(android.R.string.ok, null);
            builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_FINE_LOCATION);
                }
            });
            builder.show();
        }



        Intent bleService = new Intent(this, BluetoothConect.class);
        startService(bleService);
        bindService(bleService, connection, Context.BIND_AUTO_CREATE);

        LocalBroadcastManager.getInstance(this).registerReceiver(
                mStatusReceiver, new IntentFilter("BLEConnection"));

        LocalBroadcastManager.getInstance(this).registerReceiver(
                mDataReceiver, new IntentFilter("BLENewData"));



    }



    private BroadcastReceiver mStatusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Get extra data included in the Intent
            String message = intent.getStringExtra("Status");
            TextView t= findViewById(R.id.configText);
            t.setText(message);
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
            checkForPVMessages();
        }
    };

    private BroadcastReceiver mDataReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Get extra data included in the Intent
            String message = intent.getStringExtra("TXData");
            String[] pvPixConfig = splitString(message);
            if (pvPixConfig.length == 2) {
                setPvPixConfig(pvPixConfig);
            }

        }
    };

    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            BluetoothConect.LocalBinder binder = (BluetoothConect.LocalBinder) service;
            mService = binder.getService();
//            mBound = true;
        }
        @Override
        public void onServiceDisconnected(ComponentName arg0) {
//            mBound = false;
        }
    };

    public void onDestroy() {
        super.onDestroy();
        unbindService(connection);
    }

    private void setPvPixConfig(String[] pvPixConfig) {
        switch (Integer.parseInt(pvPixConfig[0])){
            case 0: setButtonState(findViewById(R.id.one), pvPixConfig[1]);
                break;
            case 1: setButtonState(findViewById(R.id.two), pvPixConfig[1]);
                break;
            case 2: setButtonState(findViewById(R.id.three), pvPixConfig[1]);
                break;
            case 3: setButtonState(findViewById(R.id.four), pvPixConfig[1]);
                break;
            default:
                break;
        }
        try {
            postPVMessage();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    //Function for writing to Arduino
    private boolean writeVal(String text){
        if(mService.m_characteristicTX != null) {
            mService.writeVal(text);
        } else {
            String[] titles = splitString(text);
            for (int i=0; i<titles.length-1; i++) {
                switch (i) {
                    case 0: setButtonState(findViewById(R.id.one),titles[i]);
                        break;
                    case 1: setButtonState(findViewById(R.id.two),titles[i]);
                        break;
                    case 2: setButtonState(findViewById(R.id.three),titles[i]);
                        break;
                    case 3: setButtonState(findViewById(R.id.four), titles[i]);
                        break;
                    default:
                        Log.e("error", "no matching button");
                }
            }
            return false;
        }
        return true;
    }

    private void setButtonState(Button btn, String value){
        if (btn != null) {
            if (("0").equals(value)) {
                btn.setBackgroundColor(Color.parseColor("#4c4c4c"));
                btn.setTag(0);
            } else {
                btn.setBackgroundColor(Color.parseColor("#ed126d"));
                btn.setTag(1);
            }
        }
    }

////////////////////////////////////////////////////////////

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
//            scanLeDevice();
            return true;
        }
        if (id == R.id.write_value) {
            this.writeVal("1:1:1:1:;");
        }
        return super.onOptionsItemSelected(item);
    }


    public void registerButtons(){
        register(R.id.one);
        register(R.id.three);
        register(R.id.two);
        register(R.id.four);
    }

    private void register(int buttonResourceId){
        findViewById(buttonResourceId).setOnClickListener(buttonClickListener);
        findViewById(buttonResourceId).setBackgroundColor(Color.parseColor("#4c4c4c"));
        findViewById(buttonResourceId).setTag(0);
    }

    private View.OnClickListener buttonClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v){
            if (Integer.parseInt(v.getTag()
                    .toString()) == 1) {
                v.setBackgroundColor(Color.parseColor("#4c4c4c"));
                v.setTag(0);
            } else {
                v.setBackgroundColor(Color.parseColor("#ed126d"));
                v.setTag(1);
            }
        }
    };

    public String[] splitString(String currentString){
        String[] separated = new String[0];
        if (currentString != null) {
            separated = currentString.split(":");
        }
        return separated;
    }

    private View.OnClickListener sendClickListener = v -> {
        if (v.getId() == R.id.send){
            TextView t=(TextView)findViewById(R.id.configText);
            try {
                getNewPVMessages();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    };

    public String getButtonStates(){
        String config = "";
        config = config.concat((findViewById(R.id.one).getTag().toString()));
        config = config.concat(":" + findViewById(R.id.two).getTag().toString());
        config = config.concat((":" + findViewById(R.id.three).getTag().toString()));
        config = config.concat((":" + findViewById(R.id.four).getTag().toString()+":;"));
        Log.v("tag",config);
        return config;
    }

    public void getNewPVMessages() throws IOException {
        OkHttpClient client = new OkHttpClient();
        RequestBody formBody = new FormBody.Builder()
                .add("lastTime", String.valueOf(recover()))
                .add("user", USERNAME)
                .build();

        Request request = new Request.Builder()
                .url(BASE_URL_GET)
                .post(formBody)
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                String mMessage = e.getMessage().toString();
                Log.v("cdsteer", "get: "+mMessage);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String mMessage = response.body().string();
                store(getTimestamp());
                try {
                    JSONArray pvMessages = new JSONArray(mMessage);
//                    Log.v("cdsteer", pvMessages.toString());
                    String[] newPvMessages =  new String[pvMessages.length()];
                    for(int i=0; i<pvMessages.length(); i++){
                        newPvMessages[i] = String.valueOf(pvMessages.getJSONObject(i).get("pv"));
                    }
                    playBack(newPvMessages);
                } catch (JSONException e) {
                    Log.e("cdsteer", "Failed to convet JSON" );
                }
            }
        });
    }

    private void playBack(String[] newPvMessages) {
        for(int i=0; i<newPvMessages.length; i++){
            writeVal(newPvMessages[i]);
        }
    }

    public void postPVMessage() throws IOException {
        OkHttpClient client = new OkHttpClient();
        RequestBody formBody = new FormBody.Builder()
                .add("time", String.valueOf(getTimestamp()))
                .add("pv", getButtonStates())
                .add("user", USERNAME)
                .build();

        Request request = new Request.Builder()
                .url(BASE_URL_POST)
                .post(formBody)
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                String mMessage = e.getMessage().toString();
                Log.w("cdsteer", mMessage);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String mMessage = response.body().string();
                Log.e("cdsteer", "onResponse: " + mMessage);
            }
        });
    }

    public static final String PREFS_NAME = "TIMEOF";

    private void store(long timestamp){
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putLong("cdsteer", timestamp);
        editor.commit();
    }

    private long recover(){
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        long test = settings.getLong("test", getTimestamp());
//        Log.v("cdsteer", String.valueOf(test));
        return test;
    }

    private long getTimestamp(){
        return System.currentTimeMillis()/1000;
    }

    // background thread that wakes up to check for device after a disconnecting
    private void checkForPVMessages(){
        new Thread(() -> {
            while (true) {
                while (mService.ismPVConnected()) {
                    try {
                        getNewPVMessages();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

}







