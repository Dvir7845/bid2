package com.example.tobid.DataModels;

/**
 * Enum representing different actions that can be performed on the server.
 */
public enum Action {
    GET_CATEGORIES,
    PLACE_BID,
    BUY_NOW,
    AUTO_BID, //for auto bidding
    GET_ALL_BIDS_IN_CATEGORY, // If passed "All" as the category field, will yield all the bids
    GET_PAST_BIDS,
    GET_ACTIVE_BIDS, // Get the users created and participating bids
    GET_AMOUNT_OF_ONGOING_BIDS,
    CREATE_BID,
    GET_IMAGE_BY_PATH,
    LOGIN,
    REGISTER,
    GET_USER_BY_ID,
    CHANGE_USERNAME_AND_PICTURE,
    GET_USER_NOTIFICATIONS,
    REMOVE_NOTIFICATION_BY_ID, // Remove notification after clicking on it
    GET_BID_BY_BID_ID,

    GET_USER_PHONE //for the buyer to get the phone number of the seller
}
