package pt.edequinox.fairshare.client.model.fsm.states;

import pt.edequinox.fairshare.client.model.data.ClientData;
import pt.edequinox.fairshare.client.model.fsm.ClientContext;
import pt.edequinox.fairshare.client.model.fsm.ClientState;
import pt.edequinox.fairshare.client.model.fsm.ClientStateAdapter;
import pt.edequinox.fairshare.client.model.fsm.IClientState;

public class NewGroupState extends ClientStateAdapter {
    public NewGroupState(ClientContext context, ClientData data) {
        super(context, data);
    }

    @Override
    public boolean saveGroup() {
        return false;
    }

    @Override
    public ClientState getState() {
        return null;
    }
}
