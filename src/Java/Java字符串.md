# String对象

String不是基本数据类型，是引用类型。

内部使用char数组存储，并且被final修饰，初始化的时候拷贝数组赋值，长度固定，之后不可修改，每次修改都会创建一个新的字符串对象。

```java
private final char[] value;
```

## substring原理

JDK6中，调用substring方法生成一个新的字符串，但是会共用char数组，通过offset和count索引。

问题：多个对象指向同一个char数组，可能导致char数组无法被释放，造成内存泄漏。

```java
//JDK 6
String(int offset, int count, char value[]) {
    this.value = value;
    this.offset = offset;
    this.count = count;
}
public String substring(int beginIndex, int endIndex) {
    return new String(offset + beginIndex, endIndex - beginIndex, value);
}
```

JDK7中调用substring创建一个新的字符串，并创建一个新的数组，通过拷贝原数组赋值。

```java
//JDK 7
public String(char value[], int offset, int count) {
    this.value = Arrays.copyOfRange(value, offset, offset + count);
}
public String substring(int beginIndex, int endIndex) {
    int subLen = endIndex - beginIndex;
    return new String(value, beginIndex, subLen);
}
```

## 字符串替换

1. `replace(CharSequence target, CharSequence replacement)`：匹配字符串，替换所有target
2. `replaceAll(String regex, String replacement)`：匹配正则表达式，替换所有匹配项
3. `replaceFirst(String regex, String replacement)`：匹配正则表达式，只替换第一项

## String对"+"的重载

Java不支持**运算符重载**。使用"+"拼接字符串是Java提供的一个**语法糖**。

原理是编译时使用`StringBuilder`进行拼接。如下：

```java
String str1= "Hello";
String str2= "World";
String str3 = str1 + str2;
//反编译结果如下
String var1 = "Hello";
String var2 = "World";
(new StringBuilder()).append(var1).append(var2).toString();
```

如果是直接拼接字符串**字面量或常量**，编译器会进行**常量折叠**。如下：

```java
int i1 = 2;
final int i2 = 2;
String str1 = "1" + i1; //StringBuilder拼接，不会加入字符串池
String str2 = "1" + i2; //i2是常量，进行常量折叠，会加入字符串池
String str3 = "Hello" + " World"; //常量折叠，，会加入字符串池
//反编译结果如下：
byte var1 = 2;
(new StringBuilder()).append("1").append(var1).toString();
String var4 = "12";
String var5 = "Hello World";`
```

> final常量不会变，编译器可以直接进行折叠。如果是变量，无法确定拼接之前会不会被修改，在运行的时候才能确定。

## int转String

```java
int i = 5;
String i1 = "" + i; //会编译成StringBuilder进行拼接
String i2 = String.valueOf(i); //内部调用Integer.toString
String i3 = Integer.toString(i);
```

## String长度限制

编译期限制：字符串**字面量**长度不能超过65535。否则javac会编译失败（常量字符串过长）。字面量长度为65534可以编译通过

> javac将java文件编译成class文件，class文件是在JVM上运行的，因此要遵循JVM字符串常量池规范。

运行期限制：不能超过int表示范围，否则会抛异常。

> **String内部使用char数组存储，数组长度不能超过int范围。**

# String不可变性

## 什么是不可变对象？

> 对象创建之后不能修改内部状态

## 为什么说String是不可变的？

**String内部使用char数组存储，并且被final修饰，不可修改**

**拼接字符串，实际上是在堆上创建了一个新的字符串对象，并修改变量的引用**。

## 如何证明是一个新对象？

1. 使用`System.identityHashCode(str)`打印内存地址可以发现值不一样
2. 查看`concat()`方法能够发现new了一个新字符串对象。同样的`replace`、`substring`等内部也是new了一个新的字符串对象

```java
public String concat(String var1) {
    int var2 = var1.length();
    if (var2 == 0) {
        return this;
    } else {
        int var3 = this.value.length;
        char[] var4 = Arrays.copyOf(this.value, var3 + var2);
        var1.getChars(var4, var3);
        return new String(var4, true);
    }
}
```

**避免在循环中直接使用String拼接字符串。因为会不断创建新的对象**

**如果想要一个可修改的字符串，可以使用`StringBuffer`或者`StringBuilder`**

## 为什么String设计成不可变？

基于线程安全、性能、缓存等方面考虑

1. 线程安全：由于字符串不可变，因此多个线程访问的时候，如果线程修改了值，会创建一个新的字符串，而不是修改相同的值。
2. 性能：通过字符串池和hashCode缓存，更加高效。
3. 缓存：字符串使用广泛，频繁创建消耗资源。因此JVM专门开辟了一个空间存储字符串，即字符串常量池，对字符串进行缓存，可以节省堆空间，堆中字符串变量指向字符串池中的对象（两个相同的字符串变量，指向字符串池中的同一个对象）。如果字符串可变的话，修改了内容，所有引用都会跟着修改。
4. hashCode缓存：HashMap、HashTable、HashSet等集合类，会调用hashCode计算hash值，由于字符串不可变，因此String可以重写hashCode方法，首次调用保存hash值，之后访问的时候直接返回缓存的hash值。

hashCode缓存源码如下

```java
//String.class
//hash = s[0]*31^(n-1) + s[1]*31^(n-2) + ... + s[n-1]
public int hashCode() {
    int h = hash;
    final int len = length();
    if (h == 0 && len > 0) {
        for (int i = 0; i < len; i++) {
            h = 31 * h + charAt(i);
        }
        hash = h;
    }
    //hash值不为空，直接返回，为空时返回0
    return h;
}
```

# String常量池

为了减少相同字符串重复创建，节省内存。JVM单独开辟了一块内存区域保存字符串常量，即字符串常量池。

> 缓存池基本都是解决频繁、重复创建问题的，让引用能够共用池中的对象。常量本质也是使用池实现。
>
> 如果一个类或方法从未被加载/调用，其中定义的任何常量将不会被加载到池中。
>
> 一般说的常量池一般指**运行时常量池**，不同于**字符串常量池**（也叫字符串池，String pool，String Table）

* JDK7以前，**运行时常量池**（包括字符串常量池）放在方法区中，方法区的实现是永久代。永久代空间大小固定，不会被回收，频繁调用intern会导致永久代内存溢出（`java.lang.OutOfMemoryError: PermGen`）。
* JDK7将字符串常量池放到了堆内存中，并且参与GC，回收重复的字符串对象。运行时常量池还在方法区。频繁调用intern会导致堆内存溢出（`java.lang.OutOfMemoryError: Java heap space`）。
* JDK8中，方法区使用元空间实现（替代永久代），**运行时常量池在元空间中，字符串常量池还在堆中**。

```java
String a = "123";
String b = "123";
System.out.println(a == b);//true：同一个对象。
//使用双引号（字符串字面量）创建的字符串对象存储在常量池中
//创建字符串常量时会先判断常量池中是否已经存在，如果存在则直接引用

String a = "123"; //池中对象
String b = new String("123"); //堆中对象
System.out.println(a == b);//false
//使用new String创建的是字符串对象，存放在堆中，两个引用不相等
//实际上是将常量池中的123复制到了堆中，如果常量池没有，会先在常量池中创建，如下

String a = new String("123");
//实际上创建了两个对象，一个在常量池，一个在堆中
//如果常量池已经有该字符串，则只创建一个对象。
```

什么情况会将字符串加入常量池？

> 1. 使用字符串字面量给变量赋值
> 2. 使用intern方法

`String str = new String("a") + new String("B");`会创建几个对象？

`javap`反编译查看字节码如下

```sh
Compiled from "Main.java"
public class Main {
  #...
  public static void main(java.lang.String[]);
    Code:
       0: new           #2                  // class java/lang/StringBuilder
       3: dup
       4: invokespecial #3                  // Method java/lang/StringBuilder."<init>":()V
       7: new           #4                  // class java/lang/String
      10: dup
      11: ldc           #5                  // String a
      13: invokespecial #6                  // Method java/lang/String."<init>":(Ljava/lang/String;)V
      16: invokevirtual #7                  // Method java/lang/StringBuilder.append:(Ljava/lang/String;)Ljava/lang/StringBuilder;
      19: new           #4                  // class java/lang/String
      22: dup
      23: ldc           #8                  // String B
      25: invokespecial #6                  // Method java/lang/String."<init>":(Ljava/lang/String;)V
      28: invokevirtual #7                  // Method java/lang/StringBuilder.append:(Ljava/lang/String;)Ljava/lang/StringBuilder;
      31: invokevirtual #9                  // Method java/lang/StringBuilder.toString:()Ljava/lang/String;
      34: astore_1
      35: return
}
```

> 1. 创建一个StringBuilder对象：上面提到"+"号编译时改为StringBuilder拼接---->对应字节码行号0
> 2. new String在堆中创建一个"a"对象---->7
> 3. 字符串常量池中的创建一个"a"对象---->11，ldc（load constant）表示加载常量
> 4. new String在堆中创建一个"b"对象---->19
> 5. 字符串常量池中的创建一个"b"对象---->23
> 6. toString()方法会调用new String("ab")，在堆中创建一个"ab"对象---->31
>
> **此时不会在字符串常量池中创建"ab"对象**

## intern方法

* JDK1.6中：字符串调用`intern`方法，会先去字符串常量池中查找是否存在，如果有则**返回池中对象的地址**。否则把字符串常量加到字符串池中，再返回池中对象的地址。
* JDK1.7中：字符串调用`intern`方法，会先去字符串常量池中查找是否存在，如果有则**返回池中对象的地址**。否则将堆中的**字符串对象的地址**添加到常量池中，再返回池中对象的地址。（由于添加的是堆中对象的地址而不是字符串对象，即字符串常量池指向堆中对象的地址，因此返回的其实也是堆中对象的地址。）

> Java6上频繁调用intern会导致字符串池出现内存溢出（`java.lang.OutOfMemoryError: PermGen`）
>
> Java7之后字符串池在堆中，并且参与GC，回收重复的字符串对象。

intern案例1：

```java
String s = new String("aa"); //先在字符串池中创建一个"aa"对象，再在堆中创建一个字符串"aa"对象(将"aa"从字符串池复制到堆中)，赋值给s
String s1 = s.intern(); //检查字符串池中是否存在"aa"，此处存在，直接返回字符串池中的对象
String s2 = "aa"; //使用双引号创建直接返回池中的对象
System.out.println(s == s2);  // false，s引用的对象在堆中
System.out.println(s1 == s2); // true，s1和s2引用的对象都在池中
```

intern案例2：

```java
String s1 = new String("a") + new String("b"); //上面提到过，此时不会在字符串池中创建"ab"对象
String s2 = s1.intern(); //字符串池中不存在
//jdk1.6在池中创建一个新的字符串对象"ab"，并返回
//jdk1.7之后会把堆中的引用放到常量池中并返回，因此返回的也是堆中对象的地址
String s3 = "ab"; //字符串池中的对象

System.out.println(s1 == s3);
//JDK 1.6 false。s1是堆中对象，s3是池中对象。
//JDK 1.7 true。字符串池中对象s3指向堆中对象，因此相等
System.out.println(s2 == s3);  // true，s2和s3都是返回池中的对象
```

## hashCode和identifyHashCode

* hashCode：Object默认的`hashCode()`方法返回对象内存地址的hash值。当`hashCode()`方法被重写之后，返回值就不能用于唯一标识该对象。
  * `hashCode()`是Object的实例方法
* identifyHashCode：返回对象内存地址的hash值，即使`hashCode()`方法被重写。如果两个对象identifyHashCode相同，表示是同一个对象。
  * `identifyHashCode()`是System的静态方法

> 简单来说，地址相同，一定是一个对象，equal或hashCode相同，不一定是一个对象

String重写了`hashCode()`方法，如下：

```java
String str1 = new String("Hello");
String str2 = new String("Hello");
System.out.println(str1.hashCode() == str2.hashCode()); //true
System.out.println(System.identityHashCode(str1) == System.identityHashCode(str2)); //false

null.hashCode(); //空指针
System.identifyHashCode(null); //返回0
```

# StringBuilder和StringBuffer

`StringBuilder`和`StringBuffer`内部也封装了一个字符数组。都继承自`AbstractStringBuilder`。

与String不同的是，char数组不是`final`的，内部有一个count变量表示已使用的字符个数。

效率更高：通过数组扩容来拼接字符串，不需要创建新字符串对象

```java
public final class StringBuilder extends AbstractStringBuilder implements Serializable, CharSequence {
  public StringBuilder() {
    super(16); //默认长度为16
  }
  public StringBuilder(int var1) {
    super(var1); //指定长度
  }
  public StringBuilder(String var1) {
    super(var1.length() + 16); //初始字符串长度+16
    this.append(var1);
  }
  public StringBuilder append(String str) {
    super.append(str);
    return this;
  }
}
//父类
abstract class AbstractStringBuilder implements Appendable, CharSequence {
  char[] value;
  int count;
  //拼接字符串
  public AbstractStringBuilder append(String str) {
    if (str == null) {
      return this.appendNull();
    } else {
      int len = str.length();
      this.ensureCapacityInternal(this.count + len);
      str.getChars(0, len, this.value, this.count);
      this.count += len;
      return this;
    }
  }
  //数组扩容
  private void ensureCapacityInternal(int minCapacity) {
    //需要的长度大于数组容量才会扩容
    if (minCapacity - this.value.length > 0) {
      this.value = Arrays.copyOf(this.value, this.newCapacity(minCapacity));
    }
  }
  private int newCapacity(int minCapacity) {
    // 默认扩容是原来的2倍+2
    int newCapacity = (value.length << 1) + 2;
    //如果默认扩容之后小于需要的，就使用实际需要的容量
    if (newCapacity - minCapacity < 0) {
      newCapacity = minCapacity;
    }
    //小于0或大于(Integer.MAX_VALUE-8)，扩充至最大值
    return (newCapacity <= 0 || MAX_ARRAY_SIZE - newCapacity < 0)
      ? hugeCapacity(minCapacity)
      : newCapacity;
  }
  private int hugeCapacity(int minCapacity) {
    if (Integer.MAX_VALUE - minCapacity < 0) { // 超过int容量，越界
      throw new OutOfMemoryError();
    }
    //最大值为Integer.MAX_VALUE-8
    return (minCapacity > MAX_ARRAY_SIZE)
      ? minCapacity : MAX_ARRAY_SIZE;
  }
}
```

`StringBuffer`：所有操作都加了`synchronized`关键字，因此是线程安全的

> 1. String适用于少量字符串操作
> 2. StringBuilder适用单线程下进行大量字符串操作的情况（线程不安全）
> 3. StringBuffer适用多线程下进行大量字符串操作的情况（线程安全）

# Switch支持String

Java7之后switch支持字符串：编译时将字符串生成hashCode方法调用，并且通过`equals()`方法比较字符串值，**防止hash碰撞**。

如下：

```java
public class Main {
  public static void main(String[] args) {
    String str = "hello";
    switch (str) {
      case "hello": break;
      case "world": break;
    }
  }
}
//反编译查看代码如下（省略部分代码）
public static void main(String[] var0) {
    String var1 = "hello";
    byte var3 = -1;
    switch(var1.hashCode()) {
    case 99162322:
        if (var1.equals("hello")) {
            var3 = 0;
        }
        break;
    case 113318802:
        if (var1.equals("world")) {
            var3 = 1;
        }
    }
}
```

> **switch只支持整型**，char、String、枚举都是转为整型之后进行比较

# 字符串分割

1. 字符`|`、`*`、`+`都得加上转义字符，前面加上`\`。
2. 而如果是`\`，那么就得写成`\\\\`。
3. 如果一个字符串中有多个分隔符，可以用`|`作为连字符。

```java
String str = "Java string-split#test";
str.split(" |-|#"); 
//分割为[Java,string,split,test]
```
