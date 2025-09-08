package myioc;

import java.io.File;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.*;


public class MyIOC {
    // 存放bean对象的集合
    private Set<Object> beanSet = new HashSet<>();
    // 要扫描的包名，不支持模糊表达
    private List<String> packageNameList;

    public MyIOC(List<String> packageNames) throws ReflectiveOperationException {
        this.packageNameList = packageNames;
        // 首先注册bean对象
        for (String packageName : packageNameList) {
            this.register(packageName);
        }
        // 然后注入bean对象
        this.inject();
    }

    private void register(String packageName) throws ReflectiveOperationException {
        // 扫描包名对应的文件夹，将其中含有@MyComponent注解的类，创建对象并保存到bean中

        String folderPath = packageName.replace('.', '/');
        URL url = MyIOC.class.getClassLoader().getResource(folderPath);
        // ClassLoader只能访问classpath下的资源，也就是编译后的.class 文件其他资源文件，而不是源码.java
        if (url == null) {
            throw new RuntimeException(packageName + " " + folderPath + "读取错误");
        }
        File file = new File(url.getFile());
        if (!file.isDirectory()) {
            throw new RuntimeException(packageName + " " + folderPath + "不是一个包或文件夹");
        }

        File[] childFiles = file.listFiles();
        if (childFiles != null) {
            for (File childFile : childFiles) {
                String name = packageName + "." +  childFile.getName();
                if (childFile.isDirectory()) {
                    // 对于子目录，递归扫描
                    this.register(name);
                } else if (childFile.getName().endsWith(".class")) {
                    // 对于.class子文件，获取完整className并创建对象，如果有@MyComponent则存入bean中
                    String className = name.substring(0, name.length() - 6);
                    Class<?> clazz = Class.forName(className);
                    MyComponent myComponent = clazz.getAnnotation(MyComponent.class);
                    if (myComponent != null) {
                        Object instance = clazz.getDeclaredConstructor().newInstance();
                        this.beanSet.add(instance);
                    }
                }
            }
        }
    }

    private void inject() throws IllegalAccessException {
        // 遍历bean中每个对象的每个属性，如果有@MyAutowired，则从bean中获取对象并注入到该属性

        for (Object originBean : this.beanSet) {
            Field[] fields = originBean.getClass().getDeclaredFields();
            for (Field field : fields) {
                MyAutowired autowired = field.getAnnotation(MyAutowired.class);
                if (autowired != null) {
                    field.setAccessible(true);
                    boolean injectFlag = false;
                    for (Object injectBean : this.beanSet) {
                        // 按类型注入，找到该类型或自类型的对象，则注入
                        if (field.getType().isAssignableFrom(injectBean.getClass())) {
                            if (injectFlag) {
                                throw new RuntimeException(field.getType().getName() + "存在多个Bean对象");
                            } else {
                                injectFlag = true;
                            }
                            field.set(originBean, injectBean);
                        }
                    }
                    if (!injectFlag) {
                        throw new RuntimeException(field.getType().getName() + "未找到Bean对象");
                    }
                }
            }
        }
    }

    public Object getBean(Class<?> clazz) {
        // 根据class获取bean对象
        for (Object bean : this.beanSet) {
            if (clazz.isAssignableFrom(bean.getClass())) {
                return bean;
            }
        }
        throw new RuntimeException(clazz.getName() + "不存在于bean");
    }


}




