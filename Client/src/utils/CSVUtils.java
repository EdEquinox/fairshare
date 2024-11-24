package utils;

import javafx.collections.ObservableList;
import model.Expense;
import model.Payment;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class CSVUtils {

    public static <T> File exportToCSV(ObservableList<T> data, String filePath) throws IOException {
        // Cria o ficheiro no path especificado
        File file = new File(filePath);

        try (FileWriter writer = new FileWriter(file)) {
            // Define o cabeçalho e o conteúdo com base no tipo de dados
            if (!data.isEmpty()) {
                if (data.get(0) instanceof Expense) {
                    writer.append("ID,Group ID,Paid By,Paid By Name,Amount,Description,Date,Shared With\n");
                    for (T item : data) {
                        Expense expense = (Expense) item;
                        writer.append(String.format("%d,%d,%d,%s,%.2f,%s,%s,%s\n",
                                expense.getId(),
                                expense.getGroupId(),
                                expense.getPaidBy(),
                                expense.getPaidByName(),
                                expense.getAmount(),
                                expense.getDescription(),
                                expense.getDate(),
                                expense.getSharedWithNames() // Assume que é uma String
                        ));
                    }
                } else if (data.get(0) instanceof Payment) {
                    writer.append("ID,Group ID,Paid By,Paid By Name,Received By,Received By Name,Amount,Date\n");
                    for (T item : data) {
                        Payment payment = (Payment) item;
                        writer.append(String.format("%d,%d,%d,%s,%d,%s,%.2f,%s\n",
                                payment.getId(),
                                payment.getGroupId(),
                                payment.getPaidBy(),
                                payment.getPaidByName(),
                                payment.getReceivedBy(),
                                payment.getReceivedByName(),
                                payment.getAmount(),
                                payment.getDate()
                        ));
                    }
                } else {
                    throw new IllegalArgumentException("Unsupported data type for CSV export.");
                }
            }
        }

        return file;
    }
}
