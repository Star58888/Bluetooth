package com.star.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Connection;
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

    private AcceptThread btAcceptThread;
    private ConnectingThread btConnectingThread;
    private ConnectedThread btConnectedThread;


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
        if (btConnectingThread != null)
        {
            btConnectingThread.cancel();
            btConnectingThread = null;
        }

        if (btConnectedThread != null)
        {
            btConnectedThread.cancel();
            btConnectedThread = null;
        }
        //確認有沒有開過 有開過就不在開
        if (btAcceptThread == null)
        {
            btAcceptThread = new AcceptThread();
            btAcceptThread.start();
        }

    }

    public void connect(BluetoothDevice device)
    {
        Log.d(TAG , "connect to : " + device);

        if (btConnectingThread != null)
        {
            btConnectingThread.cancel();
            btConnectingThread = null;
        }

        if (btConnectedThread != null)
        {
            btConnectedThread.cancel();
            btConnectedThread = null;
        }
        if (btAcceptThread != null)
        {
            btAcceptThread.cancel();
            btAcceptThread = null;
        }
        //沒連結 就開始做連結
        btConnectingThread = new ConnectingThread(device);
        btConnectingThread.start();

    }

    public synchronized void stop()
    {
        Log.d(TAG , "Stop is threads. ");

        if (btConnectingThread != null)
        {
            btConnectingThread.cancel();
            btConnectingThread = null;
        }

        if (btConnectedThread != null)
        {
            btConnectedThread.cancel();
            btConnectedThread = null;
        }
        if (btAcceptThread != null)
        {
            btAcceptThread.cancel();
            btAcceptThread = null;
        }
        btState = STATE_STOP; // 停止就會砍掉
    }

    public void BTWrite(byte[] out)
    {
        if (btState != STATE_CONNECTED) {
            return;
        }
        btConnectedThread.write(out);
    }

    private class AcceptThread extends Thread
    {
        private final BluetoothServerSocket BTServerSocket;
        BluetoothServerSocket tempSocket;
        BluetoothSocket btSocket;
        private BluetoothDevice device;

        public AcceptThread()
        {

            try {
                tempSocket = btAdaper.listenUsingRfcommWithServiceRecord("BT Char" , UUID_String);
                Log.d(TAG , "Get BT ServerSocket OK");
            } catch (IOException e) {
                Log.d(TAG , "Get BT ServerSocket failed");
//                e.printStackTrace();
            }

            BTServerSocket = tempSocket;
            btState = STATE_WaitingConnectng;

        }

        public void run()
        {
            while ((btState != STATE_CONNECTED))
            {
                try {
                    btSocket = BTServerSocket.accept();
                } catch (IOException e) {
                    Log.d(TAG , "Get BT Socket fail" + e);
//                    e.printStackTrace();
                    break;
                }
            }

            if (btSocket != null)
            {
                switch (btState)
                {
                    case STATE_WaitingConnectng:

                    case STATE_CONECTING:
                        device = btSocket.getRemoteDevice();
                        //藍芽
                        if (btConnectingThread != null)
                        {
                            btConnectingThread.cancel();
                            btConnectingThread = null;
                        }

                        if (btConnectedThread != null)
                        {
                            btConnectedThread.cancel();
                            btConnectedThread = null;
                        }
                        if (btAcceptThread != null)
                        {
                            btAcceptThread.cancel();
                            btAcceptThread = null;
                        }
                        btConnectedThread = new ConnectedThread(btSocket);
                        btConnectedThread.start();

                        //顯示動作
                        Message msg = btHandler.obtainMessage(Constants.MESSAGE_DEVICE_NAME);
                        Bundle bundle = new Bundle();
                        bundle.putString(Constants.DEVICE_NAME , device.getName());
                        msg.setData(bundle);
                        btHandler.sendMessage(msg);
                        break;

                    case STATE_NORMAL:
                    case STATE_CONNECTED:
                        try {
                            btSocket.close();
                        } catch (IOException e) {
                            Log.d(TAG , "close failed" );
//                            e.printStackTrace();
                        }
                        break;
                }
            }
        }

        public void cancel()
        {
            try {
                BTServerSocket.close();
            } catch (IOException e) {

//                e.printStackTrace();
            }
        }
    }

    private class ConnectedThread extends Thread
    {
        private final BluetoothSocket btSocket;
        private final InputStream btInputStream;
        private final OutputStream btOutputStream;

        public ConnectedThread(BluetoothSocket socket)
        {
            btSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }

            btInputStream = tmpIn;
            btOutputStream = tmpOut;
            btState = STATE_CONNECTED;
        }

        public void run()
        {
            byte[] buffer = new byte[1024];
            int bytesReadLength;

            while (btState == STATE_CONNECTED)
            {
                try {
                    bytesReadLength = btInputStream.read(buffer);
                    //直接呼叫Handler的Message
                    btHandler.obtainMessage( Constants.MESSAGE_READ , bytesReadLength , -1 , buffer).sendToTarget();
                } catch (IOException e) {
                    Message msg = btHandler.obtainMessage(Constants.MESSAGE_DEVICE_NAME);
                    Bundle bundle = new Bundle();
                    bundle.putString(Constants.TOAST , "Device connection is lost. ");
                    msg.setData(bundle);
                    btHandler.sendMessage(msg);

                    if (btState != STATE_STOP)
                    {
                        btState = STATE_NORMAL;
                        serverStart();
                    }
                    break;
                }
            }
        }

        public void write(byte[] buffer)
        {
            try {
                btOutputStream.write(buffer);
            } catch (IOException e) {
//                e.printStackTrace();
            }
        }

        public void cancel()
        {
            try {
                btSocket.close();
            } catch (IOException e) {

//                e.printStackTrace();
            }
        }
    }

    private class ConnectingThread extends Thread
    {
        private final BluetoothSocket btSocket;
        private final BluetoothDevice btDevice;

        public ConnectingThread(BluetoothDevice device)
        {
            btDevice = device;
            BluetoothSocket tmpSocket = null;

            try {
                tmpSocket = device.createInsecureRfcommSocketToServiceRecord(UUID_String);
            } catch (IOException e) {
//                e.printStackTrace();
            }
            btSocket = tmpSocket;
            btState = STATE_CONECTING;
        }

        public void run()
        {
            try {
                btSocket.connect();
            } catch (IOException e) {
                try {
                    btSocket.close();
                } catch (IOException e1) {
//                    e1.printStackTrace();
                }

                Message msg = btHandler.obtainMessage(Constants.MESSAGE_DEVICE_NAME);
                Bundle bundle = new Bundle();
                bundle.putString(Constants.TOAST , "Unable to connect device. ");
                msg.setData(bundle);
                btHandler.sendMessage(msg);

                if (btState != STATE_STOP)
                {
                    btState = STATE_NORMAL;
                    serverStart();
                }
                return;
//                e.printStackTrace();
            }

            if (btConnectedThread != null)
            {
                btConnectedThread.cancel();
                btConnectedThread = null;
            }
            if (btAcceptThread != null)
            {
                btAcceptThread.cancel();
                btAcceptThread = null;
            }
            btConnectedThread = new ConnectedThread(btSocket);
            btConnectedThread.start();

            Message msg = btHandler.obtainMessage(Constants.MESSAGE_DEVICE_NAME);
            Bundle bundle = new Bundle();
            bundle.putString(Constants.DEVICE_NAME , btDevice.getName());
            msg.setData(bundle);
            btHandler.sendMessage(msg);
        }

        public void cancel()
        {
            try {
                btSocket.close();
            } catch (IOException e) {

//                e.printStackTrace();
            }
        }

    }

}
