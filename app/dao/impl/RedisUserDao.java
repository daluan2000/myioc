package app.dao.impl;

import app.dao.UserDao;
import myioc.MyComponent;

@MyComponent
public class RedisUserDao implements UserDao {
    @Override
    public void selectUser() {
        System.out.println("RedisUserDao执行了selectUser方法");
    }
}
