package com.app.udp_app;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.StrictMode;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private EditText ipInput, portInput;
    private Button sendButton;
    private TextView sensorValues, orientationValues;

    private SensorManager sensorManager;
    private Sensor rotationVector;

    private DatagramSocket udpSocket;
    private InetAddress serverAddress;
    private int serverPort;

    private boolean sending = false;
    private Thread sendingThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize views
        ipInput = findViewById(R.id.ipInput);
        portInput = findViewById(R.id.portInput);
        sendButton = findViewById(R.id.sendButton);
        sensorValues = findViewById(R.id.sensorValues);
        orientationValues = findViewById(R.id.orientationValues);

        // Initialize sensor manager and sensor
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        rotationVector = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);

        // Initialize UDP socket
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        try {
            udpSocket = new DatagramSocket();
        } catch (Exception e) {
            e.printStackTrace();
        }

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!sending) {
                    startSending();
                } else {
                    stopSending();
                }
            }
        });
    }

    private void startSending() {
        sending = true;
        sendButton.setText("Stop Sending");
        sendingThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (sending) {
                    sendData();
                    try {
                        Thread.sleep(100); // Adjust the delay between each sending iteration as needed
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        sendingThread.start();
    }

    private void stopSending() {
        sending = false;
        sendButton.setText("Send UDP");
    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this, rotationVector, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor == rotationVector) {
            float[] rotationMatrix = new float[9];
            float[] orientationAngles = new float[3];
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values);
            SensorManager.getOrientation(rotationMatrix, orientationAngles);

            // Convert radians to degrees
            float pitch = (float) Math.toDegrees(orientationAngles[1]);
            float roll = (float) Math.toDegrees(orientationAngles[2]);
            float yaw = (float) Math.toDegrees(orientationAngles[0]);

            // Adjust to range from 0 to 360
            pitch = (pitch + 360) % 360;
            roll = (roll + 360) % 360;
            yaw = (yaw + 360) % 360;

            // Update TextView to display orientation values
            orientationValues.setText("Orientation:\nPitch: " + pitch + "\nRoll: " + roll + "\nYaw: " + yaw);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Not used
    }

    private void sendData() {
        // Get IP and port from input fields
        String ip = ipInput.getText().toString();
        int port = Integer.parseInt(portInput.getText().toString());

        // Get orientation values from TextView
        String orientationData = orientationValues.getText().toString();

        // Create data to send
        String data = "Orientation:\n" + orientationData;

        try {
            // Create UDP packet
            serverAddress = InetAddress.getByName(ip);
            byte[] buffer = data.getBytes();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, serverAddress, port);

            // Send UDP packet
            udpSocket.send(packet);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
