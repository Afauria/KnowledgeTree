动态代理、适配器、工厂

```java
new Retrofit.Builder()
        .baseUrl(Config.getConfigString(context, Config.CONFIG_PLUGIN_SERVER_HOST))
        .addConverterFactory(GsonConverterFactory.create())
        .addCallAdapterFactory(RxJava2CallAdapterFactory.create()) //返回RxJava Observable对象
        .callbackExecutor(Executors.newSingleThreadExecutor()) //指定回调线程池
        .callFactory(new okhttp3.Call.Factory() { //自定义Call工厂，默认使用OkHttpClient的RealCall
            @Override
            public okhttp3.Call newCall(Request request) {
                return null;
            }
        })
        .build();
```

自定义三个工厂：调用get方法，根据返回值创建不同的适配器，封装Call对象

1. `CallAdapter.Factory`：可以包含多个，前两个都是私有类，只有`RxJavaCallAdapterFactory`可被外部使用
   1. `DefaultCallAdapterFactory`：默认适配器，什么也不做，返回OkHttp的Call对象，通过execute和enqueue发出请求
   2. `ExecutorCallAdapterFactory`：可以指定线程，判断Android平台默认通过Handler在主线程Callback，自定义则封装自定义Call，调用enqueue方法，代理原始Call的enqueue
   3. `RxJavaCallAdapterFactory`：返回Observable对象，subscribe的时候调用call发出请求，如果返回值不是Observable、Flowable等，则不创建适配器
2. `Converter.Factory`：可以包含多个，将数据转成特定对象
3. `Call.Factory`：只能有一个，默认使用OkHttpClient

为什么自定义的是工厂，不是具体对象？

> 自定义工厂可以创建多个对象，自定义对象只能使用一次

```java
static final class ExecutorCallbackCall<T> implements Call<T> {
  final Executor callbackExecutor;
  final Call<T> delegate;

  ExecutorCallbackCall(Executor callbackExecutor, Call<T> delegate) {
    this.callbackExecutor = callbackExecutor;
    this.delegate = delegate;
  }

  @Override public void enqueue(final Callback<T> callback) {
    checkNotNull(callback, "callback == null");
    //代理原始Call，回调的时候通过Executor调度线程
    delegate.enqueue(new Callback<T>() {
      @Override public void onResponse(Call<T> call, final Response<T> response) {
        callbackExecutor.execute(new Runnable() {
          @Override public void run() {
            if (delegate.isCanceled()) {
              // Emulate OkHttp's behavior of throwing/delivering an IOException on cancellation.
              callback.onFailure(ExecutorCallbackCall.this, new IOException("Canceled"));
            } else {
              callback.onResponse(ExecutorCallbackCall.this, response);
            }
          }
        });
      }

      @Override public void onFailure(Call<T> call, final Throwable t) {
        callbackExecutor.execute(new Runnable() {
          @Override public void run() {
            callback.onFailure(ExecutorCallbackCall.this, t);
          }
        });
      }
    });
  }

  @Override public boolean isExecuted() {
    return delegate.isExecuted();
  }

  @Override public Response<T> execute() throws IOException {
    return delegate.execute();
  }
  ...
}
```



```java
final class RxJava2CallAdapter<R> implements CallAdapter<R, Object> {

  @Override public Type responseType() {
    return responseType;
  }

  @Override public Object adapt(Call<R> call) {
    Observable<Response<R>> responseObservable = isAsync
        ? new CallEnqueueObservable<>(call)
        : new CallExecuteObservable<>(call);
    //Subscribe的时候调用Call的enqueue或者execute发出请求，onNext通知观察者
    Observable<?> observable;
    if (isResult) {
      observable = new ResultObservable<>(responseObservable);
    } else if (isBody) {
      observable = new BodyObservable<>(responseObservable);
    } else {
      observable = responseObservable;
    }

    if (scheduler != null) {
      observable = observable.subscribeOn(scheduler);
    }
    ...
    return observable;
  }
}
```

动态代理Service接口，调用接口方法，运行时解析注解，生成`ServiceMethod`，返回Call对象，如果设置了CallAdapter适配器，会包装Call对象，返回Observable对象，调用subscribe时触发

```java
public <T> T create(final Class<T> service) {
  Utils.validateServiceInterface(service);
  if (validateEagerly) {
    eagerlyValidateMethods(service);
  }
  return (T) Proxy.newProxyInstance(service.getClassLoader(), new Class<?>[] { service },
      new InvocationHandler() {
        private final Platform platform = Platform.get();

        @Override public Object invoke(Object proxy, Method method, @Nullable Object[] args)
            throws Throwable {
          // If the method is a method from Object then defer to normal invocation.
          if (method.getDeclaringClass() == Object.class) {
            return method.invoke(this, args);
          }
          if (platform.isDefaultMethod(method)) {
            return platform.invokeDefaultMethod(method, service, proxy, args);
          }
          ServiceMethod<Object, Object> serviceMethod =
              (ServiceMethod<Object, Object>) loadServiceMethod(method); //解析Service的方法注解，生成ServiceMethod并缓存，避免下次解析
          OkHttpCall<Object> okHttpCall = new OkHttpCall<>(serviceMethod, args);
          //ServiceMethod中调用自定义CallAdapter
          return serviceMethod.adapt(okHttpCall);
        }
      });
}
```
