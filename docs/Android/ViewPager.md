

### FragmentPageAdapter和FragmentStatePageAdapter

`FragmentPageAdapter`移出屏幕detach  Fragment。`FragmentStatePageAdapter`移出屏幕remove Fragment。

detach之后Fragment从视图中销毁，但实例还保存在FragmentManger中（androidx保存在的FragmentStore中）。mAdded列表中移除，但mActive中还保存着，findFragment的时候会从mActive中查找。

`FragmentStatePageAdapter`自行保存了一份mFragments列表和mSavedState列表，一一对应，移除的时候移除相应的实例。并从FragmentManager中移出。


```java
@Override
public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
    Fragment fragment = (Fragment) object;

    if (mCurTransaction == null) {
        mCurTransaction = mFragmentManager.beginTransaction();
    }
    if (DEBUG) Log.v(TAG, "Detaching item #" + getItemId(position) + ": f=" + object
            + " v=" + fragment.getView());
    mCurTransaction.detach(fragment);
    if (fragment.equals(mCurrentPrimaryItem)) {
        mCurrentPrimaryItem = null;
    }
}
```

```java
@Override
public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
    Fragment fragment = (Fragment) object;

    if (mCurTransaction == null) {
        mCurTransaction = mFragmentManager.beginTransaction();
    }
    if (DEBUG) Log.v(TAG, "Removing item #" + position + ": f=" + object
            + " v=" + ((Fragment)object).getView());
    while (mSavedState.size() <= position) {
        mSavedState.add(null);
    }
    mSavedState.set(position, fragment.isAdded()
            ? mFragmentManager.saveFragmentInstanceState(fragment) : null);
    mFragments.set(position, null);

    mCurTransaction.remove(fragment);
    if (fragment.equals(mCurrentPrimaryItem)) {
        mCurrentPrimaryItem = null;
    }
}
```

`FragmentStatePageAdapter`适用于页面较多的情况，`FragmentPageAdapter`适用于页面较少

ViewPager通过无限itemCount方式实现无限轮播效果，如果使用FragmentPageAdapter，每次都创建对象，会导致mActive中的Fragment实例增多，反射获取`getActiveFragmentCount`，数量一直增加。

解决方案：Adapter中保存fragment列表，getItem方法中取余复用fragment。-->会闪退，无法添加重复的fragment，如下

FragmentPageAdapter添加Fragment的时候，会设置tag（`FragmentStatePageAdapter`不会设置）。

FragmentPageAdapter通过containerId+itemId的方式生成tag，Transaction添加的时候如果同一个fragment实例添加进去的tag不一样，会抛异常，如下。

```java
@Override
public Object instantiateItem(@NonNull ViewGroup container, int position) {
    if (mCurTransaction == null) {
        mCurTransaction = mFragmentManager.beginTransaction();
    }

    final long itemId = getItemId(position);

    // getItemId默认返回position
    String name = makeFragmentName(container.getId(), itemId);
    Fragment fragment = mFragmentManager.findFragmentByTag(name);
  	//该position已经添加过Fragment，从FragmentManger中取出重新attach
    if (fragment != null) {
        if (DEBUG) Log.v(TAG, "Attaching item #" + itemId + ": f=" + fragment);
        mCurTransaction.attach(fragment);
    } else {
        //创建新的fragment
        fragment = getItem(position);
        if (DEBUG) Log.v(TAG, "Adding item #" + itemId + ": f=" + fragment);
        mCurTransaction.add(container.getId(), fragment,
                makeFragmentName(container.getId(), itemId));
    }
    if (fragment != mCurrentPrimaryItem) {
        fragment.setMenuVisibility(false);
        if (mBehavior == BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
            mCurTransaction.setMaxLifecycle(fragment, Lifecycle.State.STARTED);
        } else {
            fragment.setUserVisibleHint(false);
        }
    }

    return fragment;
}
//tag名称拼接
private static String makeFragmentName(int viewId, long id) {
	return "android:switcher:" + viewId + ":" + id;
}
```

```java
void doAddOp(int containerViewId, Fragment fragment, @Nullable String tag, int opcmd) {
    //...
    if (tag != null) {
       //将同一个fragment再次添加到视图的时候，如果tag和原来的不一致，无法添加
        if (fragment.mTag != null && !tag.equals(fragment.mTag)) {
            throw new IllegalStateException("Can't change tag of fragment "
                    + fragment + ": was " + fragment.mTag
                    + " now " + tag);
        }
        fragment.mTag = tag;
    }

    if (containerViewId != 0) {
        if (containerViewId == View.NO_ID) {
            throw new IllegalArgumentException("Can't add fragment "
                    + fragment + " with tag " + tag + " to container view with no id");
        }
        if (fragment.mFragmentId != 0 && fragment.mFragmentId != containerViewId) {
            throw new IllegalStateException("Can't change container ID of fragment "
                    + fragment + ": was " + fragment.mFragmentId
                    + " now " + containerViewId);
        }
        fragment.mContainerId = fragment.mFragmentId = containerViewId;
    }

    addOp(new Op(opcmd, fragment));
}
```

解决方案：可以重写`getItemId`让tag保持一样---->复用了fragment，反射获取`getActiveFragmentCount`，数量不会一直增加


```kotlin
class GuidePagerAdapter(fm: FragmentManager) :
    FragmentPagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

    private val mFragments = mutableMapOf<Int, Fragment>()

    override fun getItem(position: Int): Fragment {
        val pos = position % 6
        if (mFragments.containsKey(pos)) {
            return mFragments[pos]!!
        }
        val fragment = when (pos) {
            0 -> GuideLoginFragment()
            5 -> GuideFeatureFragment()
            else ->
                GuideOperationFragment.newInstance(
                    OPERATION_TITLES[pos - 1],
                    OPERATION_DESCRIPTION[pos - 1],
                    OPERATION_PICS[pos - 1]
                )
        }
        mFragments[pos] = fragment
        return fragment
    }

    override fun getItemId(position: Int): Long {
        return (position % 6).toLong()
    }

    override fun getCount(): Int = 500
}
```

