# Subject

* `AsyncSubject`：接收`onComplete`之前的最后一个数据，只有调用`onComplete`之后才会发送数据和接收
* `BehaviorSubject`：接收订阅前的最后一个数据，以及订阅后的所有数据
* `ReplaySubject`：接收订阅前和订阅后的所有数据，缓存订阅前的数据，可以设置缓存大小
* `PublishSubject`：只接收订阅后的数据
