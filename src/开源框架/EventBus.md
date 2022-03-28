使用：

1. 方法添加注解
2. 注册`EventBus.getDefault().register();`
3. post发送消息，根据参数找到对应的方法，反射调用

旧版本通过方法名，反射找到注册类的前缀为onEvent的方法注册：onEvent、onEventMainThread、onEventBackgroundThread和onEventAsync

EventBus3.0使用注解：运行时注解，注册的时候反射解析注解，生成SubscribeMethod方法，存入`Subsctiption(方法所在的类的对象，SubscribeMethod)`，再存入Map中`<eventType, Subscriptions>`

发送事件的时候根据参数类型（eventType）找到对应的订阅者列表，反射调用方法，传入类的对象

```java
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface Subscribe {
    ThreadMode threadMode() default ThreadMode.POSTING;
    boolean sticky() default false;
    int priority() default 0;
}
```

线程：

```java
public enum ThreadMode {
  //使用事件发送线程
  POSTING,
  //主线程
  MAIN,
  //后台线程：如果当前不是UI线程，则使用当前线程。如果是UI线程，则使用线程池，一个一个事件处理
  BACKGROUND,
  //异步线程：可以多个线程一起处理
  ASYNC
}
```

粘性事件：可以先发送事件再注册，保存到队列中

https://www.jianshu.com/p/d9516884dbd4
