package pt.edequinox.fairshare.client.model.fsm.states;

import pt.edequinox.fairshare.client.model.data.ClientData;
import pt.edequinox.fairshare.client.model.fsm.ClientContext;
import pt.edequinox.fairshare.client.model.fsm.ClientState;
import pt.edequinox.fairshare.client.model.fsm.ClientStateAdapter;

public class UserSettingsState extends ClientStateAdapter {
    public UserSettingsState(ClientContext context, ClientData data) {
        super(context, data);
    }

    @Override
    public ClientState getState() {
        return ClientState.USER_SETTINGS;
    }

    @Override
    public boolean saveUser() {
        data.saveUser();
        return true;
    }
}
