package com.example.pvbluetooth;

import android.app.Service;
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
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.IOException;
import java.util.List;
import java.util.Random;
import java.util.UUID;


public class BluetoothConect extends Service {

    int startMode;       // indicates how to behave if the service is killed
    boolean allowRebind; // indicates whether onRebind should be used

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

    Intent intent;

    private Context context = this;
    private static final String DEVICE_ADDRESS = "B4:52:A9:12:A6:AA";

    BluetoothGattCharacteristic m_characteristicTX;
    List<BluetoothGattService> m_gattServices;
    BluetoothGattCharacteristic m_characteristicRead;

    public boolean ismPVConnected() {
        return mPVConnected;
    }

    private boolean mPVConnected = false;

    // Random number generator
    private final IBinder binder = new LocalBinder();
    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        BluetoothConect getService() {
            // Return this instance of LocalService so clients can call public methods
            return BluetoothConect.this;
        }
    }

    public BluetoothConect() {

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
            intent = new Intent("BLENewData");
            intent.putExtra("TXData", txData);
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
            mPVConnected = true;
        }

        //!Function to discover services
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            gatt.discoverServices();
            switch (newState) {
                case BluetoothProfile.STATE_CONNECTED:
                    Log.v("cdsteer", String.valueOf("STATE_CONNECTED"));
                    mPVConnected = true;
//                    try {
//                        getNewPVMessages();
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
                case BluetoothProfile.STATE_DISCONNECTED:
                    Log.v("cdsteer", String.valueOf("STATE_DISCONNECTED"));
                    mPVConnected = false;
                    intent = new Intent("BLEConnection");
                    intent.putExtra("Status", "PV Disconnected");
                    LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
//                    try {
//                        postPVMessage();
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
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
//                    try {
////                        getNewPVMessages();
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
                    Log.v("cdsteer", "connected");
                    intent = new Intent("BLEConnection");
                    intent.putExtra("Status", "PV Connected");
                    LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
                    writeVal("ping");
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

    public void enableReadNotifications(BluetoothGattCharacteristic characteristic) {
        mGatt.setCharacteristicNotification(characteristic, true);
        // Enable the local machine to watch changes to this characteristic
        // Then, change the peripheral to notify observers of changes in its payload.
        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
                UUID.fromString(SaticResources.CLIENT_CHARACTERISTIC_CONFIGURATION_DESCRIPTOR));
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        mGatt.writeDescriptor(descriptor);
    }

    //Function for writing to Arduino
    public boolean writeVal(String text){
        if(m_characteristicTX != null) {
            final byte[] tx = text.getBytes();
            m_characteristicTX.setValue(tx);
            mGatt.writeCharacteristic(m_characteristicTX);
            enableReadNotifications(m_characteristicTX);
        }
        return true;
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
                        Toast.makeText(getApplicationContext(), "Bluetooth Connected", Toast.LENGTH_SHORT).show();

                        Intent intent = new Intent("BLEConnection");
                        intent.putExtra("Status", "Scanning..");
                        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
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

    public void scanLeDevice() {
        if(bluetoothLeScanner != null) {
            if (!scanning) {
                // Stops scanning after a pre-defined scan period.
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        scanning = false;
                        bluetoothLeScanner.stopScan(leScanCallback);
//                        Log.v("cdsteer","stopScan..");
                    }
                }, SCAN_PERIOD);
                scanning = true;
                bluetoothLeScanner.startScan(leScanCallback);
//                Log.v("cdsteer","startScan..");
            } else {
                scanning = false;
                bluetoothLeScanner.stopScan(leScanCallback);
//                Log.v("cdsteer","stopScan2..");
            }
        }
    }

    @Override
    public void onCreate() {
        // The service is being created
        bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();

        handler = new Handler();
        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
        }

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // The service is starting, due to a call to startService()
        findBLE();
        return startMode;
    }
    @Override
    public IBinder onBind(Intent intent) {
        // A client is binding to the service with bindService()
        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // All clients have unbound with unbindService()
        return allowRebind;
    }
    @Override
    public void onRebind(Intent intent) {
        // A client is binding to the service with bindService(),
        // after onUnbind() has already been called
    }

    @Override
    public void onDestroy() {
        //todo link this to a button so the users can stop the BLE if their phone is dieing
        Toast.makeText(this, "service done", Toast.LENGTH_SHORT).show();
    }

    // background thread that wakes up to check for device after a disconnecting
    private void findBLE(){
        new Thread(() -> {
            while (true) {
                while (!mPVConnected) {
                    scanLeDevice();
//                    Log.v("cdsteer", "check for PV");
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