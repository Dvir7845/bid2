package com.example.tobid.Server;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Iterator;

import com.example.tobid.DataModels.FirebaseStorageService;
import com.example.tobid.DataModels.Item;
import com.example.tobid.DataModels.Notification;
import com.example.tobid.DataModels.Request;
import com.example.tobid.DataModels.Response;
import com.example.tobid.DataModels.Sale;
import com.example.tobid.DataModels.User;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Bucket;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class ClientHandler extends Thread {
    private Socket socket;
    
    private FirebaseDatabase database;
    private DatabaseReference myRef;

    public ClientHandler(Socket socket) {
        this.socket = socket;
        
        database = FirebaseDatabase.getInstance();
        myRef = database.getReference();
    }

    @Override
    public void run() {
       
        try (
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream())
        ) {
        	System.out.println("reading request");
            Request request = (Request) in.readObject();
            System.out.println("got request");
            Response response = processRequest(request);
            
            out.writeObject(response);
            out.flush();
            System.out.println("Request processed. Closing connection.");

        } 
        catch (Exception e) {
            System.out.println("Error handling client: " + e.getMessage());
        }
    }

    private Response processRequest(Request request) {
    	System.out.println("Request: " + request.getAction().toString());
    	Response response = null;
        switch (request.getAction()) {
            case PLACE_BID:
                if (placeBid()) {
                    return new Response(true, "Bid received", null);
                } else {
                    return new Response(false, "Illegal Bid", null);
                }
            case BUY_NOW:
                return new Response(true, "Purchase completed", null);
            case AUTO_BID:
                return new Response(true, "Auto bid activated", null);
            case GET_SALE:
            	response = handleGetSale(request);
            	return response;
            case GET_ALL_SALES:
            	break;
            case CREATE_SALE:
            	response = handleCreateSale(request);
            	return response;
            case LOGIN:
            	break;
            case REGISTER:
            	response = handleRegister(request);
            	return response;
            default:
                return new Response(false, "Unknown request", null);
        }
        
		return null; // Should be unreachable
    }
    private Response handleCreateSale(Request request) {
    	try {
	    	String bidId = (String) request.getData("bidId");
	    	Sale sale = (Sale) request.getData("Sale");
	    	String[] imagePaths = (String[]) request.getData("imagePaths");
	    	
	    	Collection<byte[]> imagesToUpload = request.getFiles().values();
	    	
	    	System.out.println("Got data");
	    	
	    	// Upload Item images to storage
	    	FirebaseStorageService storageService = FirebaseStorageService.getInstance();
	        Bucket bucket = storageService.getBucket();
	        Iterator<byte[]> iterator = imagesToUpload.iterator();
	        for (String imagePath : imagePaths) {
	        	if (!iterator.hasNext()) break;
	        	
	        	byte[] imageBytes = iterator.next();
	        	bucket.create(imagePath, imageBytes, "image/png");
	        	System.out.println("Created image!");
	        }
	
	        // Upload sale to database
	        myRef = database.getReference().child("Bids").child(sale.getItem().getCategory()).child(bidId);
	        myRef.setValueAsync(sale).get();
	        System.out.println("Uploaded to db");
	
			return new Response(true, "Bid creation successfully.", null);
    	} catch (Exception e) {
    		System.err.print("Bid creation failed. ");
			e.printStackTrace();
			
			return new Response(false, "Bid creation failed: " + e.getMessage(), null);
    	}
	}

	/**
     * 
     * @param request
     * @return
     */
    private Response handleRegister(Request request){
    	System.out.println("Inside handle");
		User newUser = (User) request.getData("userObject");
		String uid = newUser.getId();
		
		String defaultPfpPath = "DefaultPfp/DefaultPfp.png";
		String userImgPath = "Users/" + uid + "/profile.png";
		
		// Save the user profile in the database
		try {
	        myRef = database.getReference().child("Users").child(uid);
	        myRef.setValueAsync(newUser).get();
	        
	        FirebaseStorageService storageService = FirebaseStorageService.getInstance();
	        Bucket bucket = storageService.getBucket();
	        
	        // Get default profile picture from storage
	        Blob defaultBlob = bucket.get(defaultPfpPath);
	        if (defaultBlob == null || !defaultBlob.exists()) {
	        	return new Response(false, "Couldn't fetch default profile picture.", null);
	        }
	        
	        byte[] defaultPfpBytes = defaultBlob.getContent();
	        
	        // Upload the image to the storage
	        bucket.create(userImgPath, defaultPfpBytes, "image/png");
	        
	        // Update image path in the database
	        myRef = database.getReference().child("Users").child(uid).child("/img");
	        myRef.setValueAsync(userImgPath).get();
	        
	        // Create a welcome notification for the user
            String notificationText = "Welcome to 2Bid! Start setting up your profile by exploring new biddings.";
            Notification signUpNotification = new Notification(
                    "2Bid-" + Calendar.getInstance().getTimeInMillis(),
                    "2Bid", "2Bid", newUser.getImg(), notificationText);

            // Save the notification in the database
            myRef = database.getReference("/Users/" + uid + "/notifications/" + signUpNotification.getId());
            myRef.setValueAsync(signUpNotification).get();
            
	        return new Response(true, "Registration successfull.", null);
		} catch (Exception e) {
			System.err.print("Registration failed for UID: " + uid);
			e.printStackTrace();
			
			// Delete user Authentication data!
			if (uid != null) {
	            try {
	                FirebaseAuth.getInstance().deleteUser(uid);
	                System.err.println("Rollback successful: Deleted orphaned user " + uid);
	            } catch (FirebaseAuthException rollbackError) {
	                System.err.println("Rollback failed: Could not delete user " + uid);
	            }
	        }
			
			return new Response(false, "Uploading user data or media failed: " + e.getMessage(), null);
		}
	}

	private Response handleGetSale(Request request) {


		return null;
	}

	private boolean placeBid() {
        
        return true; 
    }
}