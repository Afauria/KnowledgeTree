# 系统架构

<img src="Android入门\Android系统架构.jpg" alt="Android系统架构" style="zoom: 50%;" />

Binder使用Linux内核的动态模块加载机制

# SDK说明

1. tools目录：包括测试、调试、第三方工具。模拟器、数据管理工具等。
2. platform-tools目录：包含开发app的平台依赖的开发和调试工具。从android2.3开始划出此目录，存放公用开发工具，比如adb、sqlite3等，被划分到了这里。包括：adb：android调试工具
3. fastboot：boot工具
4. build-tools目录：编译工具目录，包含了转化为davlik虚拟机的编译工具。包括：
   1. aapt（Android Asset Packaging Tool）：编译应用程序的资源文件（包括AndroidManifest.xml和你的Activities的xml文件），生成R.java文件，这样你就可以从你的java代码中引用资源。
   2. aidl：把.aidl 接口转换成java接口.
   3. dx：转化.class中间代码为davik中间代码,所有经过java编译的生成.class文件都需要此工具进行转换,最后打包进apk文件中
   4. dexdump：dump davik中间代码
5. `platforms/<android-version>`目录：包括android的平台库。包含在android.jar库中。你必须指一个平台为你的编译目标。
6. system-images目录：编译好的系统映像。模拟器可以直接加载。
7. sources目录：android sdk的源码目录
8. samples目录：sdk例子

# 应用清单文件

`AndroidManifest.xml`

# 历史版本

| Android版本名称Code name            | Android版本 | 版本发布时间   | 对应API      |
| ----------------------------------- | ----------- | -------------- | ------------ |
| (no code name)                      | 1.0         | 2008年9月23日  | API 1        |
| (no code name)                      | 1.1         | 2009年2月2日   | API 2        |
| Cupcake（仙女蛋糕）                 | 1.5         | 2009年4月17日  | API 3,NDK 1  |
| Donut（甜甜圈）                     | 1.6         | 2009年9月15日  | API 4,NDK 2  |
| Eclair（闪电泡芙）                  | 2.0.1       | 2009年12月3日  | API 6        |
| Eclair（闪电泡芙）                  | 2.1         | 2010年1月12日  | API 7,NDK3   |
| Froyo（冻酸奶）                     | 2.2.x       | 2010年1月12日  | API 8,NDK 4  |
| Gingerbread（姜饼）                 | 2.3-2.3.2   | 2011年1月1日   | API 9,NDK5   |
| Gingerbread（姜饼）                 | 2.3.3-2.3.7 | 2011年9月2日   | API 10       |
| Honeycomb（蜂巢）                   | 3.0         | 2011年2月24日  | API 11       |
| Honeycomb（蜂巢）                   | 3.1         | 2011年5月10日  | API 12,NDK 6 |
| Honeycomb（蜂巢）                   | 3.2.x       | 2011年7月15日  | API 13       |
| Ice Cream Sandwich（冰淇淋三明治）  | 4.0.1-4.0.2 | 2011年10月19日 | API 14,NDK 7 |
| Ice Cream Sandwich（冰淇淋三明治）  | 4.0.3-4.0.4 | 2012年2月6日   | API 15,NDK 8 |
| Jelly Bean（彩虹糖）                | 4.1         | 2012年6月28日  | API 16       |
| Jelly Bean（彩虹糖）                | 4.1.1       | 2012年6月28日  | API 16       |
| Jelly Bean（彩虹糖）                | 4.2-4.2.2   | 2012年11月     | API 17       |
| Jelly Bean（彩虹糖）                | 4.3         | 2013年7月      | API 18       |
| KitKat（奇巧，巧克力饼干）          | 4.4         | 2013年7月24日  | API 19       |
| Kitkat Watch（奇巧巧克力-穿戴设备） | 4.4W        | 2014年6月      | API 20       |
| Lollipop(Android L)（棒棒糖）       | 5.0         | 2014年6月25日  | API 21       |
| Lollipop(Android L)（棒棒糖）       | 5.1         | 2014年6月25日  | API 22       |
| Marshmallow(Android M)（棉花糖）    | 6.0         | 2015年5月28日  | API 23       |
| Nougat(Android N)（牛轧糖）         | 7.0         | 2016年5月18日  | API 24       |
| Nougat(Android N)（牛轧糖）         | 7.1         | 2016年12月     | API 25       |
| Oreo（Android O）（奥利奥）         | 8.0         | 2017年8月22日  | API 26       |
| Oreo（Android O）（奥利奥）         | 8.1         | 2017年12月5日  | API 27       |
| Pie （Android P）（馅饼）           | 9.0         | 2018年8月7日   | API 28       |
| （Android Q）                       | 10          |                | API 29       |
| （Android R)                        | 11          |                | API 30       |

# 虚拟机

## 方法数64K引用限制

[MultiDex](https://developer.android.com/studio/build/multidex?hl=zh-cn)

dex方法数限制为65535，即64K

> 65535：Davik和Art虚拟机使用`unsigned-short`类型（16位，64K）存储方法的索引。方法索引是在将class文件转化成dex文件时产生的，保存在dex文件中，目的是为了加快类加载过程的链接过程，即把索引链接换成方法在内存中的地址。
>
> HotSpot虚拟机中通过符号（包含类名、方法名信息）引用方法，在类加载的链接过程 中，把符号链接换成实际的内存地址（在java 内存的方法区）

解决：

1. 缩减代码
   1. 优化代码逻辑
   2. R8缩减没有使用的代码
   3. 减少依赖库
2. 缩减之后如果还超出，则需要使用MultiDex
   1. 使用MultiDex，拆分成多个dex文件，`MultiDex.install`的时候加入app的ClassLoader中的PathList中，内部包含一个Element数组（DexFile数组）
   2. 5.0以上默认支持多dex，将dex编译成单个`oat`文件，预编译执行。minSdkVersion设置为21或以上

## Dalvik和ART对比

Java虚拟机：基于栈。Dalvik：基于寄存器

1. Dalvik采用JIT。ART采用了AOT，并且在Android7.0加入了JIT，提高运行效率
2. DVM针对32位CPU设计，ART支持64位
3. ART优化了垃圾回收机制，和运行时内存空间分配：新增`Image Space`和`Large Object Space`

# Android虚拟机优化历程

**Java之所以比C/C++慢（Android之所以比iOS慢），主要原因就在于前者是编译成字节码之后JVM解释执行，后者是编译成本地代码之后直接执行。**

## Android 1.0 Dalvik（DVM）+解释器

DVM中解释器边运行边解释，运行速度慢。

> DVM是Google为Android平台开发的虚拟机，而不使用Java提供的虚拟机。可读取`.dex`的字节码。

## Android 2.2 DVM+JIT编译

通过JIT（Just In Time，即时编译）缓存热点代码为机器码，提高运行速度。

缺点：启动速度慢，每次运行都要重新编译，非热点代码还是解释执行

## Android 5.0 ART+AOT

采用AOT（Ahead of Time，提前编译）技术，在应用安装的时候预编译成机器码（dex文件转为oat文件），避免每次运行进行JIT编译

缺点：应用安装APP时间变长。编译质量不如JIT，机器码需要的存储空间更大

## Android 7.0 ART+AOT+JIT混合编译

应用安装的时候不进行编译，快速启动，在执行的时候分析热点代码。在系统空闲的时候进行AOT，编译热点代码（不会编译所有代码）。

## Android 8.0 改进解释器

提高解释执行效率

## Android 9.0 改进编译模版

开发阶段可以配置编译模版，指定热点代码，ART优先编译这部分代码。

# Google Nexus和Google Pixel

* Nexus：Google授权合作硬件厂商生产的Android手机，由三星、LG、HTC、华为、华硕等代工。
* Pixel：Google自主品牌手机，由Google自行设计硬件。

Nexus长久以来被视作专业开发者用的工程机，能第一时间升级最新版本的Android系统。最初是希望推动Android系统覆盖尽可能多的硬件，这也是造成性能不如Apple的原因。另外Google作为一家软件公司，也不可能在Android刚刚问世的时候专门去开一家手机厂，但实际上此举却成全了各大代工厂品牌，Nexus在销量上远不如代工厂的自有品牌。

由于Nexus定位和策略，以及命名混乱（例如Nexus 7代号被用于平板）等原因，Google推出了自主手机品牌Pixel，取代Nexus。

# 结语

参考资料：

* [9102年了，还不知道Android为什么卡？](https://juejin.cn/post/6844903912206499853)
