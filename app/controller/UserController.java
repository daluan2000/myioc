package app.controller;

import app.service.UserService;
import myioc.MyAutowired;
import myioc.MyComponent;

@MyComponent
public class UserController {
    @MyAutowired
    private UserService userService;

    public void login() {
        this.userService.login();
        System.out.println("UserController执行了login方法");
    }
}
