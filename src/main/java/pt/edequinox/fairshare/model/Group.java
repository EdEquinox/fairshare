package pt.edequinox.fairshare.model;

public class Group {

        public String name;
        public String description;
        public String password;

        public void changeInfo(String name, String description, String password) {
            this.name = name;
            this.description = description;
            this.password = password;
        }

        public void printInfo() {
            System.out.println("Name: " + name);
            System.out.println("Description: " + description);
            System.out.println("Password: " + password);
        }
}
