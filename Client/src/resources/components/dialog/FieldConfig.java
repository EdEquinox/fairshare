package resources.components.dialog;

import java.util.List;

public class FieldConfig<T> {
    private final String id;
    private final String label;
    private final String prompt;
    private final FieldType type;
    private final boolean editable;
    private final T defaultValue;
    private final List<ComboBoxOption> items;

    public FieldConfig(String id, String label, String prompt, FieldType type, boolean editable, T defaultValue, List<ComboBoxOption> items) {
        this.id = id;
        this.label = label;
        this.prompt = prompt;
        this.type = type;
        this.editable = editable;
        this.defaultValue = defaultValue;
        this.items = items;
    }

    public String getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public String getPrompt() {
        return prompt;
    }

    public FieldType getType() {
        return type;
    }

    public boolean isEditable() {
        return editable;
    }

    public T getDefaultValue() {
        return defaultValue;
    }

    public List<ComboBoxOption> getItems() {
        return items;
    }
}