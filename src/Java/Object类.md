# Object类

## Object、Type和Class

Object是所有类的父类，任何类都默认继承Object。包括Class类和数组。

所有类都有Class对象，包含该类的信息。Class不能表示泛型，Type可以。

Type是Class类的父接口，代表了Java中的所有类型：包括泛型、基本类型等

## Object类有哪些方法？

1. `clone`：实现对象的浅拷贝。需要重写并且实现了`Cloneable`接口才可以调用该方法，否则抛出`CloneNotSupportedException`异常。
2. `finalize`：GC的时候发现没有其他引用会调用，可以通过重写该方法来阻止GC回收。
3. `equals`：比较两个对象是否相等。默认内部通过`==`进行比较，没有重写的话，`equals`和`==`等价。String重写了equals方法，因此字符串比较中`equals`和`==`结果不相同
4. `getClass`：获取对象运行时类型。
5. `hashCode`：默认返回对象内存地址的hash值，作为对象的唯一标识。一般用于哈希查找。
6. `toString`：返回对象的字符串表示，默认是`类名+@+16进制hashCode`。一般需要重写。
7. `wait`：让当前线程进入等待状态，等待该对象的锁。直到获得对象锁或者被中断
8. `wait(long timeout)`：同wait，可以设定超时间隔，如果在规定时间内没有获得锁就返回。参数为0的时候等价于`wait()`方法
9. `notify`：唤醒等待该对象锁的**任意一个线程**。
10. `notifyAll`：同notify，唤醒等待该对象锁的**所有线程**。

final声明的方法，无法重写：wait、notify、notifyAll、getClass

wait状态如何唤醒？

> 1. 其他线程调用了该对象的notify方法获得锁。
> 2. 其他线程调用了该对象的notifyAll方法获得锁。
> 3. 其他线程调用了interrupt中断该线程。
> 4. 时间间隔到了。
>
> 时间间隔到了或中断并不会释放锁，只是唤醒了线程，线程还是会等待锁

wait、notify、notifyAll要求当前线程是该对象的所有者，即持有该对象的锁，否则调用该方法会抛出`IllegalMonitorStateException`异常

一次只能有一个线程持有对象的锁

# 对象的clone

需要重写并且实现`Cloneable`接口才可以调用该方法，否则抛出`CloneNotSupportedException`异常。

```java
/**
 * @throws  CloneNotSupportedException  if the object's class does not
 *               support the {@code Cloneable} interface. Subclasses
 *               that override the {@code clone} method can also
 *               throw this exception to indicate that an instance cannot
 *               be cloned.
 * @see java.lang.Cloneable
 */
protected native Object clone() throws CloneNotSupportedException;
```

# 结语

参考资料：

* [Object类有哪些方法](https://blog.csdn.net/k678mh/article/details/8594769)
