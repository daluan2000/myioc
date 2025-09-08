package app.dao.impl;

import app.dao.UserDao;
import myioc.MyComponent;

@MyComponent
public class UserDaoImpl implements UserDao {
    @Override
    public void selectUser() {
        System.out.println("UserDaoImpl执行了selectUser方法");
    }
}
