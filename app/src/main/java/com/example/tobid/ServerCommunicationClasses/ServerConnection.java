package com.example.tobid.ServerCommunicationClasses;

import com.example.tobid.DataModels.Request;
import com.example.tobid.DataModels.Response;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ServerConnection {

    //private static final String SERVER_IP = "10.0.2.2"; // ip for simulator
    private static final String SERVER_IP = "192.168.1.242"; // Yaniv's ip
    //private static final String SERVER_IP = "10.0.0.1" ;
    //private static final String SERVER_IP = "10.52.148.72" ; // Dvir hotspot
    private static final int SERVER_PORT = 8080;
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
                    e.printStackTrace();

                    callback.onResponseReceived(
                            new Response(false,
                                    "Connection error: " + e.getMessage()));
                }
            }}).start();
    }
}