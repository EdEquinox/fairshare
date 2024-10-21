package pt.edequinox.fairshare.model;

import java.util.Arrays;

public class Group {

        public String name;
        public User[] members;
        public Expense[] expenses;

        public void changeName(String name) {
            this.name = name;
        }

        public void printInfo() {
            System.out.println("Name: " + name);
            System.out.println("Members: " + Arrays.toString(members));
            System.out.println("Expenses: " + Arrays.toString(expenses));
        }
}
