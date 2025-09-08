import app.controller.UserController;
import myioc.MyIOC;

import java.util.ArrayList;
import java.util.Arrays;

public class Main {
    public static void main(String[] args) throws ReflectiveOperationException {
        MyIOC myIOC = new MyIOC(new ArrayList<>(Arrays.asList("app.controller", "app.dao", "app.service")));
        // MyIOC myIOC = new MyIOC(new ArrayList<>(Arrays.asList("app")));
        UserController userController = (UserController) myIOC.getBean(UserController.class);
        userController.login();
    }
}
