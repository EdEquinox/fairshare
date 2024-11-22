package utils;

import javafx.collections.ObservableList;
import model.Expense;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class CSVUtils {

    public static File exportToCSV(ObservableList<Expense> expenses, String filePath) throws IOException {
        //Cria o ficheiro no path
        File file = new File(filePath);
        //Escreve no ficheiro as informaçoes das expenses
        try (FileWriter writer = new FileWriter(file)) {
            writer.append("ID,Group ID,Paid By,Amount,Description,Date\n");
            for (Expense expense : expenses) {
                writer.append(String.format("%d,%d,%d,%.2f,%s,%s\n",
                        expense.getId(),
                        expense.getGroupId(),
                        expense.getPaidBy(),
                        expense.getAmount(),
                        expense.getDescription(),
                        expense.getDate()
                ));
            }
        }
        return file;
    }
}
