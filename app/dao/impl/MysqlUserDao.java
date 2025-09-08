package app.dao.impl;

import app.dao.UserDao;
import myioc.MyComponent;

@MyComponent("mysqlUserDao")
public class MysqlUserDao implements UserDao {
    @Override
    public void selectUser() {
        System.out.println("MysqlUserDao执行了selectUser方法");
    }
}
