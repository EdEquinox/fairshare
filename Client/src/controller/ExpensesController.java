package controller;

import communication.ClientService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import model.Expense;
import model.Group;
import model.Message;
import model.ServerResponse;
import utils.*;

import java.lang.reflect.Type;
import com.google.gson.reflect.TypeToken;
import com.google.gson.Gson;

import java.io.File;
import java.util.List;

public class ExpensesController {

    @FXML
    private TableView<Expense> expensesTable;

    @FXML
    private TableColumn<Expense, String> dateColumn;

    @FXML
    private TableColumn<Expense, String> descriptionColumn;

    @FXML
    private TableColumn<Expense, Double> amountColumn;

    @FXML
    private TableColumn<Expense, Integer> paidByColumn;

    @FXML
    private Label totalExpensesLabel;

    private ClientService clientService;

    private ObservableList<Expense> expenses;

    public void initialize() {
        clientService = ClientService.getInstance();

        // Colunas apresentadas nas expenses
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("date"));
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        amountColumn.setCellValueFactory(new PropertyValueFactory<>("amount"));
        paidByColumn.setCellValueFactory(new PropertyValueFactory<>("paidBy"));

        // Inicializa a lista de expenses
        expenses = FXCollections.observableArrayList();
        expensesTable.setItems(expenses);

        // Vai buscar o grupo que foi guardado no dashboardcontroller
        Group selectedGroup = SharedState.getSelectedGroup();
        if (selectedGroup != null) {
            fetchExpensesForGroup(selectedGroup); // Vai buscar as expenses para esse grupo
        } else {
            Logger.error("No group selected in SharedState.");
            AlertUtils.showError("Error", "No group selected. Please go back and select a group.");
        }
    }

    private void fetchExpensesForGroup(Group selectedGroup) {

        //No caso de o grupo nao ser bem guardado
        if (selectedGroup == null) {
            Logger.error("Selected group is null. Cannot fetch expenses.");
            AlertUtils.showError("Error", "No group selected for fetching expenses.");
            return;
        }

        // Cria thread, verifica a conexao ao cliente e envia o grupo selecionado para procurar as expenses do mesmo
        new Thread(() -> {
            if (clientService.isClientConnected()) {
                ServerResponse response = clientService.sendRequest(new Message(Message.Type.GET_EXPENSES, selectedGroup));

                javafx.application.Platform.runLater(() -> {
                    if (response.isSuccess()) {
                        try {
                            // Cria o tipo generico e devolve como lista de expenses
                            Type expenseListType = new TypeToken<List<Expense>>() {}.getType();
                            List<Expense> fetchedExpenses = new Gson().fromJson(new Gson().toJson(response.payload()), expenseListType);
                            // Atualiza a lista de expenses
                            expenses.setAll(fetchedExpenses);

                            // Calcula o valor total de expenses para mostrar ao utilizador
                            double total = fetchedExpenses.stream()
                                    .mapToDouble(Expense::getAmount)
                                    .sum();
                            totalExpensesLabel.setText("Total Expenses: " + total + "€");

                            Logger.info("Expenses fetched successfully for group: " + selectedGroup.name());
                        } catch (Exception e) {
                            Logger.error("Failed to deserialize expenses: " + e.getMessage());
                            AlertUtils.showError("Error", "Invalid server response format.");
                        }
                    } else {
                        AlertUtils.showError("Error", "Failed to fetch expenses: " + response.message());
                        Logger.error("Failed to fetch expenses for group: " + selectedGroup.name());
                    }
                });
            }
        }).start();
    }

    public void handleExportToCSV() {
        if (expenses.isEmpty()) {
            AlertUtils.showError("Error", "No expenses to export.");
            return;
        }

        try {
            // Guarda o root path do projecto
            String projectDir = System.getProperty("user.dir");
            Logger.info("Project directory: " + projectDir);

            // Cria e/ou verifica se existe a pasta exports dentro da out
            File exportDir = new File(projectDir, "out/exports");
            Logger.info("Export directory: " + exportDir.getAbsolutePath());
            // No caso de falhar mostra um alerta
            if (!exportDir.exists()) {
                boolean dirCreated = exportDir.mkdirs();
                if (!dirCreated) {
                    Logger.error("Failed to create 'exports' directory.");
                    AlertUtils.showError("Error", "Could not create 'exports' directory.");
                    return;
                }
            }

            // Constroi o caminho final para o ficheiro csv
            File file = new File(exportDir, "expenses.csv");

            // Log para verificar se o caminho está correto
            String filePath = file.getCanonicalPath();
            Logger.info("CSV file path: " + filePath);

            // Exporta o csv
            CSVUtils.exportToCSV(expenses, filePath);

            Logger.info("CSV file saved at: " + filePath);
            AlertUtils.showSuccess("Success", "Expenses exported to " + filePath);
        } catch (Exception e) {
            Logger.error("Error exporting expenses to CSV: " + e.getMessage());
            AlertUtils.showError("Error", "Failed to export expenses to CSV.");
        }
    }


    public void handleGoToGroupsPage(ActionEvent actionEvent) {
        NavigationManager.switchScene(Routes.GROUP);
    }
}
