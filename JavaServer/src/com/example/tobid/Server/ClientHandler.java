package com.example.tobid.Server;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.Iterator;

import com.example.tobid.DataModels.FirebaseStorageService;
import com.example.tobid.DataModels.Item;
import com.example.tobid.DataModels.Notification;
import com.example.tobid.DataModels.NotificationType;
import com.example.tobid.DataModels.Request;
import com.example.tobid.DataModels.Response;
import com.example.tobid.DataModels.Bid;
import com.example.tobid.DataModels.User;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Bucket;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

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
            System.out.println("Error handling client: " + e.toString());
        }
    }

    private Response processRequest(Request request) {
    	System.out.println("Request: " + request.getAction().toString());
    	Response response = null;
        switch (request.getAction()) {
            case PLACE_BID:
                response = handlePlaceBid(request);
                return response;
            case BUY_NOW:
                return new Response(true, "Purchase completed");
            case AUTO_BID:
                return new Response(true, "Auto bid activated");
            case GET_ALL_BIDS_IN_CATEGORY:
            	response = handleGetAllBidsInCategory(request);
            	return response;
            case CREATE_BID:
            	response = handleCreateBid(request);
            	return response;
            case LOGIN:
            	response = handleLogin(request);
            	return response;
            case REGISTER:
            	response = handleRegister(request);
            	return response;
            case GET_USER_NOTIFICATIONS:
            	response = handleGetUserNotifications(request);
            	return response;
            case REMOVE_NOTIFICATION_BY_ID:
            	response = handleRemoveNotificationById(request);
            	return response;
            case GET_BID_BY_BID_ID:
            	response = handleGetBidById(request);
            	return response;
            default:
                return new Response(false, "Unknown request");
        }
    }

	private Response handleGetAllBidsInCategory(Request request) {
		LocalDate currentDate = LocalDate.now();
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
		
		final java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
		ArrayList<Bid> ongoingBids = new ArrayList<>();
		ArrayList<Bid> futureBids = new ArrayList<>();
		try {
			String category = (String) request.getData("Category");
			
			myRef = database.getReference();
			if ("All".equals(category))
				myRef = myRef.child("Bids");
			else
				myRef = myRef.child("Bids").child(category);
			
			myRef.addListenerForSingleValueEvent(new ValueEventListener() {
				@Override
				public void onDataChange(DataSnapshot snapshot) {
					if ("All".equals(category)) {
						// For each category
	                    for (DataSnapshot bidsCategorySnapshot : snapshot.getChildren()) {
	                        for (DataSnapshot bidSnapshot : bidsCategorySnapshot.getChildren()) {
	                            Bid bid = bidSnapshot.getValue(Bid.class);
	                            if (bid == null) continue;
	                            addBidIfValid(request, bid, category, 
	                            		currentDate, formatter,
	                            		ongoingBids, futureBids);
	                        }
	                    }
					} else {
						// A specific category is selected
	                    for (DataSnapshot bidSnapshot : snapshot.getChildren()) {
                            Bid bid = bidSnapshot.getValue(Bid.class);
                            if (bid == null) continue;
                            
                            addBidIfValid(request, bid, category, 
                            		currentDate, formatter,
                            		ongoingBids, futureBids);
	                    }
					}
					latch.countDown();
				}

				@Override
				public void onCancelled(DatabaseError error) {
					latch.countDown();
				}
			});
			
			// Wait until data fetching is complete
			latch.await();
			
			Response response = new Response(true, "Bids fetched successfully.");
			
			response.putData("ongoingBids", ongoingBids);
			response.putData("futureBids", futureBids);
			
			return response;
			
    	} catch (Exception e) {
    		System.err.print("Get bid request failed: " + e.getMessage());
			e.printStackTrace();
			
			return new Response(false, "Get bid request failed.");
    	}
	}

	protected void addBidIfValid(Request request, Bid bid, String category, 
			LocalDate currentDate, DateTimeFormatter formatter, 
			ArrayList<Bid> ongoingBids, ArrayList<Bid> futureBids) {
		
        String startDate = bid.getStartDate();
        String endDate = bid.getEndDate();
        
        // Get start and end date
        LocalDate bidStartDate = LocalDate.parse(bid.getStartDate(), formatter);
        LocalDate bidEndDate = LocalDate.parse(bid.getEndDate(), formatter);
        
        if (currentDate.isAfter(bidEndDate)) {
        	// TODO: Bid has ended. Process appropriately
        	handleEndedBid(request, category);
        } 
        else if (currentDate.isBefore(bidStartDate)) {
        	futureBids.add(bid);
        }
        else {
        	ongoingBids.add(bid);
        }
	}

	/**
	 * Is called from within the handleGetAllBidsInCategory function (When found a bid that ended).
	 * TODO: This function should handle all of the post bid things. Like:
	 * 1. Move bid from the Bids dir into the Seller and Winners past bids (hosted bids and participated bids respectively)
	 * 2. Send a won bid \ bid ended notifications for the seller and winner.
	 * 3. Make it so the seller and winner can't bet anymore on that screen and make the creators phone number visible
	 * @param request
	 */
	protected void handleEndedBid(Request request, String category) {
		// TODO IMPLEMENT!
		
	}

	private Response handleGetBidById(Request request) {
    	try {
    		final java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
    		
    		String bidId = (String) request.getData("bidId");
    		final Bid[] result = new Bid[1];

    		myRef = database.getReference().child("Bids");
    		myRef.addListenerForSingleValueEvent(new ValueEventListener() {

				@Override
				public void onDataChange(DataSnapshot snapshot) {
					for (DataSnapshot categorySnapshot : snapshot.getChildren()) {
						if (categorySnapshot.hasChild(bidId)) {
							DataSnapshot bidSnapshot = categorySnapshot.child(bidId);
							result[0] = bidSnapshot.getValue(Bid.class);
							latch.countDown();
						}
					}
				}

				@Override
				public void onCancelled(DatabaseError error) {
					latch.countDown();
				}
    			
    		});
    		latch.await();
    		
    		if (result[0] != null) {
    			Response response = new Response(true, "Bid by id retrieval succeeded.");
    	        response.putData("Bid", result[0]);
    	        return response;
    	    } else {
    	        return new Response(false, "Bid not found.");
    	    }
    	} catch (Exception e) {
    		System.err.print("Bid by id retrieval failed: " + e.getMessage());
			e.printStackTrace();
			
			return new Response(false, "Bid by id retrieval failed.");
    	}
	}

	private Response handleRemoveNotificationById(Request request) {
    	try {
    		String uid = (String) request.getData("uid");
    		String notificationId = (String) request.getData("notificationId");
    		
    		myRef = database.getReference().child("Users").child(uid).child("notifications").child(notificationId);
    		myRef.setValueAsync(null).get();
    		
    		return new Response(true, "Notification removal succeeded.");
    	} catch (Exception e) {
    		System.err.print("Notification removal failed: " + e.getMessage());
			e.printStackTrace();
			
			return new Response(false, "Notification removal failed.");
    	}
	}

	private Response handleGetUserNotifications(Request request) {
    	final java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
    	
    	ArrayList<Notification> notifications = new ArrayList<>();
		try {
			String uid = (String) request.getData("uid");
			myRef = database.getReference().child("Users").child(uid).child("notifications");

	        myRef.addListenerForSingleValueEvent(new ValueEventListener() {

				@Override
				public void onDataChange(DataSnapshot snapshot) {
					System.out.println("fetched notifications");
					for (DataSnapshot datas : snapshot.getChildren()) {
	                    Notification notification = datas.getValue(Notification.class);
	                    notifications.add(notification);
	                }
					System.out.println("got notifications");
					latch.countDown();
				}

				@Override
				public void onCancelled(DatabaseError error) {
					System.out.println("Error fetching requests for user: " + uid);
					latch.countDown();
				}
	        });
			
	        // Wait for the db to fetch the notifications
	        System.out.println("waiting for latch");
	        latch.await();
	        System.out.println("latch received");
	        Response response = new Response(true, "Notifications fetch successfuly");
	        response.putData("notifications", notifications);
	        return response; 
			
		} catch (Exception e) {
    		System.err.print("Notifications retrieval failed: " + e.getMessage());
			e.printStackTrace();
			
			return new Response(false, "Notifications retrieval failed.");
    	}
		
		
	}

	private Response handleLogin(Request request) {
		try {
			String idToken = (String) request.getData("idToken");
			
			FirebaseToken token = FirebaseAuth.getInstance().verifyIdToken(idToken);
			String uid = token.getUid();
			
			System.out.println("Authenticated user: " + uid);
			return new Response(true, "Signin successfful");
			
		} catch (FirebaseAuthException e) {
			return new Response(false, "Invalid token");
		} catch (Exception e) {
    		System.err.print("Signin failed. ");
			e.printStackTrace();
			
			return new Response(false, "Signin failed: " + e.getMessage());
    	}
	}

	private Response handleCreateBid(Request request) {
    	try {
	    	String bidId = (String) request.getData("bidId");
	    	Bid bid = (Bid) request.getData("Bid");
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
	
	        // Upload bid to database
	        Item item = bid.getItem();
	        myRef = database.getReference().child("Bids").child(item.getCategory()).child(bidId);
	        myRef.setValueAsync(bid).get();
	        System.out.println("Uploaded to db");
	
	        // Create a bid created notification for the user
            String notificationText = "Bid " + bid.getItem().getItemName() + " successfuly created.";
            Notification bidCreatedNotification = new Notification(NotificationType.BID_CREATED,
                    bidId + "-" + Calendar.getInstance().getTimeInMillis(),
                    bidId, "2Bid", imagePaths[0], notificationText);

            // Save the notification in the database
            myRef = database.getReference().child("Users").child(item.getSellerUID())
            		.child("notifications").child(bidCreatedNotification.getId());
            myRef.setValueAsync(bidCreatedNotification).get();
	        
	        
			return new Response(true, "Bid creation successfully.");
    	} catch (Exception e) {
    		System.err.print("Bid creation failed. ");
			e.printStackTrace();
			
			return new Response(false, "Bid creation failed: " + e.getMessage());
    	}
	}

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
	        	return new Response(false, "Couldn't fetch default profile picture.");
	        }
	        
	        byte[] defaultPfpBytes = defaultBlob.getContent();
	        
	        // Upload the image to the storage
	        bucket.create(userImgPath, defaultPfpBytes, "image/png");
	        
	        // Update image path in the database
	        myRef = database.getReference().child("Users").child(uid).child("/img");
	        myRef.setValueAsync(userImgPath).get();
	        
	        // Create a welcome notification for the user
            String notificationText = "Welcome to 2Bid! Start setting up your profile by exploring new biddings.";
            Notification signUpNotification = new Notification(NotificationType.SIGNUP,
                    "2Bid-" + Calendar.getInstance().getTimeInMillis(),
                    "2Bid", "2Bid", newUser.getImg(), notificationText);

            // Save the notification in the database
            myRef = database.getReference("/Users/" + uid + "/notifications/" + signUpNotification.getId());
            myRef.setValueAsync(signUpNotification).get();
            
	        return new Response(true, "Registration successfull.");
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
			
			return new Response(false, "Uploading user data or media failed: " + e.getMessage());
		}
	}

	
    private Response handlePlaceBid(Request request) {
    	try {
    		
    		
    		return new Response(true, "Bid placed successfully.");
    	} catch (Exception e) {
    		System.err.print("placing bid failed: " + e.getMessage());
			e.printStackTrace();
			
			return new Response(false, "Bid not placed.");
    	}
	}

}