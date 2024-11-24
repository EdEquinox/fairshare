package model;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class Invite {


    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

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
    private String groupName;
    private String inviter;

    public Invite(int id, int groupId, int fromUserId, int toUserId, String groupName, String inviter) {
        this.id = id;
        this.groupId = groupId;
        this.status = Status.INVITE;
        this.fromUserId = fromUserId;
        this.toUserId = toUserId;
        this.groupName = groupName;
        this.inviter = inviter;
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


    @Override
    public String toString() {
        return "Invite to join group " + groupName + " from " + inviter;
    }

}
