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

/**
 * Main server class for the ToBid application.
 * Responsible for initializing the Firebase connection and listening for incoming TCP client connections.
 */
public class MainServer {
	

    public static void main(String[] args) {
    	int port =  50406;
    	try {
    		// Load the local Firebase credentials key from the JSON file
    	    InputStream serviceAccount = new FileInputStream("serviceAccountKey.json");
         //connect to firebase 
    	    FirebaseOptions options = FirebaseOptions.builder()
    	        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
    	        .setDatabaseUrl("https://tobid-3032c-default-rtdb.firebaseio.com")
    	        .setStorageBucket("tobid-3032c.firebasestorage.app")
    	        .build();
    	 // Initialize the Firebase App instance
    	    FirebaseApp.initializeApp(options);
    	    System.out.println("Firebase initialized successfully!");

    	} catch (IOException e) {
    	    System.err.println("Failed to initialize Firebase: " + e.getMessage());
    	    return;
    	}

        System.out.println("Server starting on port " + port);
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server is running, waiting for connections...");
            while (true) { // Infinite loop to continuously accept incoming client connections
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected: " + clientSocket.getInetAddress());
                //Creates a new process to handle the client to allow the main process to continue listening
                new ClientHandler(clientSocket).start(); 
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}