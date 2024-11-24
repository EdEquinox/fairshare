package model;

import java.util.List;

public class Expense {
    private int id;
    private int groupId;
    private int paidBy; // ID of the user who paid
    private int addedBy; // ID of the user who added the expense
    private double amount;
    private String description;
    private String date;
    private List<Integer> sharedWith; // IDs of users with whom the expense is shared

    // Full constructor including sharedWith
    public Expense(int id, int groupId, int paidBy, int addedBy, double amount, String description, String date, List<Integer> sharedWith) {
        this.id = id;
        this.groupId = groupId;
        this.paidBy = paidBy;
        this.addedBy = addedBy;
        this.amount = amount;
        this.description = description;
        this.date = date;
        this.sharedWith = sharedWith;
    }

    // Getters and setters
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

    public int getPaidBy() {
        return paidBy;
    }

    public void setPaidBy(int paidBy) {
        this.paidBy = paidBy;
    }

    public int getAddedBy() {
        return addedBy;
    }

    public void setAddedBy(int addedBy) {
        this.addedBy = addedBy;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public List<Integer> getSharedWith() {
        return sharedWith;
    }

    public void setSharedWith(List<Integer> sharedWith) {
        this.sharedWith = sharedWith;
    }

    // toString for debugging and logging purposes
    @Override
    public String toString() {
        return "Expense{" +
                "id=" + id +
                ", groupId=" + groupId +
                ", paidBy=" + paidBy +
                ", addedBy=" + addedBy +
                ", amount=" + amount +
                ", description='" + description + '\'' +
                ", date='" + date + '\'' +
                ", sharedWith=" + sharedWith +
                '}';
    }
}