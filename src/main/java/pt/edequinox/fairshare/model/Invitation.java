package pt.edequinox.fairshare.model;

public class Invitation {

        public String email;
        public String group;
        public String sender;
        public String status;

        public void changeInfo(String email, String group, String sender, String status) {
            this.email = email;
            this.group = group;
            this.sender = sender;
            this.status = status;
        }

        public void printInfo() {
            System.out.println("Email: " + email);
            System.out.println("Group: " + group);
            System.out.println("Sender: " + sender);
            System.out.println("Status: " + status);
        }
}
