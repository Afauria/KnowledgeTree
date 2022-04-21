# 简介

Square出品，用于检测Android内存泄漏对象。

> 使用崩溃检测无法定位OOM异常，因为OOM可能发生在任何位置，崩溃堆栈不相同

## 内存泄漏

内存泄漏：程序保存了不需要的对象的引用，导致GC无法回收，最终会导致OOM。

> 例如静态变量引用Activity对象，Activity onDestroy之后无法被回收。

Java GC判断一个对象是否能被回收、是通过检测是否被GC Root持有。常见的GC Root如下：

1. 虚拟机栈中引用的对象，即局部变量、活动线程的实例
2. 方法区中静态变量和常量引用的对象
3. 本地方法栈中JNI引用的对象，即Native引用

## LearkCanary工作原理

1. 通过Android生命周期Hook检测对象：onDestroy的时候将对象传给ObjectWatcher，通过弱引用持有，并关联引用队列，如果5s之后引用队列中不存在该对象，则认为是可能泄漏的存活对象，输出到Logcat。
   1. 销毁的 `Activity` 实例
   2. 销毁的 `Fragment` 实例
   3. 销毁的 Fragment `View` 实例
   4. clear的 `ViewModel` 实例
2. Dump Heap：调用`Debug.dumpHprofData(filePath)`方法生成`.hprof`文件进行存储
   1. 应用可见时，存活对象数量达到5个时开始dump
   2. 应用不可见时，存活对象达到1个时，5s内进行dump
   3. 或者点击通知栏立即进行dump
3. 堆分析：通过Shark工具进行分析
4. 内存泄漏归类：对相同错误的泄漏进行分组

> LeakCanary 2.0使用Kotlin+Shark工具进行分析，减少内存占用，提高解析速度
>
> 低版本使用HAHA或者MAT工具分析

Shark是Kotlin的堆分析工具，基于Okio，支持以下功能

1. Shark Hprof：解析`.hprof`文件
2. Shark Graph生成对象引用图
3. Shark：生成分析报告
4. Shark Android：生成Android特定报告
5. Shark CLI：通过命令行调试，不需要依赖LeakCanary

# 使用

[官方文档](https://square.github.io/leakcanary/)

添加依赖：使用`debugImplementation`

```groovy
dependencies {
  debugImplementation 'com.squareup.leakcanary:leakcanary-android:2.8.1'
}
```

使用：LeakCanary2.0只需要添加依赖即可，LeakCanary1.0需要在Application中调用`LeakCanary.install(this);`

> LeakCanary2.0通过ContentProvider进行初始化，在Application onCreate方法之前，不需要手动install

# 知识点

如何判断对象是否被回收？

> Activity被销毁时，使用ObjectWatcher进行弱引用。
>
> 如果一个对象只有弱引用，没有被其他地方引用，GC的时候会回收弱引用对象，并添加到关联的引用队列。
>
> 因此`ReferenceQueue`中存在对象时则表示对象已经被回收。否则认为没有回收，手动再运行一次GC，如果还保留，则确认发生了泄漏。

为什么要延迟5s执行任务？

> 页面销毁后GC不是立马触发的，预留5s给GC

监听生命周期？

> 通过`Application.registerActivityLifecycleCallbacks`方法绑定Activity生命周期

内存分析模块在子进程中，install中判断如果是分析进程则不启用分析。

是否可以用于线上？——不可以

> 1. 每次泄漏都会生成`.hprof`并解析，占用手机CPU，
> 2. 保存`hprof`文件，占用存储空间
> 3. 多次GC，对性能产生影响
> 4. `hprof`文件较大，上报回捞较麻烦

# 源码解析

//todo

https://blog.csdn.net/u011033906/article/details/117958603
