package pt.edequinox.fairshare.client.model.fsm;

import pt.edequinox.fairshare.client.model.data.ClientData;
import pt.edequinox.fairshare.client.model.fsm.states.*;

public enum ClientState {
    LOGGED_OUT,
    USER_INVITATIONS,
    GROUP_SETTINGS,
    GROUPS,
    GROUP,
    MAIN_MENU,
    INVITE,
    USER_SETTINGS,
    MEMBER_INFO,
    EXPENSE,
    NEW_GROUP,
    TRANSACTION;


    static IClientState createState(ClientState state, ClientContext context, ClientData data) {
        switch (state) {
            case LOGGED_OUT -> new LoggedOutState(context, data);
            case GROUP_SETTINGS -> new GroupSettingsState(context, data);
            case USER_INVITATIONS -> new UserInvitationsState(context, data);
            case MAIN_MENU -> new MainMenuState(context, data);
            case USER_SETTINGS -> new UserSettingsState(context, data);
            case GROUPS -> new GroupsState(context, data);
            case GROUP -> new GroupState(context, data);
            case INVITE -> new InviteState(context, data);
            case MEMBER_INFO -> new MemberInfoState(context, data);
            case EXPENSE -> new ExpenseState(context, data);
            case TRANSACTION -> new TransactionState(context, data);
        };
        return null;
    }
}
