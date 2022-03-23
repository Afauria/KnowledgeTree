还未迁移，见[博客](https://blog.afauria.xyz/categories/Android/)

# 大图片

1. Android 3.0 之前：Bitmap像素数据存储在 native 堆中，数据的释放依赖 Java对象的 finalize() 方法回调，该方法的调用不太可靠，而且现在已经被 Java 标记为废弃。
2. Android 3.0 ~ 7.0：Bitmap 像素数据存储在 Java 堆中，确实解决了可靠释放的问题；也带来一个新问题：Android 应用程序对 Java 堆的限制是很严格的，创建Bitmap这种大对象容易引起 GC。
3. Android 8.0 及以后：Bitmap 像素数据存储在 native 堆中，同时引入了 NativeAllocationRegistry 机制保证了 native 内存释放的可靠性，同时可以用的空间大大增加。
