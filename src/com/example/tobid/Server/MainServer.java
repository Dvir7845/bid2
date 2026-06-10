package com.example.tobid.Server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class MainServer {
    private static final int PORT = 8080;

    public static void main(String[] args) {
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