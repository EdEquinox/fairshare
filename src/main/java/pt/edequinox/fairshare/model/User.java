package pt.edequinox.fairshare.model;

public class User {

    public String name;
    public String email;
    public String password;
    public String phone;

    public static void main(String[] args) {
        System.out.println("Hello world!");
    }

    public void changeInfo(String name, String email, String password, String phone) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.phone = phone;
    }

    public void printInfo() {
        System.out.println("Name: " + name);
        System.out.println("Email: " + email);
        System.out.println("Password: " + password);
        System.out.println("Phone: " + phone);
    }

}

