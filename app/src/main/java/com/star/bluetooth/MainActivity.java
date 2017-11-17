package com.star.bluetooth;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Set;

public class MainActivity extends Activity {

    private static final int REQUEST_ENABLE_BT = 2;
    private TextView devicesInfo_1, devicesInfo_2 ;
    private Button SearchBtn, DiscoverBtn, EditBtn;
    private BluetoothAdapter btAdapter;
    private Set<BluetoothDevice> devices;
 //   private BTReceiver receiver;
    private boolean receiverFlag=false;

    private Context context;
    private ListAdapter adapter;
    private ListView listView;
    private ArrayList<String> btDeviceList;

    private static final String TAG = "BT_Test";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 已 connected 藍芽設備
       // devicesInfo_1 = (TextView) findViewById(R.id.Bt_textView1);
      //  devicesInfo_1.setText("");
        // 尋找藍芽設備
     //   devicesInfo_2 = (TextView)findViewById(R.id.devicesInfo_2);
        //  devicesInfo_2.setText("");
        Log.d(TAG, "OnCreate_start");

        context = this;
        SearchBtn = (Button)findViewById(R.id.Search_btn);
        SearchBtn.setOnClickListener(new BtnOnClickListener());

        DiscoverBtn = (Button)findViewById(R.id.Discover_btn);
        DiscoverBtn.setOnClickListener(new BtnOnClickListener());

        EditBtn = (Button)findViewById(R.id.Edit_btn);
        EditBtn.setOnClickListener(new BtnOnClickListener());

        btDeviceList = new ArrayList<String>();

        listView = (ListView) findViewById(R.id.BT_ListView);
        listView.setAdapter(null);
        listView.setOnItemClickListener(new MyOnItemClickListener());

        btAdapter = BluetoothAdapter.getDefaultAdapter();

        if(btAdapter == null) {
            Toast.makeText(this, "There is no Bluetooth.", Toast.LENGTH_SHORT).show();
            finish();
        } else if(!btAdapter.isEnabled()) {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            Log.d(TAG, "Request_enable_start");
            startActivityForResult(intent, REQUEST_ENABLE_BT);

            IntentFilter btFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
            Log.d(TAG, "check BT state change");
            registerReceiver(receiver, btFilter);
            receiverFlag=true;
            Toast.makeText(context , "Monitor BT State change", Toast.LENGTH_SHORT).show();


        }else {

            devices = btAdapter.getBondedDevices();
            Log.d(TAG, "GetBonded_device1");
            if (devices.size() > 0) {
                for (BluetoothDevice device : devices) {
                    //    devicesInfo_1.append(device.getName() + " :   " +
                    //            device.getAddress() + "   ==>  " + device.getBondState() + "\n");

                    btDeviceList.add("Paired :  " + device.getName() + "\n" + device.getAddress());
                    Log.d(TAG, device.getName() + "\n" + device.getAddress());
                }
                    listView.setAdapter(new ArrayAdapter<String>(context,
                            android.R.layout.simple_list_item_1, btDeviceList));

                Log.d(TAG, "GetBonded_device2");
            }

        }

    }   // end of onCreate()

    // Check if enable BT module successfully ? if not, close APP
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == REQUEST_ENABLE_BT) {
            Log.d(TAG, "Request_enable_result");
            if (resultCode == RESULT_CANCELED) {
                Toast.makeText(this, "Enabling Bluetooth failed.", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "close APP");
                finish();
            } else if(resultCode == RESULT_OK) {
                Log.d(TAG,"enable BT");
                btAdapter = BluetoothAdapter.getDefaultAdapter();
                devices = btAdapter.getBondedDevices();
                if(devices.size() > 0) {
                    for(BluetoothDevice device : devices) {
                        btDeviceList.add("Paired :  " + device.getName() + "\n" + device.getAddress());
                    }
                        listView.setAdapter(new ArrayAdapter<String>(context,
                                android.R.layout.simple_list_item_1, btDeviceList));

                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    //按下List Item監聽器 , start connecting remote BT device
    private class MyOnItemClickListener implements AdapterView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            btAdapter.cancelDiscovery();
            Log.d(TAG,"onItemClick");
            // 取得Item內容
            String remoteDeviceName = parent.getItemAtPosition(position).toString();
            Log.d(TAG, remoteDeviceName);
         //   Toast.makeText(context , remoteDeviceName, Toast.LENGTH_SHORT).show();

            Log.d(TAG,"Intent to EditActivity");
            Intent newActivityIntent = new Intent(context , EditActivity.class);
            newActivityIntent.putExtra("remoteDevice",remoteDeviceName);
            startActivity (newActivityIntent);
        }
    }


// Check if any button is  pressed
    private class BtnOnClickListener implements View.OnClickListener {

        @Override
        public void onClick(View view) {
            switch(view.getId()) {

                case R.id.Search_btn:           // press Search BT button and list all remote BT devices
                    Log.d(TAG,"Search Click");
                    btAdapter.startDiscovery();
               //     receiver = new BTReceiver();
                    IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
                    registerReceiver(receiver, filter);
                    receiverFlag=true;
                    Toast.makeText(context , "Begin to scan", Toast.LENGTH_SHORT).show();
                    break;

               case R.id.Discover_btn:            // press Discoverable button to make BT module discoverable for 180 sec
                    Log.d(TAG,"Discoverable Click");
                    Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                    discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 150);
                    startActivity(discoverableIntent);
                    break;

                case R.id.Edit_btn:        // press Edit button to enter BT server mode
                    Log.d(TAG,"Edit Click");
                    Intent newActivityIntent = new Intent(context, EditActivity.class);
                    startActivity (newActivityIntent);

                    break;
            }
        }

    }

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG,"onReceive");
            String action = intent.getAction();
          // if(BluetoothDevice.ACTION_FOUND.equals(action)) {
            if(action.equals(BluetoothDevice.ACTION_FOUND)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

             // you can add code to compare BT device name with ListArray *********
                btDeviceList.add("Found :   " + device.getName() + "\n" + device.getAddress() );
                Log.d(TAG, device.getName() + "\n" + device.getAddress());
                listView.setAdapter(new ArrayAdapter<String>(context,
                        android.R.layout.simple_list_item_1, btDeviceList));
            }

            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                switch(state) {
                    case BluetoothAdapter.STATE_OFF:
                        Log.d(TAG,"BT State is  off");
                        Toast.makeText(context , "BT State is  off", Toast.LENGTH_SHORT).show();
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        Log.d(TAG,"BT State is  turing off");
                        Toast.makeText(context , "BT State is turning off", Toast.LENGTH_SHORT).show();
                        break;
                    case BluetoothAdapter.STATE_ON:
                        Log.d(TAG,"BT State is  on");
                        Toast.makeText(context , "BT State is on", Toast.LENGTH_SHORT).show();

                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        Log.d(TAG,"BT State is  turning on");
                        Toast.makeText(context , "BT State is turing on", Toast.LENGTH_SHORT).show();
                        break;
                }

            }

        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        btAdapter.cancelDiscovery();
        if(receiver != null) {
            if(receiverFlag) {
                Log.d(TAG, "unregister receiver" + receiver);
                unregisterReceiver(receiver);
            }
        }

    }


}
