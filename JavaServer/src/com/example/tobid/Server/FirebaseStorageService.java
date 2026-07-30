package com.example.tobid.Server;

import com.google.cloud.storage.Bucket;
import com.google.firebase.cloud.StorageClient;

/**
 * Singleton service for managing Firebase Cloud Storage operations.
 * Provides centralized access to the storage bucket for uploading and retrieving files (e.g., images).
 */
public class FirebaseStorageService {
    private static FirebaseStorageService instance;
    private final Bucket bucket;

    // Private constructor initializes the bucket reference once
    private FirebaseStorageService() {
        this.bucket = StorageClient.getInstance().bucket();
    }

    public static synchronized FirebaseStorageService getInstance() {
        if (instance == null) {
            instance = new FirebaseStorageService();
        }
        return instance;
    }

    // Expose the bucket safely
    public Bucket getBucket() {
        return this.bucket;
    }
    
    // Abstract standard operations directly inside this class
    public void uploadBytes(String path, byte[] content, String contentType) {
        this.bucket.create(path, content, contentType);
    }
}
