package app.service.impl;

import app.dao.UserDao;
import app.service.UserService;
import myioc.MyAutowired;
import myioc.MyComponent;

@MyComponent
public class RemoteUserService implements UserService {

    @MyAutowired
    private UserDao redisUserDao;

    @Override
    public void login() {
        this.redisUserDao.selectUser();
        System.out.println("RemoteUserService执行了login方法");
    }
}
