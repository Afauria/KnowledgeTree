

**HashMap 初始容量 10000 即 new HashMap(10000)，当往里 put 10000 个元素时，需要 resize 几次（初始化的那次不算）？**

A. 1 次
B. 2 次
C. 3 次
D. 0 次

# HashMap

HashMap和HashTable的区别？

| HashTable                 | HashMap        |
| ------------------------- | -------------- |
| 继承Dictionary类          | 实现Map接口    |
| 线程安全                  | 线程不安全     |
| 不允许null值              | 允许null值     |
| 默认容量：11              | 默认容量：16   |
| 扩容：x2+1                | 扩容：x2       |
| 直接使用Key的`hashCode()` | 自定义哈希算法 |

HashMap怎么解决hash冲突的？数组+链表+红黑树

   * jdk8以前，出现hash冲突，加入链表中
   * Jdk8中：如果冲突数量（TREEIFY_THRESHOLD）小于8，则是以链表方式解决冲突。冲突大于等于8且容量大于64时，就会将冲突的Entry转换为**红黑树**进行存储。
   * 单链表遍历时间复杂度是O(n)，红黑树查找时间复杂度为 O(logn)
   * 在jdk1.8之前是插入头部的，在jdk1.8中是插入尾部的。
     * 多线程下，头插法在扩容的时候可能会出现死循环。当然HashMap本身就是线程不安全的，应该尽量避免多线程使用



![](HashMap/HashMap.png)

put步骤：

1. 计算Key的hash值：`hash = hashCode() ^ (hashCode >>> 16)`，高16位与低16位做异或运算，减少冲突，高16位的特征会被加入到计算中
   1. 还有平方取中法，除留余数法，伪随机数法
2. 计算数组下标：`(length - 1) & hash`，&（与）运算比%（取余）效率更高
   1. HashMap容量永远是2的幂次，此时`length - 1`二进制全为1，因此计算下标的时候存在`(length - 1) & hash = hash % length`，提高运算效率
3. 如果计算的下标不存在，则直接插入，如果发生hash冲突，且equals相等，则更新键值对，否则插入链表或红黑树
4. 当容器中元素个数大于`capacity * loadfactor`时，会扩容到2倍大小
5. 如果table == null, 则为HashMap的初始化, 生成空table返回即可;
6. 如果table不为空, 需要重新计算table的长度, newLength = oldLength << 1(注, 如果原oldLength已经到了上限, 则newLength = oldLength);
3. 遍历oldTable:
   1. 首节点为空, 本次循环结束;
   2. 无后续节点, 重新计算hash位, 本次循环结束;
   3. 当前是红黑树, 走红黑树的重定位;
   4. 当前是链表
      1. 创建两个链表，低头loHead和高头hiHead
      2. 遍历计算链表的每个节点， 判断是否需要换位置。JAVA7时需要重新计算hash位，但是JAVA8做了优化，通过(e.hash & oldCap) == 0来判断是否需要移位; 如果为真则在原位不动（加到loHead链表中），否则需要移动到当前hash槽位 + oldCap的位置（加到hiHead链表中）
      3. loHead链表放到数组原位置table[index]，hiHead链表放到数组新位置table[index + oldCap]

扩容的时候不会重新计算hash值，而是将hash值与容量做&运算，低位会多出一位

例如 hash & 16，低位会多出一位，如果该位是1则移动位置，如果是0则不移动位置

0.75：`table.length * 3/4` 可以被优化为`(table.length >> 2) << 2) - (table.length >> 2) == table.length - (table.length >> 2)`, JAVA的位运算比乘除的效率更高, 所以取3/4在保证hash冲突小的情况下兼顾了效率;

HashMap如何扩容?

* size：大小
* capality：容量
* loadFactor：扩容因子（加载因子）默认是0.75，用于衡量map是否满了。和实时加载因子比较size/capality
* threshold：阈值，插入的时候判断size大于threshold开始扩容
* `threshold = capality * loadFactor`
* 默认长度16，每次扩大为原来的两倍

为什么扩容因子是0.75：空间利用率、避免冲突链表过长，减少查询成本。

初始容量声明为10，实际会创建16的数组

处理hash冲突方法：

* 开放定址法：冲突位置往后查询空位置
* 再hash法：计算新hash
* 拉链法：冲突位置构造成链表
* 公共溢出区：冲突之后填入溢出表，使用新表存储冲突元素

LinkedHashMap：通过双链表记录插入顺序，可以按顺序取出，

# ConcurrentHashMap

线程安全

* 1.8之前，segment分段锁设计`ReentrantLock + Segment` + `HashEntry`的方式进行实现，分为16个桶+16把锁，锁粒度是Segment
* 1.8之后，通过Node` + `CAS（无锁算法）` + `Synchronized，锁粒度是首节点，提高了并发性，其他线程此时可以访问其他Node
* ConcurrentHashMap键值不允许为null

![](HashMap/Java7ConcurrentHashMap.png)

![](HashMap/Java8ConcurrentHashMap.png)

# 其他

## 链表和数组

* 数组需要申请连续内存空间，大小固定。

* 数组应用场景：数据比较少；经常做的运算是按序号访问数据元素；数组更容易实现，任何高级语言都支持；构建的线性表较稳定。
* 链表应用场景：对线性表的长度或者规模难以估计；频繁做插入删除操作；构建动态性比较强的线性表。

https://blog.csdn.net/weixin_39667787/article/details/86678215
