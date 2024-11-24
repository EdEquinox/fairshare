package model;

public class Invite {
    public enum Status {
        PENDING,
        ACCEPTED,
        DENIED
    }

    private int id;
    private int groupId;
    private int senderId;
    private int receiverId;
    private String groupName;
    private String senderEmail;
    private String receiverEmail;
    private Status status;

    // Construtor completo
    public Invite(int id, int groupId, int senderId, int receiverId, String groupName, String senderEmail, String receiverEmail, Status status) {
        this.id = id;
        this.groupId = groupId;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.groupName = groupName;
        this.senderEmail = senderEmail;
        this.receiverEmail = receiverEmail;
        this.status = status;
    }

    // Getters e Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getGroupId() {
        return groupId;
    }

    public void setGroupId(int groupId) {
        this.groupId = groupId;
    }

    public int getSenderId() {
        return senderId;
    }

    public void setSenderId(int senderId) {
        this.senderId = senderId;
    }

    public int getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(int receiverId) {
        this.receiverId = receiverId;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getSenderEmail() {
        return senderEmail;
    }

    public void setSenderEmail(String senderEmail) {
        this.senderEmail = senderEmail;
    }

    public String getReceiverEmail() {
        return receiverEmail;
    }

    public void setReceiverEmail(String receiverEmail) {
        this.receiverEmail = receiverEmail;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "Invite{" +
                "id=" + id +
                ", groupId=" + groupId +
                ", senderId=" + senderId +
                ", receiverId=" + receiverId +
                ", groupName='" + groupName + '\'' +
                ", senderEmail='" + senderEmail + '\'' +
                ", receiverEmail='" + receiverEmail + '\'' +
                ", status=" + status +
                '}';
    }
}