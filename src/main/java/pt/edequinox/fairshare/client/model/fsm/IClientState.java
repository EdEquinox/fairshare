package pt.edequinox.fairshare.client.model.fsm;

public interface IClientState {

    boolean register(String username, String password, String name, String phone);
    boolean login(String email, String password);
    boolean logout();
    boolean seeInvites();
    boolean acceptInvite();
    boolean refuseInvite();
    boolean backMain();
    boolean seeGroups();
    boolean editCredentials();
    boolean saveUser();
    boolean exportCSV();
    boolean editName();
    boolean saveGroup();
    boolean newGroup();
    boolean backGroups();
    boolean selectGroup();
    boolean leaveGroup();
    boolean sendInvite();
    boolean createInvite();
    boolean addExpense();
    boolean saveExpense();
    boolean editExpenses();
    boolean memberInfo();
    boolean backGroup();
    boolean confirmPayment();
    boolean payTo();

    ClientState getState();
}
