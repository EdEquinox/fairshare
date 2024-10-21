package pt.edequinox.fairshare.client.model.fsm;

import pt.edequinox.fairshare.client.model.data.ClientData;

public class ClientContext {

    private IClientState state;
    private final ClientData data;


    public ClientContext() {
        this.data = new ClientData();
        this.state = ClientState.createState(ClientState.LOGGED_OUT, this, data);
    }

    public void changeState(IClientState state) {
        this.state = state;
    }

    // TRANSITION TO STATES
    public void register(String email, String password, String name, String phone) {
        state.register( email, password, name, phone);
    }
    public void login(String email, String password) {
        state.login(email, password);
    }
    public void logout() {
        state.logout();
    }
    public void seeInvites() {
        state.seeInvites();
    }
    public void acceptInvite() {
        state.acceptInvite();
    }
    public void refuseInvite() {
        state.refuseInvite();
    }
    public void backMain() {
        state.backMain();
    }
    public void seeGroups() {
        state.seeGroups();
    }
    public void editCredentials() {
        state.editCredentials();
    }
    public void saveUser() {
        state.saveUser();
    }
    public void exportCSV() {
        state.exportCSV();
    }
    public void editName() {
        state.editName();
    }
    public void saveGroup() {
        state.saveGroup();
    }
    public void newGroup() {
        state.newGroup();
    }
    public void backGroups() {
        state.backGroups();
    }
    public void selectGroup() {
        state.selectGroup();
    }
    public void leaveGroup() {
        state.leaveGroup();
    }
    public void sendInvite() {
        state.sendInvite();
    }
    public void createInvite() {
        state.createInvite();
    }
    public void addExpense() {
        state.addExpense();
    }
    public void saveExpense() {
        state.saveExpense();
    }
    public void editExpenses() {
        state.editExpenses();
    }
    public void memberInfo() {
        state.memberInfo();
    }
    public void backGroup() {
        state.backGroup();
    }
    public void confirmPayment() {
        state.confirmPayment();
    }
    public void payTo() {
        state.payTo();
    }

    // GETTERS
    public ClientState getState() {
        return state.getState();
    }

}
