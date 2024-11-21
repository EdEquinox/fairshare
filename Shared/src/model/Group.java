package model;

public record Group(String name, int ownerId) {

    public Group(String name, int ownerId) {
        this.name = name;
        this.ownerId = ownerId;
    }

    public String getName() {
        return name;
    }

    public int getOwnerId() {
        return ownerId;
    }

    @Override
    public String toString() {
        return name;
    }

}

