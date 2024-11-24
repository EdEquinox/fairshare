package model;

public record Message(model.Message.Type type, Object payload) {

    public enum Type {
        REGISTER, LOGIN, LOGOUT,
        EDIT_PROFILE, GET_PROFILE,
        CREATE_INVITE, ACCEPT_INVITE, DECLINE_INVITE, GET_INVITES,
        CREATE_GROUP, GET_GROUPS, GET_USERS_FOR_GROUP, GET_GROUP_NAME,
        ADD_EXPENSE, EDIT_EXPENSE, DELETE_EXPENSE, GET_EXPENSES, GET_EXPENSES_USER,
        ADD_PAYMENT,
        STOP_BACKUP, UPDATE_BACKUP, BACKUP_INIT,  HEARTBEAT
    }

    @Override
    public String toString() {
        return "Message [type=" + type + ", payload=" + payload + "]";
    }
}
