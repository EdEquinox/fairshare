package model;

public record Message(model.Message.Type type, Object payload) {

    public enum Type {
        REGISTER, LOGIN, LOGOUT, EDIT_PROFILE, GET_PROFILE, CREATE_GROUP, GET_GROUPS, GET_USERS_FOR_GROUP, GET_EXPENSES
    }

    @Override
    public String toString() {
        return "Message [type=" + type + ", payload=" + payload + "]";
    }
}
