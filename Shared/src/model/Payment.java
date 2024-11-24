package model;

public class Payment {
    private int id;
    private int groupId;
    private int paidBy;
    private int receivedBy;
    private double amount;
    private String date;
    private String paidByName; // Nome do pagador
    private String receivedByName; // Nome do recebedor

    // Construtor completo
    public Payment(int id, int groupId, int paidBy, int receivedBy, double amount, String date, String paidByName, String receivedByName) {
        this.id = id;
        this.groupId = groupId;
        this.paidBy = paidBy;
        this.receivedBy = receivedBy;
        this.amount = amount;
        this.date = date;
        this.paidByName = paidByName;
        this.receivedByName = receivedByName;
    }

    // Construtor sem nomes (para cenários onde os nomes não são necessários)
    public Payment(int id, int groupId, int paidBy, int receivedBy, double amount, String date) {
        this(id, groupId, paidBy, receivedBy, amount, date, null, null);
    }

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

    public int getReceivedBy() {
        return receivedBy;
    }

    public void setReceivedBy(int receivedBy) {
        this.receivedBy = receivedBy;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getPaidByName() {
        return paidByName;
    }

    public void setPaidByName(String paidByName) {
        this.paidByName = paidByName;
    }

    public String getReceivedByName() {
        return receivedByName;
    }

    public void setReceivedByName(String receivedByName) {
        this.receivedByName = receivedByName;
    }

    @Override
    public String toString() {
        return "Payment{" +
                "id=" + id +
                ", groupId=" + groupId +
                ", paidBy=" + paidBy +
                ", receivedBy=" + receivedBy +
                ", amount=" + amount +
                ", date='" + date + '\'' +
                ", paidByName='" + paidByName + '\'' +
                ", receivedByName='" + receivedByName + '\'' +
                '}';
    }
}