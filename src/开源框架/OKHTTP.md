使用ExecutorService线程池，默认核心线程数为0，最大线程数Max，队列为空

Builder、责任链模式

`interceptors（自定义拦截器）-->BridgeIntercept（添加各种头）-->CachedIntercept（判断是否有缓存，或者取出缓存）-->ConnectionIntercept（建立网络连接）-->NetworkIntercept（自定义网络拦截器）-->CallServerInterceptor（读写IO）`

```java
public class RealInterceptChain {
    private final List<Interceptor> interceptors;
    public Response proceed() {
      RealInterceptorChain next = new RealInterceptorChain(interceptors, ..., index + 1);
      Response response = interceptor.get(index).intercept(next);
    }
}

public interface Interceptor {
    Response intercept(Chain chain);
}
public class XXXInterceptor {
    @Override 
    public Response intercept(Chain chain) throws IOException {
        //doSomething，发出请求之前，处理请求数据
        Response networkResponse = chain.proceed(requestBuilder.build());
        //doSomething，请求返回之后，处理返回数据
    }
}
```

应用拦截器

- 不需要关心像重定向和重试这样的中间响应。
- 总是调用一次，即使HTTP响应从缓存中获取服务。
- 监视应用原始意图。不关心OkHttp注入的像If-None-Match头。
- 允许短路并不调用Chain.proceed()。
- 允许重试并执行多个Chain.proceed()调用。

网络拦截器

- 可以操作像重定向和重试这样的中间响应。
- 对于短路网络的缓存响应不会调用。
- 监视即将要通过网络传输的数据。
- 访问运输请求的Connection。

Dispatcher内部有三个队列：

1. `runningSyncCalls`进行中的同步请求
2. `runningAsyncCalls`进行中的异步请求
3. `readyAsyncCalls`异步请求等待队列，超过最大请求数64，或者同一个服务端超过5个正在运行的请求

`ConnectionPool`中维护了一个`RealConnection`队列，RealConnection中封装了Socket操作

判断连接是否可重用：比较host或者路由信息。默认空闲连接为5，最长空闲连接为5分钟
