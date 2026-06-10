package com.example.tobid.DataModels;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ServerConnection {

    private static final String SERVER_IP = "10.0.2.2"; // כתובת השרת
    private static final int SERVER_PORT = 8080;

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