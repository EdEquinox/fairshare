package model;

public record Message(model.Message.Type type, Object payload) {

    public enum Type {
        REGISTER, LOGIN, LOGOUT, EDIT_PROFILE, GET_PROFILE,CREATE_GROUP, INVITE, GET_GROUPS, GET_INVITES, GET_PENDING_INVITES
    }

    @Override
    public String toString() {
        return "Message [type=" + type + ", payload=" + payload + "]";
    }
}
