# Java运行时栈帧

在JVM内存模型中存在虚拟机栈，且每个线程独立，栈帧就是虚拟机栈中的元素。

JVM中方法调用都是以方法栈的形式执行，每一个方法都是一个栈帧，方法调用即栈帧的入栈出栈过程。栈帧包含方法的局部变量表、操作数栈、动态链接和返回地址等。

# 初识字节码

1. 编译：使用`javac`命令将`.java`源文件编译为`.class`字节码文件，使用二进制存储。例如`javac Test.java`
2. 反汇编查看字节码：使用`javap`命令反编译查看字节码指令，例如`javap -c -v Test`，`-v`可以查看附加信息，例如常量池等

> Java虚拟机指令（字节码指令）例如`istore、iload、iadd等`，可以理解为是JVM的汇编语法，以助记符的形式代替机器指令，便于阅读。
>
> Class文件只能被JVM识别，无法直接运行。

实际开发中可能并不需要分析字节码，主要用于学习和理解，例如：JVM如何执行Java代码、Java语法糖原理等

## Java常用语法糖

Java中提供了很多语法糖，在编译时会进行脱糖，转成JVM规范的字节码，例如

1. 泛型（ParamterizedType，参数化类型）类型擦除是什么
2. 自动拆装箱：基本数据类型与包装类互相转换
3. 条件编译：条件分支剪支优化
4. for-each循环：转换为迭代器
5. 枚举对象转换为普通对象
6. 内部类：非静态内部类持有外部类的引用
7. 可变长参数：转换为数组

## 常用字节码指令

介绍下常用的字节码指令，看字面意思大概能明白

1. 加载：x一般为i（整型）、f（浮点型）、l（长整型）、a（引用类型）等
   1. `xload_`从局部变量表中加载，加入操作数栈
   2. `xconst_`、`bipush`、`sipush`、`ldc`等加载常量，加入操作数栈
2. 存储：`xstore`弹出栈顶元素，保存到局部变量表（本地变量表）
3. 算术相关：
   1. `xadd`加法、`xsub`减法、`xmul`乘法、`idiv`除法、`xrem`取余等指令，弹出两个操作数，并将运算结果加入操作数栈
   2. 位运算指令：`xshl`位移、`xor`按位或、`xand`按位与、`xxor`按位异或
4. 方法调用：弹出栈顶的元素，依次作为方法的参数
   1. `invokevirtual`：调用对象实例方法
   2. `invokestatic`：调用静态方法（类方法）
5. goto：跳转到某一条指令，用于控制循环
6. 字段访问：
   1. `getfield`：获取成员变量（实例变量），get弹出一个操作数，表示实例，并将结果入栈，即`obj.field`
   2. `putfield`：修改成员变量，put传入两个操作数，第一个表示赋值的结果，第二个表示实例，即`obj.field=result;`
   3. `getstatic`、`putstatic`：访问静态变量（类变量）
7. 此外还有条件判断、同步控制（synchronized）、异常处理、强制转换等指令

> 字节码指令集比较多，用到的时候去查即可：
>
> * [字节码指令集：Wiki百科](https://en.wikipedia.org/wiki/List_of_Java_bytecode_instructions)
> * [字节码指令集：中文掘金](https://juejin.cn/post/6844903461339807757)

## 示例1

```java
public class Test {                                                              
    public static void main(String[] args) {                                     
        int a = 12;                                                              
        int b = 21;                                                              
        int c = a + b;                                                           
        System.out.println(String.valueOf(c));                                   
    }                                                                            
}
```

`javap -c Test`查看字节码

```shell
Compiled from "Test.java"
public class Test {
  public Test();
    Code:
       0: aload_0
       1: invokespecial #1                  // Method java/lang/Object."<init>":()V
       4: return

  public static void main(java.lang.String[]);
    Code:
       # 将12加入操作数栈
       0: bipush        12
       # 弹出操作数栈顶元素12，保存到局部变量表中的第1个位置，即变量1=12
       2: istore_1
       # 将21加入操作数栈
       3: bipush        21
       # 弹出操作数栈顶元素21，保存到局部变量表中的第2个位置，即变量2=21
       5: istore_2
       # 从局部变量表中获取变量1，即12，加入操作数栈
       6: iload_1
       # 从局部变量表中获取变量2，即21，加入操作数栈
       7: iload_2
       # 弹出操作数栈顶两个元素12和21，进行相加，得到33，并加入操作数栈中
       8: iadd
       # 弹出操作数栈顶元素33，保存到局部变量表中的第3个位置，即变量3=33
       9: istore_3
       # 读取System的静态变量out，加入操作数栈
      10: getstatic     #2                  // Field java/lang/System.out:Ljava/io/PrintStream;
      # 从局部变量表中获取变量3，即33，加入操作数栈
      13: iload_3
      # 弹出栈顶的33，调用String.valueOf方法，并将返回值"33"加入操作数栈中
      14: invokestatic  #3                  // Method java/lang/String.valueOf:(I)Ljava/lang/String;
      # 弹出栈顶的"33"，并调用System.out变量的println方法
      17: invokevirtual #4                  // Method java/io/PrintStream.println:(Ljava/lang/String;)V
      # 退出方法
      20: return
}
```

## 示例2

```java
public class Test {
    int a = 1;
    int b = 10;
    int c = 300;
    int d = 60000;
    char e = 'A';
    char f = '中';
    boolean g = true;
}
```

查看字节码如下

```shell
public class Test {
  int a;
  int b;
  int c;
  int d;
  char e;
  boolean f;

  public Test();
    Code:
       0: aload_0
       1: invokespecial #1                  // Method java/lang/Object."<init>":()V
       4: aload_0
       # 加载常量1，入栈
       5: iconst_1
       6: putfield      #2                  // Field a:I
       9: aload_0
       # 加载常量10，入栈
      10: bipush        10
      12: putfield      #3                  // Field b:I
      15: aload_0
      # 加载常量100，入栈
      16: sipush        300
      19: putfield      #4                  // Field c:I
      22: aload_0
      # 加载常量60000，入栈
      23: ldc           #5                  // int 60000
      25: putfield      #6                  // Field d:I
      28: aload_0
      # 加载常量65，入栈
      29: bipush        65
      31: putfield      #7                  // Field e:C
      34: aload_0
      # 加载常量20013
      35: sipush        20013
      38: putfield      #8                  // Field f:C
      41: aload_0
       # 加载常量1，入栈
      42: iconst_1
      43: putfield      #9                  // Field g:Z
      46: return
}
```

boolean和char都使用int表示，char范围是2个字节。

可以看到上面有多种加载常量的方法：

1. `iconst_0~5`：加载int型，范围是0～5
2. `bipush`：加载单字节常量`[-128～127]`
3. `sipush`：加载短整型常量，2个字节`[-32768~32767]`
4. `ldc`：加载常量池中的值

# 枚举类原理

看个例子

```java
enum TIME_UNIT {
    HOUR, MINUTE, SECOND;
}
```

使用javap反汇编查看汇编代码

```shell
$ javap -c TIME_UNIT
Compiled from "Main.java"
# 继承Enum<TIME_UNIT>抽象类
final class TIME_UNIT extends java.lang.Enum<TIME_UNIT> {
  # 将枚举编译成静态常量
  public static final TIME_UNIT HOUR;
  public static final TIME_UNIT MINUTE;
  public static final TIME_UNIT SECOND;
  
  # 生成一个$VALUES数组存储三个静态常量，并生成values方法
  public static TIME_UNIT[] values();
    Code:
       0: getstatic     #1                  // Field $VALUES:[LTIME_UNIT;
       3: invokevirtual #2                  // Method "[LTIME_UNIT;".clone:()Ljava/lang/Object;
       6: checkcast     #3                  // class "[LTIME_UNIT;"
       9: areturn
       
  # 生成valueOf方法，调用Enum父类的valueOf方法
  public static TIME_UNIT valueOf(java.lang.String);
    Code:
       0: ldc           #4                  // class TIME_UNIT
       2: aload_0
       3: invokestatic  #5                  // Method java/lang/Enum.valueOf:(Ljava/lang/Class;Ljava/lang/String;)Ljava/lang/Enum;
       6: checkcast     #4                  // class TIME_UNIT
       9: areturn

  # 实例化三个TIME_UNIT对象
  static {};
    Code:
       0: new           #4                  // class TIME_UNIT
       3: dup
       4: ldc           #7                  // String HOUR
       6: iconst_0
       7: invokespecial #8                  // Method "<init>":(Ljava/lang/String;I)V
      10: putstatic     #9                  // Field HOUR:LTIME_UNIT;
      13: new           #4                  // class TIME_UNIT
      16: dup
      17: ldc           #10                 // String MINUTE
      19: iconst_1
      20: invokespecial #8                  // Method "<init>":(Ljava/lang/String;I)V
      23: putstatic     #11                 // Field MINUTE:LTIME_UNIT;
      26: new           #4                  // class TIME_UNIT
      29: dup
      30: ldc           #12                 // String SECOND
      32: iconst_2
      33: invokespecial #8                  // Method "<init>":(Ljava/lang/String;I)V
      36: putstatic     #13                 // Field SECOND:LTIME_UNIT;
      39: iconst_3
      # 初始化一个长度为3的数组
      40: anewarray     #4                  // class TIME_UNIT
      43: dup
      44: iconst_0
      45: getstatic     #9                  // Field HOUR:LTIME_UNIT;
      48: aastore
      49: dup
      50: iconst_1
      51: getstatic     #11                 // Field MINUTE:LTIME_UNIT;
      54: aastore
      55: dup
      56: iconst_2
      57: getstatic     #13                 // Field SECOND:LTIME_UNIT;
      60: aastore
      61: putstatic     #1                  // Field $VALUES:[LTIME_UNIT;
      64: return
}
```

相当于下面的Java代码

```java
class TIME_UNIT extends Enum<TIME_UNIT> {
    public static final HOUR;
    public static final MINUTE;
    public static final SECOND;
    
    static {
      HOUR = new TIME_UNIT("HOUR", 0);
      MINUTE = new TIME_UNIT("MINUTE", 1);
      SECOND = new TIME_UNIT("SECOND", 2);
      $VALUES = new TIME_UNIT[3];
      $VALUES[0] = HOUR;
      $VALUES[1] = MINUTE;
      $VALUES[2] = SECOND;
    }
}
```

总结：

1. 枚举类会被编译成普通类，继承Enum抽象类，由于Java是单继承，因此枚举类无法继承其他类，但是可以实现接口
2. 枚举类中定义的枚举会被编译成静态常量，并在静态代码块中实例化枚举，并存入一个数组中
3. 生成`values()`和`valueOf()`方法
4. 枚举类中定义了多少个枚举，就有多少个实例对象，无法再通过new或反射来手动实例化

枚举的抽象类如下：每个枚举对象都有一个名称和序号

```java
public abstract class Enum<E extends Enum<E>> implements Comparable<E>, Serializable {
    private final String name;
    public final String name() {
        return name;
    }
    private final int ordinal;
    public final int ordinal() {
        return ordinal;
    }
    protected Enum(String name, int ordinal) {
        this.name = name;
        this.ordinal = ordinal;
    }
}
```

# 非静态内部类持有外部类引用

```java
public class Test {
    int a = 0;
    void test() {
        Runnable r = new Runnable() {
            @Override
            public void run() {
                //这里的this实际就是对外部类的引用
                this.a++;
            }
        };
    }
}
```

使用javac编译后得到两个class文件：Test.class、Test$1.class，`XXX$1`表示匿名内部类，非匿名内部类表示为`XXX$InnerClass`。

`javap -c Test1$1`反编译查看匿名内部类字节码

```shell
Compiled from "Test.java"
class Test$1 implements java.lang.Runnable {
  # 声明成员变量this$0
  final Test this$0;
  # 构造函数中传入外部类对象Test，并赋值给this$0
  Test$1(Test);
    Code:
       # 从局部变量表中获取第0个变量，即Test$1内部类对象本身this，加入栈中
       0: aload_0
       # 从局部变量表中获取第1个变量，即参数Test对象，加入栈中
       1: aload_1
       # 弹出栈顶两个元素，将Test对象赋值给Test$1的成员变量this$0
       2: putfield      #1                  // Field this$0:LTest;
       # 将Test$1对象加入栈中
       5: aload_0
       # 调用Test$1对象的初始化方法
       6: invokespecial #2                  // Method java/lang/Object."<init>":()V
       9: return

  # 内部类run方法
  public void run();
    Code:
       # 从局部变量表中获取第0个变量，即Test$1内部类对象本身，加入栈中
       0: aload_0
       # 弹出栈顶的Test$1对象，获取内部类成员变量this$0，并加入栈中
       1: getfield      #1                  // Field this$0:LTest;
       # 复制栈顶元素，即栈中存在两个this$0对象
       4: dup
       # 弹出栈顶的this$0对象，获取成员变量a，并加入栈中
       5: getfield      #3                  // Field Test.a:I
       # 将常量1加入栈中
       8: iconst_1
       # 弹出栈顶的两个元素1和变量a，相加之后加入栈中，即栈顶为1
       9: iadd
       # 弹除栈顶的两个元素1和变量this$0，将1赋值给this$0的成员变量a，即this$0.a=1
      10: putfield      #3                  // Field Test.a:I
      13: return
}
```

对应的Java代码为

```java
public class Test$1 implements java.lang.Runnable {
    final Test this$0;

    public Test$1(Test test) {
        this.this$0 = test;
    }

    @Override
    public void run() {
        this.this$0.a = this.this$0.a + 1;
    }
}
```

总结：非静态内部类默认持有外部类的引用，编译时会将外部类对象作为构造函数参数传给内部类

# 匿名内部类引用局部变量需要声明为final

内部类可以通过外部类的对象引用外部类的成员变量，那么如何引用局部变量呢？

```java
public class Test {
    void test() {
        //可以声明final，也可以不声明，但是不能被修改
        String a = "Hello";
        final String b = new String("World");
        final String c = "!";
        Runnable r = new Runnable() {
            @Override
            public void run() {
                System.out.println(a);
                System.out.println(b);
                System.out.println(c);
                //编译报错，提示需要声明为final才能被内部类使用
                //a = "foo";
                //编译报错，提示final无法修改
                //c = "foo";
            }
        };
    }
}
```

同样的方法查看内部类字节码：

```shell
class Test$1 implements java.lang.Runnable {
  final java.lang.String val$a;

  final java.lang.String val$b;

  final Test this$0;
  # 构造函数中传入外部类对象Test和String对象，并分别赋值给this$0、val$a和val$b
  Test$1(Test, java.lang.String, java.lang.String);
    Code:
       0: aload_0
       1: aload_1
       2: putfield      #1                  // Field this$0:LTest;
       5: aload_0
       6: aload_2
       7: putfield      #2                  // Field val$a:Ljava/lang/String;
      10: aload_0
      11: aload_3
      12: putfield      #3                  // Field val$b:Ljava/lang/String;
      15: aload_0
      16: invokespecial #4                  // Method java/lang/Object."<init>":()V
      19: return

  public void run();
    Code:
       0: getstatic     #5                  // Field java/lang/System.out:Ljava/io/PrintStream;
       # 从局部变量表获取第0个变量this，并入栈
       3: aload_0
       # 弹出栈顶元素，获取val$a成员变量，并入栈
       4: getfield      #2                  // Field val$a:Ljava/lang/String;
       # 弹出栈顶元素，输出
       7: invokevirtual #6                  // Method java/io/PrintStream.println:(Ljava/lang/String;)V
       
      10: getstatic     #5                  // Field java/lang/System.out:Ljava/io/PrintStream;
      # 从局部变量表获取第0个变量this，并入栈
      13: aload_0
      # 弹出栈顶元素，获取val$b成员变量，并入栈
      14: getfield      #3                  // Field val$b:Ljava/lang/String;
      17: invokevirtual #6                  // Method java/io/PrintStream.println:(Ljava/lang/String;)V
      
      20: getstatic     #5                  // Field java/lang/System.out:Ljava/io/PrintStream;
      # 加载字符串常量"!"，并入栈
      23: ldc           #7                  // String !
      25: invokevirtual #6                  // Method java/io/PrintStream.println:(Ljava/lang/String;)V
      28: return
}
```

结论：

1. 内部类使用外部类的局部变量，需要声明为常量，不能被修改，否则编译会报错
2. 编译时会将局部变量作为构造函数参数传给内部类
3. 如果局部变量是字符串常量、整型常量等，内部类可以从常量池中读取对象：使用bipush、sipush、ldc等指令从常量池中加载

# 泛型类型擦除

```java
public class Test {
    public void test() {
        foo1("Hello World!");
        foo2("Hello World!");
    }
    public <T> void foo1(T a) {}
    public <T extends String> void foo2(T a) {}
}
```

使用`javap -s Main`查看汇编代码：`-s`可以显示方法内部签名

```sh
public class Test {
  ...
  public void test();
    descriptor: ()V
    Code:
       0: aload_0
       1: ldc           #2                  // String Hello World!
       # 方法调用时方法签名不同
       3: invokevirtual #3                  // Method foo1:(Ljava/lang/Object;)V
       6: aload_0
       7: ldc           #2                  // String Hello World!
       9: invokevirtual #4                  // Method foo2:(Ljava/lang/String;)V
      12: return

  public <T> void foo1(T);
    descriptor: (Ljava/lang/Object;)V # 方法签名变为Object

  public <T extends java.lang.String> void foo2(T);
    descriptor: (Ljava/lang/String;)V # 方法签名变为String
    ...
}
```

结论：Java编译时会发生类型擦除，即编译后不存在泛型，会转换成Object类型，或者extend的类型。

# 自动拆装箱

```java
public class Test {
    public void test() {
        Integer a = 1; //自动装箱
        int b = a; //自动拆箱
    }
}
```

分析字节码：

```shell
public class Test {
  ...
  public void test();
    Code:
       0: iconst_1
       # 调用Integer.valueOf装箱
       1: invokestatic  #2                  // Method java/lang/Integer.valueOf:(I)Ljava/lang/Integer;
       4: astore_1
       5: aload_1
       # 调用Integer的intValue方法拆箱
       6: invokevirtual #3                  // Method java/lang/Integer.intValue:()I
       9: istore_2
      10: return
}
```

对应的Java代码为

```java
public void test(){
  Integer a = Integer.valueOf(1); //装箱
  int b = a.intValue(); //拆箱
}
```

结论：编译时调用包装类的`Integer.valueOf()`和`xxxValue()`方法进行拆装箱

# Java可变长参数

```java
public class Test {
    public void test() {
        foo(1, 2);
    }
    public void foo(int... args) {}
}
```

分析字节码

```shell
public class Test {
  ...
  public void test();
    descriptor: ()V
    Code:
       0: aload_0
       # 将2入栈
       1: iconst_2
       # 弹出栈顶元素2，新建长度为2的int数组，并将数组入栈
       2: newarray       int
       # 复制栈顶元素
       4: dup
       # 将0和1入栈，栈变为[arr, arr, 0, 1]
       5: iconst_0
       6: iconst_1
       # 弹出栈顶3个元素，1作为值，存入数组arr的索引0中，即arr[0]=1
       7: iastore
       # 复制栈顶元素，栈变为[arr, arr]
       8: dup
       # 将1和2入栈
       9: iconst_1
      10: iconst_2
      # 弹出栈顶3个元素，2作为值，存入数组arr的索引1中，即arr[1]=2
      11: iastore
      # 调用方法，弹出栈顶元素arr，作为参数传入
      12: invokevirtual #2                  // Method foo:([I)V
      15: return

  public void foo(int...);
    descriptor: ([I)V # 方法签名多了个[，表示数组，
    Code:
       0: return
}
```

结论：可变长参数编译时会转成数组传入

# Synchronized修饰方法和代码块

```java
public class Test {
    //修饰方法
    synchronized void foo1(String a, String b) { }
    //修饰代码块
    void foo2() { synchronized(this) {} }
}
```

`javap -c -v Test`查看字节码

```shell
Compiled from "Test.java"
public class Test
...
{
  ...
  synchronized void foo1(java.lang.String, java.lang.String);
    descriptor: (Ljava/lang/String;Ljava/lang/String;)V
    # 隐式Flag
    flags: ACC_SYNCHRONIZED
    Code:
      stack=0, locals=3, args_size=3
         0: return
      LineNumberTable:
        line 4: 0

  void foo2();
    descriptor: ()V
    flags:
    Code:
      stack=2, locals=3, args_size=1
         0: aload_0
         1: dup
         2: astore_1
         # 进入监视器
         3: monitorenter
         4: aload_1
         # 退出监视器
         5: monitorexit
         6: goto          14
         # 发生异常时
         9: astore_2
        10: aload_1
        # 退出监视器
        11: monitorexit
        12: aload_2
        13: athrow
        14: return
      Exception table:
         # 4到6行出现异常时，跳到第9行
         from    to  target type
             4     6     9   any
             9    12     9   any
      ...
}
SourceFile: "Test.java"
```

结论：

1. 同步方法：无需通过字节码指令控制，在方法常量池的方法表结构中添加`ACC_SYNCHRONIZED`访问标志。方法调用时检查该访问标志，让当前线程获取monitor对象，方法完成时释放。
2. 同步代码块：通过`monitorenter`和`monitorexit`指令控制。
   1. `monitorenter`进入监视器，对象处于锁定状态：判断当前对象的监视器计数器为0，则进入同步块，若为1，则判断监视器线程是否是当前线程，如果是，则进入同步块（重入）。否则进行等待，直到监视器计数器为0。
   2. `moniterexit`退出监视器
3. 发生异常时，自动释放锁

# 结语

参考文章：

* [大概优秀的java程序员都要会分析class文件吧](https://blog.islinjw.cn/2019/03/22/%E5%A4%A7%E6%A6%82%E4%BC%98%E7%A7%80%E7%9A%84java%E7%A8%8B%E5%BA%8F%E5%91%98%E9%83%BD%E8%A6%81%E4%BC%9A%E5%88%86%E6%9E%90class%E6%96%87%E4%BB%B6%E5%90%A7/)
* [字节码指令集：Wiki百科](https://en.wikipedia.org/wiki/List_of_Java_bytecode_instructions)
* [字节码指令集：中文掘金](https://juejin.cn/post/6844903461339807757)

