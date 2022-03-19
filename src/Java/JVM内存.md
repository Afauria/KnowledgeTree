JVM内存

https://www.cnblogs.com/itplay/p/11137526.html

https://blog.islinjw.cn/2017/11/10/%E6%88%91%E4%B9%9F%E6%9D%A5%E8%B0%88%E4%B8%80%E8%B0%88Java-GC/

https://en.wikipedia.org/wiki/Garbage_collection_(computer_science)



# 四种引用和引用队列

1. 强引用（StrongReference）：普通的引用，如果对象存在强引用，则不会被GC回收
2. 软引用（SoftReference）：内存不足时被回收。
3. 弱引用（WeakReference）：除了弱引用之外没有其他的引用，则GC的时候会被回收。
4. 虚引用/幽灵引用（PhantomReference）：相当于没有引用，**必须与引用队列配合使用**，GC的时候会被回收。使对象进入不可用状态，下次GC回收时会加入到引用队列中，从引用队列中poll之后对象才被真正清除。

```java
// 强引用
String str = new String("Strong");
str = null; //引用置空后String对象可以被回收

// 软引用
SoftReference<String> sr = new SoftReference<String>(new String("Soft"));
String str = sr.get(); //获取对象

// 弱引用
WeakReference<String> wr = new WeakReference<String>(new String("Weak"));
String str = wr.get();
// 弱引用Map
WeakHashMap<String, String> whm = new WeakHashMap<String, String>();

// 虚引用和引用队列
ReferenceQueue<String> rq = new ReferenceQueue<String>();
PhantomReference<String> pr = new PhantomReference<String>(new String("Phantom"), rq);
// 永远为null（相当于无引用）
System.out.println(pr.get());
// 从引用队列中取出一个对象
Reference<? extends String> ref = rq.poll();
```

软引用和弱引用一般用于缓存一些不重要的数据，防止内存泄漏，也可以配合引用队列使用

> 例如加载大量图片，每次都从磁盘读取影响性能，一次全部读取又可能会造成内存不足。使用Map+软引用可以避免这个问题，内存不足时自动释放。

```java
ReferenceQueue<String> rq = new ReferenceQueue<String>();
SoftReference<String> sr = new SoftReference<String>(new String("Soft"), rq);
WeakReference<String> wr = new WeakReference<String>(new String("Weak"), rq);
```

引用队列：关联了引用队列之后，GC之后会将回收的对象放入引用队列。用于监测GC运行情况和行为，观察哪些对象被GC回收了，可以做相应的处理。
