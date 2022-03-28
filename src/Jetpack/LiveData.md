 LiveData：包装数据。
  1. 可以感知生命周期，UI处于显示状态的时候才刷新，
  2. 粘性效果，UI恢复的时候立马刷新
  3. 防止内存泄漏，
  4. 外部可以注册监听器监听数据变化

 通常我们处理View事件会做两类事情：操作Model（业务逻辑）、操作UI（视图逻辑）
 UI事件分为两类：显示变更（数据绑定）、作出行为（事件绑定）

 事件绑定一般在Activity中，触发事件之后调用Presenter方法，数据变化之后回调通知View刷新

事件注册：

  1. MVVM中监听器可以直接在Activity中注册，触发事件、调用ViewModel方法
        2. 也可以通过Databinding的形式，传入listener，绑定到UI，在xml中注册监听，触发事件，使用lambda表达式调用listener方法
         1. listener可以在Activity中实现：`android:onClick="onClick"`，这种方式是在View中setOnClickListener，onClick中通过反射调用Activity的onClick方法
         2. 如果需要调用model，可以在ViewModel中实现，如`android:onClick="@{()->viewmodel.onDeviceClick(deviceBean)}"`，这种方式是生成BindingImpl类，View setOnClickListener，最终调用`_internalCallbackOnClick(sourceId,View)`方法，如果有多个View设置了监听器，则switch判断`sourceId`


回调：消息监听，在ViewModel中创建LiveData，Activity观察LiveData，LiveData变化之后操作UI（例如弹Toast）

```java
  public final void _internalCallbackOnClick(int sourceId, android.view.View callbackArg_0) {
        switch (sourceId) {
            case 1: {
                // localize variables for thread safety
                // viewmodel != null
                boolean viewmodelJavaLangObjectNull = false;
                // viewmodel
                com.cvte.tv.smarthome.home.HomeViewModel viewmodel = mViewmodel;
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
                com.cvte.tv.smarthome.home.HomeViewModel viewmodel = mViewmodel;
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



# ViewModel

## 基本使用

`activity-ktx`封装了`ViewModelProvider`创建ViewModel

```kotlin
//使用ViewModelProvider
val model3 = ViewModelProvider(this).get(HomeViewModel::class.java)
//Fragment使用Activity的ViewModel，用于多个Fragment共享
val model4 = ViewModelProvider(requireActivity()).get(HomeViewModel::class.java)

//使用activity-ktx
val model: HomeViewModel by viewModels()
//Fragment使用Activity的ViewModel
val model2: HomeViewModel by activityViewModels()
```

早期是使用`ViewModelProviders.of(this)`创建ViewModelProvider，后来改为直接构造

AndroidViewModel：持有Application对象

ViewModelScope：自动销毁协程，ViewModel clear的时候遍历Tag，调用Closeable的close方法。在主线程clear

```kotlin
val ViewModel.viewModelScope: CoroutineScope
        get() {
            val scope: CoroutineScope? = this.getTag(JOB_KEY)
            if (scope != null) {
                return scope
            }
            return setTagIfAbsent(JOB_KEY,
                CloseableCoroutineScope(SupervisorJob() + Dispatchers.Main.immediate))
        }

internal class CloseableCoroutineScope(context: CoroutineContext) : Closeable, CoroutineScope {
    override val coroutineContext: CoroutineContext = context

    override fun close() {
        coroutineContext.cancel()
    }
}
```



## 源码

ViewModelProvider

ViewModelStore

VIewModel



1. `ViewModelStore`：包含一个Map，根据Key找到对应的ViewModel，不设置的话使用默认Key。每个Activity或Fragment都包含一个Store，监听页面`ON_DESTROY`销毁，如果不是配置变更`isChangingConfigurations()`，则调用clear清除ViewModel，清除之前先遍历调用ViewModel的clear

2. `ViewModelProvider`：提供给外部调用，用于创建ViewModel，优先从ViewModelStore中获取，可以选择不同的工厂来创建ViewModel

3. `ViewModelProvider(this)`传入的this如果是Activity或Fragment，会使用`SavedStateViewModelFactory`工厂，否则使用`NewInstanceFactory`工厂

   ```java
   public ViewModelProvider(@NonNull ViewModelStoreOwner owner) {
     this(owner.getViewModelStore(), owner instanceof HasDefaultViewModelProviderFactory
       ? ((HasDefaultViewModelProviderFactory) owner).getDefaultViewModelProviderFactory()
       : NewInstanceFactory.getInstance());
   }
   ```



ViewModelStore使用HashMap保存多个ViewModel统一管理

1. 在页面重建的时候进行恢复，ViewModelStore保存在NonConfigurationInstances中，该变量保存在Activity外部，Activity重建之后可以从中恢复

* `NewInstanceFactory`：直接反射Class创建ViewModel实例
* `AndroidViewModelFactory`：创建ViewModel并传入application对象
* `SavedStateViewModelFactory`：
  * 判断Class继承`AndroidViewModel`则使用`AndroidViewModelFactory`
  * 否则创建``

整体原理类似Flutter GetX的`put<T>([key], Object)`和`find<T>([key])`，不设置Key的话就根据泛型类型查找

## 销毁的时候如何保存数据

重建的时候AMS远程调用ActivityThread方法，ActivityThread将要重建的Activity保存到`mRelaunchingActivities`列表中

ActivityThread中`performDestroyActivity`销毁Activity，判断是配置变更，则调用`retainNonConfigurationInstances()`，将数据保存到`ActivityClientRecord`中，

```java
//ActivityThread.java
//保存了ActivityClientRecord的Map，根据token找到对应的Record
final ArrayMap<IBinder, ActivityClientRecord> mActivities = new ArrayMap<>();

ActivityClientRecord performDestroyActivity(IBinder token, boolean finishing,
        int configChanges, boolean getNonConfigInstance, String reason) {
    ActivityClientRecord r = mActivities.get(token);
    if (localLOGV) Slog.v(TAG, "Performing finish of " + r);
    if (r != null) {
        r.activity.mConfigChangeFlags |= configChanges;
        if (finishing) {
            r.activity.mFinished = true;
        }

        performPauseActivityIfNeeded(r, "destroy");

        if (!r.stopped) {
            callActivityOnStop(r, false /* saveState */, "destroy");
        }
        if (getNonConfigInstance) {
          r.lastNonConfigurationInstances = r.activity.retainNonConfigurationInstances();
        }
        mInstrumentation.callActivityOnDestroy(r.activity);
    }
    //移除mActivities中保存的Record
    synchronized (mResourcesManager) {
        mActivities.remove(token);
    }
    return r;
}
```

`ComponentActivity`子类重写onRetainNonConfigurationInstance，保存ViewModelStore到NonConfigurationInstances中

```java
//ComponentActivity.java
public final Object onRetainNonConfigurationInstance() {
    Object custom = onRetainCustomNonConfigurationInstance();

    ViewModelStore viewModelStore = mViewModelStore;
    if (viewModelStore == null) {
        // No one called getViewModelStore(), so see if there was an existing
        // ViewModelStore from our last NonConfigurationInstance
        NonConfigurationInstances nc =
                (NonConfigurationInstances) getLastNonConfigurationInstance();
        if (nc != null) {
            viewModelStore = nc.viewModelStore;
        }
    }

    if (viewModelStore == null && custom == null) {
        return null;
    }

    NonConfigurationInstances nci = new NonConfigurationInstances();
    nci.custom = custom;
    nci.viewModelStore = viewModelStore;
    return nci;
}
```

TODO：画图

页面恢复的时候`ActivityThread`从`mRelaunchingActivities`中获取Record，存入mActivities中

`ActivityThread`创建Activity，并将对应token的Record中保存的`lastNonConfigurationInstances`传给Activity。

```java
private Activity performLaunchActivity(ActivityClientRecord r, Intent customIntent) {
    activity.attach(appContext, this, getInstrumentation(), r.token,
        r.ident, app, r.intent, r.activityInfo, title, r.parent,
        r.embeddedID, r.lastNonConfigurationInstances, config,
        r.referrer, r.voiceInteractor, window, r.configCallback,
        r.assistToken);
}
```

Activity中`getViewModelStore`返回保存的ViewModel，如果没有则新建

```java
public ViewModelStore getViewModelStore() {
    if (getApplication() == null) {
        throw new IllegalStateException("Your activity is not yet attached to the "
                + "Application instance. You can't request ViewModel before onCreate call.");
    }
    if (mViewModelStore == null) {
        NonConfigurationInstances nc =
                (NonConfigurationInstances) getLastNonConfigurationInstance();
        if (nc != null) {
            // Restore the ViewModelStore from NonConfigurationInstances
            mViewModelStore = nc.viewModelStore;
        }
        if (mViewModelStore == null) {
            mViewModelStore = new ViewModelStore();
        }
    }
    return mViewModelStore;
}
```



ApplicationThread接收Binder调用，通过sendMessage发给ActivityThread执行