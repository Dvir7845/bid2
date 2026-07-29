package com.example.tobid.Server;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import com.example.tobid.DataModels.Request;
import com.example.tobid.DataModels.Response;
import com.google.firebase.database.FirebaseDatabase;

/**
 * Worker thread class responsible for handling client network communication.
 * Each connected client runs on its own instance of ClientHandler to allow
 * concurrent request processing.
 */
public class ClientHandler extends Thread {
    private Socket socket;
    
    private FirebaseDatabase database;
    public ClientHandler(Socket socket) {
        this.socket = socket;
     // Initialize Firebase Database reference
        database = FirebaseDatabase.getInstance();
        database.getReference();
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
         // Delegate business logic processing based on request action
            Response response = processRequest(request);
         // Send the response object back to the client
            out.writeObject(response);
            out.flush();
            System.out.println("Request processed. Closing connection.");
         // Silently ignore corrupted stream errors (e.g., automated port scanners or bots)
    } catch (java.io.StreamCorruptedException e) {
    	// Silently ignore End-Of-File exceptions when clients disconnect abruptly
    } catch (java.io.EOFException e) {
    } catch (Exception e) {
            System.out.println("Error handling client: " + e.toString());
        }
    }

    
     //Routes incoming requests to their corresponding service handlers based on the RequestAction type.
    private Response processRequest(Request request) {
        System.out.println("Request: " + request.getAction().toString());
        
        switch (request.getAction()) {
            // ==================== BID SERVICE ROUTING ====================
        	case GET_CATEGORIES:
        		return BidService.getInstance().handleGetCategories(request);
            case PLACE_BID:
                return BidService.getInstance().handlePlaceBid(request);
            case BUY_NOW:
                return BidService.getInstance().handleBuyNow(request);
            case AUTO_BID:
                return BidService.getInstance().handleAutoBid(request);
            case GET_ALL_BIDS_IN_CATEGORY:
                return BidService.getInstance().handleGetAllBidsInCategory(request);
            case GET_PAST_BIDS:
            	return BidService.getInstance().handleGetPastBids(request);
            case GET_ACTIVE_BIDS:
            	return BidService.getInstance().handleGetActiveBids(request);
            case GET_AMOUNT_OF_ONGOING_BIDS:
            	return BidService.getInstance().handleGetAmountOfOngoingBids(request);
            case CREATE_BID:
                return BidService.getInstance().handleCreateBid(request);
            case GET_BID_BY_BID_ID:
                return BidService.getInstance().handleGetBidById(request);
            case GET_IMAGE_BY_PATH:
            	return BidService.getInstance().handleGetImageByPath(request);

            // ==================== USER SERVICE ROUTING ====================
            case LOGIN:
                return UserService.getInstance().handleLogin(request);
            case REGISTER:
                return UserService.getInstance().handleRegister(request);
            case GET_USER_BY_ID:
            	return UserService.getInstance().handleGetUserById(request);
            case CHANGE_USERNAME_AND_PICTURE:
            	return UserService.getInstance().handleChangeUsernameAndPicture(request);
            	
            	
            case GET_USER_PHONE:
            	return UserService.getInstance().handleGetUserPhone(request);
            	// ==================== NOTIFICATION SERVICE ROUTING ====================
            case GET_USER_NOTIFICATIONS:
                return NotificationService.getInstance().handleGetUserNotifications(request);
            case REMOVE_NOTIFICATION_BY_ID:
                return NotificationService.getInstance().handleRemoveNotificationById(request);
                
            default:
                return new Response(false, "Unknown request");
        }
    }

	

	
	

	

	

	

	

	

   
	
    
    
    
    
    
   
}