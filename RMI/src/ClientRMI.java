import model.Message;
import model.ServerResponse;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.List;
import java.util.Scanner;

public class ClientRMI {
    public static void main(String[] args) {
        try {
            Registry registry = LocateRegistry.getRegistry("localhost");
            IServerRmiService server = (IServerRmiService) registry.lookup("Server");

            ClientRmiService client = new ClientRmiService();
            server.registerClient(client);

            Thread notificationThread = new Thread(() -> {
                try {
                    while (true) {
                        Thread.sleep(5000);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            notificationThread.start();

            Scanner scanner = new Scanner(System.in);
            boolean running = true;

            while (running) {
                System.out.println("Menu:");
                System.out.println("1 - List users");
                System.out.println("2 - List groups");
                System.out.println("3 - Exit");
                System.out.println("Choose an option:");
                int option = scanner.nextInt();
                scanner.nextLine();

                switch (option) {
                    case 1:
                        List<String> users = server.listUsers();
                        System.out.println("Users: " + users);
                        break;
                    case 2:
                        List<String> groups = server.listGroups();
                        System.out.println("Groups: " + groups);
                        break;
                    case 3:
                        running = false;
                        server.unregisterClient(client);
                        notificationThread.interrupt();
                        break;
                    default:
                        System.out.println("Invalid option");
                        break;
                }
            }


        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}