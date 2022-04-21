使用：

1. 方法添加注解`@Subscribe`
2. 注册`EventBus.getDefault().register();`
3. post发送消息，根据参数找到对应的方法，反射调用

旧版本通过方法名，反射找到注册类的前缀为onEvent的方法注册：onEvent、onEventMainThread、onEventBackgroundThread和onEventAsync

EventBus3.0使用注解：

1. 运行时注解，register注册的时候反射解析注解
2. 查找该类使用了注解的方法，生成SubscribeMethod方法，并缓存下来`Map<Class, List<SubscribeMethod>>`
3. 存入`Subsctiption(方法所在的类的对象，SubscribeMethod)`，再存入Map中`<eventType, Subscriptions>`

发送事件的时候根据参数类型（eventType）找到对应的订阅者列表，反射调用方法，传入类的对象

```java
public void post(Object event) {
        // currentPostingThreadState是一个PostingThreadState类型的ThreadLocal
        // PostingThreadState类保存了事件队列和线程模式等信息
        PostingThreadState postingState = currentPostingThreadState.get();
        List<Object> eventQueue = postingState.eventQueue;
        // 将要发送的事件添加到事件队列
        eventQueue.add(event);
        // isPosting默认为false
        if (!postingState.isPosting) {
            // 是否为主线程
            postingState.isMainThread = isMainThread();
            postingState.isPosting = true;
            if (postingState.canceled) {
                throw new EventBusException("Internal error. Abort state was not reset");
            }
            try {
                // 遍历事件队列
                while (!eventQueue.isEmpty()) {
                    // 发送单个事件
                    // eventQueue.remove(0)，从事件队列移除事件
                    postSingleEvent(eventQueue.remove(0), postingState);
                }
            } finally {
                postingState.isPosting = false;
                postingState.isMainThread = false;
            }
        }
    }
private void postSingleEvent(Object event, PostingThreadState postingState) throws Error {
        Class<?> eventClass = event.getClass();
        boolean subscriptionFound = false;
        // eventInheritance默认为true，表示是否向上查找事件的父类
        if (eventInheritance) {
            // 查找当前事件类型的Class，连同当前事件类型的Class保存到集合
            List<Class<?>> eventTypes = lookupAllEventTypes(eventClass);
            int countTypes = eventTypes.size();
            // 遍历Class集合，继续处理事件
            for (int h = 0; h < countTypes; h++) {
                Class<?> clazz = eventTypes.get(h);
                subscriptionFound |= postSingleEventForEventType(event, postingState, clazz);
            }
        } else {
            subscriptionFound = postSingleEventForEventType(event, postingState, eventClass);
        }
        if (!subscriptionFound) {
            if (logNoSubscriberMessages) {
                logger.log(Level.FINE, "No subscribers registered for event " + eventClass);
            }
            if (sendNoSubscriberEvent && eventClass != NoSubscriberEvent.class &&
                    eventClass != SubscriberExceptionEvent.class) {
                post(new NoSubscriberEvent(this, event));
            }
        }
    }
private boolean postSingleEventForEventType(Object event, PostingThreadState postingState, Class<?> eventClass) {
        CopyOnWriteArrayList<Subscription> subscriptions;
        synchronized (this) {
            // 获取事件类型对应的Subscription集合
            subscriptions = subscriptionsByEventType.get(eventClass);
        }
        // 如果已订阅了对应类型的事件
        if (subscriptions != null && !subscriptions.isEmpty()) {
            for (Subscription subscription : subscriptions) {
                // 记录事件
                postingState.event = event;
                // 记录对应的subscription
                postingState.subscription = subscription;
                boolean aborted = false;
                try {
                    // 最终的事件处理
                    postToSubscription(subscription, event, postingState.isMainThread);
                    aborted = postingState.canceled;
                } finally {
                    postingState.event = null;
                    postingState.subscription = null;
                    postingState.canceled = false;
                }
                if (aborted) {
                    break;
                }
            }
            return true;
        }
        return false;
    }
```



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

# 线程

## 类型

```java
public enum ThreadMode {
  //使用事件发送线程
  POSTING,
  //主线程，如果在子线程则立即执行，否则通过Handler添加到主线程消息队列执行
  MAIN,
  //主线程，始终通过Handler添加到主线程消息队列执行
  MAIN_ORDERED,
  //后台线程：如果当前不是UI线程，则使用当前线程。如果是UI线程，则使用线程池，一个一个事件处理
  BACKGROUND,
  //异步线程：可以多个线程一起处理
  ASYNC
}
```

## 原理

```java
private void postToSubscription(Subscription subscription, Object event, boolean isMainThread) {
        // 判断订阅事件方法的线程模式
        switch (subscription.subscriberMethod.threadMode) {
            // 默认的线程模式，在那个线程发送事件就在那个线程处理事件
            case POSTING:
                invokeSubscriber(subscription, event);
                break;
            // 在主线程处理事件
            case MAIN:
                // 如果在主线程发送事件，则直接在主线程通过反射处理事件
                if (isMainThread) {
                    invokeSubscriber(subscription, event);
                } else {
                     // 如果是在子线程发送事件，则将事件入队列，通过Handler切换到主线程执行处理事件
                    // mainThreadPoster 不为空
                    mainThreadPoster.enqueue(subscription, event);
                }
                break;
            // 无论在那个线程发送事件，都先将事件入队列，然后通过 Handler 切换到主线程，依次处理事件。
            // mainThreadPoster 不为空
            case MAIN_ORDERED:
                if (mainThreadPoster != null) {
                    mainThreadPoster.enqueue(subscription, event);
                } else {
                    invokeSubscriber(subscription, event);
                }
                break;
            case BACKGROUND:
                // 如果在主线程发送事件，则先将事件入队列，然后通过线程池依次处理事件
                if (isMainThread) {
                    backgroundPoster.enqueue(subscription, event);
                } else {
                    // 如果在子线程发送事件，则直接在发送事件的线程通过反射处理事件
                    invokeSubscriber(subscription, event);
                }
                break;
            // 无论在那个线程发送事件，都将事件入队列，然后通过线程池处理。
            case ASYNC:
                asyncPoster.enqueue(subscription, event);
                break;
            default:
                throw new IllegalStateException("Unknown thread mode: " + subscription.subscriberMethod.threadMode);
        }
    }
```

![img](https://upload-images.jianshu.io/upload_images/1633070-2cc84089d0fff8dc.png?imageMogr2/auto-orient/strip|imageView2/2/w/854/format/webp)

![img](https://upload-images.jianshu.io/upload_images/1633070-3637c94ce7ff4b07.png?imageMogr2/auto-orient/strip|imageView2/2/w/688/format/webp)

![img](https://upload-images.jianshu.io/upload_images/1633070-b3b3a17a97d95d02.png?imageMogr2/auto-orient/strip|imageView2/2/w/609/format/webp)

粘性事件：可以先发送事件再注册，保存到队列中

https://www.jianshu.com/p/d9516884dbd4

为什么不使用编译时注解？

> 1. apt生成静态代理类，继承SubscribeMethod
> 2. register的时候存入Map中
> 3. post的时候找到代理类，但是要调用哪个方法呢？
