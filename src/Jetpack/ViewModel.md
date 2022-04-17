TODO

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

ViewModelStoreOwner

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

* `NewInstanceFactory`：直接反射Class创建ViewModel实例
* `AndroidViewModelFactory`：创建ViewModel并传入application对象，判断Class继承`AndroidViewModel`则使用`AndroidViewModelFactory`
* `SavedStateViewModelFactory`：

整体原理类似Flutter GetX的`put<T>([key], Object)`和`find<T>([key])`，不设置Key的话就根据泛型类型查找

## 销毁的时候如何保存数据

重建的时候AMS远程调用ActivityThread方法，ActivityThread将要重建的Activity保存到`mRelaunchingActivities`列表中

ActivityThread中`performDestroyActivity`销毁Activity，判断是配置变更，则调用`retainNonConfigurationInstances()`，将数据保存到`ActivityClientRecord`中。

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
        NonConfigurationInstances nc = (NonConfigurationInstances) getLastNonConfigurationInstance();
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
        NonConfigurationInstances nc = (NonConfigurationInstances) getLastNonConfigurationInstance();
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

# SavedStateViewModelFactory

SaveStateHandle