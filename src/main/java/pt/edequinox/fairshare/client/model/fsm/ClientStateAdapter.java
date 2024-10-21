package pt.edequinox.fairshare.client.model.fsm;

import pt.edequinox.fairshare.client.model.data.ClientData;

public abstract class ClientStateAdapter implements IClientState {

   protected ClientContext context;
    protected ClientData data;

   protected ClientStateAdapter(ClientContext context, ClientData data) {
       this.context = context;
   }

   protected void changeState(ClientState state) {
       context.changeState(ClientState.createState(state, context, data));
   }

    public boolean register(String username, String password, String name, String phone) {
        return false;
    }
    public boolean login(String email, String password) {
        return false;
    }
    public boolean logout() {
        return false;
    }
    public boolean seeInvites() {
        return false;
    }
    public boolean acceptInvite() {
        return false;
    }
    public boolean refuseInvite() {
        return false;
    }
    public boolean backMain() {
        return false;
    }
    public boolean seeGroups() {
        return false;
    }
    public boolean editCredentials() {
        return false;
    }
    public boolean saveUser() {
        return false;
    }
    public boolean exportCSV() {
        return false;
    }
    public boolean editName() {
        return false;
    }
    public boolean saveGroup() {
        return false;
    }
    public boolean newGroup() {
        return false;
    }
    public boolean backGroups() {
        return false;
    }
    public boolean selectGroup() {
        return false;
    }
    public boolean leaveGroup() {
        return false;
    }
    public boolean sendInvite() {
        return false;
    }
    public boolean createInvite() {
        return false;
    }
    public boolean addExpense() {return false;}
    public boolean saveExpense() {
        return false;
    }
    public boolean editExpenses() {
        return false;
    }
    public boolean memberInfo() {
        return false;
    }
    public boolean backGroup() {
        return false;
    }
    public boolean confirmPayment() {
        return false;
    }
    public boolean payTo() {
        return false;
    }

    public ClientState getState() {
        return null;
    }

}
