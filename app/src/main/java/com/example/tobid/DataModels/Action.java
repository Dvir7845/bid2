package com.example.tobid.DataModels;

public enum Action {
    PLACE_BID,
    BUY_NOW,
    AUTO_BID,
    GET_SALE,
    GET_ALL_SALES,
    CREATE_SALE,
    LOGIN,
    REGISTER,
    GET_USER_NOTIFICATIONS, // Must have uid field
    REMOVE_NOTIFICATION_BY_ID, // Must have uid and notificationId field
    GET_BID_BY_BID_ID // Must have bidId
}
