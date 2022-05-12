package ru.eneanuch.megafacebookapp.other;

import java.util.HashMap;
import java.util.HashSet;

public class Variables {

    public static final int IMAGE_QUALITY = 80;
    public static final int IMAGE_WIDTH = 200;

    public static final String USERS_TABLE = "users";
    public static final String USER_ID = "id";
    public static final String USER_NAME = "name";
    public static final String USER_EMAIL = "email";
    public static final String USER_PASSWORD = "password";
    public static final String USER_AUTHORIZED = "isAuthorized";
    public static final String USER_IMAGE = "image";

    public static final String PREFERENCE_NAME = "MegaFacebookPreference";

    public static final String TOKEN = "token";

    public static final String USER = "user";

    public static final String MESSAGES_TABLE = "messages";
    public static final String SENDER_ID = "senderId";
    public static final String RECEIVER_ID = "receiverId";
    public static final String MESSAGE = "message";
    public static final String DATETIME = "datetime";

    public static final String CONVERSATIONS_TABLE = "conversations";
    public static final String SENDER_NAME = "senderName";
    public static final String RECEIVER_NAME = "receiverName";
    public static final String SENDER_IMAGE = "senderImage";
    public static final String RECEIVER_IMAGE = "receiverImage";
    public static final String LAST_MESSAGE = "lastMessage";

    public static final String USER_ONLINE = "online";
    public static final String MSG_AUTH = "Authorization";
    public static final String MSG_CONTENT_TYPE = "Content-Type";
    public static final String MSG_DATA = "data";
    public static final String MSG_REGISTER_IDS = "registration_ids";

    public static HashMap<String, String> msgHeaders = null;
    public static HashMap<String, String> getMsgHeaders() {
        if (msgHeaders == null) {
            msgHeaders = new HashMap<>();
            msgHeaders.put(
                MSG_AUTH,
                    "key=AAAAYnMLrkU:APA91bGizLGSZFxCDXRkOQkK9zRQu6Vzs-8fNJq0-j2Pb2LFaJfXQQldNdjuL7rGrwsWVxxI-ofPsEYxxgGiE33CxrIMUNaatLaRODT6-FEOenwWMutubTH7ZUjz5aCs6Dn94XknBfEE"
            );
            msgHeaders.put(
                MSG_CONTENT_TYPE,
                "application/json"
            );
        }
        return msgHeaders;
    }
}
