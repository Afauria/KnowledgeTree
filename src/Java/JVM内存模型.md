

https://www.cnblogs.com/itplay/p/11137526.html
https://blog.islinjw.cn/2017/11/10/%E6%88%91%E4%B9%9F%E6%9D%A5%E8%B0%88%E4%B8%80%E8%B0%88Java-GC/

https://en.wikipedia.org/wiki/Garbage_collection_(computer_science)

# 四种引用和引用队列

1. 强引用（StrongReference）：普通的引用，如果对象存在强引用，则不会被GC回收
2. 软引用（SoftReference）：内存不足时被回收。
3. 弱引用（WeakReference）：除了弱引用之外没有其他的引用，则GC的时候会被回收。
4. 虚引用/幽灵引用（PhantomReference）：相当于没有引用，**必须与引用队列配合使用**，使对象进入不可用状态，**GC回收时会加入到引用队列中，从引用队列中poll之后对象才被真正清除**。

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

引用队列：关联了引用队列之后，GC会将要回收的对象放入引用队列，因此如果对象在引用队列中，则表示被回收了。用于跟踪GC过程，观察哪些对象被GC回收了，可以做相应的处理。

# JVM内存模型

* 程序计数器（Program Counter Register）：线程私有，记录当前执行的指令的地址
* 虚拟机栈（VM Stack）：线程私有，描述Java方法执行的内存模型。栈中只保存基本数据对象和引用对象的引用地址，对象本身存放在堆区中。
  * 每个方法执行都会创建一个栈帧（Stack Frame），栈顶的栈帧就是当前线程的活动栈，方法执行完成之后出栈。栈帧用于存储局部变量表（Local Variables）、操作数栈（Operand Stack）、当前类的运行时常量池的引用（Reference to runtime constant pool）、方法返回地址（Return Address）和一些额外的附加信息，
* 本地方法栈：为Native方法服务
* 堆：线程共享，存放实例对象。
  * JVM只有一个堆区，被所有线程共享。
  * GC回收的主要区域，分为新生代、老年代。

* 方法区：线程共享，存放的都是程序运行中唯一的元素，例如被JVM加载过的类的信息（方法名、变量名、代码等）、静态变量、静态方法、常量、JIT编译后的代码等。包括运行时常量池。
  * JDK 1.8之前的实现是永久代（Permanent）
  * JDK 1.8之后的实现是元空间

字符串常量池：使用StrignTable的Hash表

* JDK 1.7之前字符串常量池在方法区中
* JDK 1.7之后字符串常量池放到了堆中。

**Class对象存放在堆区，不是方法区，类的元数据才是存放在方法区**

方法区也存在GC

# Java GC

## 什么时候发生GC

GC执行时机不可控，手动调用`System.gc()`并不会立马GC，而是建议执行GC。

## GC类型

* `minor gc`：回收新生代（新生代空间不足）
* `major gc`：回收年老代（年老代空间不足，或对新生代分配担保不足）
* `full gc`：回收所有

## 回收对象

主要回收没有引用的对象，判断方式：

1. 引用计数法：计算被引用的次数，为0时表示没有引用，可以被回收。很难解决循环引用问题
2. 可达性分析法：判断对象是否被GC Root引用。

常见的GC Root如下：

1. 虚拟机栈（栈帧中的本地变量表）中引用的对象。
2. 方法区中类静态属性引用的对象。
3. 方法区中常量引用的对象。
4. 本地方法栈中JNI（即一般说的Native方法）引用的对象。

## 回收机制

1. 标记-清除算法（Mark-Sweep）：标记出所有需要回收的对象，标记完成后统一回收所有被标记的对象。会产生大量不连续的内存碎片。
2. 复制算法（copy）：将可用的内存分为两块，每次只用其中一块，当这一块内存用完了，就将还存活着的对象复制到另外一块上面，然后再把已经使用过的内存空间一次性清理掉。适用于新生代，少量对象存活
3. 标记-整理算法（Mark-Compact）：标记出所有需要回收的对象，让所有存活对象都向一端移动，然后直接清理掉边界以外的内存。
4. 分代收集算法：对不同代使用不同的算法。
   1. 新生代（Young Gen）：使用复制算法。回收频繁，存活对象少，新生代包括Edan和两块Survivor
   2. 老年代（Tenured Gen）：使用标记-整理算法。回收不频繁，存放生命周期较长的对象，经过多次gc仍然存活。
   3. 永久代：不回收。永久代是hotspot对方法区规范的一种实现，在java8中，使用元空间实现方法区


GC扫描Edan和SurvivorA，把幸存的对象移到SurvivorB，下次扫描Edan和SurvivorB，幸存的移到SurvivorA，多次之后还幸存的移到Tenured。客户端新生代和年老代是1:2，服务端是1:8，Edan和Survivor是32:1:1，不同算法有不同的分配比例

在GC之前会调用对象的`finalize`方法，可以重写该方法阻止GC。
