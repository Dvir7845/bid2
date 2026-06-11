package com.example.tobid.DataModels;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ServerConnection {

    private static final String SERVER_IP = "10.0.2.2"; // ip for simulator
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

    public Response sendRequest(Request request) {

        try (
                Socket socket = new Socket(SERVER_IP, SERVER_PORT);

                ObjectOutputStream out =
                        new ObjectOutputStream(socket.getOutputStream());

                ObjectInputStream in =
                        new ObjectInputStream(socket.getInputStream())
        ) {

            out.writeObject(request);
            out.flush();

            return (Response) in.readObject();

        } catch (Exception e) {
            e.printStackTrace();

            return new Response(
                    false,
                    "Connection error: " + e.getMessage(),
                    null
            );
        }
    }
}