package controller;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import communication.ClientService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.text.Text;
import model.*;
import resources.components.dialog.ComboBoxOption;
import resources.components.dialog.CustomDialog;
import resources.components.dialog.FieldConfig;
import resources.components.dialog.FieldType;
import utils.*;

import java.io.File;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.*;

public class DashboardController implements Initializable {

    @FXML
    public Label totalSpentLabel;
    @FXML
    public Label amountToPayLabel;
    @FXML
    public ListView payListView;
    @FXML
    public Label amountToReceiveLabel;
    @FXML
    public ListView receiveListView;

    @FXML
    private Text userText;

    @FXML
    private ListView<Group> groupList;

    @FXML
    private ListView<User> userListView;

    @FXML
    private TableView<Expense> expensesTableView;

    // ContextMenu for the TableView
    private ContextMenu expensesContextMenu;

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
        //expenses = FXCollections.observableArrayList();

        groupList.setItems(groups);
        userListView.setItems(usersInGroup);

        initializeContextMenu();

        // Add a listener for right-click events
        expensesTableView.setOnContextMenuRequested(event -> {
            Expense selectedExpense = expensesTableView.getSelectionModel().getSelectedItem();

            // Enable/Disable context menu items based on the selection
            configureContextMenuItems(selectedExpense);

            // Show the context menu
            expensesContextMenu.show(expensesTableView, event.getScreenX(), event.getScreenY());
        });

        // Hide context menu on left-click
        expensesTableView.setOnMouseClicked(event -> {
            if (event.isPrimaryButtonDown()) {
                expensesContextMenu.hide();
            }
        });

        //dateColumn.setCellValueFactory(new PropertyValueFactory<>("date"));
        //descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        //amountColumn.setCellValueFactory(new PropertyValueFactory<>("amount"));
        //paidByColumn.setCellValueFactory(new PropertyValueFactory<>("paidBy"));

        // Listener for group selection
        /*groupList.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
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
*/
        // Fetch initial group data
        fetchGroups();
    }

    private void initializeContextMenu() {
        expensesContextMenu = new ContextMenu();

        MenuItem addExpenseItem = new MenuItem("Add Expense");
        addExpenseItem.setOnAction(event -> handleNewExpense());

        MenuItem editExpenseItem = new MenuItem("Edit Expense");
        editExpenseItem.setOnAction(event -> handleEditExpense());

        MenuItem removeExpenseItem = new MenuItem("Remove Expense");
        removeExpenseItem.setOnAction(event -> handleDeleteExpense());

        MenuItem exportToCsvItem = new MenuItem("Export to CSV");
        exportToCsvItem.setOnAction(event -> handleExportToCSV());

        expensesContextMenu.getItems().addAll(addExpenseItem, editExpenseItem, removeExpenseItem, exportToCsvItem);
    }

    private void configureContextMenuItems(Expense selectedExpense) {
        // Enable/Disable "Edit" and "Remove" based on selection
        expensesContextMenu.getItems().get(1).setDisable(selectedExpense == null); // Edit
        expensesContextMenu.getItems().get(2).setDisable(selectedExpense == null); // Remove

        // Enable/Disable "Export to CSV" based on table state
        expensesContextMenu.getItems().get(3).setDisable(expensesTableView.getItems().isEmpty()); // Export to CSV
    }

    private void fetchGroups() {
        new Thread(() -> {
            if (clientService.isClientConnected()) {
                ServerResponse response = clientService.sendRequest(new Message(Message.Type.GET_GROUPS, currentUser));
                javafx.application.Platform.runLater(() -> {
                    if (response.isSuccess()) {
                        try {
                            Type groupListType = new TypeToken<List<Group>>() {
                            }.getType();
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
                            Type userListType = new TypeToken<List<User>>() {
                            }.getType();
                            Logger.info("Server payload: " + gson.toJson(response.payload())); // Log the raw payload

                            List<User> users = gson.fromJson(gson.toJson(response.payload()), userListType);
                            Logger.info("Deserialized users: " + users);

                            // Update the observable list with User objects
                            usersInGroup.setAll(users);

                            // Set the cell factory to display usernames in the ListView
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
                            Type expenseListType = new TypeToken<List<Expense>>() {
                            }.getType();
                            List<Expense> fetchedExpenses = gson.fromJson(gson.toJson(response.payload()), expenseListType);

                            // Clear and set fetched expenses in the table
                            expenses.setAll(fetchedExpenses);

                            Logger.info("Expenses fetched successfully for group: " + group.name());
                        } catch (Exception e) {
                            Logger.error("Failed to deserialize expenses: " + e.getMessage());
                            AlertUtils.showError("Error", "Failed to parse server response for expenses.");
                        }
                    } else {
                        // Clear the table and allow adding new expenses
                        expenses.clear();

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
                            Type expenseListType = new TypeToken<List<Expense>>() {
                            }.getType();
                            List<Expense> fetchedExpenses = gson.fromJson(gson.toJson(response.payload()), expenseListType);

                            // Clear and set fetched expenses in the table
                            expenses.setAll(fetchedExpenses);

                            Logger.info("Expenses fetched successfully for user: " + user.getName());
                        } catch (Exception e) {
                            Logger.error("Failed to deserialize expenses: " + e.getMessage());
                            AlertUtils.showError("Error", "Failed to parse server response.");
                        }
                    } else {
                        // Instead of showing a warning, just clear the expenses table
                        expenses.clear();

                        Logger.info("No expenses found for user: " + user.getName());
                    }
                });
            }
        }).start();
    }

    @FXML
    public void handleNewExpense() {
        List<ComboBoxOption> userOptions = usersInGroup.stream()
                .map(user -> new ComboBoxOption(user.getId(), user.getName()))
                .toList();

        // Define os campos para o diálogo
        List<FieldConfig> fields = Arrays.asList(
                new FieldConfig<>("date", "Date", null, FieldType.DATE, true, null, null),
                new FieldConfig<>("description", "Description", "Enter expense description", FieldType.TEXT, true, null, null),
                new FieldConfig<>("amount", "Value", "Enter the amount", FieldType.TEXT, true, null, null),
                new FieldConfig<>("paidBy", "Paid By", "Select who paid", FieldType.COMBOBOX, true, null, userOptions),
                new FieldConfig<>("sharedWith", "Shared With", "Select users to share with", FieldType.MULTI_SELECT, true, null, userOptions)
        );

        Dialog<Map<String, Object>> dialog = CustomDialog.createDialog("Add New Expense", fields);

        Optional<Map<String, Object>> result = dialog.showAndWait();
        result.ifPresent(data -> {
            try {
                String date = (String) data.get("date");
                String description = (String) data.get("description");
                double amount = Double.parseDouble((String) data.get("amount"));
                ComboBoxOption paidBy = (ComboBoxOption) data.get("paidBy");
                List<ComboBoxOption> sharedWith = (List<ComboBoxOption>) data.get("sharedWith");

                if (paidBy == null || sharedWith.isEmpty()) {
                    AlertUtils.showError("Invalid Data", "Please select a payer and at least one user to share with.");
                    return;
                }

                List<Integer> sharedWithIds = sharedWith.stream()
                        .map(ComboBoxOption::getId)
                        .toList();

                Expense newExpense = new Expense(
                        0, // ID será definido pelo servidor
                        selectedGroup.getId(),
                        paidBy.getId(),
                        currentUser.getId(),
                        amount,
                        description,
                        date,
                        sharedWithIds
                );

                // Envia a nova despesa ao servidor
                new Thread(() -> {
                    ServerResponse response = clientService.sendRequest(new Message(Message.Type.ADD_EXPENSE, newExpense));
                    javafx.application.Platform.runLater(() -> {
                        if (response.isSuccess()) {
                            expenses.add(newExpense);
                            AlertUtils.showSuccess("Success", "Expense added successfully!");
                        } else {
                            AlertUtils.showError("Error", "Failed to add expense: " + response.message());
                        }
                    });
                }).start();
            } catch (Exception e) {
                AlertUtils.showError("Error", "Invalid data provided: " + e.getMessage());
            }
        });
    }

    private void sendExpenseToServer(Expense expense) {
        new Thread(() -> {
            ServerResponse response = clientService.sendRequest(new Message(Message.Type.ADD_EXPENSE, expense));
            javafx.application.Platform.runLater(() -> {
                if (response.isSuccess()) {
                    Logger.info("Expense successfully added to server: " + expense);
                    AlertUtils.showSuccess("Success", "Expense added successfully.");
                } else {
                    AlertUtils.showError("Error", "Failed to add expense to server: " + response.message());
                    Logger.error("Failed to add expense to server: " + response.message());
                }
            });
        }).start();
    }

    @FXML
    public void handleEditExpense() {
        Expense selectedExpense = null;
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
        Expense selectedExpense = null;
        if (selectedExpense != null) {
            // Send delete request to the server
            ServerResponse response = clientService.sendRequest(new Message(Message.Type.DELETE_EXPENSE, selectedExpense.getId()));
            if (response.isSuccess()) {
                expenses.remove(selectedExpense);
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
        List<FieldConfig> fields = Arrays.asList(
                new FieldConfig<>("name", "Name", "Enter your name", FieldType.TEXT, true, currentUser.getName(), null),
                new FieldConfig<>("email", "Email", null, FieldType.TEXT, false, currentUser.getEmail(), null),
                new FieldConfig<>("phone", "Phone", "Enter your phone", FieldType.TEXT, true, currentUser.getPhone(), null),
                new FieldConfig<>("password", "Password", "Enter your new password", FieldType.PASSWORD, true, null, null)
        );

        Dialog<Map<String, Object>> dialog = CustomDialog.createDialog("Edit Profile", fields);

        Optional<Map<String, Object>> result = dialog.showAndWait();
        result.ifPresent(data -> {
            String name = (String) data.get("name");
            String email = (String) data.get("email");
            String phone = (String) data.get("phone");
            String password = (String) data.get("password");

            if (name.isEmpty() || email.isEmpty() || phone.isEmpty()) {
                AlertUtils.showError("Invalid Fields", "Please fill in all fields.");
                return;
            }

            String encodedPassword = password != null ? Base64.getEncoder().encodeToString(password.getBytes()) : currentUser.getPassword();

            new Thread(() -> {
                ServerResponse response = clientService.sendRequest(
                        new Message(Message.Type.EDIT_PROFILE, new User(currentUser.getId(), name, email, phone, encodedPassword))
                );

                javafx.application.Platform.runLater(() -> {
                    if (response.isSuccess()) {
                        AlertUtils.showSuccess("Success", "Your profile has been updated.");
                        NavigationManager.switchScene(Routes.DASHBOARD);
                    } else {
                        AlertUtils.showError("Error", response.message());
                    }
                });
            }).start();
        });
    }

    @FXML
    public void handleNewGroup() {
        List<FieldConfig> fields = Collections.singletonList(
                new FieldConfig<>("groupName", "Group Name", "Enter the group name", FieldType.TEXT, true, null, null)
        );

        Dialog<Map<String, Object>> dialog = CustomDialog.createDialog("Create New Group", fields);

        Optional<Map<String, Object>> result = dialog.showAndWait();

        result.ifPresent(data -> {
            String groupName = (String) data.get("groupName");

            if (groupName == null || groupName.isBlank()) {
                AlertUtils.showError("Invalid Group Name", "Please enter a valid group name.");
                return;
            }

            new Thread(() -> {
                ServerResponse response = clientService.sendRequest(
                        new Message(Message.Type.CREATE_GROUP, new Group(groupName, currentUser.getId()))
                );

                javafx.application.Platform.runLater(() -> {
                    if (response.isSuccess()) {
                        AlertUtils.showSuccess("Success", "Group '" + groupName + "' has been created successfully.");
                        fetchGroups(); // Atualiza a lista de grupos
                    } else {
                        AlertUtils.showError("Error", "Failed to create group: " + response.message());
                    }
                });
            }).start();
        });
    }

    @FXML
    public void handleGroupInvites() {
        NavigationManager.switchScene(Routes.INVITE);
    }

    @FXML
    public void handleLogout() {
        NavigationManager.switchScene(Routes.HOME);
    }
}
