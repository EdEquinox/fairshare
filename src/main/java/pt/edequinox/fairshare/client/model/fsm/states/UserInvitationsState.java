package pt.edequinox.fairshare.client.model.fsm.states;

import pt.edequinox.fairshare.client.model.data.ClientData;
import pt.edequinox.fairshare.client.model.fsm.ClientContext;
import pt.edequinox.fairshare.client.model.fsm.ClientState;
import pt.edequinox.fairshare.client.model.fsm.ClientStateAdapter;

public class UserInvitationsState extends ClientStateAdapter {
    public UserInvitationsState(ClientContext clientContext, ClientData data) {
        super(clientContext, data);
    }

    @Override
    public ClientState getState() {
        return ClientState.USER_INVITATIONS;
    }

    @Override
    public boolean refuseInvite() {
        data.refuseInvite();
        return true;
    }

    @Override
    public boolean acceptInvite() {
        data.acceptInvite();
        return true;
    }

    @Override
    public boolean backMain() {
        data.backMain();
        return true;
    }
}
