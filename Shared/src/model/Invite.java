package model;

public class Invite {

    public enum Status {
        INVITE, ACCEPT, DECLINE
    }

    private int id;
    private int fromUserId;
    private int toUserId;
    private int groupId;
    private String toUserEmail;
    private String date;
    private Status status;

    public Invite(String toUserEmail, int groupId) {
        this.toUserEmail = toUserEmail;
        this.groupId = groupId;
        this.status = Status.INVITE;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getToUserEmail() {
        return toUserEmail;
    }

    public void setToUserEmail(String toUserEmail) {
        this.toUserEmail = toUserEmail;
    }

    public int getFromUserId() {
        return fromUserId;
    }

    public void setFromUserId(int fromUserId) {
        this.fromUserId = fromUserId;
    }

    public int getToUserId() {
        return toUserId;
    }

    public void setToUserId(int toUserId) {
        this.toUserId = toUserId;
    }

    public int getGroupId() {
        return groupId;
    }

    public void setGroupId(int groupId) {
        this.groupId = groupId;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public boolean isAccepted() {
        return status == Status.ACCEPT;
    }

    public void setAccepted(Status status) {
        this.status = status;
    }

}
