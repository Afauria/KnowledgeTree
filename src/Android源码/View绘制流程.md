requestLayout检查线程，调用scheduleTraversals

```java
@Override
public void requestLayout() {
    if (!mHandlingLayoutInLayoutRequest) {
        checkThread();
        mLayoutRequested = true;
        scheduleTraversals();
    }
}
void scheduleTraversals() {
    if (!mTraversalScheduled) {
        mTraversalScheduled = true;
        // 设置同步屏障
        mTraversalBarrier = mHandler.getLooper().getQueue().postSyncBarrier();
        // 注册监听
        mChoreographer.postCallback(Choreographer.CALLBACK_TRAVERSAL, mTraversalRunnable, null);
        if (!mUnbufferedInputDispatch) {
            scheduleConsumeBatchedInput();
        }
        notifyRendererOfFramePending();
        pokeDrawLockIfNeeded();
    }
}
// Choreographer回调doTraversal
final class TraversalRunnable implements Runnable {
    @Override
    public void run() {
        doTraversal();
    }
}
final TraversalRunnable mTraversalRunnable = new TraversalRunnable();

void doTraversal() {
    if (mTraversalScheduled) {
        mTraversalScheduled = false;
        // 移除同步屏障
        mHandler.getLooper().getQueue().removeSyncBarrier(mTraversalBarrier);
      
        //是否打开方法跟踪
        if (mProfile) {
            Debug.startMethodTracing("ViewAncestor");
        }
        // 开始遍历子View
        performTraversals();

        if (mProfile) {
            Debug.stopMethodTracing();
            mProfile = false;
        }
    }
}
private void performTraversals() {  
  ...
  performMeasure(childWidthMeasureSpec, childHeightMeasureSpec);
  ...
  performLayout(lp, desiredWindowWidth, desiredWindowHeight);
  ...
  performDraw();
  }
  ... 
}
//measure、layout、draw方法中会调用onMeasure、onLayout、onDraw方法，ViewGroup会重新这三个方法，遍历子View递归的获取子View布局信息
private void performMeasure(int childWidthMeasureSpec, int childHeightMeasureSpec) {
    ...
    mView.measure(childWidthMeasureSpec, childHeightMeasureSpec);
    ...
}
private void performLayout(WindowManager.LayoutParams lp, int desiredWindowWidth,
        int desiredWindowHeight) {
    ...
    final View host = mView;
    host.layout(0, 0, host.getMeasuredWidth(), host.getMeasuredHeight());
    ...
}
private void performDraw() {
    ...
    mView.draw(canvas);
    ...
}
```

手动调用`invalidate，postInvalidate，requestInvalidate`也会触发`performTraversals`重新绘制View

Draw方法比较特殊，我们可以通过重写控制绘制顺序

1. `drawBackground`：绘制背景
2. `onDraw`：绘制自身
3. `dispatchDraw`：绘制子View
4. `onDrawForeground`：绘制前景

```java
public void draw(Canvas canvas) {
    final int privateFlags = mPrivateFlags;
    mPrivateFlags = (privateFlags & ~PFLAG_DIRTY_MASK) | PFLAG_DRAWN;

    /*
     * Draw traversal performs several drawing steps which must be executed
     * in the appropriate order:
     *
     *      1. Draw the background
     *      2. If necessary, save the canvas' layers to prepare for fading
     *      3. Draw view's content
     *      4. Draw children
     *      5. If necessary, draw the fading edges and restore layers
     *      6. Draw decorations (scrollbars for instance)
     */
    // Step 1, draw the background, if needed
    int saveCount;
    drawBackground(canvas);
    // skip step 2 & 5 if possible (common case)
    final int viewFlags = mViewFlags;
    boolean horizontalEdges = (viewFlags & FADING_EDGE_HORIZONTAL) != 0;
    boolean verticalEdges = (viewFlags & FADING_EDGE_VERTICAL) != 0;
    if (!verticalEdges && !horizontalEdges) {
        // Step 3, draw the content
        onDraw(canvas);
        // Step 4, draw the children
        dispatchDraw(canvas);
        drawAutofilledHighlight(canvas);
        // Overlay is part of the content and draws beneath Foreground
        if (mOverlay != null && !mOverlay.isEmpty()) {
            mOverlay.getOverlayView().dispatchDraw(canvas);
        }
        // Step 6, draw decorations (foreground, scrollbars)
        onDrawForeground(canvas);
        // Step 7, draw the default focus highlight
        drawDefaultFocusHighlight(canvas);
        if (debugDraw()) {
            debugDrawFocus(canvas);
        }
        // we're done...
        return;
    }
}
```



```java
//ViewRootImpl.java
void scheduleTraversals() {
    if (!mTraversalScheduled) {
        mTraversalScheduled = true;
        // 发一个同步屏障
        mTraversalBarrier = mHandler.getLooper().getQueue().postSyncBarrier();
        // 保存一个Callback
        mChoreographer.postCallback(
                Choreographer.CALLBACK_TRAVERSAL, mTraversalRunnable, null);
        if (!mUnbufferedInputDispatch) {
            scheduleConsumeBatchedInput();
        }
        notifyRendererOfFramePending();
        pokeDrawLockIfNeeded();
    }
}
```

```java
//大小为4，保存四种类型的Callback队列
CallbackQueue[] mCallbackQueues;

public static final int CALLBACK_INPUT = 0;
public static final int CALLBACK_ANIMATION = 1;
public static final int CALLBACK_TRAVERSAL = 2;
public static final int CALLBACK_COMMIT = 3;
```

onVsync之后发Message，调用doFrame方法，doFrame中按顺序处理callback，开始绘制

```java
void doTraversal() {
    if (mTraversalScheduled) {
        mTraversalScheduled = false;
        // 移除同步屏障
        mHandler.getLooper().getQueue().removeSyncBarrier(mTraversalBarrier);
        // 开始绘制
        performTraversals();
    }
}
```







forceLayout和requestLayout？

> forceLayout不会主动触发布局，而是添加PFLAG_FORCE_LAYOUT，在下一次布局时检查Flag，对自身进行布局

```java
public class View {
     /**
     * Forces this view to be laid out during the next layout pass.
     * This method does not call requestLayout() or forceLayout()
     * on the parent.
     */
    public void forceLayout() {
        if (mMeasureCache != null) mMeasureCache.clear();
        mPrivateFlags |= PFLAG_FORCE_LAYOUT;
        mPrivateFlags |= PFLAG_INVALIDATED;
    }
}
```

