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

* Java虚拟机内存
  * 程序计数器：每个线程独立，记录当前执行的指令的地址
  * java栈（虚拟机栈）：栈帧：局部变量表(Local Variables)、操作数栈(Operand Stack)、指向当前方法所属的类的运行时常量池的引用(Reference to runtime constant pool)、方法返回地址(Return Address)和一些额外的附加信息，一个线程对应一个栈
  * 本地方法栈
  * 堆：对象所在的空间，所有线程共享。包括新生代（Young Gen）、年老代（Tenured Gen，存放生命周期较长的对象，经过多次gc仍然存活）。新生代包括Edan和两块Survivor
  * 方法区：持久代（Permanent），存放类信息、静态变量、静态方法、常量等
* Java GC
  * 执行时期：不可控，手动调用`System.gc()`，建议执行GC。
    `minor gc`:回收新生代（新生代空间不足）
    `major gc`:回收年老代（年老代空间不足，或对新生代分配担保不足）
    `full gc`:回收所有
  * 回收哪些对象：没有引用的对象有可能被回收。
    判断：引用计数法（很难解决循环引用问题）、可达性分析法（从GCroots出发搜索，没有引用链的时候证明对象不可达）GCRoots：栈中对象，方法区中静态变量，方法区中常量，本地方法栈（JNI）中的对象
  * 怎么回收：标记-清除收集算法、复制算法（适用于新生代：少量对象存活。）、标记-整理算法（适合老年代：老年代存活率高，复制效率低）、分代收集算法（对不同代使用不同的算法）
  * GC扫描Edan和SurvivorA，把幸存的对象移到SurvivorB，下次扫描Edan和SurvivorB，幸存的移到SurvivorA，多次之后还幸存的移到Tenured。客户端新生代和年老代是1:2，服务端是1:8，Edan和Survivor是32:1:1，不同算法有不同的分配比例
* finalize方法，只会执行一次，在GC将要回收之前能够自救一次
* 引用：强引用（普通的引用）>软引用（内存不足时被回收）>弱引用（除了弱引用外没有其他的引用则会被回收）>虚引用（相当于没有引用，要与引用队列配合使用）

堆区: 
1.存储的全部是对象，每个对象都包含一个与之对应的class的信息。(class的目的是得到操作指令) 
2.jvm只有一个堆区(heap)被所有线程共享，堆中不存放基本类型和对象引用，只存放对象本身 
栈区: 
1.每个线程包含一个栈区，栈中只保存基础数据类型的对象和自定义对象的引用(不是对象)，对象都存放在堆区中 
2.每个栈中的数据(原始类型和对象引用)都是私有的，其他栈不能访问。 
3.栈分为3个部分：基本类型变量区、执行环境上下文、操作指令区(存放操作指令)。 
方法区: 
1.又叫静态区，跟堆一样，被所有的线程共享。方法区包含所有的class和static变量。 
2.方法区中包含的都是在整个程序中永远唯一的元素，如class，static变量。 



每启动一个java程序，都是一个单独的虚拟机。
java虚拟机本身就是一个进程，该进程只支持一个java程序

1、进程是系统中正在运行的一个程序，程序一旦运行就是进程。
进程可以看成程序执行的一个实例。进程是系统资源分配的独立实体，每个进程都拥有独立的地址空间。一个进程无法访问另一个进程的变量和数据结构，如果想让一个进程访问另一个进程的资源，需要使用进程间通信，比如管道，文件，套接字等。

一个进程可以拥有多个线程，每个线程使用其所属进程的栈空间。线程与进程的一个主要区别是，统一进程内的一个主要区别是，同一进程内的多个线程会共享部分状态，多个线程可以读写同一块内存（一个进程无法直接访问另一进程的内存）。

2、线程是进程的一个实体，是进程的一条执行路径。

线程是进程的一个特定执行路径。当一个线程修改了进程的资源，它的兄弟线程可以立即看到这种变化。同时，每个线程还拥有自己的寄存器和栈，其他线程可以读写这些栈内存。

进程和线程主要差别：

1、在于它们是不同的操作系统资源管理方式。进程有独立的地址空间，一个进程崩溃后，在保护模式下不会对其它进程产生影响，而线程只是一个进程中的不同执行路径。

2、线程有自己的堆栈和局部变量，但线程之间没有单独的地址空间，一个线程死掉就等于整个进程死掉，所以多进程的程序要比多线程的程序健壮，但在进程切换时，耗费资源较大，效率要差一些。

3、但对于一些要求同时进行并且又要共享某些变量的并发操作，只能用线程，不能用进程。

4、地址空间和其它资源（如打开文件）：进程间相互独立，同一进程的各线程间共享。某进程内的线程在其它进程不可见。

5、通信：进程间通信IPC，线程间可以直接读写进程数据段（如全局变量）来进行通信——需要进程同步和互斥手段的辅助，以保证数据的一致性。

6、调度和切换：线程上下文切换比进程上下文切换要快得多。

7、在多线程OS中，线程不是一个可执行的实体。
