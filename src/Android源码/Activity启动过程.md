# Launcher

应用一般需要在清单文件中注册主入口，如下

```xml
<activity android:name=".MainActivity">
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent-filter>
</activity>
```

Launcher本身也是一个应用，和普通应用有一些差异：

1. 主入口需要声明`<category android:name="android.intent.category.HOME" />`
2. Launcher启动Intent会保存到AMS的`mHomeIntent`中，开机或者按Home键时会启动`mHomeIntent`
3. Launcher的Activity使用独立的`ActivityStack`，type是home

桌面应用图标：

1. 开机PMS会解析已安装应用的`AndroidManifest`清单文件，缓存应用信息
2. Launcher通过PMS获取应用信息，并将应用主入口显示为桌面图标
3. 点击图标之后，会通过`startActivity`启动应用

# startActivity启动过程

`startActivity`大致流程如下：

1. Launcher通知AMS启动一个Activity
2. AMS保存Activity信息，通知Launcher进入pause状态
3. Launcher进入paused状态后，通知AMS完成
4. AMS判断不存在目标应用进程，通知Zygote fork一个新进程
5. 新进程启动后执行`ActivityThread`的main方法
6. `main`方法中开启主线程loop循环，并创建一个IBinder对象`ApplicationThread`，用于和AMS通信
7. 通知AMS进程已启动，传递`ApplicationThread`对象给AMS
8. AMS通知目标应用bindApplication，并启动MainActivity
9. 目标应用在ActivityThread创建`MainActivity`，执行生命周期方法

# 源码分析

假设A应用启动B应用（这里以Android30为例，低版本会有一些差异）

## A应用

`startActivity`最终会调用`startActivityForResult`

```java
//Activity.java
public void startActivityForResult(@RequiresPermission Intent intent, int requestCode, @Nullable Bundle options) {
    ...
    Instrumentation.ActivityResult ar = mInstrumentation.execStartActivity(
        this, mMainThread.getApplicationThread(), mToken, this, intent, requestCode, options);
    ...
}
```

`Instrumentation`（仪表盘）用于管理Activity，控制Activity生命周期等：

```java
//Instrumentation.java
public ActivityResult execStartActivity(
        Context who, IBinder contextThread, IBinder token, Activity target,
        Intent intent, int requestCode, Bundle options) {
    IApplicationThread whoThread = (IApplicationThread) contextThread;
    ...
    try {
        //通过`ActivityTaskManager`获取AMS代理对象（低版本可能是`ActivityManagerNative`或者`ActivityManager`）
        int result = ActivityTaskManager.getService().startActivity(
          whoThread, //即ApplicationThread，是一个IBinder对象
          who.getBasePackageName(), 
          intent, 
          intent.resolveTypeIfNeeded(who.getContentResolver()),
          token, // Activity attach时传入的IBinder对象，标识Activity，有可能为空，例如其他非Activity的Context启动
          target != null ? target.mEmbeddedID : null,
          requestCode, 0, null, options);
        checkStartActivityResult(result, intent);
    } catch (RemoteException e) {
        throw new RuntimeException("Failure from system", e);
    }
    return null;
}
```

> 这里暂时跳过`ApplicationThread`和`Token`对象作用，在B应用中介绍

`ActivityTaskManager.getService`：获取`IActivityTaskManager`，这是AMS在客户端的代理对象，具体的实现是`ActivityTaskManagerService`

```java
//ActivityTaskManager.java
public static IActivityTaskManager getService() {
    return IActivityTaskManagerSingleton.get();
}
@UnsupportedAppUsage(trackingBug = 129726065)
private static final Singleton<IActivityTaskManager> IActivityTaskManagerSingleton =
        new Singleton<IActivityTaskManager>() {
            @Override
            protected IActivityTaskManager create() {
                final IBinder b = ServiceManager.getService(Context.ACTIVITY_TASK_SERVICE);
                return IActivityTaskManager.Stub.asInterface(b);
            }
        };
```

## AMS

Binder通信中，客户端和服务端共用一个AIDL接口文件。例如`IActivityTaskManager`，客户端（应用）只负责调用接口，服务端（`system_server`进程中的AMS）负责实现。

这里我们直接找到`ActivityTaskManagerService`类：使用Builder模式构造一个`ActivityStarter`任务对象，并执行

```java
//ActivityTaskManagerService.java
@Override
public final int startActivity(IApplicationThread caller, String callingPackage,
        String callingFeatureId, Intent intent, String resolvedType, IBinder resultTo,
        String resultWho, int requestCode, int startFlags, ProfilerInfo profilerInfo,
        Bundle bOptions) {
    return startActivityAsUser(...);
}
...
private int startActivityAsUser(IApplicationThread caller, String callingPackage,
        @Nullable String callingFeatureId, Intent intent, String resolvedType,
        IBinder resultTo, String resultWho, int requestCode, int startFlags,
        ProfilerInfo profilerInfo, Bundle bOptions, int userId, boolean validateIncomingUser) {
    //根据callingUid找到调用方userId
    userId = getActivityStartController().checkTargetUser(userId, validateIncomingUser,
            Binder.getCallingPid(), Binder.getCallingUid(), "startActivityAsUser");

    return getActivityStartController().obtainStarter(intent, "startActivityAsUser")
            .setCaller(caller)
            .setCallingPackage(callingPackage)
            .setCallingFeatureId(callingFeatureId)
            .setResolvedType(resolvedType)
            .setResultTo(resultTo)
            .setResultWho(resultWho)
            .setRequestCode(requestCode)
            .setStartFlags(startFlags)
            .setProfilerInfo(profilerInfo)
            .setActivityOptions(bOptions)
            .setUserId(userId)
            .execute();

}
```

`ActivityStarter.execute()`：

```java
int execute() {
    try {
        //解析Intent，调用PMS找到合适的Activity
        if (mRequest.activityInfo == null) {
            mRequest.resolveActivity(mSupervisor);
        }
        int res;
        synchronized (mService.mGlobalLock) {
            ...
            res = executeRequest(mRequest);
            ...
        }
    } finally {
        onExecutionComplete();
    }
}
```

调用`executeRequest`执行请求：这里面会做大量的Flag、Task、权限之类的校验，并检查是否有可重用的Task或Activity，直接看启动新进程的代码

```java
private int executeRequest(Request request) {
    ...
    final int userId = aInfo != null && aInfo.applicationInfo != null
            ? UserHandle.getUserId(aInfo.applicationInfo.uid) : 0;
    //经常看到的log打印
    if (err == ActivityManager.START_SUCCESS) {
        Slog.i(TAG, "START u" + userId + " {" + intent.toShortString(true, true, true, false)
                + "} from uid " + callingUid);
    }
    ...
    final ActivityRecord r = new ActivityRecord(mService, callerApp, callingPid, callingUid,
            callingPackage, callingFeatureId, intent, resolvedType, aInfo,
            mService.getGlobalConfiguration(), resultRecord, resultWho, requestCode,
            request.componentSpecified, voiceSession != null, mSupervisor, checkedOptions,
            sourceRecord);
    mLastStartActivityRecord = r;
    ...
    mLastStartActivityResult = startActivityUnchecked(r, sourceRecord, voiceSession,
            request.voiceInteractor, startFlags, true /* doResume */, checkedOptions, inTask,
            restrictedBgActivity, intentGrants);
    return mLastStartActivityResult;
}

private int startActivityUnchecked(final ActivityRecord r, ActivityRecord sourceRecord,
            IVoiceInteractionSession voiceSession, IVoiceInteractor voiceInteractor,
            int startFlags, boolean doResume, ActivityOptions options, Task inTask,
            boolean restrictedBgActivity, NeededUriGrants intentGrants) {
    result = startActivityInner(r, sourceRecord, voiceSession, voiceInteractor, startFlags, doResume, options, inTask, restrictedBgActivity, intentGrants);
    return result;
}

int startActivityInner(final ActivityRecord r, ActivityRecord sourceRecord,
        IVoiceInteractionSession voiceSession, IVoiceInteractor voiceInteractor,
        int startFlags, boolean doResume, ActivityOptions options, Task inTask,
        boolean restrictedBgActivity, NeededUriGrants intentGrants) {
    ...
    if (mDoResume) {
        ...
        mRootWindowContainer.resumeFocusedStacksTopActivities(mTargetStack, mStartActivity, mOptions);
    }
    ...
    return START_SUCCESS;
}
```

`resumeFocusedStacksTopActivities`会找到焦点Stack，并调用到`ActivityStack.resumeTopActivityUncheckedLocked`方法：

1. `startPausingLocked`：通过`ActivityTaskManagerService`跨进程调用`ApplicationThread`的方法，通知A应用pause
2. `startProcessAsync`：通过`ActivityTaskManagerService`异步启动一个新进程

```java
boolean resumeTopActivityUncheckedLocked(ActivityRecord prev, ActivityOptions options) {
    if (mInResumeTopActivity) {
        // Don't even start recursing.
        return false;
    }

    boolean result = false;
    try {
        // Protect against recursion.
        mInResumeTopActivity = true;
        result = resumeTopActivityInnerLocked(prev, options);
        final ActivityRecord next = topRunningActivity(true /* focusableOnly */);
        if (next == null || !next.canTurnScreenOn()) {
            checkReadyForSleep();
        }
    } finally {
        mInResumeTopActivity = false;
    }

    return result;
}
private boolean resumeTopActivityInnerLocked(ActivityRecord prev, ActivityOptions options) {
    ActivityRecord next = topRunningActivity(true /* focusableOnly */);
    ...
    boolean pausing = taskDisplayArea.pauseBackStacks(userLeaving, next);
    if (mResumedActivity != null) {
        if (DEBUG_STATES) Slog.d(TAG_STATES, "resumeTopActivityLocked: Pausing " + mResumedActivity);
        //通知A应用的Activity进入Pause状态
        pausing |= startPausingLocked(userLeaving, false /* uiSleeping */, next);
    }
    if (pausing) {
        //如果进程已启动
        if (next.attachedToProcess()) {
            next.app.updateProcessInfo(false /* updateServiceConnectionActivities */,
                    true /* activityChange */, false /* updateOomAdj */,
                    false /* addPendingTopUid */);
        } else if (!next.isProcessRunning()) {
            //如果进程未启动，则创建新进程
            final boolean isTop = this == taskDisplayArea.getFocusedStack();
            mAtmService.startProcessAsync(next, false /* knownToBeDead */, isTop,
                    isTop ? "pre-top-activity" : "pre-activity");
        }
        return true;
    }
    ...
    return true;
}
```

`ActivityTaskManagerService`通过sendMessage执行任务，需要注意的是`PooledLambda.obtainMessage`第一个参数是Callback，因此消息不是在`handleMessage`中处理，直接看`ActivityManagerInternal`的`startProcess`方法即可

```java
//ActivityTaskManagerService.java
void startProcessAsync(ActivityRecord activity, boolean knownToBeDead, boolean isTop,
        String hostingType) {
    try {
        if (Trace.isTagEnabled(TRACE_TAG_WINDOW_MANAGER)) {
            Trace.traceBegin(TRACE_TAG_WINDOW_MANAGER, "dispatchingStartProcess:"
                    + activity.processName);
        }
        // Post message to start process to avoid possible deadlock of calling into AMS with the
        // ATMS lock held.
        final Message m = PooledLambda.obtainMessage(ActivityManagerInternal::startProcess,
                mAmInternal, activity.processName, activity.info.applicationInfo, knownToBeDead,
                isTop, hostingType, activity.intent.getComponent());
        mH.sendMessage(m);
    } finally {
        Trace.traceEnd(TRACE_TAG_WINDOW_MANAGER);
    }
}
```

`startProcess`在`LocalService`中实现，继承`ActivityManagerInternal`，最终会调用`ActivityManagerService`的`startProcessLocked`方法

```java
//ActivityManagerService.java
final ProcessRecord startProcessLocked(String processName,
        ApplicationInfo info, boolean knownToBeDead, int intentFlags,
        HostingRecord hostingRecord, int zygotePolicyFlags, boolean allowWhileBooting,
        boolean isolated, boolean keepIfLarge) {
    return mProcessList.startProcessLocked(processName, info, knownToBeDead, intentFlags,
            hostingRecord, zygotePolicyFlags, allowWhileBooting, isolated, 0 /* isolatedUid */,
            keepIfLarge, null /* ABI override */, null /* entryPoint */,
            null /* entryPointArgs */, null /* crashHandler */);
}
```

`ProcessList`有多个`startProcessLocked`重载方法，最终调用`startProcess`方法：注意这里的`entryPoint`参数为`ActivityThread`完整类名。

```java
//ProcessList.java
boolean startProcessLocked(HostingRecord hostingRecord, String entryPoint, ProcessRecord app,
        int uid, int[] gids, int runtimeFlags, int zygotePolicyFlags, int mountExternal,
        String seInfo, String requiredAbi, String instructionSet, String invokeWith,
        long startTime) {
    ...
            final String entryPoint = "android.app.ActivityThread";
    ...
            final Process.ProcessStartResult startResult = startProcess(hostingRecord,
                    entryPoint, app,
                    uid, gids, runtimeFlags, zygotePolicyFlags, mountExternal, seInfo,
                    requiredAbi, instructionSet, invokeWith, startTime);
            handleProcessStartedLocked(app, startResult.pid, startResult.usingWrapper,
                    startSeq, false);
    ...
}
```

`startProcess`中分为三个Zygote进程，`AppZygote`和`WebViewZygote`都是Zygote的子进程，这里不用在意，最终都会调用`ZygoteProcess.start`方法。内部是通过Socket IO通信，将参数传给Zygote进程。

```java
//ProcessList.java
private Process.ProcessStartResult startProcess(HostingRecord hostingRecord, String entryPoint,
        ProcessRecord app, int uid, int[] gids, int runtimeFlags, int zygotePolicyFlags,
        int mountExternal, String seInfo, String requiredAbi, String instructionSet,
        String invokeWith, long startTime) {
    try {
        Trace.traceBegin(Trace.TRACE_TAG_ACTIVITY_MANAGER, "Start proc: " +
                app.processName);
        ...
        final Process.ProcessStartResult startResult;
        if (hostingRecord.usesWebviewZygote()) {
            //从WebViewZygote进程启动
            startResult = startWebView(entryPoint, ...);
        } else if (hostingRecord.usesAppZygote()) {
            //从AppZygote进程启动
            final AppZygote appZygote = createAppZygoteForProcessIfNeeded(app);
            // We can't isolate app data and storage data as parent zygote already did that.
            startResult = appZygote.getProcess().start(entryPoint, ...);
        } else {
            //从Zygote进程启动
            startResult = Process.start(entryPoint, ...);
        }
        checkSlow(startTime, "startProcess: returned from zygote!");
        return startResult;
    } finally {
        Trace.traceEnd(Trace.TRACE_TAG_ACTIVITY_MANAGER);
    }
}
```

## B应用

### ActivityThread.main

Zygote进程fork出新的应用进程后，执行`ActivityThread`入口类的main方法。

```java
//ActivityThread.java
public static void main(String[] args) {
    ...
    // 创建主线程Looper
    Looper.prepareMainLooper();
    ...
    // 创建ActivityThread实例
    ActivityThread thread = new ActivityThread();
    // 调用attach方法
    thread.attach(false, startSeq);
    // 静态变量引用Handler，这里应该是为了防止Handler被回收？
    if (sMainThreadHandler == null) {
        sMainThreadHandler = thread.getHandler();
    }
    // 开始循环
    Looper.loop();
    // loop循环结束，应用自然就退出了
    throw new RuntimeException("Main thread loop unexpectedly exited");
}
```

再看`attach`方法：将`ApplicationThread`传给AMS，这样AMS可以通过Binder调用控制应用

```java
final ApplicationThread mAppThread = new ApplicationThread();
final H mH = new H();
private void attach(boolean system, long startSeq) {
    sCurrentActivityThread = this;
    mSystemThread = system;
    if (!system) {
        android.ddm.DdmHandleAppName.setAppName("<pre-initialized>",
                                                UserHandle.myUserId());
        RuntimeInit.setApplicationObject(mAppThread.asBinder());
        final IActivityManager mgr = ActivityManager.getService();
        try {
            // 通知AMS绑定ApplicationThread
            mgr.attachApplication(mAppThread, startSeq);
        } catch (RemoteException ex) {
            throw ex.rethrowFromSystemServer();
        }
    }
    ...
    // ConfigChange之后回调通知ActivityThread
    ViewRootImpl.addConfigCallback(configChangedCallback);
}
```

B应用到这里就开始坐等消息处理了，接下来就是AMS控制应用Activity启动、生命周期调用等

### AMS绑定ApplicationThread

再回到AMS中：（又是一坨代码）

1. 创建死亡代理，应用进程被杀之后通知AMS
2. 通过`ApplicationThread`调用`bindApplication`方法
3. 查看是否有需要启动的组件：`mAtmInternal.attachApplication`启动Activity

```java
//ActivityManagerService.java
@Override
public final void attachApplication(IApplicationThread thread, long startSeq) {
    if (thread == null) {
        throw new SecurityException("Invalid application interface");
    }
    synchronized (this) {
        int callingPid = Binder.getCallingPid();
        final int callingUid = Binder.getCallingUid();
        final long origId = Binder.clearCallingIdentity();
        // 调用加锁方法attachApplicationLocked
        attachApplicationLocked(thread, callingPid, callingUid, startSeq);
        Binder.restoreCallingIdentity(origId);
    }
}
@GuardedBy("this")
private boolean attachApplicationLocked(@NonNull IApplicationThread thread,
        int pid, int callingUid, long startSeq) {

    ProcessRecord app;
    long startTime = SystemClock.uptimeMillis();
    long bindApplicationTimeMillis;
    
    // 找到ProcessList中启动的进程记录，如果找不到就杀掉进程
    if (app == null && startSeq > 0) {
        final ProcessRecord pending = mProcessList.mPendingStarts.get(startSeq);
        if (pending != null && pending.startUid == callingUid 
                && pending.startSeq == startSeq
                && mProcessList.handleProcessStartedLocked(pending, pid, pending.isUsingWrapper(), startSeq, true)) {
            app = pending;
        }
    }

    final String processName = app.processName;
    try {
        // 创建死亡代理，当进程被杀后，通知AMS。Binder连接断开后会触发binderDied回调
        AppDeathRecipient adr = new AppDeathRecipient(app, pid, thread);
        thread.asBinder().linkToDeath(adr, 0);
        app.deathRecipient = adr;
    } catch (RemoteException e) {
        app.resetPackageList(mProcessStats);
        mProcessList.startProcessLocked(app, new HostingRecord("link fail", processName), 
                                        ZYGOTE_POLICY_FLAG_EMPTY);
        return false;
    }
    // 移除超时监听
    mHandler.removeMessages(PROC_START_TIMEOUT_MSG, app);

    try {
      ...
            // 调用ApplicationThread的bindApplication方法
            thread.bindApplication(processName, appInfo, providerList, null, profilerInfo,
                    null, null, null, testMode,
                    mBinderTransactionTrackingEnabled, enableTrackAllocation,
                    isRestrictedBackupMode || !normalMode, app.isPersistent(),
                    new Configuration(app.getWindowProcessController().getConfiguration()),
                    app.compat, getCommonServicesLocked(app.isolated),
                    mCoreSettingsObserver.getCoreSettingsLocked(),
                    buildSerial, autofillOptions, contentCaptureOptions,
                    app.mDisabledCompatChanges);
       ...
    } catch (Exception e) {
        // 杀掉进程
        return false;
    }
  
    boolean badApp = false; // 为true表示启动失败，杀掉进程
    boolean didSomething = false;
    // 检查是否有需要启动的Activity、Service、广播、Backup等
    // See if the top visible activity is waiting to run in this process...
    if (normalMode) {
        try {
            didSomething = mAtmInternal.attachApplication(app.getWindowProcessController());
        } catch (Exception e) {
            Slog.wtf(TAG, "Exception thrown launching activities in " + app, e);
            badApp = true;
        }
    }
    // Find any services that should be running in this process...
    // Check if a next-broadcast receiver is in this process...
    ...
    return true;
}
```

### bindApplication

先看B应用的`bindApplication`：AMS发给`ApplicationThread`的方法，最终都会调用`sendMessage`交给`ActivityThread`的`Handler`处理（后面不再赘述）

```java
//ActivityThread.java
private class ApplicationThread extends IApplicationThread.Stub {
    @Override
    public final void bindApplication(...) {
        ...
        //sendMessage发给Handler
        sendMessage(H.BIND_APPLICATION, data);
    }
}
class H extends Handler {
    ...
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case BIND_APPLICATION:
                Trace.traceBegin(Trace.TRACE_TAG_ACTIVITY_MANAGER, "bindApplication");
                AppBindData data = (AppBindData)msg.obj;
                //开始绑定Application
                handleBindApplication(data);
                Trace.traceEnd(Trace.TRACE_TAG_ACTIVITY_MANAGER);
                break;
            case EXIT_APPLICATION:
                if (mInitialApplication != null) {
                    mInitialApplication.onTerminate();
                }
                Looper.myLooper().quit();
                break;
        }
    }
}
```

`handleBindApplication`：

1. 进程基本参数设置，例如进程名、时区、资源、配置等。Trace和Profiler性能监控设置
2. 创建`LoadedApk`、`AppContext`、`Instrumentation`等对象
3. 通过`Instrumentation`创建`Application`对象，并调用Application的attach方法
3. 调用`installContentProvider`，初始化ContentProvider：反射创建Provider，调用`attachInfo`方法，最后调用`AMS.publishContentProviders`，通知AMS取消ANR定时
3. 通过`Instrumentation`调用Application的`onCreate`生命周期方法

```java
//ActivityThread.java
@UnsupportedAppUsage
private void handleBindApplication(AppBindData data) {
    ...
    // 根据AMS传过来的ApplicationInfo创建LoadedApk对象
    data.info = getPackageInfoNoCheck(data.appInfo, data.compatInfo);
    ...
    //创建AppContext
    final ContextImpl appContext = ContextImpl.createAppContext(this, data.info);
  
    // Continue loading instrumentation.
    if (ii != null) {
        ...
    } else {
        // 创建Instrumentation，监控所有Activity生命周期和行为
        mInstrumentation = new Instrumentation();
        mInstrumentation.basicInit(this);
    }
    ...
    Application app;
    try {
        // 通过Instrumentation创建Application对象
        app = data.info.makeApplication(data.restrictedBackupMode, null);
        mInitialApplication = app;
        try {
            if (!data.restrictedBackupMode) {
                if (!ArrayUtils.isEmpty(data.providers)) {
                    //启动ContentProvider
                    installContentProviders(app, data.providers);
                }
            }
            mInstrumentation.onCreate(data.instrumentationArgs);
            ...
            // 调用Application的onCreate方法
            mInstrumentation.callApplicationOnCreate(app);
        } catch (Exception e) {
            ...
        }
    }
    ...
}
```

### AMS通知启动Activity

再看AMS的`mAtmInternal.attachApplication`方法

经过ATMS和`RootWIndowContainer`几次调用后会到`ActivityStackSupervisor.realStartActivityLocked`方法：

1. 创建`LaunchActivityItem`和`ResumeActivityItem`
2. 交给`ApplicationThread.scheduleTransaction`执行，这里是将两个Item合成一个`ClientTransaction`发送给B应用执行

```java
//ActivityStackSupervisor.java
boolean realStartActivityLocked(ActivityRecord r, WindowProcessController proc,
        boolean andResume, boolean checkConfig) throws RemoteException {
    final Task task = r.getTask();
    final ActivityStack stack = task.getStack();

    try {
      ...
            // Create activity launch transaction.
            final ClientTransaction clientTransaction = ClientTransaction.obtain(
                    proc.getThread(), r.appToken);
            clientTransaction.addCallback(LaunchActivityItem.obtain(new Intent(r.intent), ...));

            final ActivityLifecycleItem lifecycleItem;
            if (andResume) {
                lifecycleItem = ResumeActivityItem.obtain(dc.isNextTransitionForward());
            } else {
                lifecycleItem = PauseActivityItem.obtain();
            }
            clientTransaction.setLifecycleStateRequest(lifecycleItem);
            // Schedule transaction.
            mService.getLifecycleManager().scheduleTransaction(clientTransaction);
    }
    ...
    return true;
}
```

高版本将Activity生命周期调用都封装到了`ClientTransactionItem`中，通过`ClientTransactionHandler`处理，最终还是交给`ActivityThread`处理

1. 通过接口约束`ActivityThread`实现`ClientTransactionHandler`方法。
2. 减少了Handler的Message类型

```java
//ActivityThread.java
private class ApplicationThread extends IApplicationThread.Stub {
    @Override
    public void scheduleTransaction(ClientTransaction transaction) throws RemoteException {
        ActivityThread.this.scheduleTransaction(transaction);
    }
}
class H extends Handler {
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case EXECUTE_TRANSACTION:
                final ClientTransaction transaction = (ClientTransaction) msg.obj;
                mTransactionExecutor.execute(transaction);
                if (isSystem()) {
                    transaction.recycle();
                }
        }
    }
}
//ClientTransactionHandler.java
public abstract class ClientTransactionHandler {
    void scheduleTransaction(ClientTransaction transaction) {
        transaction.preExecute(this);
        sendMessage(ActivityThread.H.EXECUTE_TRANSACTION, transaction);
    }
}
```

1. `LaunchActivityItem`会调用`ActivityThread.handleLaunchActivity`
2. `ResumeActivityItem`会调用`ActivityThread.handleResumeActivity`

```java
public class LaunchActivityItem extends ClientTransactionItem {
    @Override
    public void execute(ClientTransactionHandler client, IBinder token,
            PendingTransactionActions pendingActions) {
        // 创建ActivityClientRecord，传入token
        ActivityClientRecord r = new ActivityClientRecord(...);
        client.handleLaunchActivity(r, pendingActions, null /* customIntent */);
    }
}
public class ResumeActivityItem extends ActivityLifecycleItem {
    @Override
    public void execute(ClientTransactionHandler client, IBinder token,
            PendingTransactionActions pendingActions) {
        client.handleResumeActivity(token, true /* finalStateRequest */, mIsForward, "RESUME_ACTIVITY");
    }
}
```

### 执行Activity生命周期

先看`handleLaunchActivity`方法

```java
@Override
public Activity handleLaunchActivity(ActivityClientRecord r,
        PendingTransactionActions pendingActions, Intent customIntent) {
    // Make sure we are running with the most recent config.
    handleConfigurationChanged(null, null);
    final Activity a = performLaunchActivity(r, customIntent);
    if (a != null) {
        r.createdConfig = new Configuration(mConfiguration);
        reportSizeConfigurations(r);
        // 如果不是finish退出，需要恢复状态
        if (!r.activity.mFinished && pendingActions != null) {
            pendingActions.setOldState(r.state);
            pendingActions.setRestoreInstanceState(true);
            pendingActions.setCallOnPostCreate(true);
        }
    } else {
        // 创建Activity失败，通知AMS finishActivity
    }
    return a;
}
```

`performLaunchActivity`：

1. 通过`Instrumentation`实例化Activity对象
2. 调用`Activity.attach`方法
3. 通过`Instrumentation`执行`onCreate`方法

```java
private Activity performLaunchActivity(ActivityClientRecord r, Intent customIntent) {
    // 解析Intent，获取Activity信息
    ActivityInfo aInfo = r.activityInfo;
    if (r.packageInfo == null) {
        r.packageInfo = getPackageInfo(aInfo.applicationInfo, r.compatInfo, 
                                       Context.CONTEXT_INCLUDE_CODE);
    }
    ComponentName component = r.intent.getComponent();
    if (component == null) {
        component = r.intent.resolveActivity(mInitialApplication.getPackageManager());
        r.intent.setComponent(component);
    }
    if (r.activityInfo.targetActivity != null) {
        component = new ComponentName(r.activityInfo.packageName, r.activityInfo.targetActivity);
    }
    ContextImpl appContext = createBaseContextForActivity(r);
    Activity activity = null;
    try {
        // 通过Instrumentation实例化Activity对象
        java.lang.ClassLoader cl = appContext.getClassLoader();
        activity = mInstrumentation.newActivity(cl, component.getClassName(), r.intent);
        StrictMode.incrementExpectedActivityCount(activity.getClass());
        r.intent.setExtrasClassLoader(cl);
        r.intent.prepareToEnterProcess();
        if (r.state != null) {
            r.state.setClassLoader(cl);
        }
    } catch (Exception e) {
        // 实例化Activity失败
        if (!mInstrumentation.onException(activity, e)) {
            throw new RuntimeException(
                "Unable to instantiate activity " + component
                + ": " + e.toString(), e);
        }
    }
    try {
        // 获取Application对象，上面已经创建过了，不需要重复创建
        Application app = r.packageInfo.makeApplication(false, mInstrumentation);
        if (activity != null) {
            CharSequence title = r.activityInfo.loadLabel(appContext.getPackageManager());
            Configuration config = new Configuration(mCompatConfiguration);
            if (r.overrideConfig != null) {
                config.updateFrom(r.overrideConfig);
            }
            Window window = null;
            if (r.mPendingRemoveWindow != null && r.mPreserveWindow) {
                window = r.mPendingRemoveWindow;
                r.mPendingRemoveWindow = null;
                r.mPendingRemoveWindowManager = null;
            }
            // Activity resources must be initialized with the same loaders as the application context.
            appContext.getResources().addLoaders(
                    app.getResources().getLoaders().toArray(new ResourcesLoader[0]));

            appContext.setOuterContext(activity);
            // 调用Activity attach方法
            activity.attach(appContext, this, getInstrumentation(), r.token,
                    r.ident, app, r.intent, r.activityInfo, title, r.parent,
                    r.embeddedID, r.lastNonConfigurationInstances, config,
                    r.referrer, r.voiceInteractor, window, r.configCallback,
                    r.assistToken);

            if (customIntent != null) {
                activity.mIntent = customIntent;
            }
            r.lastNonConfigurationInstances = null;
            checkAndBlockForNetworkAccess();
            activity.mStartedActivity = false;
            int theme = r.activityInfo.getThemeResource();
            if (theme != 0) {
                activity.setTheme(theme);
            }

            activity.mCalled = false;
            // 调用Activity onCreate生命周期
            if (r.isPersistable()) {
                mInstrumentation.callActivityOnCreate(activity, r.state, r.persistentState);
            } else {
                mInstrumentation.callActivityOnCreate(activity, r.state);
            }
            if (!activity.mCalled) {
                throw new SuperNotCalledException(
                    "Activity " + r.intent.getComponent().toShortString() +
                    " did not call through to super.onCreate()");
            }
            r.activity = activity;
            mLastReportedWindowingMode.put(activity.getActivityToken(),
                    config.windowConfiguration.getWindowingMode());
        }
        r.setState(ON_CREATE);
        // 保存ActivityClientRecord
        synchronized (mResourcesManager) {
            mActivities.put(r.token, r);
        }
    } 
    ...

    return activity;
}
```

`Activity.attach`方法：

1. 创建`PhonwWindow`
2. 设置`Window.Callback`：在`View attachToWindow`、事件分发的时候`DecorView`回调通知Activity
3. 保存`WindowManager`对象

```java
//Activity.java
// ActivityThread实例化Activity之后调用
final void attach(...) {
    attachBaseContext(context);
    // 创建PhoneWindow
    mWindow = new PhoneWindow(this, window, activityConfigCallback);
    // 设置Callback
    mWindow.setCallback(this);
    // 通过getSystemService获取WindowManager对象，Window关联Activity的Token
    mWindow.setWindowManager((WindowManager)context.getSystemService(Context.WINDOW_SERVICE),
        mToken, mComponent.flattenToString(), (info.flags & ActivityInfo.FLAG_HARDWARE_ACCELERATED) != 0);
    mWindowManager = mWindow.getWindowManager();
}
```

再看`handleResumeActivity`：

1. 调用`performResumeActivity`执行Activity的onStart和onResume生命周期
2. 调用`WindowManager.addView`将DecorView加入WM中，此时窗口可见

```java
@Override
public void handleResumeActivity(IBinder token, boolean finalStateRequest, boolean isForward,
        String reason) {
    // 取消后台GC任务
    unscheduleGcIdler();
    mSomeActivitiesChanged = true;
    //执行onStart和onResume生命周期
    final ActivityClientRecord r = performResumeActivity(token, finalStateRequest, reason);
    if (r == null) {
        // We didn't actually resume the activity, so skipping any follow-up actions.
        return;
    }
    final Activity a = r.activity;
    boolean willBeVisible = !a.mStartedActivity;
    if (!willBeVisible) {
        try {
            willBeVisible = ActivityTaskManager.getService().willActivityBeVisible(a.getActivityToken());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }
    if (r.window == null && !a.mFinished && willBeVisible) {
        // 从Activity中获取Window和DecorView
        r.window = r.activity.getWindow();
        View decor = r.window.getDecorView();
        decor.setVisibility(View.INVISIBLE);
        // 从Activity中获取WindowManager
        ViewManager wm = a.getWindowManager();
        WindowManager.LayoutParams l = r.window.getAttributes();
        a.mDecor = decor;
        l.type = WindowManager.LayoutParams.TYPE_BASE_APPLICATION;
        l.softInputMode |= forwardBit;
        if (r.mPreserveWindow) {
            a.mWindowAdded = true;
            r.mPreserveWindow = false;
            ViewRootImpl impl = decor.getViewRootImpl();
            if (impl != null) {
                impl.notifyChildRebuilt();
            }
        }
        if (a.mVisibleFromClient) {
            if (!a.mWindowAdded) {
                a.mWindowAdded = true;
                // 将DecorView加入WindowManager
                wm.addView(decor, l);
            } else {
                a.onWindowAttributesChanged(l);
            }
        }
    } else if (!willBeVisible) {
        if (localLOGV) Slog.v(TAG, "Launch " + r + " mStartedActivity set");
        r.hideForNow = true;
    }

    // Window可见
    if (!r.activity.mFinished && willBeVisible && r.activity.mDecor != null && !r.hideForNow) {
        if (r.newConfig != null) {
            performConfigurationChangedForActivity(r, r.newConfig);
            if (DEBUG_CONFIGURATION) {
                Slog.v(TAG, "Resuming activity " + r.activityInfo.name + " with newConfig "
                        + r.activity.mCurrentConfig);
            }
            r.newConfig = null;
        }
        if (localLOGV) Slog.v(TAG, "Resuming " + r + " with isForward=" + isForward);
        ViewRootImpl impl = r.window.getDecorView().getViewRootImpl();
        WindowManager.LayoutParams l = impl != null ? impl.mWindowAttributes : r.window.getAttributes();
        if ((l.softInputMode
                & WindowManager.LayoutParams.SOFT_INPUT_IS_FORWARD_NAVIGATION) != forwardBit) {
            l.softInputMode = (l.softInputMode
                    & (~WindowManager.LayoutParams.SOFT_INPUT_IS_FORWARD_NAVIGATION))
                    | forwardBit;
            if (r.activity.mVisibleFromClient) {
                ViewManager wm = a.getWindowManager();
                View decor = r.window.getDecorView();
                wm.updateViewLayout(decor, l);
            }
        }

        r.activity.mVisibleFromServer = true;
        mNumVisibleActivities++;
        if (r.activity.mVisibleFromClient) {
            r.activity.makeVisible();
        }
    }
    r.nextIdle = mNewActivities;
    mNewActivities = r;
    // 空闲的时候执行下Idler任务
    Looper.myQueue().addIdleHandler(new Idler());
}
```

## 总结

TODO：画图

A应用：`startActivity-->Instrumentation.execStartActivity-->ActivityTaskManager.getService().startActivity`

AMS：`ActivityTaskManagerService.startActivity-->ActivityStarter.execute-->RootWindowContainer.resumeFocusedStacksTopActivities-->ActivityStack.resumeTopActivityUncheckedLocked-->ActivityTaskManagerService.startProcessAsync-->ActivityManagerService.startProcessLocked-->ProcessList.startProcess-->ZygoteProcess.start`

B应用：`ActivityThread.main-->AMS.attachApplication（传递ApplicationThread，用于后续通信）-->ApplicationThread.bindApplication（实例化Application对象，初始化ContentProvider，调用Application的onCreate方法）-->ApplicationThread.scheduleTransaction-->ActivityThread.handleLaunchActivity（Activity实例化、执行attach方法，调用onCreate生命周期）-->ActivityThread.handleResumeActivity（调用onStart和onResume生命周期，将DecorView添加到WM中）`




activity启动流程

* Activity代理ContextImpl。调用startActivity
* ContextImpl从ActivityThread中获取Instrumentation
* 调用Instrumentation的execStartActivity方法
* Instrumentation调用ActivityManager.getService()方法获取AMS（Binder IPC）。调用AMS的startActivity方法。
* AMS检查Manifest是否注册（其他进程）
* AMS通过Binder IPC（回到应用进程）调用ActivityThread启动Activity
* ActivityThread.scheduleLaunchActivity()通过sendMessage交给Handler处理。
* Handler调用handleLaunchActivity->performLauncherActivity()。
* 通过mInstrumentation.newActivity创建activity
* 并调用Instrumentation.callActivityOnCreate(activity)->activity.performCreate()->onCreate()

# 常见概念

经常看到一些类似名称的现象，很容易搞混，这里做下区分

* `ActivityThread`：应用主线程（UI线程）
* `ApplicationThread`：用于和AMS通信
* `ActivityStackSupervisor`：维护所有`ActivityStack`

# 结语

参考资料：

* [Android Launcher 启动 Activity 的工作过程](https://blog.csdn.net/qian520ao/article/details/78156214)

http://gityuan.com/2016/03/06/start-service/
