package communication;

import model.ServerResponse;

/**
 * This interface is used to listen for broadcasts from the server.
 */
public interface BroadcastListener {

    /**
     * Called when a broadcast message is received from the server.
     *
     * @param response The server response containing the broadcast message.
     */
    void onBroadcastReceived(ServerResponse response);
}
