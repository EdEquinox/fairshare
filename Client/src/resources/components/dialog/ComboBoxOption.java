package resources.components.dialog;

public class ComboBoxOption {
    private final int id;
    private final String name;

    public ComboBoxOption(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name; // Exibe apenas o nome na interface do usuário
    }
}
