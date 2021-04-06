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
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
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

    BluetoothManager bluetoothManager;
    BluetoothAdapter bluetoothAdapter;
    boolean scanning;
    Handler handler;

    private BluetoothGatt mGatt;

    private static final int REQUEST_ENABLE_BT = 1;
    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 10000;
    private BluetoothLeScanner bluetoothLeScanner;

    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;
    private static final int PERMISSION_REQUEST_FINE_LOCATION = 1;

    private Context context = this;
    private static final String DEVICE_ADDRESS = "B4:52:A9:12:A6:AA";

    BluetoothGattCharacteristic m_characteristicTX;
    List<BluetoothGattService> m_gattServices;
    BluetoothGattCharacteristic m_characteristicRead;


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

        bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();

        handler = new Handler();
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
            bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
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

        try {
            getNewPVMessages();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Device scan callback.
    private ScanCallback leScanCallback =
        new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                super.onScanResult(callbackType, result);
                if (result.getDevice().getAddress().equals(DEVICE_ADDRESS)){
                    Log.v("cdsteer", ("scan: "+ result.getDevice().getAddress()));
                    Log.v("cdsteer", "Found it!");
                    mGatt = result.getDevice().connectGatt(context, false, bCallback);
                }
            }
            @Override
            public void onBatchScanResults(List<ScanResult> results) {
                super.onBatchScanResults(results);
                Log.v("cdsteer", ("onBatchScanResults"));

            }
            @Override
            public void onScanFailed(int errorCode) {
                super.onScanFailed(errorCode);
                Log.e("cdsteer", "scan failed");
            }
        };

    /**
     * Callback to find services of device.
     */
    private final BluetoothGattCallback bCallback = new BluetoothGattCallback() {
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            byte[] rawData = characteristic.getValue();
            String txData = new String(rawData).trim(); // toString does not work, but new String()
            Log.i("cdsteer", "MSP Data = " + txData);
            String[] pvPixConfig = splitString(txData);
            if (pvPixConfig.length == 2) {
                setPvPixConfig(pvPixConfig);
            }
        }

        //!Function to discover services
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {

            gatt.discoverServices();
            switch (newState) {
                case BluetoothProfile.STATE_CONNECTED:
                    Log.v("cdsteer", String.valueOf("STATE_CONNECTED"));
                    try {
                        getNewPVMessages();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                case BluetoothProfile.STATE_DISCONNECTED:
                    Log.v("cdsteer", String.valueOf("STATE_DISCONNECTED"));
                    try {
                        postPVMessage();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                default:
                    Log.v("cdsteer", String.valueOf(newState));
            }
        }

        //!Function to deal with discovered services
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == gatt.GATT_SUCCESS) {
                m_gattServices = mGatt.getServices();
                m_characteristicTX = findCharacteristic(SaticResources.HM10_SERIAL_DATA, m_gattServices);
                m_characteristicRead = findCharacteristic(SaticResources.HM10_CONFIG, m_gattServices);
                String foundSuccess = SaticResources.SERVICES_DISCOVERY_CHARACTERISTIC_FAILURE;
                if (m_characteristicTX != null) {
                    foundSuccess = SaticResources.SERVICES_DISCOVERY_CHARACTERISTIC_SUCCESS;
                    try {
                        getNewPVMessages();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    Log.v("cdsteer", "connected");
                }
            } else {
                // Many reasons it could fail to find services
                Toast.makeText(context, "onServicesDiscovered:Else", Toast.LENGTH_SHORT).show();
            }
        }

        //!Function to deal with characteristic writes
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Log.v("cdsteer","onCharacteristicWrite");
            if (status == BluetoothGatt.GATT_SUCCESS){
                Log.v("cdsteer","Write to Characteristic Success!");
            } else {
                Log.v("cdsteer","Blast!Foiled!");
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.v("cdsteer", "Read Characteristic Success!");
//                Log.d("cdsteer", ("Data Read: " + characteristic.getStringValue(0)));
                byte[] rawData = characteristic.getValue();
                String txData = new String(rawData).trim(); // toString does not work, but new String()
                Log.i("cdsteer",
                        "TxData = " + txData);
            }
        }
    };

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

//        try {
//           postPVMessage();
//        } catch (IOException e) {
//           e.printStackTrace();
//        }
    }

    public void scanLeDevice() {
        if(bluetoothLeScanner != null) {
            if (!scanning) {
                // Stops scanning after a pre-defined scan period.
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        scanning = false;
                        bluetoothLeScanner.stopScan(leScanCallback);
                        Log.v("cdsteer","stopScan..");
                    }
                }, SCAN_PERIOD);
                scanning = true;
                bluetoothLeScanner.startScan(leScanCallback);
                Log.v("cdsteer","startScan..");
            } else {
                scanning = false;
                bluetoothLeScanner.stopScan(leScanCallback);
                Log.v("cdsteer","stopScan2..");
            }
        }
    }

    private BluetoothGattCharacteristic findCharacteristic(String uuidString, List<BluetoothGattService> possibleServices) {
        final UUID desiredUuid = UUID.fromString(uuidString);
        for (BluetoothGattService gattService : possibleServices) {
            BluetoothGattCharacteristic desiredCharacteristic = gattService.getCharacteristic(
                    desiredUuid);
            if(desiredCharacteristic !=null) {
                return desiredCharacteristic;
            }
        }
        return null;
    }

    public void enableReadNotifications(BluetoothGattCharacteristic characteristic) {
        mGatt.setCharacteristicNotification(characteristic, true);
        // Enable the local machine to watch changes to this characteristic
        // Then, change the peripheral to notify observers of changes in its payload.
        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
                UUID.fromString(SaticResources.CLIENT_CHARACTERISTIC_CONFIGURATION_DESCRIPTOR));
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        mGatt.writeDescriptor(descriptor);
    }

    public boolean readVal(){
        if(m_characteristicTX != null) {
            mGatt.setCharacteristicNotification(m_characteristicTX, true);
            mGatt.readCharacteristic(m_characteristicTX);
        } else {
            Toast.makeText(this.context, "Tried to read without having connection", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    //Function for writing to Arduino
    public boolean writeVal(String text){
        if(m_characteristicTX != null) {
            final byte[] tx = text.getBytes();
            m_characteristicTX.setValue(tx);
            mGatt.writeCharacteristic(m_characteristicTX);
            enableReadNotifications(m_characteristicTX);
        } else {
            String[] titles = splitString(text);
            Button btn;
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
            scanLeDevice();
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
        String[] separated = currentString.split(":");
        return separated;
    }

    private View.OnClickListener sendClickListener = v -> {
        if (v.getId() == R.id.send){
            TextView t=(TextView)findViewById(R.id.sendConfig);
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
        Log.v("cdsteer", String.valueOf(test));
        return test;
    }

    private long getTimestamp(){
        return System.currentTimeMillis()/1000;
    }

}







