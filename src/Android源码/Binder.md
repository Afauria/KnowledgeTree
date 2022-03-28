# Binder

Service Binder机制，

1. 服务端创建Service实现onBind抽象方法
2. 返回一个IBinder对象
3. 客户端bindService方法启动服务，传入一个ServiceConnection接口对象，
4. 当服务启动成功后回调onServiceConnected（解绑的时候回调onServiceDisConnected方法），传入IBinder对象，通过getService可以获取到服务

# Messenger

如果是使用Messenger进行线程间通信的话，可以new Messenger(IBinder)创建Messenger对象，然后调用Messenger的send方法发送消息，Messenger是夹在Service和Binder之间的，对Binder进行了封装







https://www.cnblogs.com/not2/p/14685579.html
