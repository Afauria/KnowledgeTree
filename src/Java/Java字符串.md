# String对象

String不是基本数据类型，是引用类型。

内部使用char数组存储，并且被final修饰，初始化的时候拷贝数组赋值，长度固定，之后不可修改。

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
    return  new String(offset + beginIndex, endIndex - beginIndex, value);
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

如果是直接拼接字符串字面量，编译器会进行常量折叠。如下：

```java
String str = "Hello" + " World";
//反编译结果如下
String var1 = "Hello World";`
```

## int转String

```java
int i = 5;
String i1 = "" + i; //会编译成StringBuilder进行拼接
String i2 = String.valueOf(i); //内部调用Integer.toString
String i3 = Integer.toString(i);
```

## String长度限制

编译期限制：字符串字面量长度不能超过65535。否则javac会编译失败（常量字符串过长）。字面量长度为65534可以编译通过

> javac将java文件编译成class文件，class文件是在JVM上运行的，因此要遵循JVM字符串常量池规范。

运行期限制：不能超过int表示范围，否则会抛异常。

> String内部使用char数组存储，长度不能超过int范围。

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

> 基于线程安全、性能、缓存等方面考虑
>
> 1. 线程安全：由于字符串不可变，因此多个线程访问的时候，如果线程修改了值，会创建一个新的字符串，而不是修改相同的值。
> 2. 性能：通过字符串池和hashCode缓存，更加高效。
> 3. 缓存：字符串使用广泛，频繁创建消耗资源。因此JVM专门开辟了一个空间存储字符串，即字符串常量池，对字符串进行缓存，可以节省堆空间。（两个相同的字符串变量，指向字符串池中的同一个对象）。如果字符串可变的话，修改了内容，所有引用都会跟着修改。
> 4. hashCode缓存：HashMap、HashTable、HashSet等集合类，会调用hashCode计算hash值，由于字符串不可变，因此String可以重写hashCode方法，首次调用保存hash值，之后访问的时候直接返回缓存的hash值。

hashCode缓存源码如下

```java
//String.class
public int hashCode() {
    int h = hash;
    final int len = length();
    if (h == 0 && len > 0) {
        for (int i = 0; i < len; i++) {
            h = 31 * h + charAt(i);
        }
        hash = h;
    }
    //hash值不为空，直接返回
    return h;
}
```

## String常量池

为了减少相同字符串重复创建，节省内存。JVM单独开辟了一块内存区域保存字符串常量，即字符串常量池。

* JDK7以前，字符串常量池放在永久代中。
* JDK7将字符串常量池放到了堆内存中。
* JDK8中，使用元空间替代永久代，字符串常量池放到了元空间中。

```java
String a = "123";
String b = "123";
System.out.println(a == b);//true：同一个对象。
//使用双引号创建的是字符串常量，存储在常量池中
//创建常量对象的时候会先判断常量池中是否已经存在，如果存在则直接引用

String a = "123";
String b = new String("123");
System.out.println(a == b);//false
//使用new String创建对象，存放在堆中，两个引用不相等
//实际上是将常量池中的123复制到了堆中，如果常量池没有，会先再常量池中创建，如下

String a = new String("123");
//实际上创建了两个对象，一个在常量池，一个在堆中
//如果前面常量池已经有该字符串，则只创建一个对象。
```

## hashCode和identifyHashCode

* hashCode：Object默认的`hashCode()`方法返回对象内存地址的hash值。当`hashCode()`方法被重写之后，返回值就不能用于唯一标识该对象。
  * `hashCode()`是Object的实例方法
* identifyHashCode：返回对象内存地址的hash值，即使`hashCode()`方法被重写。如果两个对象identifyHashCode相同，表示是同一个对象。
  * `identifyHashCode()`是System的静态方法

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
  //拼接字符串
  public AbstractStringBuilder append(String var1) {
    if (var1 == null) {
      return this.appendNull();
    } else {
      int var2 = var1.length();
      this.ensureCapacityInternal(this.count + var2);
      var1.getChars(0, var2, this.value, this.count);
      this.count += var2;
      return this;
    }
  }
  //数组扩容
  private void ensureCapacityInternal(int var1) {
    if (var1 - this.value.length > 0) {
      this.value = Arrays.copyOf(this.value, this.newCapacity(var1));
    }
  }
}
```

`StringBuffer`：所有操作都加了`synchronized`关键字，因此是线程安全的

# Switch支持String

Java7之后switch支持字符串：编译时将字符串转为对应的hashCode，通过`equals()`方法比较hashCode值。

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

> switch只支持整型，char、String、枚举都是转为整型之后进行比较
