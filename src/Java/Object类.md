# Object类

## Object、Type和Class

Object是所有类的父类，任何类都默认继承Object。包括Class类和数组。

所有类都有一个Class对象，包含该类的信息。Class不能表示泛型，Type可以。

`java.lang.reflect.Type`是**所有类型（不是类）**的公共高级接口，代表了Java中的所有类型：包括泛型、数组类型、基本数据类型等。一般用于反射获取变量类型。

Java的类型分为

- 原始类型（Type）：包括普通的Java类、枚举、数组、注解等。由Class类实现Type接口
- 基本类型：int，float等java基本类型
- 参数化类型（ParameterizedType）：如`List<String>`这个组合类型称为参数化类型
- 类型变量（TypeVariable）：如`<T>`中的`T`称为类型变量
- 数组类型（GenericArrayType）：泛型数组

> 通配符类型（WildcardType）：如`<?>`，也是Type的子接口，但不属于Java类型。

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
> 4. 超过间隔时间
>
> 时间间隔到了或中断并不会释放锁，只是唤醒了线程，线程还是会等待锁

wait、notify、notifyAll要求当前线程是该对象的所有者，即持有该对象的锁，否则调用该方法会抛出`IllegalMonitorStateException`异常

一次只能有一个线程持有对象的锁

# 对象的clone

Java创建对象（为对象分配内存空间）的方式有两种：

1. new创建一个对象：分配内存，调用构造函数，初始化对象
2. clone复制一个对象：分配内存，使用原对象中的值，初始化新对象

## 复制对象和复制引用

变量存放在栈中，对象存放在堆中。

```java
User user1 = new User(); //创建新对象
User user2 = user1;
//User2和User1指向同一个user对象，两个变量地址值相同，这种现象称为复制引用

User user1 = new User();
User user2 = user1.clone(); //复制新对象
System.out.println(user1 == user2); //false，对象地址不同
//User2和User1指向不同的user对象，两个变量地址值不同，这种现象称为复制对象
```

## 深拷贝和浅拷贝

对于基本数据类型，直接拷贝值。对于引用类型，分为两种情况：

1. 浅拷贝：将源对象字段的引用值直接赋值给新对象的字段
2. 深拷贝：根据源对象字段引用的对象，复制一个新的对象，再进行赋值

如下：可以看到java的clone实现的是浅拷贝，源对象和新对象中的name字段指向同一个字符串对象

```java
public class User implements Cloneable {
  int age;
  String name;
  //省略构造方法和getter、setter
  @Override
	protected Object clone() throws CloneNotSupportedException {
		return (Person)super.clone();
	}
}
public class Main {
  public static void main(String[] args) {
    //此处使用new String，引用堆中的对象，否则由于字符串池缓存，拷贝前后都指向池中的对象
    User user1 = new User(18, new String("Afauria")); 
    User user2 = null;
    try {
      user2 = user1.clone();
    } catch (CloneNotSupportedException e) {
      e.printStackTrace();
    }
    System.out.println(user1 == user2); //false
    System.out.println(user1.age == user2.age); //true
    System.out.println(user1.name == user2.name); //true
  }
}
```

![](Object类/浅拷贝和深拷贝.png)



**为了避免浅拷贝修改引用对象，导致其他的引用也跟着变更**，Java限制了clone方法的调用：

* 要求类实现`Cloneable`接口并且重写`clone`方法才可以调用该方法，否则会抛出`CloneNotSupportedException`异常，编译报错（提醒开发者重写clone进行深拷贝）：
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
* 并且要求类中所有引用的对象也要显式重写clone方法（传递性）：例如存在`User->Head->Face->Mouth`引用链，所有类都需要重写clone方法，否则深拷贝不彻底。
	
	> 如果引用链中的某一个类是在三方库中定义的，此时无法实现`Cloneable`接口、重写clone方法。需要使用new创建对象并手动设置初始值

String是深拷贝还是浅拷贝？

> * 从性质上讲是浅拷贝：因为拷贝前后指向同一个地址。
> * 从效果上讲是深拷贝：由于String的不可变性，修改String实际上会创建一个新的String对象，不会影响原有对象。

# 值传递和引用传递

## 误区

* 误区1：Java中对于基础数据类型是值传递，对于引用类型是引用传递
* 误区2：如果传递的是内容就是值传递，如果传递的是引用地址，就是引用传递

## 实参和形参

* 形式参数：定义方法的时候使用的参数名称，用于接收调用函数时传入的数据
* 实际参数：调用方法时真正传递给方法的数据

## 求值策略

实际参数传递给形式参数的时候分为两种情况：值传递和引用传递。

- 传值调用（值传递）：获取实际参数的值，复制值，传递给方法。由于形参拿到的是个拷贝，因此在方法中修改形参，实参的值不会改变。
- 传引用调用（引用传递）：直接把实参的引用传递给方法，不拷贝。由于传递的是引用，因此在方法中修改形参，实参的值也会发生改变。
  - 修改形参引用的对象，外部变量引用的对象**发生改变**
  - 修改形参引用的对象的属性，外部变量引用的对象的属性发生改变
- 传共享对象调用（共享对象传递）：获取到实际参数的地址，复制地址，传递给方法。由于形参和实参的地址都指向同一个对象，所以也称为"传共享对象"，在方法中修改形参，实参也会发生改变。
  - 修改形参引用的对象，外部变量引用的对象**不会改变**
  - 修改形参引用的对象的属性，外部变量引用的对象的属性发生改变

> 从结果上看，引用传递和共享对象传递都会对实参引用的对象产生影响。
>
> 从过程上看，值传递和共享对象传递都有一个关键的步骤：复制。

这里我们应该更关注过程，因此通常把**共享对象传递当作值传递的特例**。值传递和引用传递的主要区别在于是直接传递，还是传递一个副本。

举例：

> * 引用传递：你有一把钥匙，当你的朋友想要去你家的时候，如果你`直接`把你的钥匙给他了。这种情况下，如果他在钥匙上刻下了自己名字，那么这把钥匙还给你的时候，你自己的钥匙上也会多出他刻的名字。
> * 值传递：你有一把钥匙，当你的朋友想要去你家的时候，你`复刻`了一把新钥匙给他，自己的还在自己手里。这种情况下，他对这把钥匙做什么都不会影响你手里的这把钥匙。

## 为什么说Java只有值传递？

可以确定的是，对于基本数据类型是值传递，有疑问的是引用类型的传递。

误区：由于Java中的变量和对象之间存在引用关系，通过对象的引用来操纵对象，因此很多人认为对引用类型传递是引用的传递。并且在方法中修改对象的属性之后，外部对象的属性也会发生改变。

在 《The Java™ Tutorials》中描述如下：

> Primitive arguments, such as an int or a double, are passed into methods by value. This means that any changes to the values of the parameters exist only within the scope of the method. When the method returns, the parameters are gone and any changes to them are lost.
>
> （基础类型参数通过值传递给方法。这意味着对参数值的任何更改都只存在于方法的范围内。当方法返回时，参数将消失，对它们的任何更改都将丢失。）
>
> Reference data type parameters, such as objects, are also passed into methods by value. This means that when the method returns, the passed-in reference still references the same object as before. However, the values of the object’s fields can be changed in the method, if they have the proper access level.
>
> （引用数据类型参数，如对象，也按值传递给方法。这意味着，当方法返回时，传入的引用仍然引用与以前相同的对象。但是，如果**对象字段**具有适当的访问级别，则**可以在方法中更改这些字段的值**。）

可以看出Java中只有值传递，对于引用类型，传递的是对象地址的拷贝（即上文提到的传共享对象调用）。

为什么共享对象传递不属于引用传递，而是属于值传递？为什么值传递和共享对象传递现象不一致？

> 主要在于如何理解“改变值”：
>
> 1. 对形参指向的对象进行修改：值传递中外部对象的属性不会改变，共享对象传递中外部对象的属性会发生改变。
> 2. 修改形参指向的对象：值传递和共享对象传递都不会改变外部变量指向的对象。

举例：

> 对形参指向的对象进行修改：你复制了一把你家里的钥匙给到你的朋友，他拿到钥匙以后，并没有在这把钥匙上做任何改动，而是通过钥匙打开了你家里的房门，进到屋里，把你家的电视给砸了。这个过程，对你手里的钥匙来说，是没有影响的，但是你的钥匙对应的房子里面的内容却是被人改动了。
>
> 修改形参指向的对象：你复制了一把钥匙给到你的朋友，你的朋友拿到你给他的钥匙之后，找个锁匠把他修改了一下，他手里的那把钥匙变成了开他家锁的钥匙。这时候，他打开自己家，就算是把房子点了，对你手里的钥匙，和你家的房子来说都是没有任何影响的。

```java
class House {}
class Key {
    House house; //一把钥匙，能够打开一个房子
}

public class Main {
  public static void main(String[] args) {
    Key yourKey = new Key(new House());
    handle(yourKey);
  }
  private static void handle(Key copyKey) {
    //对形参指向的对象进行修改
    copyKey.house = null; //修改的是你的房子
    //修改形参指向的对象
    copyKey = new Key(new House()); //yourKey不会改变。如果是引用传递，yourKey指向的对象也会改变
    copyKey.house = null; //烧掉房子不会对你的房子造成影响
  }
}
```

总结：**Java中的对象传递是共享对象传递。如果是修改引用，不会对原来的对象产生影响，但是如果修改共享对象的属性的值，是会对原来的对象有影响的。而共享对象传递是值传递的特例，所以一般也说Java中只有值传递**
