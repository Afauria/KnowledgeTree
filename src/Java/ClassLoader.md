双亲委托

自定义类加载器

https://www.jianshu.com/p/f4318e626a22



* getName()：返回的是虚拟机里面的class的表示
* getCanonicalName()：返回的是更容易理解的表示

> 对于普通类来说,二者没什么区别,只是对于特殊的类型上有点表示差异。例如
>
> * byte[]类型，前者返回[B，后者返回byte[]
> * byte[][]类型，前者返回[[B，后者返回byte

