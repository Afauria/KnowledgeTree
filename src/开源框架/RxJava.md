# Subject

* `AsyncSubject`：接收`onComplete`之前的最后一个数据，只有调用`onComplete`之后才会发送数据和接收
* `BehaviorSubject`：接收订阅前的最后一个数据，以及订阅后的所有数据
* `ReplaySubject`：接收订阅前和订阅后的所有数据，缓存订阅前的数据，可以设置缓存大小
* `PublishSubject`：只接收订阅后的数据

# subscribeOn和observeOn

observeOn：指定观察者接收通知的线程

subscribeOn：指定事件发送的线程

1. 不指定的话默认都在当前线程
2. 只指定subscribeOn：事件发送和观察者都在该线程。（通过修改生产者线程，间接影响消费者线程）
3. 多次指定subscribeOn：只有第一次调用生效，自下向上，结果会被最上面的覆盖
4. 只指定observeOn：只会影响消费者线程，不会影响生产者线程
5. 多次指定observeOn：每次都会修改observeOn之后的消费者线程（将结果缓存下来，下一个消费者消费时切换线程），自上而下
