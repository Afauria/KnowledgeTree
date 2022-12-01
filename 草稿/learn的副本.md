[Android应用启动流程](https://www.aengus.top/?p=51)

https://mp.weixin.qq.com/s/yEAdDRijRav9bYgvR8a8QQ

**什么叫体系化的论述呢？例如启动优化：**

1. 首先你要做好App 启动阶段，**系统framework是如何启动这个 App 的**，这个阶段是否可以优化？
2. App实际启动分析，**你得知道你 App 启动时耗时方法都哪些？造成这些耗时方法的原因有哪些？启动时系统资源占用如何？**
3. 然后根据上述两轮分析，开始准备实际的优化方向；
4. 项目实践：**在大型项目中落地**查看实际的收益，很多时候理论与实际是需要实践去验证的，很多优化需要不断的调优，才能拿到实际的收益；

你看第一步，就是就涉及到了framework相关知识，把 framework与具体的性能优化相结合，那就能体验出了解它的实际意义了，也没有那么容易忘记了。



当然，说这么多，很多小伙伴自己尝试做这个流程还是比较吃力的，毕竟不是网上搜搜就能找到每个阶段系统化的分析的，而且项目实践对于很多小红包也是个老大难的问题。



**所以今天我们给大家好好讲讲这几个阶段，以及如何学习这几个阶段中的技术细节。**

**
**

**阶段 1：系统framework是如何启动 App 的，这个阶段是否可以优化？**



系统是如何启动我们 App 的，这个非常关键，除了优化之外，启动的度量也离不开它，什么时候算启动，什么时候算结束。



当然很多小火把觉得 `application#onCreate`执行完就结束了，其实不然，一般要以实际内容的首屏出现为准，怎么定义首屏出现呢？

**
**

**为此我们准备了：**

> 1. Anroid手机启动流程及原理
> 2. APP启动状态分类及启动流程
> 3. AMS加载Applicaton流程
> 4. 系统启动过程中必处理的七个问题
> 5. WMS管理UI的流程对启动优化的意义



**阶段 2：App实际启动分析，你得知道你 App 启动时耗时方法都哪些？**



很多小伙伴去分析 App 启动，有的只能疯狂添加埋点，然后一个个看方法耗时？



进阶一点就是跑systrace，然后分析trace。



这两种方案不是说不好，只是说：**缺沉淀，偏单点，易复制，简单来说，随便谁都能做到。**



更好的方案，能够沉淀工具，可以查看线上大规模数据分析，观测数据维度多。



例如我们可以搞个线上trace工具，支持上报：

1. 不仅仅是孤零零的方法耗时，**还要有这个方法执行耗时过程中，实际占用了CPU多久，有没有可能很多时候都在等锁之类的；**
2. 启动过程中的**锁等待情况**，启动代码非常复杂，很多底层逻辑可能有相互等待锁的情况；
3. 启动时的线程情况，**有多少个线程，以及各个线程优先级分布，有没有可能线程太多？又或者子线程优先级超过主线程之类的；**
4. 启动时的内存情况，**GC次数，GC 可能会 stop the world，间接导致启动变慢；**
5. 启动时的 **CPU 负载，IO 负载**等等；

这样，就非常全面了，这不比加log香？而且这个工具未来可以随着自己知识认知的全面不断扩展。



因为锁、线程优先级，内存 GC，IO，CPU 负载等，这些都会涉及到一些理论知识，我们再一次理论与实践集合了。

**
**

**肯定有很多开发这方面涉及的不多，还处于被内存分布，GC 流程的阶段，没事，跟着我们一起学习。**

**
**

**阶段 3、4：实际优化与项目落地**



当我们结束流程分析，启动相关各种日志分析完成之后，我们就要着手优化了。



这个环节，除了点对点分析各个方法，此外我们还需要注重：

> 1. **沉淀框架工具；**
> 2. **结合系统负载，沉淀系统底层优化方案；**



例如我们会教大家发现了很多启动任务之后，如何去做拓扑分析，如何去通过更合理的方式去多线程的编排任务，以及各个线程的优先级该如何调整，最终形成一套多线程启动框架。



此外，对于启动阶段，最大化利用CPU资源等等，我们都会讲述。





https://cloud.tencent.com/developer/article/1604727?from=article.detail.1924870

## 一、Android

### 1.Activity

- 1.Activity的启动流程
- 2.onSaveInstanceState(),onRestoreInstanceState的掉用时机
- 5.横竖屏切换,按home键,按返回键,锁屏与解锁屏幕,跳转透明Activity界面,启动一个 Theme 为 Dialog 的 Activity，弹出Dialog时Activity的生命周期
- 7.Activity之间传递数据的方式Intent是否有大小限制，如果传递的数据量偏大，有哪些方案
- 11.ANR 的四种场景
- 12.onCreate和onRestoreInstance方法中恢复数据时的区别
- 13.activty间传递数据的方式
- 14.跨App启动Activity的方式,注意事项
- 15.Activity任务栈是什么
- 16.有哪些Activity常用的标记位Flags
- 17.Activity的数据是怎么保存的,进程被Kill后,保存的数据怎么恢复的

### 2.Service

- 1.service 的生命周期，两种启动方式的区别
- 2.Service启动流程
- 3.Service与Activity怎么实现通信
- 4.IntentService是什么,IntentService原理，应用场景及其与Service的区别
- 5.Service 的 onStartCommand 方法有几种返回值?各代表什么意思?
- 6.bindService和startService混合使用的生命周期以及怎么关闭

### 3.BroadcastReceiver

- 1.广播的分类和使用场景
- 2.广播的两种注册方式的区别
- 3.广播发送和接收的原理
- 4.本地广播和全局广播的区别

### 4.ContentProvider

- 1.什么是ContentProvider及其使用
- 2.ContentProvider,ContentResolver,ContentObserver之间的关系
- 3.ContentProvider的实现原理
- 4.ContentProvider的优点
- 5.Uri 是什么

### 5.Handler

- 1.Handler的实现原理
- 2.子线程中能不能直接new一个Handler,为什么主线程可以
- 主线程的Looper第一次调用loop方法,什么时候,哪个类
- 3.Handler导致的内存泄露原因及其解决方案
- 4.一个线程可以有几个Handler,几个Looper,几个MessageQueue对象
- 5.Message对象创建的方式有哪些 & 区别？
- Message.obtain()怎么维护消息池的
- 6.Handler 有哪些发送消息的方法
- 7.Handler的post与sendMessage的区别和应用场景
- 8.handler postDealy后消息队列有什么变化，假设先 postDelay 10s, 再postDelay 1s, 怎么处理这2条消息
- 9.MessageQueue是什么数据结构
- 10.Handler怎么做到的一个线程对应一个Looper，如何保证只有一个MessageQueue
- ThreadLocal在Handler机制中的作用
- 11.HandlerThread是什么 & 好处 &原理 & 使用场景
- 12.IdleHandler及其使用场景
- 13.消息屏障,同步屏障机制
- 14.子线程能不能更新UI
- 15.为什么Android系统不建议子线程访问UI
- 16.Android中为什么主线程不会因为Looper.loop()里的死循环卡死
- MessageQueue#next 在没有消息的时候会阻塞，如何恢复？
- 17.Handler消息机制中，一个looper是如何区分多个Handler的
- 当Activity有多个Handler的时候，怎么样区分当前消息由哪个Handler处理
- 处理message的时候怎么知道是去哪个callback处理的
- 18.Looper.quit/quitSafely的区别
- 19.通过Handler如何实现线程的切换
- 20.Handler 如何与 Looper 关联的
- 21.Looper 如何与 Thread 关联的
- 22.Looper.loop()源码
- 23.MessageQueue的enqueueMessage()方法如何进行线程同步的
- 24.MessageQueue的next()方法内部原理
- 25.子线程中是否可以用MainLooper去创建Handler，Looper和Handler是否一定处于一个线程
- 26.ANR和Handler的联系

### 6.View绘制

- 1.View绘制流程
- 2.MeasureSpec是什么
- 3.子View创建MeasureSpec创建规则是什么
- 4.自定义Viewwrap_content不起作用的原因
- 5.在Activity中获取某个View的宽高有几种方法
- 6.为什么onCreate获取不到View的宽高
- 7.View#post与Handler#post的区别
- 8.Android绘制和屏幕刷新机制原理
- 9.Choreography原理
- 10.什么是双缓冲
- 11.为什么使用SurfaceView
- 12.什么是SurfaceView
- 13.View和SurfaceView的区别
- 14.SurfaceView为什么可以直接子线程绘制
- 15.SurfaceView、TextureView、SurfaceTexture、GLSurfaceView
- 16.getWidth()方法和getMeasureWidth()区别
- 17.invalidate() 和 postInvalidate() 的区别
- 18.Requestlayout，onlayout，onDraw，DrawChild区别与联系
- 19.LinearLayout、FrameLayout 和 RelativeLayout 哪个效率高
- 20.LinearLayout的绘制流程
- 21.自定义 View 的流程和注意事项
- 22.自定义View如何考虑机型适配
- 23.自定义控件优化方案
- 24.invalidate怎么局部刷新
- 25.View加载流程（setContentView）

### 7.View事件分发

- 1.View事件分发机制
- 2.view的onTouchEvent，OnClickListerner和OnTouchListener的onTouch方法 三者优先级
- 3.onTouch 和onTouchEvent 的区别
- 4.ACTION_CANCEL什么时候触发
- 5.事件是先到DecorView还是先到Window
- 6.点击事件被拦截，但是想传到下面的View，如何操作
- 7.如何解决View的事件冲突
- 8.在 ViewGroup 中的 onTouchEvent 中消费 ACTION_DOWN 事件，ACTION_UP事件是怎么传递
- 9.Activity ViewGroup和View都不消费ACTION_DOWN,那么ACTION_UP事件是怎么传递的
- 10.同时对父 View 和子 View 设置点击方法，优先响应哪个
- 11.requestDisallowInterceptTouchEvent的调用时机

### 8.RecycleView

- 1.RecyclerView的多级缓存机制,每一级缓存具体作用是什么,分别在什么场景下会用到哪些缓存
- 2.RecyclerView的滑动回收复用机制
- 3.RecyclerView的刷新回收复用机制
- 4.RecyclerView 为什么要预布局
- 5.ListView 与 RecyclerView区别
- 6.RecyclerView性能优化

### 9.Viewpager&Fragment

- 1.Fragment的生命周期 & 结合Activity的生命周期
- 2.Activity和Fragment的通信方式， Fragment之间如何进行通信
- 3.为什么使用Fragment.setArguments(Bundle)传递参数
- 4.FragmentPageAdapter和FragmentStatePageAdapter区别及使用场景
- 5.Fragment懒加载
- 6.ViewPager2与ViewPager区别
- 7.Fragment嵌套问题

### 10.WebView

- 1.如何提高WebView加载速度
- 2.WebView与 js的交互
- 3.WebView的漏洞
- 4.JsBridge原理

### 11.动画

- 1.动画的类型
- 2.补间动画和属性动画的区别
- 3.ObjectAnimator，ValueAnimator及其区别
- 4.TimeInterpolator插值器，自定义插值器
- 5.TypeEvaluator估值器

### 12.Bitmap

- 1.Bitmap 内存占用的计算
- 2.getByteCount() & getAllocationByteCount()的区别
- 3.Bitmap的压缩方式
- 4.LruCache & DiskLruCache原理
- 5.如何设计一个图片加载库
- 6.有一张非常大的图片,如何去加载这张大图片
- 7.如果把drawable-xxhdpi下的图片移动到drawable-xhdpi下，图片内存是如何变的。
- 8.如果在hdpi、xxhdpi下放置了图片，加载的优先级。如果是400800，10801920，加载的优先级。

### 13.mvc&mvp&mvvm

- 1.MVC及其优缺点
- 2.MVP及其优缺点
- 3.MVVM及其优缺点
- 4.MVP如何管理Presenter的生命周期，何时取消网络请求

### 14.Binder

- 1.Android中进程和线程的关系,区别
- 2.为何需要进行IPC,多进程通信可能会出现什么问题
- 3.Android中IPC方式有几种、各种方式优缺点
- 4.为何新增Binder来作为主要的IPC方式
- 5.什么是Binder
- 6.Binder的原理
- 7.Binder Driver 如何在内核空间中做到一次拷贝的？
- 7.使用Binder进行数据传输的具体过程
- 8.Binder框架中ServiceManager的作用
- 14.使用 Binder 传输数据的最大限制是多少，被占满后会导致什么问题
- 15.Binder 驱动加载过程中有哪些重要的步骤
- 16.系统服务与bindService启动的服务的区别
- 17.Activity的bindService流程
- 18.不通过AIDL，手动编码来实现Binder的通信

### 15.内存泄漏&内存溢出

- 1.什么是OOM & 什么是内存泄漏以及原因
- 2.Thread是如何造成内存泄露的，如何解决？
- 3.Handler导致的内存泄露的原因以及如何解决
- 4.如何加载Bitmap防止内存溢出
- 5.MVP中如何处理Presenter层以防止内存泄漏的

### 16.性能优化

- 1.内存优化
- 2.启动优化
- 3.布局加载和绘制优化
- 4.卡顿优化
- 5.网络优化

### 17.Window&WindowManager

- 1.什么是Window
- 2.什么是WindowManager
- 3.什么是ViewRootImpl
- 4.什么是DecorView
- 5.Activity，View，Window三者之间的关系
- 6.DecorView什么时候被WindowManager添加到Window中

### 18.WMS

- 1.什么是WMS
- 2.WMS是如何管理Window的
- 3.IWindowSession是什么，WindowSession的创建过程是怎样的
- 4.WindowToken是什么
- 5.WindowState是什么
- 6.Android窗口大概分为几种？分组原理是什么
- 7.Dialog的Context只能是Activity的Context，不能是Application的Context
- 8.App应用程序如何与SurfaceFlinger通信的
- View 的绘制是如何把数据传递给 SurfaceFlinger 的
- 9.共享内存的具体实现是什么
- 10.relayout是如何向SurfaceFlinger申请Surface
- 11.什么是Surface

### 19.AMS

- 1.ActivityManagerService是什么？什么时候初始化的？有什么作用？
- 2.ActivityThread是什么?ApplicationThread是什么?他们的区别
- 3.Instrumentation是什么？和ActivityThread是什么关系？
- 4.ActivityManagerService和zygote进程通信是如何实现的
- 5.ActivityRecord、TaskRecord、ActivityStack，ActivityStackSupervisor，ProcessRecord
- 6.ActivityManager、ActivityManagerService、ActivityManagerNative、ActivityManagerProxy的关系
- 7.手写实现简化版AMS

### 20.系统启动

- 1.android系统启动流程
- 2.SystemServer，ServiceManager，SystemServiceManager的关系
- 3.孵化应用进程这种事为什么不交给SystemServer来做，而专门设计一个Zygote
- 4.Zygote的IPC通信机制为什么使用socket而不采用binder

### 21.App启动&打包&安装

- 1.应用启动流程
- 2.apk组成和Android的打包流程
- 3.Android的签名机制，签名如何实现的,v2相比于v1签名机制的改变
- 4.APK的安装流程

### 22.序列化

- 1.什么是序列化
- 2.为什么需要使用序列化和反序列化
- 3.序列化的有哪些好处
- 4.Serializable 和 Parcelable 的区别
- 5.什么是serialVersionUID
- 6.为什么还要显示指定serialVersionUID的值?

### 23.Art & Dalvik 及其区别

- 1.Art & Dalvik 及其区别

### 24.模块化&组件化

- 1.什么是模块化
- 2.什么是组件化
- 3.组件化优点和方案
- 4.组件独立调试
- 5.组件间通信
- 6.Aplication动态加载
- 7.ARouter原理

### 25.热修复&插件化

- 1.插件化的定义
- 2.插件化的优势
- 3.插件化框架对比
- 4.插件化流程
- 5.插件化类加载原理
- 6.插件化资源加载原理
- 7.插件化Activity加载原理
- 8.热修复和插件化区别
- 9.热修复原理

### 26.AOP

- 1.AOP是什么
- 2.AOP的优点
- 3.AOP的实现方式,APT,AspectJ,ASM,epic,hook
- 27.jectpack
- 1.Navigation
- 2.DataBinding
- 3.Viewmodel
- 4.livedata
- 5.liferecycle

### 27.开源框架

- 1.Okhttp源码流程,线程池
- 2.Okhttp拦截器,addInterceptor 和 addNetworkdInterceptor区别
- 3.Okhttp责任链模式
- 4.Okhttp缓存怎么处理
- 5.Okhttp连接池和socket复用
- 6.Glide怎么绑定生命周期
- 7.Glide缓存机制,内存缓存，磁盘缓存
- 8.Glide与Picasso的区别
- 9.LruCache原理
- 10.Retrofit源码流程,动态代理
- 11.LeakCanary弱引用,源码流程
- 12.Eventbus
- 13.Rxjava

## 二、Java

### 1.HashMap

- 1.HashMap原理
- 2.HashMap中put()如何实现的
- 3.HashMap中get()如何实现的
- 4.为什么HashMap线程不安全
- 5.HashMap1.7和1.8有哪些区别
- 6.解决hash冲突的时候，为什么用红黑树
- 7.红黑树的效率高，为什么一开始不用红黑树存储
- 8.不用红黑树，用二叉查找树可以不
- 9.为什么阀值是8才转为红黑树
- 10.为什么退化为链表的阈值是6
- 11.hash冲突有哪些解决办法
- 12.HashMap在什么条件下扩容
- 13.HashMap中hash函数怎么实现的，还有哪些hash函数的实现方式
- 14.为什么不直接将hashcode作为哈希值去做取模,而是要先高16位异或低16位
- 15.为什么扩容是2的次幂
- 16.链表的查找的时间复杂度是多少
- 17.红黑树

### 2.ArrayList

### 3.Jvm

- 1.Jvm的内存模型,每个里面都保存的什么
- 2.类加载机制的几个阶段加载、验证、准备、解析、初始化、使用、卸载
- 3.对象实例化时的顺序
- 4.类加载器,双亲委派及其优势
- 5.垃圾回收机制

### 4.多线程

- 1.Java中创建线程的方式,Callable,Runnable,Future,FutureTask
- 2.线程的几种状态
- 3.谈谈线程死锁，如何有效的避免线程死锁？
- 4.如何实现多线程中的同步
- 5.synchronized和Lock的使用、区别,原理；
- 6.volatile，synchronized和volatile的区别？为何不用volatile替代synchronized？
- 7.锁的分类，锁的几种状态，CAS原理
- 8.为什么会有线程安全？如何保证线程安全
- 9.sleep()与wait()区别,run和start的区别,notify和notifyall区别,锁池,等待池
- 10.Java多线程通信
- 11.为什么Java用线程池
- 12.Java中的线程池参数,共有几种

### 5.注解

- 1.注解的分类和底层实现原理
- 2.自定义注解

### 6.反射

- 1.什么是反射
- 2.反射机制的相关类
- 3.反射中如何获取Class类的实例
- 4.如何获取一个类的属性对象 & 构造器对象 & 方法对象
- 5.Class.getField和getDeclaredField的区别，getDeclaredMethod和getMethod的区别
- 6.反射机制的优缺点

### 7.泛型

### 8.设计模式





性能优化：https://zhuanlan.zhihu.com/p/30691789

https://github.com/pengxurui/Android-NoteBook

PMS源码： https://mp.weixin.qq.com/s/eACbMYJ042YKglg7fpEb2w

##### **高频问题**

##### HashMap 1.7，1.8的差异，1.8中什么情况下转换为红黑树，构造函数中参数代表的意思。

#####  

##### 用什么Map可以保证线程安全，为什么？ConcurrentHashMap为什么能保证线程安全？1.7和1.8原理有什么差异。

#####  

##### 有多少种单例模式，枚举算不算单例，单例模式中不用volatile会导致什么问题？volatile特性是什么？为什么android中不推荐使用枚举。

#####  

##### Glide中怎么实现图片的加载进度条，Glide的缓存是怎么设计的？为什么要用弱引用。

#####  

##### implementation 和 api的区别是什么？

#####  

##### 事件分发的流程，以及怎么解决滑动冲突？

#####  

##### 事件是怎么产生的？mFirstTarget 为什么是一个链表？

#####  

##### 自定义View需要经历哪几个过程？

#####  

##### A 跳转到 B页面，两个页面的生命周期怎么走？什么情况下A的stop()不会执行。

#####  

##### Activity 的4中启动模式分别是什么，有什么不同。

#####  

##### okhttp中有几个队列？分别干什么用的？怎么取消一个请求？

#####  

##### Rxjava中map和flatMap有什么区别，都用过什么操作符。

#####  

##### 如果Rxjava组合发送任务，中间任务出现异常，其他任务该怎么处理。

#####  

##### 哪个场景会发生内存泄露，内存泄露怎么检测，怎么解决。以及leak cannery内部原理是什么？为什么新版本的不需要在Application中注册了。

#####  

##### 手机适配问题怎么处理，都有什么方案。

#####  

##### Android9 10 11 都更新了什么新特性，新版本中无法获取IMEI怎么处理。

#####  

##### 数据序列化有那俩种方式，Serialization和Parcelable区别，如果持久化需要用哪一个？

#####  

##### 组件化怎么分层，各个组件之间怎么通信。

#####  

##### 怎防止程序崩溃，如果已经到了Thread.UncaughtExceptionHandler是否可以让程序继续运行。

#####  

##### Handler Looper mesaageQueue message 之间的关系。

#####  

##### 子线程一定不能更新ui么？什么时候可以？什么时候不可以。检测逻辑是在什么阶段初始化的。

#####  

##### ANR发生的原理是什么， 怎么排查。

#####  

##### 程序怎么保活。

#####  

##### 说下路由ARoute的实现原理，怎么处理页面过多内存占用过大问题。

#####  

##### 线程池都什么时候用，怎么创建，构造函数中的参数分别代表什么意思？

#####  

##### 进程优先级。

#####  

##### 反向输出字符串。

#####  

##### 两个有序链表合并。

#####  

##### 字符串移除多余空格，且技术单词首字符大写。

#####  

##### 二叉树中和为某一值的路径。

#####  

##### 本地广播和正常广播的区别。

#####  

##### 二进制低位转高位。

#####  

##### 字符串数组判重。

#####  

##### 二叉树 判断是否为搜索二叉树。

#####  

##### Activity启动流程，Launcher启动流程。

#####  

##### **公司面试体验分享**



**天眼查（大小周，小周1.2倍）**



面试体验 ★★★★



这公司面试体验很好，一共三轮面试，技术面二面，HR一面。



一面的面试官技术水平非常可以，跟大厂是同一级别，考察的细节很到位，都是比较注意的点，并非故意刁难，沟通过程也是非常愉快。



二面是leader面，相对注重整体架构考察，沟通也相对顺利。



HR面其实体验不是很好，介绍公司政策不是很专业，总是靠挖掘，有点像挤牙膏沟通，薪资说实话一般般。中等偏下。



总体下来体验比较好，因为这个是第一家面试的公司，属于热身，学到了很多，尤其感谢1面面试官。





**北京流体网络科技有限公司**



面试体验 ★★★



面试的是1306闪电盒子部门，一共三面。二轮技术，一轮HR。



一面：一面应该是他们的同事面的，年龄偏大，技术方面见仁见智，有点偏离面试本心，问的问题我反问回去也不能解答。



举个例子：比如我做过电量优化，测试方式通过真机测试。



他问：怎么保证多台手机电池性能相同？



我答：尽量用同一型号，同一批次，电池循环次数相近的手机。



他问：这也不能保证电池性能相近。（这个问题已经脱离面试本心了，继续争论已无意义）



我反问：你们怎么解决。



他说：他们不需要电量优化。



这个电量优化其实是一个相对指标，对比对象是优化前的耗电指数，取样频率越多数据偏差越小。更精确的做法是拆卸电池，连接电流表去测试。



二面：二面是CTO面试，面试问题很多都是从项目出发，架构设计，发版节奏，以及集成链路等多方面沟通了， 沟通过程更贴近面试场景。



三面：HR面，这个过程是我比较难受的，只能说这个HR专业性太强了，她的各种计算会给你算蒙的，有很多个算法去计算你的收入。



反正按她的算法来说，他们给的薪资低，而你的综合收入反而高，后来我才发现他把公积金都算成你的收入了。FUC****,最后给了Offer但是直接拒绝了。



**酷划在线（工作时间早10-晚10）**



面试体验 ★



这个公司三轮面试，二轮技术面，一轮HR面，这个公司必须得吐槽下，太坑了。



首先招聘给你打电话的时候沟通的很好，说什么公司营业额10亿，承接头条，腾讯等一线流量对接，忽悠你接受他们的面试。



一面：视频面，面试了一个小时左右，视频面很中规中矩，沟通都很好，没有什么问题，面试官也比较满意。



但是面试不能一条龙面完比较浪费时间，所以提出此诉求，面试官也很好，说HR联系我沟通。



HR沟通：电话给我说让我去一次现场，一次性把面试流程走完。



我反问：是不是先让二面面试官仔细看下简历，避免浪费双方时间。



HR反馈：二面面试官非常感兴趣，希望尽快去现场，所以我同意了。并且去之前特意整理了下发型。



二面：骑上我心爱的小摩托，一路都没堵车，结果到早了。这个地址居然是一个商场，他们在商场的顶层，等了一会面试官，终于2点了，二面和一个妹子一起来的。



上来自我介绍，开始面试，面试官问了我几个基础问题，我回答的还算可以，问了期望薪资，然后说出去商量一下，然后招聘小能手告诉我说今天先回去，后续等3面。



.......我请了半天假，整理了发型，你来了问15分钟让我走？？？？主要是没有异常表现啊，没抬杠没吵架的。这是闹哪样？？？



后来又给我打电话，问我有没有入职其他公司，还是否考虑机会，我考虑.......



各位老铁这个公司一定避开啊，即便招聘小能手很能忽悠，咱可不能再信了。。。



**GamesVessel**



面试体验 ★★



这是一家做游戏的小公司，一共三轮面试，一轮笔试，一轮技术，最后HR，这家公司体验不是很好。



笔试：笔试比较简单，笔试的必要性不是很大，全是生命周期相关，并且笔试内容面试官都没有参考。



一面：因为是游戏公司，因此在面试之前我就打电话说明了自己的实际情况，并且把项目经验简单的进 行了简单阐述，最终以没有游戏经验打压。



HR面：HR也因为没有游戏经验打压工资，虽然给了Offer，但是这种行为真是没必要，不仅浪费公司时间，也浪费自己时间。



**58同城**



面试体验 ★★★★



面试的是58同城的58学车部门，一共四轮面试，3轮技术面，一轮HR面 他们应该是交叉面试，因此技术面相对比较全面，体验很好，交流很顺利，尤其是三面，思维方式，以及考察问题都很专业。HR面也很专业，介绍公司情况，部门前景，以及政策制度都很透彻，给了Offer，薪资相对也偏低。



以下几个是58特殊考察的几个题目：1.thread wait sleep join 有什么区别，主要考察wait sleep相关区别。2.mvvp 和mvp的区别，细节里怎么实现的双向绑定。3.打渠道包 怎么写入渠道数据（app签名在哪）这里涉及V1,V2,V3签名差异。4.rgb565和rgb888有什么区别，主要考察数据存储位。5.自定义view，实现一个自定义View，且支持按下放大，松开缩小动画。



**邦邦汽服**



面试体验 ★★



这个不知道有几轮面试，反正一面是视频面



面试官水平比较差，问的东西太多陈旧，考察点也远不够，也许是面向的岗位定位不是很高，技术类没有什么难点，应该是通关了，但是问了意向薪资后，没有了后续。



建议应聘高级工程师的可以避开这个公司了。



**博汇科技**



面试体验 ★★★



这个没啥说的，做军工项目的，我进去一看还是十几寸的显示器，打开的全是Eclipse，面试官问的东西也跟想象的相当，面试完还给了Offer，当时婉拒了，避免浪费对方时间。



**Kika（新美互通科技有限公司）**



面试体验 ★★★★★



这家公司是一个很棒的公司，无论面试体验，以及公司氛围，还有发展趋势都是一家很好的公司。



一共五轮面试，一面二面视频面，三面CTO面，四面项目负责人，五面CEO+HR主管



一面：相对基础，问了post get区别，三次握手，jvm的架构，各种viewgroup分别测量几次，



二面：相对偏Android面，主要考察Android框架源码相关，也很不错。



三面：CTO主要问项目相关，考察了下算法相关的内容。



四面：招聘助手+项目负责人一起面，主要问了一些项目周期，发版节奏，团队管理，问题处理等多维度的问题。



五面：CEO+HR主管，主要问了创业过程有啥收获，以后职业发展规划相关。



这家公司真的大赞，主营业务是做多语言输入法的，用户量6亿，后续可能会在多方面有涉猎，面试环节也大赞，除一面二面外，后续三面现场一把梭，领导没有架子。



最后面试完成还给了面试纪念品，这才是真正招人的态度。找机会的同学可以着重看下这家公司。



第二天发了Offer，钱也到位，奈何综合原因没有去，很是抱歉。



**SHAREit（茄子快传）**



面试体验★★★★★



这家公司是一家高速发展的公司，面试体验，工作氛围俱佳。



一共三轮面试，两轮技术面，一轮HR面



一面：问了很多基础，以及细节，问题内容都在顶部大类中，唯一差异让现场写生产消费者模型，面试官比较满意，直接进入二面。



二面：技术Leader，问了Glide存储方式，每一个Activity缓存图片是否分开缓存，算法相关考察了按层遍历二叉树，并输出每层的最后一个节点，并且进行了4 -5 种变化，每种变化实现方式。



在遇到问题的时候，面试官会引导你去思考，去解决，最终会给解决方案。这个行为大赞。



最后给了Offer，但是因为阿里也给了Offer，所以放弃了。



**美团优选（大小周，且每天晚上9点后下班）**



面试体验 ★★★★★



一共三轮面试，全是视频面。



一面：基础面试，面试官比较憨厚，基础部分不在赘述，特殊性考察有：两个有序链表合并。字符串移除多余空格，且技术单词首字符大写。



二面：主要考察项目，解决方案，沟通都很丝滑。



HR：没想到这个流程比较慢，可能他们候选人比较多，最后给了Offer定级L7，最后也是因为选择了阿里放弃了。



**字节跳动（大小周，更正：目前双休）**



面试体验 ★★★★★



字节跳动，我是连被干翻2次，面试官确实是牛逼。



我第一次投的是抖音部门，过了一面，二面挂，面试评语是 基础相对扎实，但是项目架构略有欠缺，因此挂在二面。



正在失望之余，字节的懂车帝又打来电话，说简历又被捞起来了，又去面了懂车帝，又挂了。



懂车帝一面确实比较牛逼，比如我记录了部分问题:



如果想统一项目的线程池，包括三方引入包的线程池，怎么处理。（最后解答可通过字节码修改实现）



如果想监控某一线程的耗时超过300毫秒的任务需要怎么操作。（可反射获取Handler抓取每个Message的执行耗时）



如果项目出现未捕获的异常，怎么预操作可以防止Crash。



如果设计一个App的启动框架，需要考虑什么问题，怎么处理同步异步的加载任务调度。



glide 加载原理，怎么感知加载和暂停。



okhttp加载原理，怎么控制同步和异步任务。



说一个项目难点，具体怎么解决的。



都用过什么设计模式。



mvp mvvm都在什么场景下使用。



一个int数组怎么判断是搜索二叉树的后续遍历。



**小米**



面试体验 ★★★



小米面试体验不是很好，一开始HR沟通是面试小米的IOT部门，从投简历到面试大概经历了一个星期。



一面：体验很好，面试官很谦和，问题也很有质量，大概持续了1小时20分钟，面试官对我也比较满意。



二面：二面跟一面差不多又过了一个多星期，这个效率真是有点差劲，关键是二面是小米电视部门来面的，跟之前的IOT不相关了。



三面：还是小米电视的人，聊了一堆项目相关的内容。



其实我是一个老米粉，从米1一直到米10，一路走来对小米这家公司是有情怀的，但是有点搞不懂为啥面试链路是这样的。希望多多改进啊，面试体检有点差。



**作业帮（每天晚上9点以后下班）**



面试体验 ★★★



这个公司是同事推荐的，因此面试流程相对迅速，第一天内推，第二天面试，面试分为三轮。



一面:同事面，问的范围比较广，面试官也比较善于沟通，很愉快。



二面:leader面，这个leader就是我同事的前同事，之前360搞浏览器内核的，不知道为啥搞Android了，性格相对比较闷，沟通反馈很差，沟通效率比较低。



为了缓解沟通尴尬，我尽量将问题回答映射到生活场景，还被他怼了，说：技术不用举例子......



**一点咨询（有不加班的文化）**



面试体验 ★★★★★



这公司也是同事内推，百度旗下的公司，一共四面，三轮技术，一轮HR。



这个公司面试体验非常棒，效率及其高，所有面试均视频完成，一天内完成了三轮技术面。



比较有代表性的是三面问了一个问题，如果给你足够的人，足够的钱，足够的资源，让你去学油画，你应该怎么做？





**阿里巴巴**



面试体验 ★★★★★



阿里巴巴，我面试的是阿里妈妈部门，是阿里比较赚钱的部门，一共5轮面试。



一面：纯算法，大概写了一个半小时，3道算法题，每一行代码都评审解读，面试官就是我现在的同事，上两天我已经嘲讽他了，并且劝他以后要懂得善良。



二面：Android相关，面试官涉猎比较广，聊了很多，最后聊到面试官说，咱今天差不多了已经一个半小时了，等你入职了咱继续聊。



三面：leader面，这个leader不是吹，太特么能砍了， 从生活到工作，到技术，到发展，整体扯了一遍，我俩又干了一个半小时。



四面：P9大Boss面，这个主要聊了一些发展规划相关的，聊了半小时左右。



五面：HR面，大概聊了1小时左右，主要问了一些之前公司的为人处事方式，还有一些基本信息，介绍了阿里的政策制度，福利待遇等等。











- 对于 App 启动，你们 App如何定义启动的开始与结束，有做过相关优化吗？**（腾讯、字节）**
- 内存一直是移动端设备的比较宝贵的资源，在 App 运行过程中，哪些对象比较耗费内存，针对这些对象做过监控或者针对性优化吗？ **（美团）**
- 在你的项目中是如何检测内存泄露的？**（阿里）**
- Apk 体积这块有做过一些极致的事情吗?举例说明一下 **（美团）**
- 自定义 View 的流程是？从性能的角度考虑有哪些注意事项 **（阿里）**
- 说下造成ANR的原因及解决办法？**（腾讯、阿里）**
- 在你的项目中是如何避免oom的？ **（网易）**
- App中存储这块了解的多吗？传统的 Sp存储存在什么问题，有什么推荐的方案吗？ **（字节）**
- App 运行过程中，卡顿这块指标你们是如何定义的？针对该指标线上如何获取以及对应的优化策略是什么 **（百度）**
- 你们的 Crash 率的指标是多少，如何解决疑难 Crash，举几个相关例子? **（字节）**
- 有深入了解过LeakCanary吗？他的检测内存泄漏的原理与基本流程是怎样？**（字节）**
- 网络这块，对于弱网的判断我们可以怎么做？如何提升弱网下 App的体验？**（腾讯）**



1.Android中多进程通信的方式有哪些？

2.进程通信你用过哪些？原理是什么？（字节跳动、小米）

3.描述下Binder机制原理？（东方头条）

4.Binder线程池的工作过程是什么样？（东方头条）

5.Handler怎么进行线程通信，原理是什么？（东方头条）

6.Handler如果没有消息处理是阻塞的还是非阻塞的？（字节跳动、小米）

7.handler.post(Runnable) runnable是如何执行的？（字节跳动、小米）

8.handler的Callback和handlemessage都存在，但callback返回true handleMessage还会执行么？（字节跳动、小米）

9.Handler的sendMessage和postDelay的区别？（字节跳动）

10.……



- View 绘制流程？onMeasure职责是做什么的？**（腾讯）**
- Activity如何与window与view进行分工合作的？**（阿里）**
- onResume函数中度量宽高有效吗 ？**（字节）**
- 子线程中view.setText 一定会报错么，为什么？**（贝壳）**
- View的绘制过程都是用的同一个canvas吗？**（小米）**
- 说下AMS startActivity流程，WMS了解吗，聊下WMS **（Vivo）**
- AMS是如何启动的？**（美团）**
- Zygote为什么不采用Binder机制进行IPC通信，而要用socket？**（拼多多）**
- Zygote、SystemServer 是干什么的，这么设计的好处知道吗？**（字节）**
- dumpsys的信息在哪里打印的？ **（百度）**
- 横竖屏切换时Activity的生命周期？**（字节）**
- Activity启动模式了解吗？栈是如何管理的呢？**（腾讯）**
- 谈谈Android系统的内存管理 **（美团）**
- 能不能介绍下Activity的启动流程？ **（阿里）**
- AMS是如何启动的？
- 如何理解startActivity的流程?
- 我们是如何应用AMS的核心原理？

# WMS

1. View绘制流程
2. View度量和布局原理
3. Activity、View、Window分工设计
4. WMS如何管理View
5. WMS如何与AMS等进程交互

# AMS

1. AMS如何启动
2. 如何理解startActivity流程
3. AMS对Activity生命周期管理
4. AMS核心原理的实际应用

9. 

### **Binder相关面试题：**

为什么Android使用Bindier

Bindier一次拷贝原理

Binder驱动作用机制

Intent传递参数限制，为什么，Binder驱动传递限制？

1. Activity 启动时与 AMS 交互，binder 交互的细节是怎么样的？
2. ActivityThread 中的 ApplicationThread 是什么作用？
3. binder 交互过程中涉及到多少个线程？
4. 启动过程中如果想监控 binder 交互的频率，怎么做？
5. binder 交互过程中，想要监控传输数据的大小，防止数据过大造成崩溃怎么做？









**【爱奇艺】一面**


1.handler里面的nativepollonce 为什么不会anr？

2.对称加密和非对称加密的区别。


3.布局嵌套过深会导致什么问题？


4.java为什么跨平台？c是跨平台吗？


5.app打包过程。


6.协程挂起和线程阻塞的区别。


7.so文件加载流程。


8.AIDL怎么实现？

9.字节码是什么？



**【货拉拉一面】**


1.简述https的握手流程，私钥加密谁都可以破解，那么有什么意义？


2.oom能否try catch？


3.公钥和私钥的区别。


4.客户端怎么安全保存私钥？


5.怎么加载一张超大图？


6.采集app启动时间原理。


7.exception分类。oom属于哪种分类？


8.弱网 无网优化。



**【货拉拉二面】**


1.说下项目中怎么运用MVVM？


2.viewModel的源码实现。


3.设计一个图片加载框架（包含内存缓存和本地缓存），参考Glide回答就行了。


4.简述hashMap put方法的流程，一些细节追问：什么时候链表转树？为啥要转成树？什么时候树再次转回链表？

5.英语口语如何（我面的是国际化业务）。



**【京东一面】**


1.简述LinkedHashMap源码实现。


2.说下Handler流程。


3.handler用于线程间通信，怎么保证线程安全？


4.界面有个Button，手指放上去慢慢移到Button外的区域，描述事件分发的流程。


5.说下View的绘制流程，包括渲染和绘制。


6.算法 两个单链表合并成有序链表，说下思路。


7.组件化的实现，WMRouter调用其他模块的对应方法怎么实现的？



**【京东二面】**


完全忘了。



**【京东三面】**


1.问项目细节，你主要负责的模块，怎么设计？


2.算法 N宫格，从左上角A出发只能向右或者向下走，求所有不同路径的条数总和。手写伪代码（用递归就可以）。





**【某安一面】**


1.多线程同步有哪些方式？


2.sycnized和lock区别，分别用于什么场景？


3.线程池原理。


4.线程池拒绝策略怎么自定义？


5.怎么实现线程加入线程池的优先级？


6.volatile底层原理？是否线程安全？


7.死锁怎么发生？怎么防止死锁？


8.介绍下apm，怎么捕获crash和native crash？腾讯matrix怎么统计app启动时间，页面加载时间。


9.gc回收算法有哪些？分别用于什么场景？


10.jvm有哪些区域？栈的作用。


11.算法 三数之和。



**【某安二面】**


1.问项目 httpdns相关：为什么要httpdns？怎么处理握手问题，ip直连能否避免dns挟持？


2.问项目 首页弹窗优先级。


3.做过哪些内存优化。


4.怎么全局捕获app所有异常，并且保证app不崩溃，不卡死。


5.如何获取app的所有线程信息？


6.怎么查找anr？


7.安卓有哪些进程间通信？为什么使用binder？mmap发生在哪个阶段？怎么找到目标进程？


8.leakcanary怎么检测内存泄露的？


9.算法 两个栈实现一个队列。



**【某安三面】**


1.gradle生命周期，怎么自定义插件？


2.统计编译耗时插件，运行在哪个阶段？


3.描述从编译到apk的过程，以及gradle有哪些task参与其中？


4.如何实现字节码插桩？


5.子线程更新ui会报什么异常？从哪里抛出来的？


6.子线程怎么更新textview不会crash？


7.activity window view显示出来，全流程源码解析，精确到类和核心方法。


8.数据库用的什么库，熟悉sql语句吗？


9.mmkv为什么比sp高效，存储的数据结构是什么样？


10.mmap为什么比io高效，说下实现原理。


11.sp存在哪些问题？


12.hashmap put流程源码解析，链表和树转换时机？扰动函数的作用？为什么hashcode & n-1？扩容时机？


13.gcroots有哪些？


14.activity有个内部类handler，描述下引用关系链路，并说明为何gcroots能访问到activity。


15.加载xml和动态创建view哪个高效？为什么加载xml效率低？


16.如何减少布局嵌套？jetpack compose的原理。


17.recycleview缓存实现？开发中怎么优化？


18.算法 给定一个字符串，输出所有不同字符组合。



**【B站一面】**


1.一句话描述协程。


2.协程只在主线程使用，怎么实现挂起和切回的?


3.协程的轻量级怎么体现的？


4.ViewModel源码实现，它在Activity重启后为什么恢复数据？数据保存在哪里？和onsaveinstancestate有啥区别？


5.LifeCycle源码实现。


6.LiveData源码实现，先后setValue两次，都能收到吗？


7.简述View的刷新机制，屏幕刷新信号从哪里发出来的？


8.硬件加速是否了解？


9.SurfaceView的工作原理是否有了解？


10.组件化相关，WMRouer为什么可以这么方便实现组件之间通信，背后原理。



**【喜马拉雅一面】**


1.有了解过直播吗？有哪些常见协议？


2.说下自定义View的一般流程。


3.多个线程异步执行任务，主线程需要等待它们都完成再继续，有哪些实现方式？


4.说下handler的流程，异步消息是什么？Android中哪些场景会发送异步消息？我们在代码中可以手动发异步消息吗？


5.弹幕怎么实现？比如直播中的流星雨特效，跑车这种怎么实现出来？


6.说下你开发中做过的一个动画。


7.了解过SurfaceView的原理吗？


8.有没有接触过Socket编程，简单说下代码实现。


9.加载一张超大图怎么实现？


10.补间动画和属性动画各自的原理。


11.内存优化有哪些手段？


12.开放性题目 开发中怎么提升整个app的稳定性？



**【喜马拉雅二面】**


1.动态代理是怎么实现的？


2.你熟悉哪些设计模式？


3.画一下动态代理的类图。


4.说下插件化原理。


5.说下热修复的基本原理？比如QQ空间的方案和Tinker。


6.算法 判断两个单链表是否相交及找到第一个交点。



**【喜马拉雅HR面】**


1.对公司感觉怎么样，之前有了解过吗？


2.是否愿意承担一些管理角色。



**【得物一面】**


1.狂问Kotlin相关，比如委托的实现原理，协程原理，kotlin的好处，kotlin编译流程，常用的kotlin技巧。


2.app的打包流程。


3.View的渲染绘制流程。



**【得物二面】**


1.inflate加载view过程。


2.简述okhttp源码，请求怎么管理，它的线程池怎么实现？


3.apt原理。


4.viewmodel为什么可以恢复数据？


5.livedata可以保存多个数据吗？


6.aac架构有什么优势？


7.mmkv了解吗?


8.算法 单链表找到倒数第K个元素。


9.算法 单链表找到中间的元素（2个题都用快慢指针就可以了）。

10.组件化怎么做的？WMRouter编译期间做了什么？



### **经验tips**



**1、必考题一定要掌握！**


【必问】Handler的工作流程，源码要记牢，细节要理解透，比如怎么唤醒主线程的，while为啥不会阻塞主线程。


【必问】View的绘制流程，包括渲染和View本身的绘制3部曲。


【必问】MVVM的含义，ViewModel+LivaData+LifeCycle的源码实现。


【必问】自定义View的一般实现流程。


【必问】内存泄漏怎么排查，OOM怎么排查？


【必问】你做过哪些性能优化，参考这个：

*https://blog.yorek.xyz/android/paid/master/
*



【必问】Retrofit的实现，动态代理。


【必问】组件化相关，WmRouter的原理。


【高频】ANR怎么排查，有哪些原因？


【高频】APP打包流程。


【高频】kotlin常见知识点的原理，如协程，委托，扩展函数，编译流程。


【高频】Android有哪些跨进程通信，AIDL的代码实现。


【高频】怎么加载一张超大图？


【高频】OkHttp的线程池实现，Java线程池原理，拒绝策略怎么自定义？



**2、算法准备**


有时间就刷剑指offer66道题目，没时间就掌握一般的套路，回答一些常见题目基本够了。参考这个：

*https://zhuanlan.zhihu.com/p/386785887*

*https://zhuanlan.zhihu.com/p/109431459*



**3、关于性能优化**


你做过哪些性能优化？这个点也是经常被问到，我的建议 不要随意把别人博客的性能优化当成自己做过的，很多都是纯KPI项目，他们可能自己都没去实践。面试官一听就知道你在瞎说，挑一些相对靠谱的结合自己项目说下，然后拓展一下还未实现但是准备去做的。





# 知识体系

性能调优
	内存优化
		现象
			卡顿
			崩溃
		检测方法
			java层检测
				allocation tracker
				mat
			native层检测
				malloc调试
				malloc钩子
		bitmap
			android3.0之前
				finallize
			android3.0-android7.0
			android8.0后
				虚引用
		android内存管理机制
			android方法超过限制原因和解决方案
			java的内存模型
				线程私有区
					程序计数器
					栈
					本地方法栈
				线程共享区
					堆
						保存对象实例数据
						方法内部变量
					方法区
						常量池
				对象的创建
					在句柄池分配句柄
					对象实例数据 mark work
					对象类型数据
					自动对齐
				reference类型和四种引用
			怎么判断是垃圾
				引用计数法
				可达性分析法
			gc垃圾回收算法
				标记清除法
				标记整理法
				标记复制法（新生代用的多）
				分代收集法
		leakCanary原理
		写入放大
	卡顿优化
		如何分析卡顿
			cpu使用率/cpu负载率
				cpu物理核数/单个cpu核心数/超线程数
				cpu使用率
					Top Linux进程状态解析
				cpu负载率
			线下分析工具
				traceview,systrace,nonosacpe
					systrace
				profiler
					Sample Java Methods
					Trace Java Methods
					Sample C/C++ Functions
						simpleperf 
					Trace System Calls
						systrace
					jvm cpu profiler原理
				dumpsys原理
				am命令
			记录一次壁纸加载慢分析
		如何线上监控
			主线程的卡顿监控
				Handler原理
			帧率卡顿监控
				屏幕刷新原理
			线程的监控
			四大组件生命周期监控
				Aspectj
				asm框架
					apk安装原理
					asm设计模式
				redex
			android8后有insturmentation
				方式
					premain
					agentmain
				api
					ClassFileTransformer
					Instrumentation
						retransformClasses
						redefineClasses
						appendToBootstrapClassLoaderSearch
						setNativeMethodPrefix
	I/O优化
		IO的框架
		IO三种方式
			标准IO
			mmap
			直接IO
		IO的监控
			主线程io
			读写buffer过小
			重复读
			资源泄露
	存储优化
	网络优化
		IO模型
			同步
				阻塞IO
				非阻塞IO
				多路复用IO
					1.5之前用select
					1.5后用epoll
						水平触发
						边沿触发
				信号驱动式IO
			异步
				异步IO
		网卡收发包流程
		网络性能评估
			延迟与带宽
			性能测量
		网络优化
			优化点
				速度
				安全
				弱网络
			网络库选择
				OKHttp
				Cronet
				Mars
			优化项
				HttpDNS
				连接复用
				压缩与加密
				安全
			前沿技术
				QUIC
				IPV6
		网络监控
			函数插桩
			native hook
			统一网络库
				监控流量
			网络大平台监控
				客户端监控
				接入层监控
				监控报警
		5G原理
		Link Turbo
	UI优化
		View的绘制原理
			硬件加速
				Gpu完成Graphic Buffer的绘制
				引入DisplayList
			VSync信号
				双缓冲
					一个Graphic Buffer用于绘制   
					一个Graphic Buffer用于显示     
				三缓冲
			屏幕适配
		CreateView 优化
			xml用了大量反射，可用代码替换（X2C开源框架）
			异步创建
			View的重复利用
			measure/layout优化
				减少UI布局层级
					ViewStub
					Merge
				优化layout开销
					使用ConstraintLayout替代RelativeLayout
				背景优化
				PreComputedText异步measure和layout
		Litho异步布局
		Flutter
		RenderThread 和 RenderScript
		屏幕适配
	安装包/编译优化
		jit编译器
		启动耗时分析-art编译
		安装包的结构
	启动优化
		android启动流程
			BootLoader
				Linux内核
					Init进程
						zygote进程(app_process)
							第一步：start Dalvik虚拟机
							第二步 ZygoteInit.java
								注册一个socket
								预加载各类资源
								如果需要就start SystemServer
									Launcher
									启动native服务
									启动java服务
										installer服务
										电量管理服务
								轮询socket的消息并且处理
									prefork进程
									fork进程
									invoke ActivityThread的main方法
							或者第二步：RutimeInit.java
						ServiceManager
						MediaServer
				FastBoot
				Recovery

NDK开发
	JNI

FrameWork层源码分析
	BroadcastReceiver
		broadcast的发送原理
		broadcast接受原理
	Service
	ContentProvider
	SharedPreferences
	android窗口机制
	WindowManagerService
	android Launcher启动Activity的流程
	activity加载并显示view的流程
	android  anr 原理分析和解决方案
		service timeout (前台20秒内，后台200秒内)
		broadcastQueue timeout (前台10s,后台60s)
		contentprovider的publish10s内没有进行完
		inputDispatching 5s内无法响应屏幕触摸和键盘输入事件

计算机网络
	物理层（bit流）
		信道分类
			单工信道
			半双工信道
			全双工信道
		信道复用
			频分复用
			时分复用
			统计时分复用
			波分复用
			码分复用
	数据链路层（帧）
		结构
			分装成帧（加帧头，帧尾）
			透明传输（转义符）
			差错检测（循环冗余校验）
		ppp协议
		Mac地址
		局域网
		以太网
	网络层（包）
		结构
			版本
			协议
			源地址（ip地址）
			目的地址（ip地址）
			其他等
		ip协议
			ipv4
				A类地址
				B类地址
				C类地址
				D类地址（多播地址）
				E类地址
				划分子网
				超网
			ipv6
				ipv6比ipv4快的原理？
			Nat路由器
		arp协议（根据ip获取mac地址）
	传输层（报文段）
		tcp
			结构
				首部
				数据内容
			三次握手，四次挥手
			可靠传输原理
				停止等待协议
				连续AQR协议
			拥塞控制
				慢启动
				避免拥塞
				快重传
				快恢复
		udp
	应用层
		dns解析
			dns解析的过程
			httpdns
				解决dns解析过长问题
				解决域名缓存失效问题
				解决出口nat的问题
			dns为什么查询根域名服务器只返回13个ip地址
		协议
			http1.0
			http1.1
				长连接
					队头阻塞原理
			http2.0
				多路复用
					TLS协议队头阻塞
					frame阻塞
				header压缩
				服务端推送
				新的二进制格式
				请求优先级
				TLS协议
				https
					内容加密
					身份认证
					校验报文的完整性
			http3.0
				零RRT建立连接
				连接迁移
				队头阻塞/多路复用
				拥塞控制新特性
					热插拔
					向前纠错FEC
					单调递增的package number
					ack Delay
					更多的ack块
					流量控制
						Stream级别
						Connect级别
				http3.0协商升级与竞赛
			文件传输协议FTP
			动态主机配置协议DHCP
	IO模型(详情看 性能优化/网络优化 章节)
	JCE
	JSSE
	客户端拿到CA后，怎么去较验证书合法的
	TCP/IP网络协议知识点总结

Java基础
	泛型原理
	反射原理
	java虚拟机原理
	java IO
	线程安全
		synchornized
			无锁/偏向锁/自旋锁/重量锁
			异常退出/正常退出
		ReetrantLock
			AQS原理
	多线程
	枚举
	failed fast
	注解
		运行时注解
		编译注解
			Annotation Processing Tool
	hashmap原理
	concurrentHashMap原理
	threadLocal

Kotlin基础
	高阶函数
	Lanbda表达式
	泛型
	协程
	伴生对象

TV相关
	leanback
	蓝牙
	TIF

虚拟机
	java虚拟机体系结构
	jvm,art,dalvik区别
	android系统上的进程调度

构建
	jenkins
	git
	apk构建原理
		R8编译器
			代码缩减
			资源压缩
			代码优化
			代码混淆
				如何解码混淆处理的堆栈轨迹
				自定义要保留的代码
			排查R8优化引起的问题
		gradle的配置
			gradle之BuildConfig构建常量
		Proguard
		apk结构
			resources.arsc文件结构
		android系统资源管理框架
			应用程序编译和打包过程
			应用程序资源的初始化过程
			应用程序资源查找过程
	安装
