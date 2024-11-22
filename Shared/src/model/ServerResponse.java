package model;

public record ServerResponse(boolean isSuccess, String message, Object payload) {

    @Override
    public String toString() {
        return "ServerResponse{" + "isSuccess=" + isSuccess + ", responseMessage='" + message + '\'' + ", payload=" + payload + '}';
    }
}
