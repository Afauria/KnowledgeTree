TODO

 LiveData：包装数据。

  1. 可以感知生命周期，UI处于显示状态的时候才刷新，
  2. 粘性效果，UI恢复的时候立马刷新
  3. 防止内存泄漏，
  4. 外部可以注册监听器监听数据变化

LiveData注册监听器，会被包装为`ObserverWrapper`，并存入Map中。

> `ObserverWrapper`中保存了版本号和是否活跃的状态，如果不采用包装组合的方式，那么开发者定义的Observer都需要继承自`ObserverWrapper`才行

```java
private SafeIterableMap<Observer<? super T>, ObserverWrapper> mObservers = new SafeIterableMap<>();
```

版本号的作用：避免页面onPause、onStart导致重复通知数据

```java
private void considerNotify(ObserverWrapper observer) {
    if (!observer.mActive) {
        return;
    }
    //判断是否处于活跃状态
    if (!observer.shouldBeActive()) {
        observer.activeStateChanged(false);
        return;
    }
    //判断Observer中的版本号，和自定义的版本号
    if (observer.mLastVersion >= mVersion) {
        return;
    }
    //更新版本号
    observer.mLastVersion = mVersion;
    observer.mObserver.onChanged((T) mData);
}
```

> 但是无法解决页面重建粘性事件的问题，Activity重建之后，观察者重新注册，版本号不一致，因此会再次发送事件。
>
> 对于视图绑定，Activity重建需要恢复，所以不是问题

MediatorLiveDarta：中介者模式，同时监听多个LiveData。当有一个数据源变化时则发出通知

通常View包含两个方面：显示变更（数据绑定）、响应事件（事件绑定）

事件响应又分为两类：操作Model（业务逻辑）、操作UI（视图逻辑）

事件绑定一般在Activity中，触发事件之后调用Presenter方法，数据变化之后回调通知View刷新

事件注册：

  1. MVVM中监听器可以直接在Activity中注册，触发事件、调用ViewModel方法
        1. 也可以通过Databinding的形式，传入listener，绑定到UI，在xml中注册监听，触发事件，使用lambda表达式调用listener方法
           2. listener可以在Activity中实现：`android:onClick="onClick"`，这种方式是在View中setOnClickListener，onClick中通过反射调用Activity的onClick方法
           3. 如果需要调用model，可以在ViewModel中实现，如`android:onClick="@{()->viewmodel.onDeviceClick(deviceBean)}"`，这种方式是生成BindingImpl类，View setOnClickListener，最终调用`_internalCallbackOnClick(sourceId,View)`方法，如果有多个View设置了监听器，则switch判断`sourceId`

回调：消息监听，在ViewModel中创建LiveData，Activity观察LiveData，LiveData变化之后操作UI（例如弹Toast）

DataBinding中监听事件

```java
  public final void _internalCallbackOnClick(int sourceId, android.view.View callbackArg_0) {
        switch (sourceId) {
            case 1: {
                // localize variables for thread safety
                // viewmodel != null
                boolean viewmodelJavaLangObjectNull = false;
                // viewmodel
                HomeViewModel viewmodel = mViewmodel;
                viewmodelJavaLangObjectNull = (viewmodel) != (null);
                if (viewmodelJavaLangObjectNull) {
                    viewmodel.onFamilyEntryClick();
                }
                break;
            }
            case 2: {
                // localize variables for thread safety
                // viewmodel != null
                boolean viewmodelJavaLangObjectNull = false;
                // viewmodel
                HomeViewModel viewmodel = mViewmodel;
                viewmodelJavaLangObjectNull = (viewmodel) != (null);
                if (viewmodelJavaLangObjectNull) {
                    viewmodel.onPIPEntryClick();
                }
                break;
            }
        }
    }
```

 前两种方式如果需要等待ViewModel返回数据，需要在Activity中使用协程，或者使用回调监听，可以用LiveData替代回调接口，例如弹Toast

假设要在Adapter中注册View监听器，调用Activity中的方法响应：
  1. 给Adapter注册监听器，item点击的时候通知外部变化，回调Activity方法。
  2. 把Presenter传入Adapter，调用Presenter方法操作数据，然后回调通知View刷新。
 1. 如果不需要操作数据，可能直接传入Activity引用，然后调用Activity方法。
    MVVM中通过数据驱动行为：将事件绑定也以数据绑定的形式处理，只操作ViewModel，修改Command消息，外部监听数据变化，作出反应

```java
 View事件触发---->改变ViewModel内的Command对象---->外部通过LiveData的方式监听Command变化---->根据Command内容改变UI
```

 好处：

1. 不需要编写额外的接口，直接利用LiveData的监听。MVC中，MVP中会存在refreshA()，refreshB()，gotoActivity()这样的接口
2. 不需要传Activity引用或者监听器，传ViewModel，ViewModel不会持有View引用，而是通过DataBinding和LiveData，实现数据观察
3. 一切皆数据，逻辑控制只操作ViewModel（业务逻辑和视图逻辑）
4. 具备LiveData的优势

 在MVP模式中

```java
 *                 用户
 *                  |
 *   View           |                             调用Presenter方法        Model
 * 注册监听器—————>触发事件————————————————————————————————————————————————>CRUD数据
 *
 * 刷新UI：播放动画/弹Toast/页面跳转/Fragment控制<——————调用View方法，需要持有View—————————————
 *                                 异步：回调通知View           异步：回调通知Presenter
 *                                 同步：直接返回数据            同步：直接返回数据
```

MVVM：Model层存放业务数据，ViewModel存放UI的数据，有些情况业务数据和UI数据不对应，需要ViewModel做转换

```java
View注册监听器————>调用ViewModel————>CRUD数据——————>回调或者同步返回：更新ViewModel数据——————>通过LiveData手动注册监听，观察数据作出行为、DataBinding notifyChange观察数据刷新显示(不需要持有View)--->刷新UI
```

ViewModel中使用LiveData替代ObservableFiled，DataBinding会自动生成监听LiveData的代码，也不用继承BaseObserveable。

LiveData相比ObservableField还能监听生命周期

时序图、类图

使用协程可以将异步回调以同步的方式编写

UICommand相当于消息体

注意：LiveData具有黏性，页面重建或者ViewPager切换重新observe的时候会重复执行，需要增加判断



# LiveData

缺点：

1. 只能在主线程更新
2. 异步线程多次postValue可能会丢失数据
3. 操作符不够强大，难以处理复杂数据源
4. setValue相同的值订阅者可以感知

# Flow

冷流：数据被订阅时，才开始发射数据，当有多个订阅者时，每个订阅者都会重新收到发布者的完整数据

热流：无论有没有被订阅，事件始终会发生，多个订阅者共享。

Flow优点：

1. 支持数据操作和处理，例如map
2. 支持线程切换、背压

Flow默认是冷流

SharedFlow：热流

```kotlin
public fun <T> MutableSharedFlow(
    replay: Int = 0,  // 当被订阅时，重发几个数据，默认为0，即新订阅者不会获取以前的数据
    extraBufferCapacity: Int = 0,  // 除了需要replay的数据之外，额外缓存多少个数据，默认为0
    onBufferOverflow: BufferOverflow = BufferOverflow.SUSPEND // 缓存策略，缓冲区满了之后如何处理，默认挂起
): MutableSharedFlow<T>
```

StateFlow：

- 它始终是有值的。
- 它的值是唯一的。
- 它允许被多个观察者共用 (因此是共享的数据流)。
- 它永远只会把最新的值重现给订阅者，这与活跃观察者的数量是无关的。

# LiveData和Flow

二者使用场景不同：LiveData用于更新UI，Flow用于处理数据、切换线程

官方建议迁移到Flow的原因是因为Flow是kotlin库中的东西，而LiveData是android独有的，这导致了LiveData在未来多平台的支持上有局限性，在Compose会支持多平台的未来，LiveData必然会被淘汰。

LiveData会被弃用吗？

* LiveData可以用于Java，Flow基于Kotlin协程。
* LiveData学习成本更低
