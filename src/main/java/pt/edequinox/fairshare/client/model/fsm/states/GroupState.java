package pt.edequinox.fairshare.client.model.fsm.states;

import pt.edequinox.fairshare.client.model.data.ClientData;
import pt.edequinox.fairshare.client.model.fsm.ClientContext;
import pt.edequinox.fairshare.client.model.fsm.ClientState;
import pt.edequinox.fairshare.client.model.fsm.ClientStateAdapter;

public class GroupState extends ClientStateAdapter {
    public GroupState(ClientContext context, ClientData data) {
        super(context, data);
    }

    @Override
    public boolean exportCSV() {
        return super.exportCSV();
    }

    @Override
    public boolean editName() {
        return super.editName();
    }

    @Override
    public boolean createInvite() {
        return super.createInvite();
    }

    @Override
    public boolean backGroups() {
        return super.backGroups();
    }

    @Override
    public boolean leaveGroup() {
        return super.leaveGroup();
    }

    @Override
    public boolean addExpense() {
        return super.addExpense();
    }

    @Override
    public boolean editExpenses() {
        return super.editExpenses();
    }

    @Override
    public boolean memberInfo() {
        return super.memberInfo();
    }

    @Override
    public boolean payTo() {
        return super.payTo();
    }

    @Override
    public ClientState getState() {
        return ClientState.GROUP;
    }
}
