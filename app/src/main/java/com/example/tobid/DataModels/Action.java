package com.example.tobid.DataModels;

public enum Action {
    PLACE_BID,
    BUY_NOW,
    AUTO_BID,
    GET_ALL_BIDS_IN_CATEGORY, // If passed "All" as the category field, will yield all the bids
    GET_PAST_BIDS,
    GET_ACTIVE_BIDS, // Get the users created and participating bids
    CREATE_BID,
    LOGIN,
    REGISTER,
    GET_USER_NOTIFICATIONS,
    REMOVE_NOTIFICATION_BY_ID,
    GET_BID_BY_BID_ID,

    GET_USER_PHONE
}
