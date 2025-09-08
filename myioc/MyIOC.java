package myioc;

import java.io.File;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.*;


public class MyIOC {
    // 存放bean对象的集合
    private final Set<Object> beanSet = new HashSet<>();

    // 存放beanName到bean的映射
    private final Map<String, Object> nameBeanMap = new HashMap<>();

    // 存放bean到beanName的映射
    private final Map<Object, String> beanNameMap = new HashMap<>();

    /**
     * 创建MyIOC对象，创建时要指定扫描的包名列表
     * @param packageNames 包名列表，包名需精确表达
     */
    public MyIOC(List<String> packageNames) throws ReflectiveOperationException {
        // 首先注册bean对象
        for (String packageName : packageNames) {
            this.register(packageName);
        }
        // 然后注入bean对象
        this.inject();
    }

    /**
     * 扫描包名对应的文件夹，将其中含有@MyComponent注解的类，创建对象并保存到bean中
     * @param packageName 要扫描的包名
     */
    private void register(String packageName) throws ReflectiveOperationException {
        String folderPath = packageName.replace('.', '/');
        // ClassLoader只能访问classpath下的资源，也就是编译后的.class文件和其他资源文件，而不是源码.java
        URL url = MyIOC.class.getClassLoader().getResource(folderPath);
        if (url == null) {
            throw new RuntimeException(packageName + " " + folderPath + "读取错误");
        }
        File file = new File(url.getFile());
        if (!file.isDirectory()) {
            throw new RuntimeException(packageName + " " + folderPath + "不是一个包或文件夹");
        }

        // 获取子目录和子文件
        File[] childFiles = file.listFiles();
        if (childFiles != null) {
            for (File childFile : childFiles) {
                String childName = packageName + "." +  childFile.getName();
                if (childFile.isDirectory()) {
                    // 对于子目录，递归扫描注册
                    this.register(childName);
                } else if (childFile.getName().endsWith(".class")) {
                    // 对于.class子文件，获取完整className并创建对象，如果有@MyComponent则将对象和名词存入bean中
                    String className = childName.substring(0, childName.length() - 6);
                    Class<?> clazz = Class.forName(className);
                    MyComponent myComponent = clazz.getAnnotation(MyComponent.class);
                    if (myComponent != null) {
                        Object instance = clazz.getDeclaredConstructor().newInstance();
                        // 存入bean对象
                        String beanName = getBeanName(clazz);
                        this.beanSet.add(instance);
                        this.nameBeanMap.put(beanName, instance);
                        this.beanNameMap.put(instance, beanName);
                    }
                }
            }
        }
    }


    /**
     * 遍历bean中每个对象的每个属性，如果有@MyAutowired，则从bean中获取对象并注入到该属性
     */
    private void inject() throws IllegalAccessException {
        // 遍历所有bean，将targetBean注入到originBean中
        for (Object originBean : this.beanSet) {
            // 获取originBean所有属性
            Field[] fields = originBean.getClass().getDeclaredFields();
            for (Field field : fields) {
                // 检查属性是否有MyAutowired注解
                MyAutowired autowired = field.getAnnotation(MyAutowired.class);
                if (autowired != null) {
                    field.setAccessible(true);
                    // targetBeans记录所有与该field类型相匹配的bean对象
                    List<Object> targetBeans = new ArrayList<>();
                    for (Object targetBean : this.beanSet) {
                        if (field.getType().isAssignableFrom(targetBean.getClass())) {
                            targetBeans.add(targetBean);
                        }
                    }
                    // 如果MyQualifier存在并且参数值不为空，那么认为是有效的
                    MyQualifier qualifier = field.getAnnotation(MyQualifier.class);
                    boolean validQualifier = qualifier != null && !qualifier.value().isEmpty();
                    try {
                        // 搜索injectBean并注入的过程
                        // 最终注入的bean
                        Object injectBean = null;
                        if (targetBeans.isEmpty()) {
                            // 如果没有类型匹配的bean
                            throw new RuntimeException("不存在该类型对应的bean：" + field.getType().getName());
                        } else if (targetBeans.size() == 1 && !validQualifier) {
                            // 如果只有一个类型匹配的bean，并且MyQualifier注解没效果，那么这个bean就是要注入的bean
                            injectBean = targetBeans.get(0);
                        } else if (targetBeans.size() > 1 && !validQualifier) {
                            // 如果有多个类型匹配的bean，并且MyQualifier无效效果，那么按field属性名称来寻找要注入的bean
                            String simpleName = field.getName();
                            simpleName = simpleName.substring(0, 1).toUpperCase() + simpleName.substring(1);
                            for (Object targetBean : targetBeans) {
                                if (simpleName.equals(targetBean.getClass().getSimpleName())) {
                                    injectBean = targetBean;
                                    break;
                                }
                            }
                            if (injectBean == null) {
                                throw new RuntimeException("多个该类型的bean，无法通过属性名称确定唯一" +  field.getName());
                            }
                        } else {
                            // 如果MyQualifier有效并且targetBeans不为空，那么按照MyQualifier参数寻找要注入的bean
                            for (Object targetBean : targetBeans) {
                                if (qualifier.value().equals(this.beanNameMap.get(targetBean))) {
                                    injectBean = targetBean;
                                    break;
                                }
                            }
                            if (injectBean == null) {
                                throw new RuntimeException("无法找到该名称对应的bean：" + qualifier.value());
                            }
                        }
                        field.set(originBean, injectBean);
                    } catch (Exception e) {
                        if (autowired.required()){
                            // 如果required=true，则抛出这些匹配不到bean的错误
                            throw e;
                        } else {
                            // 否则忽视错误，并把属性设置为null
                            field.set(originBean, null);
                        }
                    }
                }
            }
        }
    }


    /**
     * 获取一个Class的beanName
     * @param clazz 该class对象
     * @return beanName
     */
    private String getBeanName(Class<?> clazz) {
        MyComponent myComponent = clazz.getAnnotation(MyComponent.class);
        if (myComponent == null) {
            throw new RuntimeException(clazz.getName() + "的MyComponent注解为null");
        }
        String beanName = "";
        if (!myComponent.value().isEmpty()) {
            // 如果有指定名称
            beanName = myComponent.value();
        } else {
            // 如果没有指定名称，类名首字母小写作为默认名称
            beanName = clazz.getSimpleName();
            beanName = beanName.substring(0, 1).toLowerCase() + beanName.substring(1);
        }
        if (this.nameBeanMap.containsKey(beanName)) {
            throw new RuntimeException("重复的beanName：" + beanName);
        }
        return beanName;
    }


    /**
     * 根据class获取bean对象
     * @param clazz Class对象
     */
    public Object getBean(Class<?> clazz) {
        for (Object bean : this.beanSet) {
            if (clazz.isAssignableFrom(bean.getClass())) {
                return bean;
            }
        }
        throw new RuntimeException(clazz.getName() + "不存在于bean");
    }

    /**
     * 根据beanName获取bean对象
     * @param name beanName，如未在MyComponent中指定，默认为类名首字母小写
     */
    public Object getBean(String name) {
        if  (this.nameBeanMap.containsKey(name)) {
            return this.nameBeanMap.get(name);
        }
        throw new RuntimeException(name + "不存在于bean");
    }


}




