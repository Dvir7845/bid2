package com.example.tobid.Server;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import com.example.tobid.DataModels.Request;
import com.example.tobid.DataModels.Response;
import com.google.firebase.database.FirebaseDatabase;


public class ClientHandler extends Thread {
    private Socket socket;
    
    private FirebaseDatabase database;
    public ClientHandler(Socket socket) {
        this.socket = socket;
        
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
        
        switch (request.getAction()) {
            // ==================== BID SERVICE ROUTING ====================
            case PLACE_BID:
                return BidService.getInstance().handlePlaceBid(request);
            case BUY_NOW:
                return BidService.getInstance().handleBuyNow(request);
            case AUTO_BID:
                return BidService.getInstance().handleAutoBid(request);
            case GET_ALL_BIDS_IN_CATEGORY:
                return BidService.getInstance().handleGetAllBidsInCategory(request);
            case CREATE_BID:
                return BidService.getInstance().handleCreateBid(request);
            case GET_BID_BY_BID_ID:
                return BidService.getInstance().handleGetBidById(request);

            // ==================== USER SERVICE ROUTING ====================
            case LOGIN:
                return UserService.getInstance().handleLogin(request);
            case REGISTER:
                return UserService.getInstance().handleRegister(request);
            case GET_USER_NOTIFICATIONS:
                return UserService.getInstance().handleGetUserNotifications(request);
            case REMOVE_NOTIFICATION_BY_ID:
                return UserService.getInstance().handleRemoveNotificationById(request);
            case GET_USER_PHONE:
            	return UserService.getInstance().handleGetUserPhone(request);
                
            default:
                return new Response(false, "Unknown request");
        }
    }

	

	
	

	

	

	

	

	

   
	
    
    
    
    
    
   
}