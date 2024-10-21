package pt.edequinox.fairshare.client.model.fsm.states;

import pt.edequinox.fairshare.client.model.data.ClientData;
import pt.edequinox.fairshare.client.model.fsm.ClientContext;
import pt.edequinox.fairshare.client.model.fsm.ClientState;
import pt.edequinox.fairshare.client.model.fsm.ClientStateAdapter;

public class MainMenuState extends ClientStateAdapter {
    public MainMenuState(ClientContext context, ClientData data) {
        super(context, data);
    }

    @Override
    public ClientState getState() {
        return ClientState.MAIN_MENU;
    }

    @Override
    public boolean logout() {
        data.logout();
        return true;
    }

    @Override
    public boolean seeInvites() {
        data.seeInvites();
        return true;
    }

    @Override
    public boolean seeGroups() {
        data.seeGroups();
        return true;
    }

    @Override
    public boolean editCredentials() {
        data.editCredentials();
        return true;
    }

    @Override
    public boolean newGroup() {
        data.newGroup();
        return true;
    }
}
