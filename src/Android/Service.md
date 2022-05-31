

# Service启动方式和生命周期

Service是Android中实现程序后台运行的解决方案，它非常适用于去执行那些不需要和用户交互而且还要求长期运行的任务。

1. Service默认并不会运行在子线程中，它也不运行在一个独立的进程中，**本地服务**默认在UI线程执行，但是Serivce一般都进行长期操作，为了避免阻塞UI线程或者防止ANR，**一般会在Service中开启子线程执行任务**。
2. Service的运行不依赖于任何用户界面，即使程序被切换到后台或者用户打开另一个应用程序，Service仍然能够保持正常运行，这也正是Service的使用场景。**当某个应用程序进程被杀掉时，所有依赖于该进程的Service也会停止运行**
3. 服务有两种启动方式：startService和bindService。两者可以同时使用
4. Service一旦创建，需要调用相应的停止方法，如`stopService() / stopSelf()、unbindService()`。如果两种启动方式同时使用，需要两个方法都调用，才会终止服务。
5. 系统只有在内存紧张的时候才会销毁Service
6. 为服务指定一个独立的进程：在manifest文件中设置android:process属性，此时服务称为**远程服务**

## startService()：不可交互的后台服务

1. 定义一个类继承Service，重写onBind()方法，返回null即可。重写onStartCommand(Intent intent, int flags,int startId)执行任务

2. 在Manifest清单文件中注册
3. 使用Context.startService(Intent)启动服务
4. 使用stopService(Intent)停止服务，或者调用Service的stopSelf()方法

`onStartCommand(Intent intent, int flags,int startId)`：

1. flag：服务的启动方式
   1. START_FLAG_REDELIVERY：如果你实现onStartCommand()来安排异步工作或者在另一个线程中工作, 那么你可能需要使用START_FLAG_REDELIVERY来让系统重新发送一个intent。这样如果你的服务在处理它的时候被Kill掉, Intent不会丢失. 
   2. START_FLAG_RETRY：表示服务之前被设为START_STICKY，则会被传入这个标记。 
      启动service的时候，onCreate方法只有第一次会调用，onStartCommand和onStart每次都被调用。onStartCommand会告诉系统如何重启服务，如判断是否异常终止后重新启动，在何种情况下异常终止.
2. startId：用于表示此次Client执行startService(...)的请求请求标识，在多次startService(...)的情况下，呈现0,1,2....递增。可以用于stopSelf(id)
3. 返回值：
   1. START_NOT_STICKY：“非粘性的”。如果在执行完onStartCommand后，服务被异常kill掉，系统不会自动重启该服务 
   2. START_STICKY：如果service进程被kill掉，保留service的状态为开始状态，但不保留onStartCommand方法传入的intent对象。随后系统会尝试重新创建service，由于服务状态为开始状态，所以创建服务后一定会调用onStartCommand(Intent,int,int)方法。如果在此期间没有任何启动命令被传递到service，那么参数Intent将为null。 
   3. START_REDELIVER_INTENT：重传Intent。使用这个返回值时，如果在执行完onStartCommand后，服务被异常kill掉，系统会自动重启该服务，重新执行onStartCommand，并将最后一次传入的Intent的值保留下来传入。
   4. START_STICKY_COMPATIBILITY：START_STICKY的兼容版本，但不保证服务被kill后一定能重启。

生命周期：

> onCreate()->onStartCommand() / onStart()（已过时）->onDestroy()

* `onCreate()`只调用一次，再次启动只会调用`onStartCommand()`
* 服务一旦开启就跟开启者没有关系了，开启者退出，服务还在后台运行。
* 开启者不能调用服务的方法，可以使用broadcast通信，缺点是太重

## bindService()：可交互的后台服务

1. 定义一个类继承Service
2. 定义一个内部类继承Binder，作用相当于服务的代理
   1. 实现自定义接口方法提供对外。
   2. 也可以定义一个方法直接返回Service对象，如MyService.this，然后调用Service方法
3. 重写onBind()方法，返回一个Binder对象
4. 在Manifest清单文件中注册
5. 定义一个ServiceConnection对象，绑定/解绑服务会回调onServiceConnected（可以拿到IBinder对象，强制转换为自定义Binder接口）、onServiceDisconnected，
6. 使用Context.bindService(Intent, ServiceConnection, int flags)启动服务
   1. flags:指定绑定是是否自动创建Service(如果Service还未创建)   0（不自动创建）或 BIND_AUTO_CREATE(自动创建)
7. 使用unbindService(ServiceConnection)停止服务

生命周期：

> onCreate()->onBind()->onUnbind()->onDestroy

* `onCreate()`只调用一次，不会调用`onStart()`或者`onStartCommand()`方法
* 生命周期和开启者绑定，开启者销毁，服务也会被销毁。如果有多个绑定者，则一个销毁，不会执行onUnbind方法
* 当没有任何client与Service绑定时，Service会自行销毁。
* 绑定者可以调用服务的方法
* onServiceConnected返回的binder对象：
  * 如果不跨进程，返回的就是Service的onBind返回的对象。
  * 如果跨进程，返回的是Service的代理对象

广播和服务为什么可以和Context生命周期绑定？

注册广播时LoadedApk中会存储信息，Context被销毁时，取出value，遍历注销广播和服务

```java
//LoadedApk。java
private final ArrayMap<Context, ArrayMap<BroadcastReceiver, ReceiverDispatcher>> mReceivers
    = new ArrayMap<>();
private final ArrayMap<Context, ArrayMap<ServiceConnection, LoadedApk.ServiceDispatcher>> mServices
    = new ArrayMap<>();
```

最终调用`removeContextRegistrations`注销广播和服务

```shell
ActivityThread.handleDestroyActivity
-->ContextImpl.scheduleFinalCleanup
-->ActivityThread.scheduleContextCleanup
-->ContextImpl.performFinalCleanup
-->LoadedApk.removeContextRegistrations # 检查广播和Service是否取消注册，打印内存泄漏日志，然后解绑广播和Service
-->ActivityManager.getService().unregisterReceiver(rd.getIIntentReceiver());
ActivityManager.getService().unbindService(sd.getIServiceConnection());
```

**注：BroadcastReceiver的Context不能用于绑定服务，因为广播生命周期较短。但可以在广播中startService()**

![](Service/Service生命周期.png)

## 启动并绑定Service生命周期

![](Service/Service启动并绑定生命周期.png)

# Service和Thread的区别

Thread是程序执行的最小单元，可以用Thread执行异步的操作。

- 独立于Activity：当Activity被销毁，线程没有主动停止或者run方法没有执行完毕，Thread会一直执行
- Activity销毁后，不再持有Thread的引用，无法对Thread进行控制。例如做心跳服务
- 另外，不同的Activity不能控制同一个Thread。

Service是Android提供的机制，本地服务是运行在主进程的（UI线程），远程服务是运行在独立进程的主线程

- 只有一个Service实例，可以在任何有Context的地方控制同一个Service。

# 本地服务和远程服务

本地服务：依附在主进程而不是独立的进程。

> 1. 节约了资源，并且不需要IPC。
> 2. 主进程被Kill后，服务便终止。

远程服务：Service在独立的进程（在Manifest文件中设置`android:process`属性），对应进程为包名+android:process的值。

> 1. 在Manifest文件中设置`android:process`属性。android:exported="false"设置是否允许其他应用调用此服务
> 2. 可以被其他进程复用，适用于为其他应用提供公共服务的Service。为系统常驻的Service，如天气服务
> 3. 有独立进程，不受其他进程影响
> 4. 具有更好的灵活性
> 5. 占有的资源较多，需要使用AIDL等方式进行IPC

# 前台服务

由于后台服务优先级相对比较低，当系统出现内存不足的情况下，它就有可能会被回收掉，所以前台服务就是来弥补这个缺点的，它可以一直保持运行状态而不被系统回收。例如：在状态栏中的天气预报

> 在Service的基础上创建一个Notification，然后使用Service的startForeground()方法即可启动为前台服务.

Android 8.0不允许应用启动后台服务，需要使用`startForegroundService`启动前台服务，并且必须在创建服务后的五秒内调用该服务的 `startForeground()` 函数。否则会抛出异常

# IntentService

Service生命周期在主线程执行，当需要执行耗时任务时，需要开启新线程。Android封装了`IntentService`，避免开发者手动创建线程。

1. 继承`IntentService`，重写`onHandlerIntent`方法。
2. 内部使用`HandlerThread`创建Looper线程，发送消息，在子线程中调用`onHandlerIntent`，执行完之后会自动结束服务。
3. 通过startService启动服务，多次启动服务，会按顺序调用多次`onHandleIntent`。
4. 使用消息队列处理多个Intent，共用一个Looper线程，排队执行，不是多线程并发

**如果要使用多线程，需要继承Service，然后在onStartCommand中创建子线程。每次startService都会开启一个子线程**

IntentService原理：使用Handler+HandlerThread实现单线程模型。Handler内部维护消息队列，多个Intent排队处理

```java
public abstract class IntentService extends Service {
    private volatile Looper mServiceLooper;
    private volatile ServiceHandler mServiceHandler;
    //使用Handler+HandlerThread实现单线程模型。Handler内部维护消息队列，多个Intent排队处理
    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) { super(looper); }
        @Override
        public void handleMessage(Message msg) {
            //3. 处理消息
            onHandleIntent((Intent)msg.obj);
            //4. 传入startId
            stopSelf(msg.arg1);
        }
    }
    @Override
    public void onCreate() {
        super.onCreate();
        //1. 创建一个Looper线程
        HandlerThread thread = new HandlerThread("IntentService[" + mName + "]");
        thread.start();
        mServiceLooper = thread.getLooper();
        mServiceHandler = new ServiceHandler(mServiceLooper);
    }
    @Override
    public void onStart(@Nullable Intent intent, int startId) {
        //2. 发送消息
        Message msg = mServiceHandler.obtainMessage();
        msg.arg1 = startId;
        msg.obj = intent;
        mServiceHandler.sendMessage(msg);
    }
    //5. 退出Looper循环，释放线程
    @Override
    public void onDestroy() {
        mServiceLooper.quit();
    }
    //在工作线程中处理消息
    @WorkerThread
    protected abstract void onHandleIntent(@Nullable Intent intent);
}
```
