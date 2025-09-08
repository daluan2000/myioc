package app.service.impl;

import app.dao.UserDao;
import app.service.UserService;
import myioc.MyAutowired;
import myioc.MyComponent;
import myioc.MyQualifier;

import java.lang.annotation.Retention;

@MyComponent
public class LocalUserService implements UserService {

    @MyQualifier("mysqlUserDao")
    @MyAutowired
    private UserDao userDao;

    @Override
    public void login() {
        this.userDao.selectUser();
        System.out.println("LocalUserService执行了login方法");
    }
}
