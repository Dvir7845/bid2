package com.example.tobid.Server;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;

public class MainServer {
    private static final int PORT = 8080;

    public static void main(String[] args) {
        try {
   
            FileInputStream serviceAccount = new FileInputStream("serviceAccountKey.json");
            FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                .setDatabaseUrl("https://tobid-3032c-default-rtdb.firebaseio.com")
                .setStorageBucket("tobid-3032c.firebasestorage.app")
                .build();

            FirebaseApp.initializeApp(options);
            System.out.println("Firebase initialized successfully!");

        } catch (IOException e) {
            System.err.println("Failed to initialize Firebase: " + e.getMessage());
            return;
        }

        System.out.println("Server starting on port " + PORT);
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server is running, waiting for connections...");
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected: " + clientSocket.getInetAddress());
                new ClientHandler(clientSocket).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}