## Handler

### ThreadLocal：线程本地变量

每个线程各自拥有，互不影响。get到的值是不一样的

### Handler机制（占个坑，有空补图）

Handler：Looper（通过Looper.myLooper()从ThreadLocal中获取）、MessageQueue（Looper中获取）。

Handler构造方法：

* new Handler()：会从ThreadLocal中获取
* new Handler(Looper)：使用外部传递的Looper，配合HandlerThread使用，HandlerThread开启新线程，创建Looper对象，Looper循环会在HandlerThread中执行，即Handler的handleMessage会在HandlerThread中执行

MessageQueue：Message（使用单链表的形式，存储next Message），next()方法可以获取Message，没有消息则阻塞，底层使用native方法，mQuitAllowed（是否允许退出：prepareMainLooper()中设置为false，主线程不允许，prepare为true）

Message：next（存储next Message），what（消息code），obj（消息内容），target（消息的目标：Handler）

Looper：MessageQueue，`sThreadLocal= new ThreadLocal<Looper>()`（线程本地变量），Thread（Looper所在的线程，直接赋值为Thread.currentThread()），静态方法prepare()（ThreadLocal.set(new Looper())，创建自身，并设置到ThreadLocal中），静态方法loop()（不断从MessageQueue中获取消息，死循环，通过target对象执行handleMessage方法，在loop所在线程），静态方法myLooper()（ThreadLocal.get()获取当前线程的Looper），quit()终止循环（移除所有消息），quitSafely（移除延时消息，可以让已经在队列的消息执行完毕，共同点：不再接收消息）。静态方法getMainLooper()（保存了mainLooper对象，任何地方都能获取到主线程的Looper），

HandlerThread：继承Thread，在子线程中创建了Looper对象（prepare），并执行loop循环，即handleMessage在子线程

HandlerThread特点：

* 继承Thread，内部建立了Looper，并执行Looper循环，避免了手动维护Looper
* HandlerThread将loop转到子线程中处理，分担MainLooper的工作量，降低了主线程的压力，使主界面更流畅。
* 开启一个线程起到多个线程的作用。处理任务是串行执行，按消息发送顺序进行处理。HandlerThread本质是单线程，而不是并发，在线程内部，代码是串行处理的。
* 由于每一个任务都将以队列的方式逐个被执行到，一旦队列中有某个任务执行时间过长，那么就会导致后续的任务都会被延迟处理。
* 拥有自己的消息队列，它不会干扰或阻塞UI线程。
* 网络IO操作，HandlerThread并不适合，因为它只有一个线程，还得排队一个一个等着。

总结：

* Handler的Looper是在主线程，一般用法是在子线程sendMessage，然后在主线程handleMessage
* HandlerThread的Looper在子线程，即handleMessage在子线程

基本使用：

```java
HandlerThread thread = new HandlerThread(threadName);//创建线程
mHandlerThread.start();//启动线程
Handler handler = new Handler(thread.getLooper()){//传入子线程的Looper，
    //如果线程还没启动或者Looper还没创建，getLooper会wait阻塞，在run中创建完后，会notifyAll
    @Override
	public void handleMessage(Message msg)
    {
		//doSomething   
    }
}

```

HandlerThread关键代码

```java
@Override
public void run() {
	mTid = Process.myTid();
	Looper.prepare();
	synchronized (this) {
		mLooper = Looper.myLooper();
		notifyAll();//唤醒
    }
	Process.setThreadPriority(mPriority);
	onLooperPrepared();
	Looper.loop();
	mTid = -1;
}
public Looper getLooper() {
	if (!isAlive()) {
		return null;
	}
// If the thread has been started, wait until the looper has been created.
	synchronized (this) {
		while (isAlive() && mLooper == null) {
			try {
				wait();//阻塞
			} catch (InterruptedException e) {
			}
		}
	}
	return mLooper;
}
```

1. 简述Handler的运行过程，即Handler，Looper，Message如何协同工作的，同时收到多个消息时怎么处理怎么分发出去？
   * Message：
     * next
     * what
     * obj
     * target：存储Handler，分发msg给指定Handler处理
   * MessageQueue：存储管理Message，如enqueue，remove，quit等
   * ThreadLocal：线程本地变量。其他线程无法访问，不同线程获取到的值不一样。
   * Looper：
     * MessageQueue，
     * 使用sThreadLocal存储自身，任何地方可以通过静态方法获取当前线程的Looper。
     * Thread：Looper所在线程。Thread.currentThread()
     * prepare()和prepareMainLooper()：初始化。创建Looper和MessageQueue，存入ThreadLocal
     * loop()：无限循环，获取消息，分发消息（message有callback就让callback处理，没有的话就handleMessage处理）
   * Handler：
     * Looper：通过Looper.myLooper()获取，或者通过构造函数传入自定义Looper。
       * 如果是自己创建，需要在子线程中先prepare()、再创建Handler（防止获取不到myLooper抛异常），然后执行loop()
       * 也可以用HandlerThread，避免自己调用prepare和loop方法
     * MessageQueue：通过Looper获取
   * 插入消息的时候做了同步，防止多个线程同时插入消息
2. 同步消息、异步消息、屏障消息
   * postSyncBarrier发送屏障消息、屏障消息没有target，设置了同步屏障，则无法接收同步消息，只允许异步消息通过
   * 屏障消息的作用：给异步消息更高的优先级，如视图刷新
   * 异步消息：setAsynchronous，该方法是hide的。给系统开了个后门，防止上层业务加入异步消息，导致系统无法优先处理更重要的事情
   * 当ViewRootImpl调用scheduleTraversals进行measure和layout时，会向主线程的Handler添加同步屏障，遍历完成之后移除同步屏障。布局变化会触发requestLayout
3. post(Runnable)和sendMessage的区别
   * 写法上有区别，效果上没有区别
   * 一个是通过Message的callback处理消息，一个是通过handleMessage处理消息
4. HandlerThread的原理分析
   * 继承Thread，在run方法中创建了Looper（prepare），并开启循环（loop），Looper可以通过quit退出
5. ThreadLocal原理：每个线程保存了一个`ThreadLocalMap<ThreadLocal<?>, Object>`，get的时候通过当前线程获取`Thread.currentThread().threadLocalMaps.get(this)`
6. Handler内存泄漏解决：remove消息、使用静态内部类+弱引用

https://cloud.tencent.com/developer/article/1800399

https://cloud.tencent.com/developer/article/1924870?from=article.detail.1800399

https://cloud.tencent.com/developer/article/1834340?from=article.detail.1924870

https://cloud.tencent.com/developer/article/1604727?from=article.detail.1924870

https://www.icode9.com/content-4-944093.html

```java
@Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_test1:
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        mTvTest.setText("子线程真的可以更新UI吗？");
                    }
                }).start();
                break;
            case R.id.btn_test2:   //通过发送消息
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        mHandler.sendEmptyMessage(100);
                    }
                }).start();
                break;
            case R.id.btn_test3:  //通过Handler.post方法
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                mTvTest.setText("handler.post");
                            }
                        });
                    }
                }).start();
                break;
            case R.id.btn_test4:  //通过 view.post方法
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        mTvTest.post(new Runnable() {
                            @Override
                            public void run() {
                                mTvTest.setText("view.post");
                            }
                        });
                    }
                }).start();
                break;
            case R.id.btn_test5:  //通过 activity 的 runOnUiThread方法
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mTvTest.setText("runOnUIThread");
                            }
                        });
                    }
                }).start();
                break;
            default:
                break;
        }
    }
```

### **Handler 相关面试题：**

1. View.post 与 Handler.post 有何不同？
2. 主进程的 Looper 是何时创建的？
3. 线程关联的Looper 是如何与 Handler 绑定的？
4. MessageQueue 中如何提升消息的优先级？
5. 系统中的同步屏障机制是怎样的？
6. Handler源码架构
7. Looper死循环为什么不会导致应用卡斯
8. 使用Handler的postDelay消息队列有什么变化
9. 如何保证多个Handler线程安全

**
ThreadLocal 做什么的？**

**Thread 和 Handler 的关系**

**为什么不能在子线程更新 UI？**

**idleHandler 什么时候运行？**

**主线程为什么循环却不会卡死？**

**ContentProvider、Broadcast、Service 是怎么利用 Message 监控 ANR 的？**

select,poll,epoll

handler内存泄露的原因
