package utils;

import model.Group;
import model.User;

public class SharedState {
    //Class para guardar o utilizador que fez login e os grupos selecionados nas diferentes veiws
    private static Group selectedGroup;
    private static User currentUser;


    public static Group getSelectedGroup() {
        return selectedGroup;
    }

    public static void setSelectedGroup(Group group) {
        selectedGroup = group;
    }


    public static User getCurrentUser() {
        return currentUser;
    }

    public static void setCurrentUser(User user) {
        currentUser = user;
    }
}


