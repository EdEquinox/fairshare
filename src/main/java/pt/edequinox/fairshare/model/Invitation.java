package pt.edequinox.fairshare.model;

public class Invitation {

        public String email;
        public String group;
        public String senderID;
        public String status;

        public void changeInfo(String email, String group, String sender, String status) {
            this.email = email;
            this.group = group;
            this.senderID = sender;
            this.status = status;
        }

        public void printInfo() {
            System.out.println("Email: " + email);
            System.out.println("Group: " + group);
            System.out.println("Sender: " + senderID);
            System.out.println("Status: " + status);
        }
}
