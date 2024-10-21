package pt.edequinox.fairshare.client.model.fsm.states;

import pt.edequinox.fairshare.client.model.data.ClientData;
import pt.edequinox.fairshare.client.model.fsm.ClientContext;
import pt.edequinox.fairshare.client.model.fsm.ClientState;
import pt.edequinox.fairshare.client.model.fsm.ClientStateAdapter;

public class LoggedOutState extends ClientStateAdapter {

    public LoggedOutState(ClientContext context, ClientData data) {
        super(context, data);
    }

    @Override
    public ClientState getState() {
        return ClientState.LOGGED_OUT;
    }

    @Override
    public boolean login(String email, String password) {
        data.login(email, password);
        changeState(ClientState.MAIN_MENU);
        return true;
    }

    @Override
    public boolean register(String email, String password, String name, String phone) {
        data.register(email, password, name, phone);
        return true;
    }

}
