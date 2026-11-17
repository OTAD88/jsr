package com.example.data.model

enum class MessageType {
    CHAT,
    ACK_HOP,
    ACK_DELIVERY,
    HELLO,
    ROUTE_ANNOUNCE,
    FRIEND_REQUEST,
    FRIEND_ACCEPT,
    FRIEND_REMOVE,
    RESUME_APPROVAL,
    DISTANCE_CONSENT_REQUEST,
    DISTANCE_CONSENT_UPDATE,
    EMERGENCY_SOS
}

enum class DistanceConsentStatus {
    MUTUAL_ACTIVE,    // Both users agreed to share distance
    WAITING_PEER,     // Local user consented, waiting for remote peer
    LOCAL_DISABLED,   // Local user has feature turned off
    REMOTE_DISABLED   // Remote peer refused or disabled distance sharing
}

enum class DeliveryStatus {
    LOCAL_PENDING, // Saved locally on sender device
    SENDING,       // Actively transmitting/relaying across mesh
    QUEUED_RELAY,  // Unreachable now; safely stored in relay queue (Store-and-Forward)
    DELIVERED,     // Reached destination and verified by end-to-end delivery ACK
    FAILED         // Expired or retry limit reached
}

enum class TransportType {
    BLE,
    WIFI_DIRECT,
    LOCAL
}

enum class FriendStatus {
    FRIEND,
    REQUEST_SENT,
    REQUEST_RECEIVED
}

enum class AppearanceMode {
    AUTO,
    LIGHT,
    DARK,
    CUSTOM
}

enum class AppLanguage {
    AR,
    EN
}

enum class AppLockMethod {
    NONE,
    BIOMETRIC,
    PIN,
    PASSWORD,
    PATTERN
}


