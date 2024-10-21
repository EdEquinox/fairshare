package pt.edequinox.fairshare.model;

public class Transaction {

        public String sender;
        public String receiver;
        public String group;
        public double amount;

        public void changeInfo(String sender, String receiver, String group, String description, double amount) {
            this.sender = sender;
            this.receiver = receiver;
            this.group = group;
            this.amount = amount;
        }

        public void printInfo() {
            System.out.println("Sender: " + sender);
            System.out.println("Receiver: " + receiver);
            System.out.println("Group: " + group);
            System.out.println("Amount: " + amount);
        }
}
