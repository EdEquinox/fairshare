package resources.components.dialog;

import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.Node;
import javafx.geometry.*;
import java.util.*;

public class CustomDialog {
    public static Dialog<Map<String, Object>> createDialog(String title, List<FieldConfig> fields) {
        Dialog<Map<String, Object>> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.setHeaderText("Please fill out the information below:");

        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, cancelButtonType);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        Map<String, Node> fieldInputs = new HashMap<>();

        int row = 0;
        for (FieldConfig field : fields) {
            Label label = new Label(field.getLabel());
            Node inputControl;

            switch (field.getType()) {
                case DATE:
                    inputControl = new DatePicker();
                    break;

                case COMBOBOX:
                    ComboBox<ComboBoxOption> comboBox = new ComboBox<>();
                    comboBox.setPromptText(field.getPrompt());
                    if (field.getItems() != null) {
                        comboBox.getItems().addAll(field.getItems());
                    }
                    inputControl = comboBox;
                    break;

                case MULTI_SELECT:
                    ListView<ComboBoxOption> listView = new ListView<>();
                    if (field.getItems() != null) {
                        listView.getItems().addAll(field.getItems());
                    }
                    listView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
                    inputControl = listView;
                    break;

                case PASSWORD:
                    PasswordField passwordField = new PasswordField();
                    passwordField.setPromptText(field.getPrompt());
                    inputControl = passwordField;
                    break;

                case TEXT:
                default:
                    TextField textField = new TextField();
                    textField.setPromptText(field.getPrompt());
                    if (!field.isEditable()) {
                        textField.setEditable(false);
                    }
                    if (field.getDefaultValue() != null) {
                        textField.setText(field.getDefaultValue().toString());
                    }
                    inputControl = textField;
                    break;
            }

            grid.add(label, 0, row);
            grid.add(inputControl, 1, row);
            fieldInputs.put(field.getId(), inputControl);
            row++;
        }

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                Map<String, Object> results = new HashMap<>();
                fieldInputs.forEach((key, control) -> {
                    if (control instanceof TextInputControl) {
                        results.put(key, ((TextInputControl) control).getText());
                    } else if (control instanceof DatePicker) {
                        results.put(key, ((DatePicker) control).getValue());
                    } else if (control instanceof ComboBox<?>) {
                        results.put(key, ((ComboBox<?>) control).getValue());
                    } else if (control instanceof ListView<?>) {
                        results.put(key, ((ListView<?>) control).getSelectionModel().getSelectedItems());
                    }
                });
                return results;
            }
            return null;
        });

        return dialog;
    }
}