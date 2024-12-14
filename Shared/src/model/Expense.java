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
    private String paidByName; // Name of the user who paid
    private String addedByName; // Name of the user who added the expense
    private String sharedWithNames; // Names of users with whom the expense is shared

    // Full constructor including names and sharedWithNames
    public Expense(int id, int groupId, int paidBy, int addedBy, double amount, String description, String date,
                   List<Integer> sharedWith, String paidByName, String addedByName, String sharedWithNames) {
        this.id = id;
        this.groupId = groupId;
        this.paidBy = paidBy;
        this.addedBy = addedBy;
        this.amount = amount;
        this.description = description;
        this.date = date;
        this.sharedWith = sharedWith;
        this.paidByName = paidByName;
        this.addedByName = addedByName;
        this.sharedWithNames = sharedWithNames;
    }

    // Constructor without sharedWithNames (for simplicity in some cases)
    public Expense(int id, int groupId, int paidBy, int addedBy, double amount, String description, String date,
                   List<Integer> sharedWith, String paidByName, String addedByName) {
        this(id, groupId, paidBy, addedBy, amount, description, date, sharedWith, paidByName, addedByName, null);
    }

    // Constructor without names (for simplicity in some cases)
    public Expense(int id, int groupId, int paidBy, int addedBy, double amount, String description, String date,
                   List<Integer> sharedWith) {
        this(id, groupId, paidBy, addedBy, amount, description, date, sharedWith, null, null, null);
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

    public String getPaidByName() {
        return paidByName;
    }

    public void setPaidByName(String paidByName) {
        this.paidByName = paidByName;
    }

    public String getAddedByName() {
        return addedByName;
    }

    public void setAddedByName(String addedByName) {
        this.addedByName = addedByName;
    }

    public String getSharedWithNames() {
        return sharedWithNames;
    }

    public void setSharedWithNames(String sharedWithNames) {
        this.sharedWithNames = sharedWithNames;
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
                ", paidByName='" + paidByName + '\'' +
                ", addedByName='" + addedByName + '\'' +
                ", sharedWithNames='" + sharedWithNames + '\'' +
                '}';
    }
}