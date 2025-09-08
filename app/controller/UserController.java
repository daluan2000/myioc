package app.controller;

import app.service.UserService;
import myioc.MyAutowired;
import myioc.MyComponent;
import myioc.MyQualifier;

@MyComponent
public class UserController {

    // required = true时，会报错
    @MyAutowired(required = false)
    private UserService userService;

    @MyQualifier("remoteUserService")
    @MyAutowired
    private UserService userService1;

    public void login() {
        if (this.userService != null) {
            this.userService.login();
        } else {
            System.out.println("userService is null");
        }
        this.userService1.login();
        System.out.println("UserController执行了login方法");
    }
}
