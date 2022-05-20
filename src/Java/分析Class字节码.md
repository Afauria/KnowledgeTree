# 前言

Java中提供了很多语法糖，在编译时会进行脱糖，转成JVM规范的字节码，例如

1. 泛型（ParamterizedType，参数化类型）类型擦除是什么
2. 自动拆装箱：基本数据类型与包装类互相转换
3. 条件编译：条件分支剪支优化
4. for-each循环：转换为迭代器
5. 枚举对象转换为普通对象
6. 内部类：非静态内部类持有外部类的引用
7. 可变长参数：转换为数组

学会分析Class字节码，有助于理解上述结论的原理。

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

# 结语

参考文章：[大概优秀的java程序员都要会分析class文件吧](https://blog.islinjw.cn/2019/03/22/%E5%A4%A7%E6%A6%82%E4%BC%98%E7%A7%80%E7%9A%84java%E7%A8%8B%E5%BA%8F%E5%91%98%E9%83%BD%E8%A6%81%E4%BC%9A%E5%88%86%E6%9E%90class%E6%96%87%E4%BB%B6%E5%90%A7/)
