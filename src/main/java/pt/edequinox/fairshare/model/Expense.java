package pt.edequinox.fairshare.model;

public class Expense {

    public String paidBy;
    public Group group;
    public double value;
    public String date;

    public void changeInfo(String name, String description, double value, String group, String date) {
        this.paidBy = name;
        this.value = value;
        this.date = date;
    }

    public void printInfo() {
        System.out.println("Name: " + paidBy);
        System.out.println("Value: " + value);
        System.out.println("Group: " + group);
        System.out.println("Date: " + date);
    }
}
