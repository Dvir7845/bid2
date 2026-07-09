package com.example.tobid.Server;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

public class MainServer {
	

    public static void main(String[] args) {
    	String portEnv = System.getenv("PORT");
    	int port = (portEnv != null) ? Integer.parseInt(portEnv) : 50406;
        try {
        	InputStream serviceAccount;
        	String fbCredentialsEnv = System.getenv("FIREBASE_CREDENTIALS");
        	if (fbCredentialsEnv != null) {
        		serviceAccount = new ByteArrayInputStream(fbCredentialsEnv.getBytes(StandardCharsets.UTF_8));
        		
        	}else {
            serviceAccount = new FileInputStream("serviceAccountKey.json");
        	}
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

        System.out.println("Server starting on port " + port);
        try (ServerSocket serverSocket = new ServerSocket(port)) {
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