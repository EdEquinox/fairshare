package controller;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import communication.ClientService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import model.*;
import resources.components.dialog.ComboBoxOption;
import resources.components.dialog.CustomDialog;
import resources.components.dialog.FieldConfig;
import resources.components.dialog.FieldType;
import utils.*;

import java.io.File;
import java.lang.reflect.Type;
import java.net.URL;
import java.time.LocalDate;
import java.util.*;

public class DashboardController implements Initializable {

    public Text userText, groupText;
    public TableColumn paymentsPaidByColumn, paymentsReceivedByColumn, paymentsDateColumn, paymentsValueColumn;
    @FXML
    private VBox mainContent;

    @FXML
    private Label totalSpentLabel, amountToPayLabel, amountToReceiveLabel;

    @FXML
    private ListView<Group> groupList;

    @FXML
    private ListView<User> userListView;

    @FXML
    private TableView<Expense> expensesTableView;

    @FXML
    private TableView<Payment> paymentsTableView;

    private ContextMenu groupContextMenu, expensesContextMenu, paymentsContextMenu;

    private ClientService clientService;

    private ObservableList<Group> groups = FXCollections.observableArrayList();
    private ObservableList<User> usersInGroup = FXCollections.observableArrayList();
    private ObservableList<Expense> expenses = FXCollections.observableArrayList();
    private ObservableList<Payment> payments = FXCollections.observableArrayList();

    private User currentUser;
    private Group selectedGroup;
    private Expense selectedExpense;
    private Payment selectedPayment;

    private final Gson gson = new Gson();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        clientService = ClientService.getInstance();
        currentUser = clientService.getCurrentUser();
        userText.setText(currentUser.getName());

        // Hide main content until a group is selected
        toggleMainContent(false);

        initializeContextMenus();
        configureGroupList();
        configureExpensesTableView();
        configurePaymentsTableView();
        fetchGroups();
    }

    private void toggleMainContent(boolean visible) {
        mainContent.setVisible(visible);
        mainContent.setManaged(visible);
    }

    private void initializeContextMenus() {
        groupContextMenu = createGroupContextMenu();
        expensesContextMenu = createExpensesContextMenu();
        paymentsContextMenu = createPaymentsContextMenu();
    }

    private ContextMenu createGroupContextMenu() {
        MenuItem editGroup = new MenuItem("Edit Group Name");
        editGroup.setOnAction(event -> handleEditGroup());

        MenuItem removeGroup = new MenuItem("Remove Group");
        removeGroup.setOnAction(event -> handleRemoveGroup());

        return new ContextMenu(editGroup, removeGroup);
    }

    private ContextMenu createExpensesContextMenu() {
        MenuItem addExpense = new MenuItem("Add Expense");
        addExpense.setOnAction(event -> handleNewExpense());

        MenuItem editExpense = new MenuItem("Edit Expense");
        editExpense.setOnAction(event -> handleEditExpense());

        MenuItem deleteExpense = new MenuItem("Remove Expense");
        deleteExpense.setOnAction(event -> handleDeleteExpense());

        MenuItem exportExpenses = new MenuItem("Export to CSV");
        exportExpenses.setOnAction(event -> handleExportToCSV("expenses"));

        return new ContextMenu(addExpense, editExpense, deleteExpense, exportExpenses);
    }

    private ContextMenu createPaymentsContextMenu() {
        MenuItem addPayment = new MenuItem("Add Payment");
        addPayment.setOnAction(event -> handleNewPayment());

        MenuItem editPayment = new MenuItem("Edit Payment");
        editPayment.setOnAction(event -> handleEditPayment());

        MenuItem deletePayment = new MenuItem("Remove Payment");
        deletePayment.setOnAction(event -> handleDeletePayment());

        MenuItem exportPayments = new MenuItem("Export to CSV");
        exportPayments.setOnAction(event -> handleExportToCSV("payments"));

        return new ContextMenu(addPayment, editPayment, deletePayment, exportPayments);
    }

    private void configureGroupList() {
        groupList.setItems(groups);

        // Listener para seleção de grupo
        groupList.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            handleGroupSelection(newValue);
            groupText.setText(selectedGroup.getName());
        });

        // Clique do botão direito para exibir ContextMenu
        groupList.setOnMousePressed(event -> {
            if (event.isSecondaryButtonDown()) { // Clique direito
                Group selectedGroup = groupList.getSelectionModel().getSelectedItem();
                if (selectedGroup != null) {
                    groupContextMenu.show(groupList, event.getScreenX(), event.getScreenY());
                }
            } else if (event.isPrimaryButtonDown()) { // Clique esquerdo
                groupContextMenu.hide();
            }
        });
    }

    private void configureExpensesTableView() {
        // Set items to the observable list of expenses
        expensesTableView.setItems(expenses);

        // Configurar colunas
        TableColumn<Expense, String> dateColumn = (TableColumn<Expense, String>) expensesTableView.getColumns().get(0);
        dateColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getDate()));

        TableColumn<Expense, String> descriptionColumn = (TableColumn<Expense, String>) expensesTableView.getColumns().get(1);
        descriptionColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getDescription()));

        TableColumn<Expense, String> valueColumn = (TableColumn<Expense, String>) expensesTableView.getColumns().get(2);
        valueColumn.setCellValueFactory(cellData -> new SimpleStringProperty(String.valueOf(cellData.getValue().getAmount())));

        TableColumn<Expense, String> paidByColumn = (TableColumn<Expense, String>) expensesTableView.getColumns().get(3);
        paidByColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getPaidByName()));

        TableColumn<Expense, String> sharedWithColumn = (TableColumn<Expense, String>) expensesTableView.getColumns().get(4);
        sharedWithColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getSharedWithNames()));

        expensesTableView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            selectedExpense = newValue; // Atualiza a despesa selecionada
        });

        // Adicionar evento de clique do botão direito
        expensesTableView.setOnMousePressed(event -> {
            if (event.isSecondaryButtonDown()) {
                Expense selectedExpense = expensesTableView.getSelectionModel().getSelectedItem();
                configureContextMenuItems(selectedExpense);
                expensesContextMenu.show(expensesTableView, event.getScreenX(), event.getScreenY());
            } else if (event.isPrimaryButtonDown()) {
                expensesContextMenu.hide();
            }
        });
    }

    private void configurePaymentsTableView() {
        // Set items to the observable list of payments
        paymentsTableView.setItems(payments);

        // Configurar colunas
        TableColumn<Payment, String> paidByColumn = (TableColumn<Payment, String>) paymentsTableView.getColumns().get(0);
        paidByColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getPaidByName()));

        TableColumn<Payment, String> receivedByColumn = (TableColumn<Payment, String>) paymentsTableView.getColumns().get(1);
        receivedByColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getReceivedByName()));

        TableColumn<Payment, String> dateColumn = (TableColumn<Payment, String>) paymentsTableView.getColumns().get(2);
        dateColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getDate()));

        TableColumn<Payment, String> valueColumn = (TableColumn<Payment, String>) paymentsTableView.getColumns().get(3);
        valueColumn.setCellValueFactory(cellData -> new SimpleStringProperty(String.format("%.2f", cellData.getValue().getAmount())));

        // Atualiza o pagamento selecionado
        paymentsTableView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            selectedPayment = newValue; // Atualiza o pagamento selecionado
        });

        // Adicionar evento de clique do botão direito
        paymentsTableView.setOnMousePressed(event -> {
            if (event.isSecondaryButtonDown()) {
                Payment selectedPayment = paymentsTableView.getSelectionModel().getSelectedItem();
                configurePaymentsContextMenuItems(selectedPayment);
                paymentsContextMenu.show(paymentsTableView, event.getScreenX(), event.getScreenY());
            } else if (event.isPrimaryButtonDown()) {
                paymentsContextMenu.hide();
            }
        });
    }

    private void configureContextMenuItems(Expense selectedExpense) {
        expensesContextMenu.getItems().get(0).setDisable(false); // Add Expense
        expensesContextMenu.getItems().get(1).setDisable(selectedExpense == null); // Edit Expense
        expensesContextMenu.getItems().get(2).setDisable(selectedExpense == null); // Remove Expense
        expensesContextMenu.getItems().get(3).setDisable(expenses.isEmpty()); // Export to CSV
    }

    private void configurePaymentsContextMenuItems(Payment selectedPayment) {
        paymentsContextMenu.getItems().get(0).setDisable(false); // Add Payment
        paymentsContextMenu.getItems().get(1).setDisable(selectedPayment == null); // Edit Payment
        paymentsContextMenu.getItems().get(2).setDisable(selectedPayment == null); // Remove Payment
        paymentsContextMenu.getItems().get(3).setDisable(payments.isEmpty()); // Export to CSV
    }

    private void handleEditGroup() {
        Group selectedGroup = groupList.getSelectionModel().getSelectedItem();
        if (selectedGroup == null) {
            AlertUtils.showError("No group selected", "Please select a group to edit.");
            return;
        }

        TextInputDialog dialog = new TextInputDialog(selectedGroup.getName());
        dialog.setTitle("Edit Group Name");
        dialog.setHeaderText("Edit the name of the group:");
        dialog.setContentText("Group Name:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(newName -> {
            if (newName.isBlank()) {
                AlertUtils.showError("Invalid Name", "Group name cannot be empty.");
                return;
            }

            selectedGroup.setName(newName);

            new Thread(() -> {
                ServerResponse response = clientService.sendRequest(new Message(Message.Type.EDIT_GROUP, selectedGroup));
                javafx.application.Platform.runLater(() -> {
                    if (response.isSuccess()) {
                        AlertUtils.showSuccess("Group Updated", "Group name updated successfully.");
                        fetchGroups(); // Refresh the list
                    } else {
                        AlertUtils.showError("Update Failed", "Failed to update the group: " + response.message());
                    }
                });
            }).start();
        });
    }

    private void handleRemoveGroup() {
        Group selectedGroup = groupList.getSelectionModel().getSelectedItem();
        if (selectedGroup == null) {
            AlertUtils.showError("No group selected", "Please select a group to remove.");
            return;
        }

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Remove Group");
        confirmation.setHeaderText("Are you sure you want to remove this group?");
        confirmation.setContentText("This action cannot be undone.");

        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            new Thread(() -> {
                ServerResponse response = clientService.sendRequest(new Message(Message.Type.REMOVE_GROUP, selectedGroup));
                javafx.application.Platform.runLater(() -> {
                    if (response.isSuccess()) {
                        groups.remove(selectedGroup);
                        AlertUtils.showSuccess("Group Removed", "Group removed successfully.");
                    } else {
                        AlertUtils.showError("Remove Failed", "Failed to remove the group: " + response.message());
                    }
                });
            }).start();
        }
    }

    private void fetchGroups() {
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
    }

    private void fetchPayments() {
        if (clientService.isClientConnected()) {
            ServerResponse response = clientService.sendRequest(new Message(Message.Type.GET_PAYMENTS, selectedGroup.getId()));

            javafx.application.Platform.runLater(() -> {
                if (response.isSuccess()) {
                    try {
                        Type paymentListType = new TypeToken<List<Payment>>() {
                        }.getType();
                        List<Payment> fetchedPayments = gson.fromJson(gson.toJson(response.payload()), paymentListType);

                        payments.setAll(fetchedPayments);
                        Logger.info("Payments fetched successfully for group: " + selectedGroup.getName());
                    } catch (Exception e) {
                        Logger.error("Failed to parse server response for payments: " + e.getMessage());
                        AlertUtils.showError("Error", "Failed to parse server response for payments.");
                    }
                } else {
                    AlertUtils.showError("Error", "Failed to fetch payments: " + response.message());
                    Logger.error("Failed to fetch payments for group: " + selectedGroup.getName());
                }
            });
        } else {
            javafx.application.Platform.runLater(() -> {
                AlertUtils.showError("Error", "Client is not connected to the server.");
                Logger.error("Client is not connected to the server.");
            });
        }
    }

    @FXML
    public void handleGroupSelection(Group selectedGroup) {
        if (selectedGroup != null) {
            this.selectedGroup = selectedGroup;

            mainContent.setVisible(true);
            mainContent.setManaged(true);

            fetchGroupUsers(selectedGroup);
            fetchExpensesForGroup();
            fetchPayments();
        } else {
            mainContent.setVisible(false);
            mainContent.setManaged(false);
        }
    }

    private void fetchGroupUsers(Group group) {
        if (group == null) {
            AlertUtils.showError("Error", "No group selected.");
            Logger.error("No group selected for fetching users.");
            return;
        }
        if (clientService.isClientConnected()) {
            ServerResponse response = clientService.sendRequest(new Message(Message.Type.GET_GROUP_USERS, group));

            javafx.application.Platform.runLater(() -> {
                if (response.isSuccess()) {
                    try {
                        Type userListType = new TypeToken<List<User>>() {
                        }.getType();
                        List<User> users = gson.fromJson(gson.toJson(response.payload()), userListType);

                        usersInGroup.setAll(users);
                        userListView.setItems(usersInGroup);

                        userListView.setCellFactory(param -> new ListCell<User>() {
                            @Override
                            protected void updateItem(User user, boolean empty) {
                                super.updateItem(user, empty);
                                if (empty || user == null) {
                                    setText(null);
                                } else {
                                    setText(user.getName());
                                }
                            }
                        });

                        Logger.info("Users fetched successfully for group: " + group.getName());
                    } catch (Exception e) {
                        Logger.error("Failed to parse server response for users: " + e.getMessage());
                        AlertUtils.showError("Error", "Failed to parse server response for users.");
                    }
                } else {
                    AlertUtils.showError("Error", "Failed to fetch users: " + response.message());
                    Logger.error("Failed to fetch users for group: " + group.getName());
                }
            });
        } else {
            javafx.application.Platform.runLater(() -> {
                AlertUtils.showError("Error", "Client is not connected to the server.");
                Logger.error("Client is not connected to the server.");
            });
        }
    }


    private void fetchExpensesForGroup() {
        if (clientService.isClientConnected()) {
            ServerResponse response = clientService.sendRequest(new Message(Message.Type.GET_EXPENSES, selectedGroup.getId()));

            javafx.application.Platform.runLater(() -> {
                if (response.isSuccess()) {
                    try {
                        Type expenseListType = new TypeToken<List<Expense>>() {
                        }.getType();
                        List<Expense> fetchedExpenses = gson.fromJson(gson.toJson(response.payload()), expenseListType);

                        expenses.setAll(fetchedExpenses);

                        Logger.info("Expenses fetched successfully for group: " + selectedGroup.getName());
                    } catch (Exception e) {
                        Logger.error("Failed to deserialize expenses: " + e.getMessage());
                        AlertUtils.showError("Error", "Failed to parse server response for expenses.");
                    }
                } else {
                    expenses.clear();
                    Logger.info("No expenses found or error occurred for group: " + selectedGroup.getName());
                }
            });
        } else {
            javafx.application.Platform.runLater(() -> {
                AlertUtils.showError("Error", "Client is not connected to the server.");
                Logger.error("Client is not connected to the server.");
            });
        }
    }

    @FXML
    public void handleNewExpense() {
        List<ComboBoxOption> userOptions = usersInGroup.stream()
                .map(user -> new ComboBoxOption(user.getId(), user.getName()))
                .toList();

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
                LocalDate date = (LocalDate) data.get("date");
                String formattedDate = date != null ? date.toString() : null;

                String description = (String) data.get("description");
                double amount = Double.parseDouble((String) data.get("amount"));
                ComboBoxOption paidBy = (ComboBoxOption) data.get("paidBy");
                List<ComboBoxOption> sharedWith = (List<ComboBoxOption>) data.get("sharedWith");

                if (paidBy == null || sharedWith.isEmpty()) {
                    AlertUtils.showError("Invalid Data", "Please select a payer and at least one user to share with.");
                    return;
                }

                // Converter a lista de ComboBoxOption para uma String separada por vírgulas
                String sharedWithNames = sharedWith.stream()
                        .map(ComboBoxOption::getName)
                        .reduce((name1, name2) -> name1 + ", " + name2)
                        .orElse("");

                List<Integer> sharedWithIds = sharedWith.stream()
                        .map(ComboBoxOption::getId)
                        .toList();

                Expense newExpense = new Expense(0, // ID será definido pelo servidor
                        selectedGroup.getId(),
                        paidBy.getId(),
                        currentUser.getId(),
                        amount,
                        description,
                        formattedDate,
                        sharedWithIds,
                        paidBy.getName(),
                        sharedWithNames
                );

                new Thread(() -> {
                    ServerResponse response = clientService.sendRequest(new Message(Message.Type.ADD_EXPENSE, newExpense));
                    javafx.application.Platform.runLater(() -> {
                        if (response.isSuccess()) {
                            AlertUtils.showSuccess("Success", "Expense added successfully!");

                            // Realiza um fetch das despesas do grupo selecionado após adicionar com sucesso
                            fetchExpensesForGroup();
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

    @FXML
    public void handleEditExpense() {
        if (selectedExpense == null) {
            AlertUtils.showError("Error", "No expense selected for editing.");
            return;
        }

        List<ComboBoxOption> userOptions = usersInGroup.stream()
                .map(user -> new ComboBoxOption(user.getId(), user.getName()))
                .toList();

        ComboBoxOption selectedPaidByOption = userOptions.stream()
                .filter(option -> option.getId() == selectedExpense.getPaidBy())
                .findFirst()
                .orElse(null);

        List<ComboBoxOption> selectedSharedWithOptions = userOptions.stream()
                .filter(option -> selectedExpense.getSharedWith().contains(option.getId()))
                .toList();

        List<FieldConfig> fields = Arrays.asList(
                new FieldConfig<>("date", "Date", null, FieldType.DATE, true, LocalDate.parse(selectedExpense.getDate()), null),
                new FieldConfig<>("description", "Description", "Enter expense description", FieldType.TEXT, true, selectedExpense.getDescription(), null),
                new FieldConfig<>("amount", "Value", "Enter the amount", FieldType.TEXT, true, String.valueOf(selectedExpense.getAmount()), null),
                new FieldConfig<>("paidBy", "Paid By", "Select who paid", FieldType.COMBOBOX, true, selectedPaidByOption, userOptions),
                new FieldConfig<>("sharedWith", "Shared With", "Select users to share with", FieldType.MULTI_SELECT, true, selectedSharedWithOptions, userOptions)
        );

        Dialog<Map<String, Object>> dialog = CustomDialog.createDialog("Edit Expense", fields);

        Optional<Map<String, Object>> result = dialog.showAndWait();

        result.ifPresent(data -> {
            try {
                LocalDate date = (LocalDate) data.get("date");
                String formattedDate = date != null ? date.toString() : null;

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

                selectedExpense.setDate(formattedDate);
                selectedExpense.setDescription(description);
                selectedExpense.setAmount(amount);
                selectedExpense.setPaidBy(paidBy.getId());
                selectedExpense.setPaidByName(paidBy.getName());
                selectedExpense.setSharedWith(sharedWithIds);

                new Thread(() -> {
                    ServerResponse response = clientService.sendRequest(new Message(Message.Type.EDIT_EXPENSE, selectedExpense));
                    javafx.application.Platform.runLater(() -> {
                        if (response.isSuccess()) {
                            AlertUtils.showSuccess("Success", "Expense updated successfully!");
                            fetchExpensesForGroup(); // Atualiza a tabela
                        } else {
                            AlertUtils.showError("Error", "Failed to update expense: " + response.message());
                        }
                    });
                }).start();
            } catch (Exception e) {
                AlertUtils.showError("Error", "Invalid data provided: " + e.getMessage());
            }
        });
    }

    @FXML
    public void handleDeleteExpense() {
        if (selectedExpense == null) {
            AlertUtils.showError("Error", "No expense selected for deletion.");
            return;
        }

        boolean confirmed = AlertUtils.showConfirmation("Delete Expense",
                "Are you sure you want to delete this expense? This action cannot be undone.");

        if (confirmed) {
            new Thread(() -> {
                ServerResponse response = clientService.sendRequest(
                        new Message(Message.Type.DELETE_EXPENSE, selectedExpense.getId())
                );

                javafx.application.Platform.runLater(() -> {
                    if (response.isSuccess()) {
                        expenses.remove(selectedExpense);
                        Logger.info("Expense deleted successfully: " + selectedExpense);
                        AlertUtils.showSuccess("Success", "Expense deleted successfully!");
                    } else {
                        Logger.error("Failed to delete expense: " + response.message());
                        AlertUtils.showError("Error", "Failed to delete expense: " + response.message());
                    }
                });
            }).start();
        }
    }

    @FXML
    public void handleExportToCSV(String filename) {
        ObservableList<?> dataToExport;
        if ("expenses".equalsIgnoreCase(filename)) {
            dataToExport = expenses;
        } else if ("payments".equalsIgnoreCase(filename)) {
            dataToExport = payments;
        } else {
            AlertUtils.showError("Error", "Invalid filename. Must be 'expenses' or 'payments'.");
            return;
        }

        if (dataToExport.isEmpty()) {
            AlertUtils.showError("Error", "No data to export for " + filename + ".");
            return;
        }

        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Save " + filename + " to CSV");
            fileChooser.setInitialFileName(filename + ".csv");

            FileChooser.ExtensionFilter csvFilter = new FileChooser.ExtensionFilter("CSV Files (*.csv)", "*.csv");
            fileChooser.getExtensionFilters().add(csvFilter);

            File file = fileChooser.showSaveDialog(mainContent.getScene().getWindow());

            if (file == null) {
                Logger.info("File save operation canceled by user.");
                return;
            }

            CSVUtils.exportToCSV(dataToExport, file.getCanonicalPath());
            Logger.info("CSV file saved at: " + file.getCanonicalPath());
            AlertUtils.showSuccess("Success", filename + " exported to " + file.getCanonicalPath());
        } catch (Exception e) {
            Logger.error("Error exporting " + filename + " to CSV: " + e.getMessage());
            AlertUtils.showError("Error", "Failed to export " + filename + " to CSV.");
        }
    }

    @FXML
    public void handleNewPayment() {
        // Convert users in the group to ComboBoxOptions
        List<ComboBoxOption> userOptions = usersInGroup.stream()
                .map(user -> new ComboBoxOption(user.getId(), user.getName()))
                .toList();

        // Define fields for the dialog
        List<FieldConfig> fields = Arrays.asList(
                new FieldConfig<>("date", "Date", null, FieldType.DATE, true, null, null),
                new FieldConfig<>("amount", "Value", "Enter the payment amount", FieldType.TEXT, true, null, null),
                new FieldConfig<>("paidBy", "Paid By", "Select payer", FieldType.COMBOBOX, true, null, userOptions),
                new FieldConfig<>("receivedBy", "Received By", "Select receiver", FieldType.COMBOBOX, true, null, userOptions)
        );

        Dialog<Map<String, Object>> dialog = CustomDialog.createDialog("Add New Payment", fields);

        Optional<Map<String, Object>> result = dialog.showAndWait();
        result.ifPresent(data -> {
            try {
                // Convert date from LocalDate to String
                LocalDate date = (LocalDate) data.get("date");
                String formattedDate = date != null ? date.toString() : null;

                double amount = Double.parseDouble(data.get("amount").toString());
                ComboBoxOption paidBy = (ComboBoxOption) data.get("paidBy");
                ComboBoxOption receivedBy = (ComboBoxOption) data.get("receivedBy");

                if (paidBy == null || receivedBy == null) {
                    AlertUtils.showError("Invalid Data", "Please select both payer and receiver.");
                    return;
                }

                if (paidBy.getId() == receivedBy.getId()) {
                    AlertUtils.showError("Invalid Data", "Payer and receiver cannot be the same user.");
                    return;
                }

                Payment newPayment = new Payment(
                        0, // ID will be set by the server
                        selectedGroup.getId(),
                        paidBy.getId(),
                        receivedBy.getId(),
                        amount,
                        formattedDate
                );

                // Send the new payment to the server
                new Thread(() -> {
                    ServerResponse response = clientService.sendRequest(new Message(Message.Type.ADD_PAYMENT, newPayment));
                    javafx.application.Platform.runLater(() -> {
                        if (response.isSuccess()) {
                            paymentsTableView.getItems().add(newPayment);
                            AlertUtils.showSuccess("Success", "Payment added successfully!");
                            fetchPayments();
                        } else {
                            AlertUtils.showError("Error", "Failed to add payment: " + response.message());
                        }
                    });
                }).start();
            } catch (Exception e) {
                AlertUtils.showError("Error", "Invalid data provided: " + e.getMessage());
            }
        });
    }

    @FXML
    public void handleEditPayment() {
        if (selectedPayment == null) {
            AlertUtils.showError("Error", "No payment selected for editing.");
            return;
        }

        List<ComboBoxOption> userOptions = usersInGroup.stream()
                .map(user -> new ComboBoxOption(user.getId(), user.getName()))
                .toList();

        // Identificar os ComboBoxOptions corretos para PaidBy e ReceivedBy
        ComboBoxOption selectedPaidByOption = userOptions.stream()
                .filter(option -> option.getId() == selectedPayment.getPaidBy())
                .findFirst()
                .orElse(null);

        ComboBoxOption selectedReceivedByOption = userOptions.stream()
                .filter(option -> option.getId() == selectedPayment.getReceivedBy())
                .findFirst()
                .orElse(null);

        List<FieldConfig> fields = Arrays.asList(
                new FieldConfig<>("date", "Date", null, FieldType.DATE, true, LocalDate.parse(selectedPayment.getDate()), null),
                new FieldConfig<>("amount", "Value", "Enter the payment amount", FieldType.TEXT, true, String.valueOf(selectedPayment.getAmount()), null),
                new FieldConfig<>("paidBy", "Paid By", "Select who paid", FieldType.COMBOBOX, true, selectedPaidByOption, userOptions),
                new FieldConfig<>("receivedBy", "Received By", "Select receiver", FieldType.COMBOBOX, true, selectedReceivedByOption, userOptions)
        );

        Dialog<Map<String, Object>> dialog = CustomDialog.createDialog("Edit Payment", fields);

        Optional<Map<String, Object>> result = dialog.showAndWait();

        result.ifPresent(data -> {
            try {
                LocalDate date = (LocalDate) data.get("date");
                String formattedDate = date != null ? date.toString() : null;

                double amount = Double.parseDouble((String) data.get("amount"));
                ComboBoxOption paidBy = (ComboBoxOption) data.get("paidBy");
                ComboBoxOption receivedBy = (ComboBoxOption) data.get("receivedBy");

                if (paidBy == null || receivedBy == null) {
                    AlertUtils.showError("Invalid Data", "Please select both payer and receiver.");
                    return;
                }

                if (paidBy.getId() == receivedBy.getId()) {
                    AlertUtils.showError("Invalid Data", "Payer and receiver cannot be the same user.");
                    return;
                }

                // Atualizar o pagamento selecionado
                selectedPayment.setDate(formattedDate);
                selectedPayment.setAmount(amount);
                selectedPayment.setPaidBy(paidBy.getId());
                selectedPayment.setPaidByName(paidBy.getName());
                selectedPayment.setReceivedBy(receivedBy.getId());
                selectedPayment.setReceivedByName(receivedBy.getName());

                // Enviar ao servidor
                new Thread(() -> {
                    ServerResponse response = clientService.sendRequest(new Message(Message.Type.EDIT_PAYMENT, selectedPayment));
                    javafx.application.Platform.runLater(() -> {
                        if (response.isSuccess()) {
                            AlertUtils.showSuccess("Success", "Payment updated successfully!");
                            fetchPayments(); // Atualiza a tabela
                        } else {
                            AlertUtils.showError("Error", "Failed to update payment: " + response.message());
                        }
                    });
                }).start();
            } catch (Exception e) {
                AlertUtils.showError("Error", "Invalid data provided: " + e.getMessage());
            }
        });
    }

    @FXML
    public void handleDeletePayment() {
        if (selectedPayment == null) {
            AlertUtils.showError("Error", "No payment selected for deletion.");
            return;
        }

        boolean confirmed = AlertUtils.showConfirmation("Delete Payment",
                "Are you sure you want to delete this payment? This action cannot be undone.");

        if (confirmed) {
            new Thread(() -> {
                ServerResponse response = clientService.sendRequest(
                        new Message(Message.Type.DELETE_PAYMENT, selectedPayment.getId())
                );

                javafx.application.Platform.runLater(() -> {
                    if (response.isSuccess()) {
                        payments.remove(selectedPayment);
                        Logger.info("Payment deleted successfully: " + selectedPayment);
                        AlertUtils.showSuccess("Success", "Payment deleted successfully!");
                    } else {
                        Logger.error("Failed to delete payment: " + response.message());
                        AlertUtils.showError("Error", "Failed to delete payment: " + response.message());
                    }
                });
            }).start();
        }
    }

    @FXML
    public void handleEditProfile() {
        List<FieldConfig> fields = Arrays.asList(new FieldConfig<>("name", "Name", "Enter your name", FieldType.TEXT, true, currentUser.getName(), null), new FieldConfig<>("email", "Email", null, FieldType.TEXT, false, currentUser.getEmail(), null), new FieldConfig<>("phone", "Phone", "Enter your phone", FieldType.TEXT, true, currentUser.getPhone(), null), new FieldConfig<>("password", "Password", "Enter your new password", FieldType.PASSWORD, true, null, null));

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
                ServerResponse response = clientService.sendRequest(new Message(Message.Type.EDIT_PROFILE, new User(currentUser.getId(), name, email, phone, encodedPassword)));

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
        List<FieldConfig> fields = Collections.singletonList(new FieldConfig<>("groupName", "Group Name", "Enter the group name", FieldType.TEXT, true, null, null));

        Dialog<Map<String, Object>> dialog = CustomDialog.createDialog("Create New Group", fields);

        Optional<Map<String, Object>> result = dialog.showAndWait();

        result.ifPresent(data -> {
            String groupName = (String) data.get("groupName");

            if (groupName == null || groupName.isBlank()) {
                AlertUtils.showError("Invalid Group Name", "Please enter a valid group name.");
                return;
            }

            new Thread(() -> {
                ServerResponse response = clientService.sendRequest(new Message(Message.Type.CREATE_GROUP, new Group(groupName, currentUser.getId())));

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
