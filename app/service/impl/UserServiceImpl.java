package app.service.impl;

import app.dao.UserDao;
import app.service.UserService;
import myioc.MyAutowired;
import myioc.MyComponent;

@MyComponent
public class UserServiceImpl implements UserService {
    @MyAutowired
    private UserDao userDao;
    @Override
    public void login() {
        this.userDao.selectUser();
        System.out.println("UserServiceImpl执行了login方法");
    }
}
