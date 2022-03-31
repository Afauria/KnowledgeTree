- **State是如何与Activity/Fragment的生命周期绑定的？**

> 在Activity中添加一个**ReportFragment**(如果你的Activity继承AppCompatActivity，会在父类的onCreate()中添加**ReportFragment**，否则由**LifecycleDispatcher**添加)，在**ReportFragment**生命周期函数中调用**LifecycleRegistry.handleLifecycleEvent**()方法改变**State**。

- **Event事件是如何分发到LifecycleObserver的？**

> **LifecycleRegistry**在收到**handleLifecycleEvent**()后，内部调用**moveToState**()方法，改变State值，每一次State值改变，都会调用**LifecycleObserver.onStateChanged**()方法将Event分发到**LifecycleObserver**

使用ReportFragment实现无侵入

```java
//ComponentActivity.java
protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    // 注入Fragment
    ReportFragment.injectIfNeededIn(this);
}
```

ReportFragment中

1. Android10以下在ReportFragment生命周期中分发事件
2. Android10以上ReportFragment注册ActivityLifecycleCallbacks，监听生命周期变化分发给`LifecycleRegistry.handleLifecycleEvent`

```java
private void dispatch(@NonNull Lifecycle.Event event) {
    if (Build.VERSION.SDK_INT < 29) {
        // Only dispatch events from ReportFragment on API levels prior
        // to API 29. On API 29+, this is handled by the ActivityLifecycleCallbacks
        // added in ReportFragment.injectIfNeededIn
        dispatch(getActivity(), event);
    }
}
```

有的页面不继承自AppCompatActivity，则通过ContentProvider，进程启动的时候自动注册

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="androidx.lifecycle.process" >
    <application>
        <provider
            android:name="androidx.lifecycle.ProcessLifecycleOwnerInitializer"
            android:authorities="${applicationId}.lifecycle-process"
            android:exported="false"
            android:multiprocess="true" />
    </application>
</manifest>
```



```java
public class ProcessLifecycleOwnerInitializer extends ContentProvider {
    @Override
    public boolean onCreate() {
        //自动注册
        LifecycleDispatcher.init(getContext());
        ProcessLifecycleOwner.init(getContext());
        return true;
    }
}
```



```java
class LifecycleDispatcher {
    private static AtomicBoolean sInitialized = new AtomicBoolean(false);
    static void init(Context context) {
        if (sInitialized.getAndSet(true)) {
            return;
        }
        //Application监听Activity情况
        ((Application) context.getApplicationContext()).registerActivityLifecycleCallbacks(new DispatcherActivityCallback());
    }
    static class DispatcherActivityCallback extends EmptyActivityLifecycleCallbacks {
        @Override
        public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
            //Activity创建后自动注册
            ReportFragment.injectIfNeededIn(activity);
        }
        @Override
        public void onActivityStopped(Activity activity) {
        }
        @Override
        public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
        }
    }
}
```



`Lifecycle`：不包含实现

```java
public abstract class Lifecycle {
    @MainThread
    public abstract void addObserver(@NonNull LifecycleObserver observer);
    @MainThread
    public abstract void removeObserver(@NonNull LifecycleObserver observer);
    @MainThread
    @NonNull
    public abstract State getCurrentState();
    @SuppressWarnings("WeakerAccess")
    public enum Event {
      ...
    }
    public enum State {
      ...
    }
}
```

`LifecycleRegistry`：处理生命周期事件，转移状态，通知观察者

```java
public class LifecycleRegistry extends Lifecycle {
    // 保存Observer Map，封装ObserverWithState
    private FastSafeIterableMap<LifecycleObserver, ObserverWithState> mObserverMap =
            new FastSafeIterableMap<>();
    // 保存生命周期状态
    private State mState;
    // 弱引用持有Activity
    private final WeakReference<LifecycleOwner> mLifecycleOwner;
    private int mAddingObserverCounter = 0;
    private boolean mHandlingEvent = false;
    private boolean mNewEventOccurred = false;
    private ArrayList<State> mParentStates = new ArrayList<>();

    public LifecycleRegistry(@NonNull LifecycleOwner provider) {
        mLifecycleOwner = new WeakReference<>(provider);
        mState = INITIALIZED;
    }
  
    @MainThread
    public void setCurrentState(@NonNull State state) {
        moveToState(state);
    }

    public void handleLifecycleEvent(@NonNull Lifecycle.Event event) {
        State next = getStateAfter(event);
        moveToState(next);
    }

    private void moveToState(State next) {
        if (mState == next) {
            return;
        }
        mState = next;
        if (mHandlingEvent || mAddingObserverCounter != 0) {
            mNewEventOccurred = true;
            // we will figure out what to do on upper level.
            return;
        }
        mHandlingEvent = true;
        sync();
        mHandlingEvent = false;
    }

    private boolean isSynced() {
        if (mObserverMap.size() == 0) {
            return true;
        }
        State eldestObserverState = mObserverMap.eldest().getValue().mState;
        State newestObserverState = mObserverMap.newest().getValue().mState;
        return eldestObserverState == newestObserverState && mState == newestObserverState;
    }

    private State calculateTargetState(LifecycleObserver observer) {
        Entry<LifecycleObserver, ObserverWithState> previous = mObserverMap.ceil(observer);

        State siblingState = previous != null ? previous.getValue().mState : null;
        State parentState = !mParentStates.isEmpty() ? mParentStates.get(mParentStates.size() - 1)
                : null;
        return min(min(mState, siblingState), parentState);
    }

    @Override
    public void addObserver(@NonNull LifecycleObserver observer) {
        State initialState = mState == DESTROYED ? DESTROYED : INITIALIZED;
        ObserverWithState statefulObserver = new ObserverWithState(observer, initialState);
        ObserverWithState previous = mObserverMap.putIfAbsent(observer, statefulObserver);

        if (previous != null) {
            return;
        }
        LifecycleOwner lifecycleOwner = mLifecycleOwner.get();
        if (lifecycleOwner == null) {
            // it is null we should be destroyed. Fallback quickly
            return;
        }

        boolean isReentrance = mAddingObserverCounter != 0 || mHandlingEvent;
        State targetState = calculateTargetState(observer);
        mAddingObserverCounter++;
        while ((statefulObserver.mState.compareTo(targetState) < 0
                && mObserverMap.contains(observer))) {
            pushParentState(statefulObserver.mState);
            statefulObserver.dispatchEvent(lifecycleOwner, upEvent(statefulObserver.mState));
            popParentState();
            targetState = calculateTargetState(observer);
        }

        if (!isReentrance) {
            sync();
        }
        mAddingObserverCounter--;
    }

    @Override
    public void removeObserver(@NonNull LifecycleObserver observer) {
        mObserverMap.remove(observer);
    }

    @SuppressWarnings("WeakerAccess")
    public int getObserverCount() {
        return mObserverMap.size();
    }

    @NonNull
    @Override
    public State getCurrentState() {
        return mState;
    }

    private void forwardPass(LifecycleOwner lifecycleOwner) {
        Iterator<Entry<LifecycleObserver, ObserverWithState>> ascendingIterator =
                mObserverMap.iteratorWithAdditions();
        while (ascendingIterator.hasNext() && !mNewEventOccurred) {
            Entry<LifecycleObserver, ObserverWithState> entry = ascendingIterator.next();
            ObserverWithState observer = entry.getValue();
            while ((observer.mState.compareTo(mState) < 0 && !mNewEventOccurred
                    && mObserverMap.contains(entry.getKey()))) {
                pushParentState(observer.mState);
                //downEvent、upEvent计算下一个状态
                observer.dispatchEvent(lifecycleOwner, upEvent(observer.mState));
                popParentState();
            }
        }
    }
  
    private void sync() {
        LifecycleOwner lifecycleOwner = mLifecycleOwner.get();
        if (lifecycleOwner == null) {
            throw new IllegalStateException("LifecycleOwner of this LifecycleRegistry is already"
                    + "garbage collected. It is too late to change lifecycle state.");
        }
        while (!isSynced()) {
            mNewEventOccurred = false;
            if (mState.compareTo(mObserverMap.eldest().getValue().mState) < 0) {
                backwardPass(lifecycleOwner);
            }
            Entry<LifecycleObserver, ObserverWithState> newest = mObserverMap.newest();
            if (!mNewEventOccurred && newest != null
                    && mState.compareTo(newest.getValue().mState) > 0) {
                forwardPass(lifecycleOwner);
            }
        }
        mNewEventOccurred = false;
    }

    static class ObserverWithState {
        State mState;
        LifecycleEventObserver mLifecycleObserver;

        ObserverWithState(LifecycleObserver observer, State initialState) {
            // 封装ObserverWithState
            mLifecycleObserver = Lifecycling.lifecycleEventObserver(observer);
            mState = initialState;
        }

        void dispatchEvent(LifecycleOwner owner, Event event) {
            State newState = getStateAfter(event);
            mState = min(mState, newState);
            mLifecycleObserver.onStateChanged(owner, event);
            mState = newState;
        }
    }
}
```

```java
static LifecycleEventObserver lifecycleEventObserver(Object object) {
    //统一回调onStateChanged
    boolean isLifecycleEventObserver = object instanceof LifecycleEventObserver;
    //回调所有生命周期，onStateChanged中判断了Event。不过这个类是私有的，开发者无法使用
    boolean isFullLifecycleObserver = object instanceof FullLifecycleObserver;
    if (isLifecycleEventObserver && isFullLifecycleObserver) {
        return new FullLifecycleObserverAdapter((FullLifecycleObserver) object,
                (LifecycleEventObserver) object);
    }
    if (isFullLifecycleObserver) {
        // 为null表示不回调onStateChanged
        return new FullLifecycleObserverAdapter((FullLifecycleObserver) object, null);
    }

    if (isLifecycleEventObserver) {
        return (LifecycleEventObserver) object;
    }
    ...
}
```