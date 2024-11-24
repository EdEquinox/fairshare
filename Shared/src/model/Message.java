package model;

public record Message(model.Message.Type type, Object payload) {

    public enum Type {
        REGISTER, LOGIN, LOGOUT, EDIT_PROFILE, GET_PROFILE, CREATE_GROUP, GET_GROUPS, GET_USERS_FOR_GROUP, GET_EXPENSES, INVITE, GET_INVITES, GET_PENDING_INVITES, STOP_BACKUP, UPDATE_BACKUP, BACKUP_INIT, ACCEPT_INVITE, DECLINE_INVITE, GET_GROU_NAME, GET_GROUP_NAME, HEARTBEAT
    }

    @Override
    public String toString() {
        return "Message [type=" + type + ", payload=" + payload + "]";
    }
}
