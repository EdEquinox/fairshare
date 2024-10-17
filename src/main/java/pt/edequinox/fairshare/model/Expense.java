package pt.edequinox.fairshare.model;

public class Expense {

    public String name;
    public String description;
    public double value;
    public String group;
    public String date;

    public void changeInfo(String name, String description, double value, String group, String date) {
        this.name = name;
        this.description = description;
        this.value = value;
        this.group = group;
        this.date = date;
    }

    public void printInfo() {
        System.out.println("Name: " + name);
        System.out.println("Description: " + description);
        System.out.println("Value: " + value);
        System.out.println("Group: " + group);
        System.out.println("Date: " + date);
    }
}
