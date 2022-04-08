# 回收复用机制

1. mAttachedScrap、mChangedScrap
2. mCachedView：缓存屏幕外View，容量为2，可以setCacheViewSize
3. RecyclerViewPool：每种ViewType缓存5个，会触发onBindViewHolder，多个RecyclerView可以共用一个RecyclerViewPool

# 自定义LayoutManager

# 常见问题处理和优化案例

踩坑：两个RecyclerView使用LinearLayout+weight平分布局，当隐藏其中一个RecyclerView的时候，Item宽度会出现异常

* 解决方案：RecyclerView外多嵌套一层，weight属性设置给RecyclerView父布局，不要直接给RecyclerView设置
* ![异常布局](/Users/Afauria/Desktop/learn/RecyclerView使用weight异常.png)



* TV横向RecyclerView item设置为TextView跑马灯，获取焦点的时候播放跑马灯，滚动会出现异常，可能会打印该日志（也可能没有）：`requestLayout() improperly called by android.widget.TextView{...} during layout: running second layout pass`。
  * 解决方案：需要在TextView外多嵌套一层作为item



recyclerview查找焦点，会调用LayoutManager的onInterceptFocusSearch，如果返回了一个view，则直接使用，如果是null则继续查找。如果查找失败会调用LayoutManager的onFocusSearchFailed方法。

当前viewgroup找焦点，会从当前view的focusable里面找，如果没有找到则调用getParent().focusSearch从父view里面找。focusable再次扩大。因此能够找到当前view外部的焦点.

自己调用FocusFinder的`findNextFocus(root,focused,direction)`则只会找root下的焦点，不会找到view外部的焦点

focusSearch->onInterceptFocusSearch->onFocusSearchFailed->super.focusSearch->getParent.focusSearch()
可以重写onInterceptFocusSearch、onFocusSearchFailed找焦点



# RecyclerView设置居中滚动

```java
//1. 重写smoothScrollToPosition，手动调用滑动，自定义CenterScroller，重写calculateDtToFit
//      --->滚动时间设置太小、且item数量多的时候会有问题
//      --->重写LayoutManager的computeScrollVectorForPosition，返回null，会调用scrollToPosition立马滚动，滚动完再通过onTargetFound计算view位置居中calculateDtToFit
//2. 子View获取焦点，重写requestChildRectangleOnScreen： 获取焦点才能触发---->首次进入没有获取焦点，需要调用smoothScrollToPosition滑动到选中项
//3. 调用smoothScrollToPosition，onScrollStateChanged滚动idle的时候调用smoothScrollBy纠正偏移--->View在可见范围内不会触发滚动
//4. scrollToPosition：触发onLayoutChildren，通过计算anchor和mCoordinate重新布局--->计算复杂
class CenterScroller(context: Context?) : LinearSmoothScroller(context) {
        override fun calculateDtToFit(
            viewStart: Int,
            viewEnd: Int,
            boxStart: Int,
            boxEnd: Int,
            snapPreference: Int
        ): Int {
            return (boxStart + (boxEnd - boxStart) / 2) - (viewStart + (viewEnd - viewStart) / 2)
        }

        override fun calculateTimeForScrolling(dx: Int): Int {
            //滚动时间，不能为0，否则会闪退
            //设置太小，item数量多的时候，滚动过程中找不到目标view
            // ---->重写LayoutManager的computeScrollVectorForPosition，返回null，会调用scrollToPosition立马滚动，滚动完再通过onTargetFound计算view位置居中calculateDtToFit
            return 1
        }
}
```

