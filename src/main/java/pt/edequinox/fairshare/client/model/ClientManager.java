package pt.edequinox.fairshare.client.model;

import pt.edequinox.fairshare.client.model.fsm.ClientContext;
import pt.edequinox.fairshare.client.model.fsm.ClientState;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

public class ClientManager {

    ClientContext context;
    PropertyChangeSupport pcs;

    public ClientManager() {
        context = new ClientContext();
        pcs = new PropertyChangeSupport(this);
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        pcs.addPropertyChangeListener(listener);
    }


    public void login(String email, String password) {
        pcs.firePropertyChange(null, null, null);
        context.login(email, password);
    }

    public void register(String email, String password, String name, String phone) {
        pcs.firePropertyChange(null, null, null);
        context.register(email, password, name, phone);
    }

    public void logout() {
        pcs.firePropertyChange(null, null, null);
        context.logout();
    }

    public void seeInvites() {
        pcs.firePropertyChange(null, null, null);
        context.seeInvites();
    }

    public void acceptInvite() {
        pcs.firePropertyChange(null, null, null);
        context.acceptInvite();
    }

    public void refuseInvite() {
        pcs.firePropertyChange(null, null, null);
        context.refuseInvite();
    }

    public void backMain() {
        pcs.firePropertyChange(null, null, null);
        context.backMain();
    }

    public void seeGroups() {
        pcs.firePropertyChange(null, null, null);
        context.seeGroups();
    }

    public void editCredentials() {
        pcs.firePropertyChange(null, null, null);
        context.editCredentials();
    }

    public void saveUser() {
        pcs.firePropertyChange(null, null, null);
        context.saveUser();
    }

    public void exportCSV() {
        pcs.firePropertyChange(null, null, null);
        context.exportCSV();
    }

    public void editName() {
        pcs.firePropertyChange(null, null, null);
        context.editName();
    }

    public void saveGroup() {
        pcs.firePropertyChange(null, null, null);
        context.saveGroup();
    }

    public void newGroup() {
        pcs.firePropertyChange(null, null, null);
        context.newGroup();
    }

    public void backGroups() {
        pcs.firePropertyChange(null, null, null);
        context.backGroups();
    }

    public void selectGroup() {
        pcs.firePropertyChange(null, null, null);
        context.selectGroup();
    }

    public void leaveGroup() {
        pcs.firePropertyChange(null, null, null);
        context.leaveGroup();
    }

    public void sendInvite() {
        pcs.firePropertyChange(null, null, null);
        context.sendInvite();
    }

    public void createInvite() {
        pcs.firePropertyChange(null, null, null);
        context.createInvite();
    }

    public void addExpense() {
        pcs.firePropertyChange(null, null, null);
        context.addExpense();
    }

    public void saveExpense() {
        pcs.firePropertyChange(null, null, null);
        context.saveExpense();
    }

    public void editExpenses() {
        pcs.firePropertyChange(null, null, null);
        context.editExpenses();
    }

    public void memberInfo() {
        pcs.firePropertyChange(null, null, null);
        context.memberInfo();
    }

    public void backGroup() {
        pcs.firePropertyChange(null, null, null);
        context.backGroup();
    }

    public void confirmPayment() {
        pcs.firePropertyChange(null, null, null);
        context.confirmPayment();
    }

    public void payTo() {
        pcs.firePropertyChange(null, null, null);
        context.payTo();
    }

    public ClientState getState() {
        return context.getState();
    }

}
