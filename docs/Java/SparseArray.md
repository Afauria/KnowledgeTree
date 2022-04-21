SparseArray是Android特有的api

1. 内部使用`int[] mKey`和`Object[] mValue`数组，避免int自动装箱，且不需要保存链表，比HashMap更省内存
2. 使用二分查找，倒序插入效率低，每次都要拷贝整个数组
3. keyAt、valueAt：根据下标找到对应的值
4. Key只能为int类型

ArrayMap：与HashMap相比不需要构造entry对象，没有避免装箱

1. 内部使用两个数组，一个记录hash值`int[] mHashes`，一个记录Key和Value值`Object[] mArray`，使用二分法排序和查找

2. Key会自动装箱放到Object数组中

3. key在object数组的第1位，value在key的下一位：

4. ```
   mArray[index<<1] = key;
   mArray[(index<<1)+1] = value;
   ```

5. 查找的时候，计算hashCode，二分查找在hash数组中的位置，x2即key的位置，x2+1即value的位置

5. 初始容量为0，每次扩容2倍，每次至少扩容4

HashMap时间复杂度为O(1)、ArrayMap和SparseArray时间复杂度为O(logn)

SparseIntArray、SparseLongArray、SparseBooleanArray：value数组是基本类型，不需要装箱

mSize：存储真实元素数量

put流程

```java
public void put(int key, E value) {
    //二分查找Key数组
    int i = ContainerHelpers.binarySearch(mKeys, mSize, key);
    if (i >= 0) {
        //如果找到了，直接替换，key按顺序排序
        mValues[i] = value;
    } else {
        //找不到则取反，变为正数
        i = ~i;
        //如果该位置为空，则直接替换
        if (i < mSize && mValues[i] == DELETED) {
            mKeys[i] = key;
            mValues[i] = value;
            return;
        }
        //如果Key中存在无用的数值，则清理
        if (mGarbage && mSize >= mKeys.length) {
            gc();
            //下标会改变，重新查找
            i = ~ContainerHelpers.binarySearch(mKeys, mSize, key);
        }
        // 数组扩容
        mKeys = GrowingArrayUtils.insert(mKeys, mSize, i, key);
        mValues = GrowingArrayUtils.insert(mValues, mSize, i, value);
        mSize++;
    }
}
```

扩容大小：初始容量为10，扩容大小为原来的2倍

```java
//GrowArrayUtils.java
public static <T> T[] insert(T[] array, int currentSize, int index, T element) {
    assert currentSize <= array.length;
    //放不下新元素则扩容
    if (currentSize + 1 <= array.length) {
        //只会拷贝index之后的数组，因此倒序插入效率低
        System.arraycopy(array, index, array, index + 1, currentSize - index);
        array[index] = element;
        return array;
    } else {
        T[] newArray = (Object[])((Object[])Array.newInstance(array.getClass().getComponentType(), growSize(currentSize)));
        System.arraycopy(array, 0, newArray, 0, index);
        newArray[index] = element;
        System.arraycopy(array, index, newArray, index + 1, array.length - index);
        return newArray;
    }
}
public static int growSize(int currentSize) {
    return currentSize <= 4 ? 8 : currentSize * 2;
}
```

gc方法：将存储的对象从头开始填入，移除空对象，例如value

```java
private void gc() {
    int n = mSize;
    int o = 0;
    int[] keys = mKeys;
    Object[] values = mValues;

    for (int i = 0; i < n; i++) {
        Object val = values[i];
        if (val != DELETED) {
            if (i != o) {
                keys[o] = keys[i];
                values[o] = val;
                values[i] = null;
            }
            o++;
        }
    }
    mGarbage = false;
    mSize = o;
}
```

