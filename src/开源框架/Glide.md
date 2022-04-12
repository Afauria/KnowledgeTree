# 简介

快速高效的Android图片加载框架（基于Glide4）：

1. 支持多级缓存，缓存策略
2. 支持定制和应用请求选项，例如图片处理：填充模式、圆角、动画变换等
3. 自动压缩图片到适合View的大小
4. 自动和生命周期绑定
5. 支持预加载图片而不显示
6. 支持多种图片类型：Gif、WebP、Video等
7. 默认使用`HttpUrlConnection`进行网络请求，也可以使用OkHttp、Volley

[源码地址](https://github.com/bumptech/glide)

其他框架：`ImageLoader`、`Picasso`、`Fresco`

图片加载框架设计：

1. 异步加载：线程池，网络线程池，硬盘线程池
2. 切换线程：Handler
3. 缓存：LruCache、DiskLruCache
4. 防止OOM：软引用、LruCache、图片压缩、Bitmap像素存储位置
5. 内存泄露：注意ImageView的正确引用，生命周期管理
6. 列表滑动加载的问题：加载错乱、队列任务过多问题

# 使用

参考[官方文档](https://muyangmin.github.io/glide-docs-cn/)

## 添加依赖

kotlin使用kapt添加注解处理器

```groovy
repositories {
  google()
  mavenCentral()
}

dependencies {
  implementation 'com.github.bumptech.glide:glide:4.13.0'
  annotationProcessor 'com.github.bumptech.glide:compiler:4.13.0'
}
```

## 配置混淆

```shell
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.module.AppGlideModule {
 <init>(...);
}
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
  **[] $VALUES;
  public *;
}
-keep class com.bumptech.glide.load.data.ParcelFileDescriptorRewinder$InternalRewinder {
  *** rewind();
}

# for DexGuard only
-keepresourcexmlelements manifest/application/meta-data@value=GlideModule
```

## 权限

要从外部存储路径或者网络加载图片，可以申请读写权限

## 代码调用

流程可以分为：下载（网络请求）-->加载（加载到内存中）-->处理（变换）-->使用（例如设置到View中）

```java
//基本使用，url为空会清空imageView
Glide.with(fragment).load(myUrl).into(imageView);

//添加请求选项，例如占位图，填充方式、变换、缓存策略
Glide.with(fragment)
  .load(myUrl)
  .placeholder(placeholder) //占位图
  .error(R.drawable.error) //错误图
  .fallback(R.drawable.fallback) //备用图
  .thumbnail(thumbnaiUrl) //缩略图
  .override(100, 100) //指定图片大小
  .fitCenter() //居中填充
  .into(imageView);

//只加载已缓存过的图片，未缓存会加载失败（例如省流量模式）
Glide.with(fragment)
  .load(url)
  .onlyRetrieveFromCache(true)
  .into(imageView);

Glide.with(fragment)
  .load(url)
  .skipMemoryCache() //跳过内存缓存
  .diskCacheStrategy(DiskCacheStrategy.NONE) //跳过磁盘缓存
  .into(imageView);

//清除内存缓存，必须在主线程调用
Glide.get(context).clearMemory();
//清除磁盘缓存，必须在子线程调用
Glide.get(applicationContext).clearDiskCache();

//定义RequestOptions，可以复用请求选项，通过apply应用
RequestOptions sharedOptions = new RequestOptions().placeholder(placeholder).fitCenter();
Glide.with(fragment).load(myUrl).apply(sharedOptions).into(imageView);

//使用clear取消加载，如果不使用clear，setImageDrawable的图片可能会被修改回myUrl的图片
Glide.with(fragment).load(myUrl).into(imageView);
Glide.with(fragment).clear(imageView);
imageView.setImageDrawable(drawable);

//自定义Target：通过Glide加载和处理图片，但不设置到View中，而是自行设置
//例如有时候我们不想setImageDrawable，而是setBackground
Glide.with(context).load(url).into(new CustomTarget<Drawable>() {
    @Override
    public void onResourceReady(Drawable resource, Transition<Drawable> transition) {
    }

    @Override
    public void onLoadCleared(@Nullable Drawable placeholder) {
    }
  });
           
//默认在主线程加载，如果想在子线程加载，可以使用submit，返回Future对象
FutureTarget<Bitmap> futureTarget = Glide.with(context).asBitmap().load(url).submit(width, height);
Bitmap bitmap = futureTarget.get();
```

三种占位图：在主线程加载，不会应用变换。（占位图设计就是为了快速显示，不做多余的操作）

1. 占位图：请求过程中、请求失败时、url为null且没有设置error和fallback时显示。
2. 错误图：当请求失败、或者url为null且没有设置fallback时显示。
3. 备用图：url为null时显示。
4. 缩略图：有时候大图片请求比较慢，可以使用先加载较小的缩略图。

> url为null时优先级：`fallback > error > placeholder`
>
> 默认情况下url为null被视为异常情况，如果希望视为正常情况，需要显式指定fallback。
>
> （例如没有头像时显示空头像，属于正常业务逻辑，而不是发生了异常）

`RequestManager`和`RequestBuilder`：

1. `with`方法返回`RequestManager`：在图片下载前，例如`asBitmap`指定返回Bitmap类型，供后续处理。返回`RequestBuilder<Bitmap>`
2. `load`方法返回`RequestBuilder`：在图片下载后，例如图片处理，应用请求选项，设置到View中。

> load的Model可以是现成的Drawable、Bitmap，也可以是URL、Uri、File、byte[]数组等。

多个不同的View中使用同一个Drawable？

> 如果Drawable没有状态（例如焦点、选中状态等），可以共用。如果有状态，可能会产生冲突。

Target：请求到的图片最终是传给Target使用，至于怎么使用由不同的Target决定。

* `ImageViewTarget`：接收ImageView，拿到图片之后调用`setImageDrawable`设置图片
* `ViewTarget`：接收View，例如可以用来设置背景，或者自定义View，调用不同的设置方法

> `into`传入`ImageView`内部会自动转换为`ImageViewTarget`



## 自定义扩展

Glide类已经支持很多请求选项，但是如果想自定义方法或定制功能，就需要进行扩展

使用`@GlideModule`注解：只能在app模块中使用。编译之后会生成`GlideApp`类，可以像Glide一样使用

> 由于没法直接修改Glide类，所以需要生成新的类，包含Glide类的功能

```java
@GlideModule
public final class MyAppGlideModule extends AppGlideModule {}
//生成GlideApp类，使用如下
GlideApp.with(fragment)
   .load(myUrl)
   .placeholder(R.drawable.placeholder)
   .fitCenter()
   .into(imageView);
```

如果要扩展`GlideApp`方法，就需要使用`@GlideExtension`注解

> 1. 修饰扩展的类，只包含静态方法，定义私有的空构造方法，防止外部实例化对象。
> 2. 可以在多个地方使用，最终会被合并到一起。
> 3. 需要结合`@GlideOption`和`@GlideType`使用

### `@GlideOption`

使用`@GlideOption`来扩展`RequestOptions`

```java
@GlideExtension
public class MyAppExtension {
  private MyAppExtension() { }
  
  //方法必须是静态的，并且返回值是BaseRequestOptions
  //第一个参数需要继承自BaseRequestOptions，后面可以加多个参数
  @GlideOption
  public static BaseRequestOptions<?> miniThumb(BaseRequestOptions<?> options, int size) {
    return options.fitCenter().override(size);
  }
}
```

编译之后会生成一个`RequestOptions`子类

```java
public class GlideOptions extends RequestOptions {
  public GlideOptions miniThumb(int size) {
    return (GlideOptions) MyAppExtension.miniThumb(this, size);
  }
}
```

并在生成的`GlideApp`中添加自定义的扩展方法，使用如下

```java
GlideApp.with(fragment)
   .load(url)
   .miniThumb(thumbnailSize)
   .into(imageView);
```

### `@GlideType`

使用`@GlideType`来扩展`RequestManager`，用于支持新的资源类型（例如SVG、GIF等）。例如添加Gif支持（官方已经支持GIF）

```java
@GlideExtension
public class MyAppExtension {
  private static final RequestOptions DECODE_TYPE_GIF = decodeTypeOf(GifDrawable.class).lock();

  //方法必须是静态的，并且返回值是RequestBuilder
  //第一个参数需要继承自RequestBuilder，后面可以加多个参数
  @GlideType(GifDrawable.class)
  public static RequestBuilder<GifDrwable> asGif(RequestBuilder<GifDrawable> requestBuilder) {
    return requestBuilder.transition(new DrawableTransitionOptions()).apply(DECODE_TYPE_GIF);
  }
}
```

编译之后会生成一个`RequestManager`子类

```java
public class GlideRequests extends RequesetManager {
  public GlideRequest<GifDrawable> asGif() {
    return (GlideRequest<GifDrawable> MyAppExtension.asGif(this.as(GifDrawable.class));
  }
}
```

使用如下

```
GlideApp.with(fragment)
  .asGif()
  .load(url)
  .into(imageView);
```

# 知识点

## 生命周期绑定

1. with方法用来控制glide图片加载的生命周期，里面可以传入activity、fragment、application。其实主要是分为两种，一种是application，一种是非application。
2. 当传入application 的时候，glide加载的生命周期跟随应用程序一样，不需要特殊处理
3. 当传入activity或者fragment的时候，glide会和activity的生命周期绑定。这里实现的原理是，glide添加了一个透明的fragment，根据透明fragment的生命周期来监听activity生命周期。

## 缓存级别

1. 活动资源（Active Resources）：正在使用的资源
2. 内存缓存（Memory Cache）：最近被加载过，并仍存在于内存中，使用`LruResourceCache`
3. 资源类型（Resource）：经过解码、转换，并写入过磁盘缓存
4. 数据来源（Data）：从磁盘缓存中获取
5. 重新从URL请求

## 缓存Key（Cache Keys）

默认通过model来获取缓存资源（例如File、Uri、URL），如果是自定义的model，需要实现`hashCode()`和`equals()`方法。（保证资源标识唯一）。也可以使用自定义的`signature(Key)`选项，传入Key对象

如果对于图片进行了处理，例如修改了宽高、使用了变换或者选项，此时会被视为不同资源进行缓存。

## 磁盘缓存

也叫本地缓存、文件缓存，使用`DiskLruCacheWrapper`

**磁盘缓存**策略：

- `DiskCacheStrategy.AUTOMATIC`（默认策略）：对远程数据，只缓存原始图片（下载远程数据代价较大）。对本地数据，仅缓存变换过的缩略图。
- `DiskCacheStrategy.all`：缓存源图片和变换后的图片
- `DiskCacheStrategy.DATA`：只缓存源图片
- `DiskCacheStrategy.RESOURCE`：只缓存变换后的图片
- `DiskCacheStrategy.NONE`：不进行磁盘缓存

使用方式：

```java
Glide.with(fragment)
  .load(url)
  .diskCacheStrategy(DiskCacheStrategy.ALL)
  .into(imageView);
```



## into方法

主要是将图片显示view中
 into显示的时候会根据imageview.getscalseltype对图片进行处理源码中只处理了两种，centercrop和fitcenter两种

into方法中也可以传入target，我们自定义处理主要有两种，一种simpletarget和viewtargwt
 两者都可以拿到图片资源，可以处理很多事情



https://muyangmin.github.io/glide-docs-cn/doc/caching.html

## 预加载图片

有的时候我们希望提前请求图片进行缓存，等真正需要的时候就能够快速显示，然而使用into方法直接就显示了。

有两种做法：

1. 自定义Target，接收图片不做处理
2. 使用`preload()`方法，preload内部实际上就是使用`PreloadTarget`目标，请求完资源之后调用`clear`方法解除和Target的绑定。

```java
//preload缓存图片
Glide.with(this).load(url).diskCacheStrategy(DiskCacheStrategy.SOURCE).preload();
//再次请求，直接使用缓存中的图片
Glide.with(this).load(url).diskCacheStrategy(DiskCacheStrategy.SOURCE).into(imageView);
```

注：这里指定缓存策略为源文件，因为into方法会自动缩放图片大小，如果不指定的话，可能会找不到缓存资源，而去重新下载。

## 只下载图片

有的时候我们只需要下载图片，不需要加载到内存中使用，此时可以使用`downloadOnly()`方法

```java
downloadOnly(int width, int height); //在子线程中下载，返回FutureTarget对象，通过get方法返回File文件对象
downloadOnly(Y target); //在主线程中下载，需要自定义实现Target<File>接口，onResourceReady返回File对象
```

## 监听加载过程

有的时候我们想监听图片加载过程，例如下载成功、下载失败做一些事情，可以使用`listener`方法，如下

```java
Glide.with(this).load(url).listener(new RequestListerner<Drawable>() {
    @Override
    boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
    }
    @Override
    boolean onResourceReady(R resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
    }
}).into(imageView);
```

需要注意的是，返回true则事件不会继续传递，例如target中不会再回调`onResourceReady`方法

# 源码解析

设计模式

TODO

https://www.jianshu.com/p/5c8ce241199e

* [带你玩转Glide的回调与监听](https://mp.weixin.qq.com/s?__biz=MzA5MzI3NjE2MA==&mid=2650240067&idx=1&sn=382504612a3f225d2d98554118d5b3a4)
* [Glide系列第二弹，从源码的角度深入理解Glide的执行流程](https://mp.weixin.qq.com/s?__biz=MzA5MzI3NjE2MA==&mid=2650239302&idx=1&sn=593111695683337bacb066c32fd5d1a3)
* [深入探究Glide的缓存机制](https://mp.weixin.qq.com/s?__biz=MzA5MzI3NjE2MA==&mid=2650239697&idx=1&sn=663a7492cbe63442c839922cefad4a65)
