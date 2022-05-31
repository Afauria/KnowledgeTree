# Binder

Service Binder机制

1. 服务端创建Service实现onBind抽象方法
2. 返回一个IBinder对象
3. 客户端bindService方法启动服务，传入一个ServiceConnection接口对象，
4. 当服务启动成功后回调onServiceConnected（解绑的时候回调onServiceDisConnected方法），传入IBinder对象，通过getService可以获取到服务

# Messenger

如果是使用Messenger进行线程间通信的话，可以new Messenger(IBinder)创建Messenger对象，然后调用Messenger的send方法发送消息，Messenger是夹在Service和Binder之间的，对Binder进行了封装



通常**32位**Linux内核虚拟地址空间划分0~3G为用户空间，3~4G为内核空间。内核虚拟地址在高位，而对应的实际物理地址在低位

# 手写Binder

# Binder线程池



# 高端内存

使用一段虚拟地址空间，建立临时地址映射，用完后释放，可以循环使用这段地址空间，访问所有物理内存

# Binder内存限制

1M = 1024K = 256个Page = 2^8个Page

ProcessState中打开Binder驱动，mmap时传入1M-8K：一个内存保护页4K，减去2个Page保持2的倍数

oneway异步传输：为同步传输的一半`(1M-8K)/2`

mmap方法内部限制：4M

ServiceManager：`128*1024`，负责管理系统服务，只有注册、查询等功能，不涉及大数据传输



https://www.cnblogs.com/not2/p/14685579.html

https://www.jianshu.com/p/ea4fc6aefaa8

Binder调用在子线程，Binder线程池

ApplicationThread需要将AMS调用通过Handler发到主线程执行



