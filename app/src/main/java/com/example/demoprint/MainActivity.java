package com.example.demoprint;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    TextView myLabel;
    EditText myTextbox;
    BluetoothAdapter mBluetoothAdapter;
    BluetoothSocket mmSocket;
    BluetoothDevice mmDevice;
    OutputStream mmOutputStream;
    InputStream mmInputStream;
    Thread workerThread;
    byte[] readBuffer;
    int readBufferPosition;
    volatile boolean stopWorker;
    RecyclerView rvList;
    List<BluetoothDevice> bluetoothDeviceList= new ArrayList<>();
    RecyclerViewAdapter recyclerViewAdapter;
    BluetoothAdapter bluetoothAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            } else {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 999);
            }
        }
        Button openButton = (Button) findViewById(R.id.open);
        Button sendButton = (Button) findViewById(R.id.send);
        Button closeButton = (Button) findViewById(R.id.close);
        rvList = findViewById(R.id.rvList);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        rvList.setLayoutManager(linearLayoutManager);


        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            new AlertDialog.Builder(this)
                    .setTitle("Not compatible")
                    .setMessage("Your phone does not support Bluetooth")
                    .setPositiveButton("Exit", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            System.exit(0);
                        }
                    })
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
        }
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBT = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBT, 999);
        }

        myLabel = (TextView) findViewById(R.id.label);
        myTextbox = (EditText) findViewById(R.id.entry);

        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                bluetoothDeviceList.add(device);
            }
        }
        recyclerViewAdapter = new RecyclerViewAdapter(bluetoothDeviceList, new RecyclerViewAdapter.IOnClickItem() {
            @Override
            public void onClick(int position) {
                Log.e("Position",position+"");
                try {
                    openBT(bluetoothDeviceList.get(position));
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e("Position",e.getMessage());
                }
            }
        });
        rvList.setAdapter(recyclerViewAdapter);

        openButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    findBT();
                    openBT(mmDevice);
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        });
//        sendButton.setOnClickListener(new View.OnClickListener() {
//            public void onClick(View v) {
//                try {
//                    sendData();
//                } catch (IOException ex) {
//                    ex.printStackTrace();
//                }
//            }
//        });
//        closeButton.setOnClickListener(new View.OnClickListener() {
//            public void onClick(View v) {
//                try {
//                    closeBT();
//                } catch (IOException ex) {
//                    ex.printStackTrace();
//                }
//            }
//        });
        ToggleButton toggleButton = findViewById(R.id.scan);
        toggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
                if (isChecked) {
                    bluetoothDeviceList.clear();
                    registerReceiver(bReciever, filter);
                    bluetoothAdapter.startDiscovery();
                } else {
                    unregisterReceiver(bReciever);
                    bluetoothAdapter.cancelDiscovery();
                }
            }
        });
    }

    // this will find a bluetooth printer device
    void findBT() {

        try {
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

            if (mBluetoothAdapter == null) {
                myLabel.setText("No bluetooth adapter available");
            }

            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBluetooth, 0);
            }

            Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();

            if (pairedDevices.size() > 0) {
                for (BluetoothDevice device : pairedDevices) {
                    if (device.getName().equals("DESKTOP-JF7SJE3")) {
                        mmDevice = device;
                        myLabel.setText("Bluetooth device found.");
                        break;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private final BroadcastReceiver bReciever = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                bluetoothDeviceList.add(device);
                recyclerViewAdapter.notifyDataSetChanged();
            }
        }
    };

    void openBT(BluetoothDevice mmDevice) throws IOException {
        try {

            // Standard SerialPortService ID
            UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
            mmSocket = mmDevice.createRfcommSocketToServiceRecord(uuid);
            mmSocket.connect();
            mmOutputStream = mmSocket.getOutputStream();
            mmInputStream = mmSocket.getInputStream();

            beginListenForData();

            myLabel.setText("Bluetooth Opened");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void beginListenForData() {
        try {
            final Handler handler = new Handler();

            // this is the ASCII code for a newline character
            final byte delimiter = 10;

            stopWorker = false;
            readBufferPosition = 0;
            readBuffer = new byte[1024];

            workerThread = new Thread(new Runnable() {
                public void run() {

                    while (!Thread.currentThread().isInterrupted() && !stopWorker) {

                        try {

                            int bytesAvailable = mmInputStream.available();

                            if (bytesAvailable > 0) {

                                byte[] packetBytes = new byte[bytesAvailable];
                                mmInputStream.read(packetBytes);

                                for (int i = 0; i < bytesAvailable; i++) {

                                    byte b = packetBytes[i];
                                    if (b == delimiter) {

                                        byte[] encodedBytes = new byte[readBufferPosition];
                                        System.arraycopy(
                                                readBuffer, 0,
                                                encodedBytes, 0,
                                                encodedBytes.length
                                        );

                                        // specify US-ASCII encoding
                                        final String data = new String(encodedBytes, "US-ASCII");
                                        readBufferPosition = 0;

                                        // tell the user data were sent to bluetooth printer device
                                        handler.post(new Runnable() {
                                            public void run() {
                                                myLabel.setText(data);
                                            }
                                        });

                                    } else {
                                        readBuffer[readBufferPosition++] = b;
                                    }
                                }
                            }

                        } catch (IOException ex) {
                            stopWorker = true;
                        }

                    }
                }
            });

            workerThread.start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
//
//    void sendData() throws IOException {
//        try {
//
//            // the text typed by the user
//            String msg = myTextbox.getText().toString();
//            msg += "\n";
//
//            mmOutputStream.write(msg.getBytes());
//
//            // tell the user data were sent
//            myLabel.setText("Data sent.");
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//    // close the connection to bluetooth printer.
//    void closeBT() throws IOException {
//        try {
//            stopWorker = true;
//            mmOutputStream.close();
//            mmInputStream.close();
//            mmSocket.close();
//            myLabel.setText("Bluetooth Closed");
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
}