package model;

public record Message(model.Message.Type type, Object payload) {

    public enum Type {
        REGISTER, LOGIN, LOGOUT, // Authentication
        EDIT_PROFILE, GET_PROFILE, // User Profile
        CREATE_INVITE, ACCEPT_INVITE, DECLINE_INVITE, GET_INVITES, // Invites
        CREATE_GROUP, EDIT_GROUP, REMOVE_GROUP, GET_GROUP_STATS, GET_GROUPS, GET_GROUP_USERS, GET_GROUP_NAME,  // Groups
        ADD_EXPENSE, EDIT_EXPENSE, DELETE_EXPENSE, GET_EXPENSES, // Expenses
        ADD_PAYMENT, EDIT_PAYMENT, DELETE_PAYMENT, GET_PAYMENTS, // Payments
        STOP_BACKUP, UPDATE_BACKUP, BACKUP_INIT, HEARTBEAT // Server Backup
    }

    @Override
    public String toString() {
        return "Message [type=" + type + ", payload=" + payload + "]";
    }
}
