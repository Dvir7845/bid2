package com.example.tobid.Server;

import com.example.tobid.DataModels.Bid;
import com.example.tobid.DataModels.FirebaseStorageService;
import com.example.tobid.DataModels.Item;
import com.example.tobid.DataModels.NotificationType;
import com.example.tobid.DataModels.Request;
import com.example.tobid.DataModels.Response;
import com.google.cloud.storage.Bucket;
import com.google.firebase.database.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class BidService {
    private static BidService instance;
    private final FirebaseDatabase database;

    private BidService() {
        this.database = FirebaseDatabase.getInstance();
    }

    public static synchronized BidService getInstance() {
        if (instance == null) {
            instance = new BidService();
        }
        return instance;
    }
    
    
    protected Response handlePlaceBid(Request request) {
        try {
            String bidId = (String) request.getData("saleId");
            String category = ((String) request.getData("saleCategory")).toUpperCase();
            String buyerUid = (String) request.getData("uid");
            float bidAmount = ((Number) request.getData("bidAmount")).floatValue();
            Bid bid = fetchBidSync(category, bidId);
            
            if (bid == null) {
                return new Response(false, "The item could not be found.");
            }
            if (bid.isHasMaximumPrice() && bid.getHighestOfferedBid() >= bid.getMaximumPrice()) {
                return new Response(false, "This item has already been purchased or closed.");
            }
            if (buyerUid.equals(bid.getItem().getSellerUID())) {
                return new Response(false, "Sellers cannot purchase their own items.");
            }
            
            float currentHighest = bid.getHighestOfferedBid();
            float minimumAllowedBid = currentHighest + getMinimumIncrement(currentHighest);
            if (bidAmount < minimumAllowedBid) {
                return new Response(false, "Bid is too low. For this price category, the minimum bid must be at least $" + minimumAllowedBid);
            }
            
            // Update object states
            bid.setHighestOfferedBid(bidAmount);
            bid.setLeadingBidderId(buyerUid);
            
            // Commit changes to Firebase
            DatabaseReference bidRef = database.getReference().child("Bids").child(category).child(bidId);
            bidRef.setValueAsync(bid).get();
            
            NotificationService.getInstance().sendNotification(
                    bid.getItem().getSellerUID(),
                    NotificationType.LOST_LEAD_IN_BID,
                    buyerUid,
                    "2Bid System",
                    bid.getItem().getStoragePathToImg1(),
                    "New highest bid of $" + bidAmount + " on your item: " + bid.getItem().getItemName()
                );
            
            // Wake up AutoBid bots to check for counter-offers
            runAutoBidDuel(bidId, category);
            
            return new Response(true, "Bid placed successfully.");
            
        } catch (Exception e) {
            System.err.print("placing bid failed: " + e.getMessage());
            e.printStackTrace();
            return new Response(false, "Bid not placed.");
        }
    }
    
    protected Response handleAutoBid(Request request) {
        try {
            // Extract parameters from the request
            String bidId = (String) request.getData("saleId");
            String category = ((String) request.getData("saleCategory")).toUpperCase();
            String uid = (String) request.getData("uid");
            float maxAutoLimit = ((Number) request.getData("maxAutoLimit")).floatValue();

            // 1. Save the bot's maximum limit configurations under the AutoBids node
            DatabaseReference autoBidRef = database.getReference()
                    .child("AutoBids").child(category).child(bidId).child(uid);
            autoBidRef.setValueAsync(maxAutoLimit).get();
            System.out.println("AutoBid activated for user " + uid + " with limit $" + maxAutoLimit);

            // 2. Fetch the current state of the bid from Firebase synchronously using fetchBidSync
            Bid bid = fetchBidSync(category, bidId);
            
            if (bid != null) {
                DatabaseReference bidRef = database.getReference().child("Bids").child(category).child(bidId);
                // Case A: The auction has no bids yet (First bid on this item)
                if (bid.getLeadingBidderId() == null || bid.getLeadingBidderId().isEmpty()) {
                    System.out.println("[AutoBid] First bot initiating the auction with starting price: $" + bid.getStartingPrice());
                    bid.setHighestOfferedBid(bid.getStartingPrice());
                    bid.setLeadingBidderId(uid);
                    // Update the database with the starting price assignment
                    bidRef.setValueAsync(bid).get(); 
                }
                
                // Case B: There are already existing bids, or this is a secondary bot joining the pool.
                // Trigger the automated bidding duel logic.
                runAutoBidDuel(bidId, category);
            }
            return new Response(true, "AutoBid activated successfully!");
        } catch (Exception e) {
            System.err.println("Failed to activate AutoBid: " + e.getMessage());
            e.printStackTrace();
            return new Response(false, "Internal server error activating AutoBid.");
        }
    }
    
    private void runAutoBidDuel(String bidId, String category) {
        try {
            boolean duelOngoing = true;
            
            while (duelOngoing) {
                // 1. Fetch the latest state of the bid using our synchronous helper function
                Bid bid = fetchBidSync(category, bidId);
                
                // 2. Fetch all active auto-bid bots for this specific item
                final java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
                final DataSnapshot[] autoBidsSnapshotHolder = new DataSnapshot[1];
                
                DatabaseReference autoBidsRef = database.getReference().child("AutoBids").child(category).child(bidId);
                autoBidsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        autoBidsSnapshotHolder[0] = snapshot;
                        latch.countDown();
                    }
                    @Override
                    public void onCancelled(DatabaseError error) { 
                        latch.countDown(); 
                    }
                });

                latch.await();

                DataSnapshot autoBidsSnapshot = autoBidsSnapshotHolder[0];
                
                // Safety check: break the loop if the bid or bots snapshot doesn't exist
                if (bid == null || autoBidsSnapshot == null || !autoBidsSnapshot.exists()) {
                    duelOngoing = false;
                    break;
                }
                
                float currentHighestPrice = bid.getHighestOfferedBid();
                String currentLeadingBidder = bid.getLeadingBidderId();
                
                // Calculate the next required bid based on the dynamic pricing increment table
                float nextRequiredBid = currentHighestPrice + getMinimumIncrement(currentHighestPrice);
                
                // Check if the next required bid exceeds or meets the Buy Now price limit
                if (bid.isHasMaximumPrice() && nextRequiredBid >= bid.getMaximumPrice()) {
                    duelOngoing = false;
                    break;
                }
                
                String bestBotUid = null;
                float bestBotLimit = -1;
                
                // 3. Iterate through all active bots to find the one with the highest eligible limit
                for (DataSnapshot botSnapshot : autoBidsSnapshot.getChildren()) {
                    String botUid = botSnapshot.getKey();
                    float botMaxLimit = ((Number) botSnapshot.getValue()).floatValue();
                    // Skip the current leading bidder to prevent a bot from outbidding itself
                    if (botUid.equals(currentLeadingBidder)) continue; 
                    // Check if the bot can afford the next bid and has a higher limit than previously found bots
                    if (botMaxLimit >= nextRequiredBid && botMaxLimit > bestBotLimit) {
                        bestBotUid = botUid;
                        bestBotLimit = botMaxLimit;
                    }
                }
                
                // 4. If a suitable challenger bot is found, execute the automatic counter-bid
                if (bestBotUid != null) {
                    System.out.println("[AutoBid] Bot " + bestBotUid + " automatically outbid the current price to $" + nextRequiredBid);
                    
                    bid.setHighestOfferedBid(nextRequiredBid);
                    bid.setLeadingBidderId(bestBotUid);
                    
                    // Commit the new bid back to Firebase synchronously
                    DatabaseReference bidRef = database.getReference().child("Bids").child(category).child(bidId);
                    bidRef.setValueAsync(bid).get();
                    
                    // Keep the duel ongoing for another cycle to check for other reacting bots
                    duelOngoing = true; 
                } else { 
                    // No more eligible challenger bots found to outbid the current leader
                    duelOngoing = false;
                }
            }
        } catch (Exception e) {
            System.err.println("Error during AutoBid duel execution: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    protected Response handleBuyNow(Request request) {
    	try {
     //get Data from Request
            String bidId = (String) request.getData("saleId");
            String category = ((String) request.getData("saleCategory")).toUpperCase();
            String buyerUid = (String) request.getData("uid");
            
           
            Bid bid = fetchBidSync(category, bidId);
            if (bid == null) return new Response(false, "The item could not be found.");
            if (bid.getHighestOfferedBid() >= bid.getMaximumPrice()) return new Response(false, "This item has already been purchased.");
            if (buyerUid.equals(bid.getItem().getSellerUID())) return new Response(false, "Sellers cannot purchase their own items.");
            
            bid.setHighestOfferedBid(bid.getMaximumPrice());
            bid.setLeadingBidderId(buyerUid);
            handleEndedBid(bid);
            return new Response(true, "Purchase completed successfully!");
            
        } catch (Exception e) {
            return new Response(false, "Internal server error: " + e.getMessage());
        }
    }
    
    protected Response handleCreateBid(Request request) {
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
	        bid.setBidId(bidId);
	        DatabaseReference myRef = database.getReference().child("Bids").child(item.getCategory()).child(bidId);
	        myRef.setValueAsync(bid).get();
	        System.out.println("Uploaded to db");
	
	        // Create a bid created notification for the user
	        NotificationService.getInstance().sendNotification(
	        	    bid.getItem().getSellerUID(),
	        	    NotificationType.BID_CREATED,
	        	    bidId, 
	        	    "2Bid", 
	        	    imagePaths[0], 
	        	    "Bid " + bid.getItem().getItemName() + " successfully created."
	        	);
	        
	        
			return new Response(true, "Bid creation successfully.");
    	} catch (Exception e) {
    		System.err.print("Bid creation failed. ");
			e.printStackTrace();
			
			return new Response(false, "Bid creation failed: " + e.getMessage());
    	}
	}
    
    protected Response handleGetBidById(Request request) {
    	try {
    		final java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
    		
    		String bidId = (String) request.getData("bidId");
    		final Bid[] result = new Bid[1];

    		DatabaseReference myRef = database.getReference().child("Bids");
    		myRef.addListenerForSingleValueEvent(new ValueEventListener() {

				@Override
				public void onDataChange(DataSnapshot snapshot) {
					for (DataSnapshot categorySnapshot : snapshot.getChildren()) {
						if (categorySnapshot.hasChild(bidId)) {
							DataSnapshot bidSnapshot = categorySnapshot.child(bidId);
							result[0] = bidSnapshot.getValue(Bid.class);
							if (result[0] != null)
								result[0].setBidId(bidSnapshot.getKey());
							
							latch.countDown(); // If bid found, finish early
						}
					}
					
					latch.countDown(); // If bid not found, terminate to continue
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
    /**
    * Handles all post-auction processing when a bid has expired.
    * Moves data to history nodes, triggers notifications for both parties, and removes active references.
    */
    protected void handleEndedBid(Bid bid) {
    	try {
    		if (bid == null) return;
	        
	        String bidId = bid.getBidId();
	        String sellerUid = bid.getItem().getSellerUID();
	        String category = bid.getItem().getCategory();
	        String winnerUid = bid.getLeadingBidderId(); // Can be null or empty if no one placed a bid
	        
	        Map<String, Object> updates = new HashMap<>();
	        
	        System.out.println("[BidService] Processing ended bid: " + bidId);
	        // 1. Move bid to Seller's historical archive (Hosted Bids)
	        updates.put("/History/HostedBids/" + sellerUid + "/" + bidId, bid);
	        // 2. If there is a valid winner, save it to the Winner's archive (Participated Bids)
	        boolean hasWinner = (winnerUid != null && !winnerUid.isEmpty());
	        if (hasWinner) {
		        updates.put("/History/ParticipatedBids/" + winnerUid + "/" + bidId, bid);
	        }
	        // 3. Delete the active auction from the main "Bids" directory to save space and clean UI
	        updates.put("/Bids/" + category + "/" + bidId, null);
	        
	        // Perform all three updates as an atomic action (either all go through or none)
	        database.getReference().updateChildrenAsync(updates).get();
	        
	        // 4. Trigger localized system notifications 
	        if (hasWinner) {
	        	// Notify the Seller
	        	NotificationService.getInstance().sendNotification(
	        			sellerUid,
	        			NotificationType.BID_WON, // Reuse or add dynamic status
	                    "2Bid",
	                    "2Bid System",
	                    bid.getItem().getStoragePathToImg1(),
	                    "Your auction for '" + bid.getItem().getItemName() + "' ended! Winner ID: " + winnerUid + " at $" + bid.getHighestOfferedBid()
	        	);
	        	// Notify the Winner
	        	NotificationService.getInstance().sendNotification(
	        			winnerUid,
	                    NotificationType.BID_WON,
	                    "2Bid",
	                    "2Bid System",
	                    bid.getItem().getStoragePathToImg1(),
	                    "Congratulations! You won the auction for '" + bid.getItem().getItemName() + "' for $" + bid.getHighestOfferedBid()
	        	);
	        } else {
	        	// Auction ended with no bidders at all
	        	NotificationService.getInstance().sendNotification(
	        			sellerUid,
	                    NotificationType.LOST_LEAD_IN_BID, // Or an AUCTION_EXPIRED status
	                    "2Bid",
	                    "2Bid System",
	                    bid.getItem().getStoragePathToImg1(),
	                    "Your auction for '" + bid.getItem().getItemName() + "' expired with no active bids."
	        	);
	        }
    	} catch (Exception e) {
    		System.err.print("Handle ended bid failed: " + e.getMessage());
			e.printStackTrace();
    	}
	}
    
    protected Response handleGetAllBidsInCategory(Request request) {
		LocalDate currentDate = LocalDate.now();
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
		
		final java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
		ArrayList<Bid> ongoingBids = new ArrayList<>();
		ArrayList<Bid> futureBids = new ArrayList<>();
		try {
			String category = (String) request.getData("Category");
			
			DatabaseReference myRef = database.getReference();
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
	                            bid.setBidId(bidSnapshot.getKey());
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
		
        // Get start and end date
        LocalDate bidStartDate = LocalDate.parse(bid.getStartDate(), formatter);
        LocalDate bidEndDate = LocalDate.parse(bid.getEndDate(), formatter);
        
        if (currentDate.isAfter(bidEndDate)) {
        	handleEndedBid(bid);
        } 
        else if (currentDate.isBefore(bidStartDate)) {
        	futureBids.add(bid);
        }
        else {
        	ongoingBids.add(bid);
        }
	}
    
    private float getMinimumIncrement(float currentPrice) {
        if (currentPrice < 10) {
            return 0.50f;
        } else if (currentPrice < 50) {
            return 1.00f;  
        } else if (currentPrice < 250) {
            return 5.00f;  
        } else if (currentPrice < 1000) {
            return 10.00f;
        } else if(currentPrice<10000) {
            return 50.00f; 
        } else if(currentPrice<100000) {
            return 500.00f; 
        }else
            	return 1000f;
        }
    
    
    private Bid fetchBidSync(String category, String bidId) throws InterruptedException {
        final java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
        final Bid[] bidHolder = new Bid[1];
        
        DatabaseReference bidRef = database.getReference().child("Bids").child(category).child(bidId);
        bidRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) bidHolder[0] = snapshot.getValue(Bid.class);
                latch.countDown();
            }
            @Override
            public void onCancelled(DatabaseError error) { latch.countDown(); }
        });
        
        latch.await();
        return bidHolder[0];
    }
    
}
