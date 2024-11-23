package controller;

import communication.ClientService;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.text.Text;
import model.Expense;
import model.Group;
import model.Message;
import model.ServerResponse;
import model.User;
import utils.AlertUtils;
import utils.CSVUtils;
import utils.Logger;
import utils.NavigationManager;
import utils.Routes;

import java.io.File;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class DashboardController implements Initializable {

    @FXML
    private Text userText;

    @FXML
    private ListView<Group> groupList;

    @FXML
    private ListView<User> userListView;


    @FXML
    private TableView<Expense> expensesTable;

    @FXML
    private TableColumn<Expense, String> dateColumn;

    @FXML
    private TableColumn<Expense, String> descriptionColumn;

    @FXML
    private TableColumn<Expense, Double> amountColumn;

    @FXML
    private TableColumn<Expense, String> paidByColumn;

    @FXML
    private Label totalExpensesLabel;

    private ClientService clientService;
    private ObservableList<Group> groups;
    private ObservableList<User> usersInGroup;
    private ObservableList<Expense> expenses;

    private User currentUser;
    private Group selectedGroup;

    private final Gson gson = new Gson();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        clientService = ClientService.getInstance();
        currentUser = clientService.getCurrentUser();

        userText.setText(currentUser.getName());

        groups = FXCollections.observableArrayList();
        usersInGroup = FXCollections.observableArrayList();
        expenses = FXCollections.observableArrayList();

        groupList.setItems(groups);
        userListView.setItems(usersInGroup);
        expensesTable.setItems(expenses);

        dateColumn.setCellValueFactory(new PropertyValueFactory<>("date"));
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        amountColumn.setCellValueFactory(new PropertyValueFactory<>("amount"));
        paidByColumn.setCellValueFactory(new PropertyValueFactory<>("paidBy"));

        // Listener for group selection
        groupList.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                handleGroupSelection();
            }
        });

        // Listener for user selection
        userListView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                fetchExpensesForUser(newValue); // Pass the selected User object
            }
        });


        // Configure the cell factory to display user names in the userListView
        userListView.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(User user, boolean empty) {
                super.updateItem(user, empty);
                if (empty || user == null) {
                    setText(null);
                } else {
                    setText(user.getName()); // Display user name
                }
            }
        });

        // Fetch initial group data
        fetchGroups();
    }



    private void fetchGroups() {
        new Thread(() -> {
            if (clientService.isClientConnected()) {
                ServerResponse response = clientService.sendRequest(new Message(Message.Type.GET_GROUPS, currentUser));
                javafx.application.Platform.runLater(() -> {
                    if (response.isSuccess()) {
                        try {
                            Type groupListType = new TypeToken<List<Group>>() {}.getType();
                            List<Group> fetchedGroups = gson.fromJson(gson.toJson(response.payload()), groupListType);
                            groups.setAll(fetchedGroups);
                            Logger.info("Groups fetched successfully for user: " + currentUser.getName());
                        } catch (Exception e) {
                            Logger.error("Failed to deserialize groups: " + e.getMessage());
                            AlertUtils.showError("Error", "Invalid server response format.");
                        }
                    } else {
                        AlertUtils.showError("Error", "Failed to fetch groups: " + response.message());
                        Logger.error("Failed to fetch groups for user: " + currentUser.getName());
                    }
                });
            } else {
                javafx.application.Platform.runLater(() -> {
                    AlertUtils.showError("Error", "Client is not connected to the server.");
                    Logger.error("Client is not connected to the server.");
                });
            }
        }).start();
    }

    @FXML
    public void handleGroupSelection() {
        Group selectedGroup = groupList.getSelectionModel().getSelectedItem();
        if (selectedGroup != null) {
            this.selectedGroup = selectedGroup;
            fetchUsersForGroup(selectedGroup);
            fetchExpensesForGroup(selectedGroup);
        } else {
            Logger.error("No group selected.");
        }
    }

    private void fetchUsersForGroup(Group group) {
        new Thread(() -> {
            if (clientService.isClientConnected()) {
                ServerResponse response = clientService.sendRequest(new Message(Message.Type.GET_USERS_FOR_GROUP, group));
                javafx.application.Platform.runLater(() -> {
                    if (response.isSuccess()) {
                        try {
                            // Deserialize the response payload into a list of User objects
                            Type userListType = new TypeToken<List<User>>() {}.getType();
                            Logger.info("Server payload: " + gson.toJson(response.payload())); // Log the raw payload

                            List<User> users = gson.fromJson(gson.toJson(response.payload()), userListType);
                            Logger.info("Deserialized users: " + users);

                            // Update the observable list with User objects
                            usersInGroup.setAll(users);

                            // Set the cell factory to display user names in the ListView
                            userListView.setItems(usersInGroup);
                            userListView.setCellFactory(param -> new ListCell<User>() {
                                @Override
                                protected void updateItem(User user, boolean empty) {
                                    super.updateItem(user, empty);
                                    if (empty || user == null) {
                                        setText(null);
                                    } else {
                                        setText(user.getName()); // Display the user's name
                                    }
                                }
                            });

                            Logger.info("Users fetched successfully for group: " + group.name());
                        } catch (Exception e) {
                            Logger.error("Failed to deserialize users: " + e.getMessage());
                            AlertUtils.showError("Error", "Failed to parse server response for users.");
                        }
                    } else {
                        AlertUtils.showError("Error", "Failed to fetch users: " + response.message());
                        Logger.error("Failed to fetch users for group: " + group.name());
                    }
                });
            }
        }).start();
    }



    private void fetchExpensesForGroup(Group group) {
        new Thread(() -> {
            if (clientService.isClientConnected()) {
                ServerResponse response = clientService.sendRequest(new Message(Message.Type.GET_EXPENSES, group));
                javafx.application.Platform.runLater(() -> {
                    if (response.isSuccess()) {
                        try {
                            Type expenseListType = new TypeToken<List<Expense>>() {}.getType();
                            List<Expense> fetchedExpenses = gson.fromJson(gson.toJson(response.payload()), expenseListType);

                            // Clear and set fetched expenses in the table
                            expenses.setAll(fetchedExpenses);
                            updateTotalExpenses();

                            Logger.info("Expenses fetched successfully for group: " + group.name());
                        } catch (Exception e) {
                            Logger.error("Failed to deserialize expenses: " + e.getMessage());
                            AlertUtils.showError("Error", "Failed to parse server response for expenses.");
                        }
                    } else {
                        // Clear the table and allow adding new expenses
                        expenses.clear();
                        updateTotalExpenses();

                        Logger.info("No expenses found for group: " + group.name());
                    }
                });
            }
        }).start();
    }


    private void fetchExpensesForUser(User user) {
        if (user == null) {
            Logger.error("No user selected for fetching expenses.");
            AlertUtils.showError("Error", "Please select a user first.");
            return;
        }

        new Thread(() -> {
            if (clientService.isClientConnected()) {
                ServerResponse response = clientService.sendRequest(new Message(Message.Type.GET_EXPENSES_USER, user.getId()));
                javafx.application.Platform.runLater(() -> {
                    if (response.isSuccess()) {
                        try {
                            Type expenseListType = new TypeToken<List<Expense>>() {}.getType();
                            List<Expense> fetchedExpenses = gson.fromJson(gson.toJson(response.payload()), expenseListType);

                            // Clear and set fetched expenses in the table
                            expenses.setAll(fetchedExpenses);
                            updateTotalExpenses();

                            Logger.info("Expenses fetched successfully for user: " + user.getName());
                        } catch (Exception e) {
                            Logger.error("Failed to deserialize expenses: " + e.getMessage());
                            AlertUtils.showError("Error", "Failed to parse server response.");
                        }
                    } else {
                        // Instead of showing a warning, just clear the expenses table
                        expenses.clear();
                        updateTotalExpenses();

                        Logger.info("No expenses found for user: " + user.getName());
                    }
                });
            }
        }).start();
    }



    private void updateTotalExpenses() {
        double total = expenses.stream().mapToDouble(Expense::getAmount).sum();
        totalExpensesLabel.setText(String.format("Total Expenses: %.2f€", total));
    }

    @FXML
    public void handleAddExpense() {
        if (selectedGroup == null) {
            AlertUtils.showError("Error", "Please select a group before adding an expense.");
            Logger.error("No group selected for adding an expense.");
            return;
        }

        User selectedUser = userListView.getSelectionModel().getSelectedItem();
        if (selectedUser == null) {
            AlertUtils.showError("Error", "Please select a user before adding an expense.");
            Logger.error("No user selected for adding an expense.");
            return;
        }

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Add Expense");
        dialog.setHeaderText("Add a new expense for the selected user and group");
        dialog.setContentText("Enter expense details (format: date,description,amount):");

        dialog.showAndWait().ifPresent(input -> {
            try {
                String[] details = input.split(",");
                if (details.length != 3) {
                    throw new IllegalArgumentException("Invalid input format. Use: date,description,amount");
                }

                String date = details[0].trim();
                String description = details[1].trim();
                double amount = Double.parseDouble(details[2].trim());

                // Create a new expense with 'addedBy'
                Expense newExpense = new Expense(
                        (int) (Math.random() * 100000), // Temporary unique ID
                        selectedGroup.getId(),          // Group ID
                        selectedUser.getId(),           // Paid By (User ID)
                        currentUser.getId(),            // Added By (Current User ID)
                        amount,
                        description,
                        date
                );

                // Add expense locally to the table
                expenses.add(newExpense);
                updateTotalExpenses();

                // Send the expense to the server
                sendAddExpenseToServer(newExpense);

                Logger.info("New expense added: " + newExpense);
            } catch (Exception e) {
                AlertUtils.showError("Error", "Failed to add expense: " + e.getMessage());
                Logger.error("Failed to add expense: " + e.getMessage());
            }
        });
    }

    // Helper method to send the expense to the server
    private void sendAddExpenseToServer(Expense expense) {
        new Thread(() -> {
            if (clientService.isClientConnected()) {
                ServerResponse response = clientService.sendRequest(new Message(Message.Type.ADD_EXPENSE, expense));
                javafx.application.Platform.runLater(() -> {
                    if (response.isSuccess()) {
                        Logger.info("Expense successfully added to the server: " + expense);
                    } else {
                        AlertUtils.showError("Error", "Failed to add expense to the server: " + response.message());
                        Logger.error("Failed to add expense to the server: " + response.message());
                    }
                });
            } else {
                javafx.application.Platform.runLater(() -> {
                    AlertUtils.showError("Error", "Client is not connected to the server.");
                    Logger.error("Client is not connected to the server.");
                });
            }
        }).start();
    }





    @FXML
    public void handleEditExpense() {
        Expense selectedExpense = expensesTable.getSelectionModel().getSelectedItem();
        if (selectedExpense != null) {
            TextInputDialog dialog = new TextInputDialog(selectedExpense.getDescription());
            dialog.setTitle("Edit Expense");
            dialog.setHeaderText("Edit the selected expense");
            dialog.setContentText("Enter new details (format: date,description,amount):");

            Optional<String> result = dialog.showAndWait();
            result.ifPresent(input -> {
                try {
                    String[] details = input.split(",");
                    if (details.length != 3) {
                        throw new IllegalArgumentException("Invalid input format. Use: date,description,amount");
                    }

                    selectedExpense.setDate(details[0].trim());
                    selectedExpense.setDescription(details[1].trim());
                    selectedExpense.setAmount(Double.parseDouble(details[2].trim()));

                    // Send updated expense to the server
                    ServerResponse response = clientService.sendRequest(new Message(Message.Type.EDIT_EXPENSE, selectedExpense));
                    if (response.isSuccess()) {
                        expensesTable.refresh();
                        updateTotalExpenses();
                        Logger.info("Expense updated: " + selectedExpense);
                    } else {
                        AlertUtils.showError("Error", "Failed to edit expense: " + response.message());
                    }

                } catch (Exception e) {
                    AlertUtils.showError("Error", "Failed to edit expense: " + e.getMessage());
                    Logger.error("Failed to edit expense: " + e.getMessage());
                }
            });
        } else {
            AlertUtils.showError("Error", "No expense selected for editing.");
        }
    }


    @FXML
    public void handleDeleteExpense() {
        Expense selectedExpense = expensesTable.getSelectionModel().getSelectedItem();
        if (selectedExpense != null) {
            // Send delete request to the server
            ServerResponse response = clientService.sendRequest(new Message(Message.Type.DELETE_EXPENSE, selectedExpense.getId()));
            if (response.isSuccess()) {
                expenses.remove(selectedExpense);
                updateTotalExpenses();
                Logger.info("Expense deleted: " + selectedExpense);
            } else {
                AlertUtils.showError("Error", "Failed to delete expense: " + response.message());
            }
        } else {
            AlertUtils.showError("Error", "No expense selected for deletion.");
        }
    }


    @FXML
    public void handleExportToCSV() {
        if (expenses.isEmpty()) {
            AlertUtils.showError("Error", "No expenses to export.");
            return;
        }

        try {
            String projectDir = System.getProperty("user.dir");
            File exportDir = new File(projectDir, "out/exports");
            if (!exportDir.exists() && !exportDir.mkdirs()) {
                Logger.error("Failed to create 'exports' directory.");
                AlertUtils.showError("Error", "Could not create 'exports' directory.");
                return;
            }

            File file = new File(exportDir, "expenses.csv");
            CSVUtils.exportToCSV(expenses, file.getCanonicalPath());
            Logger.info("CSV file saved at: " + file.getCanonicalPath());
            AlertUtils.showSuccess("Success", "Expenses exported to " + file.getCanonicalPath());
        } catch (Exception e) {
            Logger.error("Error exporting expenses to CSV: " + e.getMessage());
            AlertUtils.showError("Error", "Failed to export expenses to CSV.");
        }
    }

    @FXML
    public void handleEditProfile() {
        NavigationManager.switchScene(Routes.EDIT_PROFILE);
    }

    @FXML
    public void handleAddNewGroup() {
        NavigationManager.switchScene(Routes.CREATE_GROUP);
    }

    @FXML
    public void handleGroupInvites() {
        NavigationManager.switchScene(Routes.INVITE);
    }


    @FXML
    public void handleLogout(ActionEvent actionEvent) {
        NavigationManager.switchScene(Routes.HOME);
    }
}
