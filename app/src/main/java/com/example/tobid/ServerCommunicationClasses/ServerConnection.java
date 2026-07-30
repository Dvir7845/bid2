package com.example.tobid.ServerCommunicationClasses;

import com.example.tobid.DataModels.Request;
import com.example.tobid.DataModels.Response;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ServerConnection {

    //private static final String SERVER_IP = "10.0.2.2"; // ip for simulator
    private static final String SERVER_IP ="136.113.101.144"; //  cloud's IPS
    //private static final String SERVER_IP = "192.168.1.242"; // Yaniv's ip
    //private static final String SERVER_IP = "10.0.0.8" ; // Dvir home
    //private static final String SERVER_IP = "10.83.85.72" ; // Dvir hotspot
    private static final int SERVER_PORT = 50406;
    private static ServerConnection instance;
    private ServerConnection() {}
    // Singleton pattern to ensure only one instance of the class is created
    public static synchronized ServerConnection getInstance() {
        if (instance == null) {
            instance = new ServerConnection();
        }
        return instance;
    }

    public void sendRequest(final Request request, final ServerCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try (
                        Socket socket = new Socket(SERVER_IP, SERVER_PORT);

                        ObjectOutputStream out =
                                new ObjectOutputStream(socket.getOutputStream());

                        ObjectInputStream in =
                                new ObjectInputStream(socket.getInputStream())
                ) {

                    out.writeObject(request);
                    out.flush();

                    final Response response = (Response) in.readObject();

                    callback.onResponseReceived(response);

                } catch (Exception e) {
                    android.util.Log.e("ToBid_Network", "CRITICAL ERROR CONNECTING TO SERVER", e);
                    e.printStackTrace();

                    callback.onResponseReceived(
                            new Response(false,
                                    "Connection error: " + e.getMessage()));
                }
            }}).start();
    }
}