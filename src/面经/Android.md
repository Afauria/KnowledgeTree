# Android笔试和面试题

- [AndroidInterview-Q-A](https://github.com/JackyAndroid/AndroidInterview-Q-A)
- [CharonChui/AndroidNote](https://github.com/CharonChui/AndroidNote)
- [linsir6/AndroidNote](https://github.com/linsir6/AndroidNote)
- [Awesome-Android-Interview](https://github.com/JsonChao/Awesome-Android-Interview)
- [Android-Daily-Interview](https://github.com/Moosphan/Android-Daily-Interview)
- [AndroidOfferKiller](https://github.com/Blankj/AndroidOfferKiller)
- [Android_QA](https://github.com/Omooo/Android_QA)
- [AndroidBookmark](https://github.com/mikaelzero/AndroidBookmark)

# Android

Android四大组件

> Activity Service Brocast ContentProvider

## Activity

Activity的生命周期

> onCreate()->onStart()->onResume()->onPause()->onStop()->onDestory()

Activity A启动B，生命周期顺序是怎样的

> A.onPause() —> B.onCreate() —>B.onStart() —>B.onResume() —>视情况回调A.onStop

A的onStop方法一定会回调吗

> Activity A 仍然可见，则不调用，如新Activity B背景透明或者没有铺满全屏(完全盖住Activity A)

onStart()和onResume()的区别

> onStart()在 Activity 即将对用户可见之前调用； onResume()在 Activity 即将开始与用户进行交互之前调用。 此时，Activity 处于 Activity 堆栈的顶层，并具有用户输入焦点。

启动一个新的Activity，它的界面具体是在什么时候能够加载完成

> Activity的界面加载与Window的联系，setContentView()方法只是把开发者自定义的布局加入到DecorView中，整个界面并未开始展示。

什么时候可以获取view的宽高？

> onWindowFocusChanged()在activity能获取到焦点时调用，此时activity的布局都绘制完成，能获取到view的宽高，界面真正能显示到前台

Activity的onNewIntent()方法什么时候会执行

> Activity复用的时候。例如singleTop和singleTask，在onResume之前调用

显式启动和隐式启动

> 显式启动：指定包名和类名，明确启动哪个组件
>
> 隐式启动：指定Action、Flag、Data，系统自动找到合适的组件启动

Activity启动方式

> * standard：标准模式，每次启动一个Activity都会重新创建一个新实例
> * singleTop：栈顶复用模式，如果新Activity已经位于任务栈的栈顶，那么Activity不会被重建，同时onNewIntent会被调用。
> * singleTask：栈内复用模式，只要Activity在一个栈中存在，那么多次启动此Activity都不会重新创建实例，同时onNewIntent会被调用。例如Home页
> * singleInstance：单实例模式，Activity单独位于一个任务栈中。例如Launcher

scheme使用场景，协议格式，如何使用

> URI（Universal Resource Identifier，统一资源标识符）：`scheme://authority/path?param`
>
> 1. 访问本地资源
> 2. FileProvider
> 3. deeplink直接跳转到APP某个页面：例如打电话、发短信、打开视频、网页等

Service启动方式

> startService()和bindService()
>
> * startService()： 生命周期与调用的Context无关，通过被调用stopService()或者自身调用stopSelf()结束.单向通信，Service响应接收到的intent但没有回调  
> * bindService()： 生命周期与调用的Context绑定，通过被调用unbindService()结束，或者调用的Context生命周期结束。双向通信，通过binder实现方法的调用与返回

Activity A使用startService()启动，Activity B和C使用bindService()启动，操作的是同一个Service，怎么结束这个Service？

> Activity A调用stopService(), Activity B和C都要调用unbindService()

广播的注册方式有几种？

> 静态注册和动态注册
>
> * 静态注册：在manifest.xml中定义注册，系统启动时由系统扫描自动注册，app即使没启动过也能接收广播
> * 动态注册：在代码中主动调用注册和注销注册的方法，只能在这段区间中接收广播

注册了的广播一定能接收到吗？

> 否 1.无法接收指定了权限的广播 2.有序广播中，具有更高优先级的接收器已经消费了广播

如何实现自定View

> 1）组合现有View 2）继承现有View
>
> onMeasure（） onLayout（） onDraw（），分别负责测量宽高，布局和绘制

View的`onMeasure(int widthMeasureSpec, int heightMeasureSpec)`中两个参数的作用是什么

> 一个32位的int值，高2位代表SpecMode测量模式，低30位代表SpecSize测量尺寸
>
> * UNSPECIFIED：父容器不对View有任何限制，一般用于系统内部，表示一种测量状态。 
> * EXACTLY：父容器已经检测出View所需要的精确大小，这个时候View的最终大小是就SpecSize，对应LayoutParams中的match_parent和具体数值两种模式。 
> * AT_MOST：父容器指定了一个可用大小即SpecSize，View的大小不能大于这个值，具体是什么值要看不同View的具体实现，它对应于LayoutParams中的wrap_content。

activity的自定义布局是一个LinearLayout，宽match_parent，高wrap_content，包含一个子控件TextView，宽match_parent，高wrap_content，问TextView中onMeasure的heightMeasureSpec是怎样的

> heightMeasureSpec中的SpecMode是AT_MOST，SpecSize是Activity的高度，而TextView的最终高度是TextView根据字符串和TextSize计算得出

View最终在屏幕上显示出来的宽高是在什么时候决定的？

>  onLayout（），会决定view的left，top，right，bottom，这四个值决定了最终绘制到屏幕上的大小， onMeasure（）的结果只是作为布局的输入信息

触摸事件分发流程？

> dispatchTouchEvent（）onInterceptTouchEvent（）onTouchEvent（）

一个LinearLayout里面包含一个子控件TextView，LinearLayout如何截取TextView的点击事件

> * 重写LinearLayout的onInterceptTouchEvent()，让其return true 
> * 重写LinearLayout的dispatchTouchEvent()，内部直接return true

TextView是否有方法阻止LinearLayout的拦截

> TextView设置parent.requestDisallowInterceptTouchEvent(true)，但只对1阶问题中第①种实现有效

一个ViewPager嵌套了一个Listview，一个是左右滑动，一个上下滑动，如何处理滑动冲突的？

> 根据具体场景决定要响应哪个方向的滑动。 在父容器ViewPager中，重写onInterceptTouchEvent()方法，判断当左右滑动时就拦截事件，上下滑动就不拦截，将事件交由子元素Listview来处理

Handler java层的实现原理是？知道Handler、Looper、Message、Message Queue之间的关系



Looper.loop（）是一个死循环，为什么不会卡死线程？

> 使用了 epoll，等待消息的时候会阻塞挂起

AIDL是什么？

> AIDLAndroid接口定义语言，用来方便实现进程间通信，Android 会根据AIDL文件自动生成java类用于进程间通讯。

Binder是什么？

> binder是Android实现的一种高效安全的IPC机制，

内存泄漏和内存溢出是什么？

> 内存泄漏：内存泄漏指程序运行过程中分配内存给临时变量，用完之后却没有被GC回收，始终占用着内存，既不能被使用也不能分配给其他程序，于是就发生了内存泄漏。
> 内存溢出就是指程序运行过程中申请的内存大于系统能够提供的内存，导致无法申请到足够的内存，于是就发生了内存溢出。

怎么检测？

> 多次进入通过Android Profiler /shell 命令 查看内存情况
> MAT工具查看内存镜像，查看gc root

什么是ANR，什么情况下会出现？

> ANR即Application Not Responding，顾名思义就是应用程序无响应。
> 在Android中，一般情况下，四大组件均是工作在主线程中的，Android 中的 Activity Manager 和 Window Manager 会随时监控应用程序的响应情况，如果因为一些耗时操作（网络请求或者IO操作）造成主线程阻塞一定时间（例如造成5s内不能响应用户事件或者BroadcastReceiver 的onReceive 方法执行时间超过 10s ），那么系统就会显示ANR对话框提示用户对应的应用处于无响应状态。

怎么分析 ANR？

> 导出 /data/anr/ 目录下生成 trace.txt 文件，trace 文件顶部的线程一般是 ANR 的元凶，同时注意死锁和等待的线程

主线程中的 Looper.loop() 处于阻塞的时候，为什么不会造成ANR？

> Looper.loop() 是在等待事件而阻塞的，但有事件过来后，会派发出去，如果这个事件没有得到及时的处理，才会出现 ANR。

MVC是什么

> 视图（View）：用户界面。控制器（Controller）：业务逻辑模型（Model）：数据保存
> View 传送指令到 Controller
> Controller 完成业务逻辑后，要求 Model 改变状态
> Model 将新的数据发送到 View，用户得到反馈

MVP是什么

> MVP 模式将 Controller 改名为 Presenter，同时改变了通信方向。
> 1. 各部分之间的通信，都是双向的。
> 2. View 与 Model 不发生联系，都通过 Presenter 传递。
> 3. View 非常薄，不部署任何业务逻辑，称为"被动视图"（Passive View），即没有任何主动性，而 Presenter非常厚，所有逻辑都部署在那里

MVC和MVP的区别

> 在MVP中View并不直接使用Model，它们之间的通信是通过Presenter (MVC中的Controller)来进行的，
> 所有的交互都发生在Presenter内部，而在MVC中View会直接从Model中读取数据而不是通过Controller

MVVM是什么

> MVVM 模式将 Presenter 改名为 ViewModel，基本上与 MVP 模式完全一致。
>
> 唯一的区别是，它采用双向绑定（data-binding）：View的变动，自动反映在 ViewModel，反之亦然。Angular 和 Ember 都采用这种模式。

Activity生命周期。

Fragment生命周期。

Fragment销毁生命周期执行顺序。

Hybrid开发流程、机制、技术选型。

WebView内存泄露。

事件分发机制。

了解过ANR吗。

什么原因会造成ANR。

Activity的响应时间为什么是5s。

事件分发机制。

怎么解决滑动冲突的。

Flutter线程机制，单线程多线程。

组件化开发。

Apk打包流程。

Apk更新流程。

多渠道打包。

怎么辨别华为的包更新别的应用市场的包。

自定义View流程。

View异步加载。

自定义View有哪些注意事项。

自定义View流程，举例。

自定义View刷新方式有哪些，执行流程。

事件分发原理，举例，延伸。

滑动冲突怎么解决。

怎么自己实现一个长按事件。

AMS了解多少。

Activity启动流程。

Binder机制。

Service是哪个线程，怎么通信。

热修复原理。

用热修复的过程中有遇到什么问题吗。

热修复，Sophix原理，ClassLoader。

全埋点是怎么做的。

录音功能是怎么做的。

编码解码相关。

Flutter相关。

Kotlin和Java混编有哪些需要注意的。

> KAPT注解处理
>
> Java @NonNull

App启动流程。

Handler消息机制。

Handler发送消息是一定立即执行的吗。

Handler运行在哪个线程。

子线程可以创建Handler吗，写法有哪些区别。

Handler是怎么实现主线程和子线程的通信的。

Handler在onCreate中发送大量数据会导致主线程卡顿吗。

Looper卡死为什么不会造成主线程的阻塞。

自定义View画板是怎么做的。

怎么检测页面的FPS。

LayoutInflater.inflate有几个参数，分别是什么意思。

Android中的Context了解多少。

Application里面可以弹窗吗。

Activity、Window、View三者的关系。

自定义一个圆角View。

有几种获取view宽高的方式。

view.post为什么可以获取到。

getWidth和getMesureWidth的区别。

# Kotlin

简单介绍下协程。

协程底层是怎么实现的。协程原理。

协程并发怎么处理的。多个协程怎么保证数据安全的。

协程launch有哪些参数。

介绍一下协程机制。

说一下Dispatchers，withContext，Scope他们的作用。

Kotlin定义变量的方式有哪些。

lateinit怎么确保使用时已经初始化。

let,run,apply介绍，区别。

inline原理。

说说by关键字。

代理和委托的区别。

双亲委托模式。

# 架构和设计

用过哪些设计模式。

介绍一下单例，饿汉式，懒汉式，双重锁。

介绍一下观察者模式。

代理模式和装饰模式的区别。

策略模式和状态模式的区别。

说一下你理解的MVP，MVVM。MVC，MVP，MVVM的区别。

使用MVVM有遇到什么问题吗。

重构做了哪些工作。

# JetPack

用过Jetpack中的哪些组件。

LiveData是怎么做数据派发的。

ViewModel屏幕旋转的时候怎么做到不丢失数据的。

使用MVVM的过程中有遇到什么问题吗。

Jetpack用了哪些组件。

ViewModel原理。

ViewModel如何数据共享。

ViewModel在Activity旋转时如何保存数据的。

ViewModel怎么创建的，存在哪。

使用ViewModel过程中有没有遇到什么问题。

LiveData底层原理，怎么派发数据的。

postValue是怎么执行的。

使用LiveData的过程中有没有遇到什么问题。

# 开源框架

有看过哪些三方库的源码。

Glide四层缓存机制。

Glide对Bitmap是怎么优化的。

Okhttp请求流程。OkHttp中有哪些设计模式。

Retrofit中的设计模式。

Retrofit.create做了哪些工作。

# 性能优化

性能优化做了哪些工作。

启动优化。

包体积优化。

绘制优化。

内存泄露，举例场景。

是怎么发现内存泄露的，怎么处理的。

线上的内存泄露是怎么监控的。

LeakCanary实现原理。

内存泄露原理，举例场景。

编译速度怎么提升的。

如果让你做一个自动化的工具去检测图片过大并自动压缩你会怎么做。
