package com.star.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Handler;
import android.util.Log;

import java.util.UUID;

/**
 * Created by Star on 2017/11/17.
 */

public class BTChatService {

    private final BluetoothAdapter btAdaper;
    private static final int STATE_NORMAL = 0;
    private static final int STATE_WaitingConnectng = 1;
    private static final int STATE_CONECTING = 2;
    public static final int STATE_CONNECTED = 3;
    private static final int STATE_STOP = 4;

    private static final UUID UUID_String = UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66");
    private final Handler btHandler;
//    private static final UUID UUID_String = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"); //BT SPP

    private int btState;
    private static final String TAG = "BT_Char";


    public BTChatService(Context context , Handler handler)
    {
        btAdaper = BluetoothAdapter.getDefaultAdapter();
        btState = STATE_NORMAL;
        btHandler = handler;
    }

    public int getState()
    {
        return btState;
    }

    public void serverStart()
    {
        Log.d(TAG , "Enter server mode. " );
    }

    public void connect(BluetoothDevice device)
    {
        Log.d(TAG , "connect to : " + device);
    }

    public synchronized void stop()
    {
        Log.d(TAG , "Stop is threads. ");
    }

    public void BTWrite(byte[] out)
    {
        if (btState != STATE_CONNECTED) {
            return;
        }
//        btConnectedThread.write(out);
    }
}
