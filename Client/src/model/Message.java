package model;

public record Message(model.Message.Type type, Object payload) {

    public enum Type {
        REGISTER, LOGIN, LOGOUT, EDIT_PROFILE, GET_PROFILE, INVITE, GET_GROUPS
    }

    @Override
    public String toString() {
        return "Message [type=" + type + ", payload=" + payload + "]";
    }
}
