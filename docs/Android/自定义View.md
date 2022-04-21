# 自定义View分类

* 继承特定View，重写绘制：例如`ImageView`上画文字，小红点
* 继承ViewGroup：inflate xml布局，例如标题栏（也可以include）
* 继承ViewGroup：add组装其他View
* 继承普通View，重写onMeasure、onLayout、onDraw方法

# 自定义View步骤

重写onMeasure()->onLayout()->onDraw()

onMeasure：参数是父View传进来的可用空间

* 如果是View，计算自身的空间
* 如果是ViewGroup，需要遍历计算子View的空间

结合可用空间和开发者定义的空间得出正确的尺寸

### MeasureSpec

32位，前2位表示3种测量模式，后30位是大小

对于 DecorView 而言，它的MeasureSpec 由窗口尺寸和其自身的 LayoutParams 共同决定；对于普通的 View，它的 MeasureSpec 由父视图的 MeasureSpec 和其自身的 LayoutParams 共同决定

| childLayoutParams/parentSpecMode | EXACTLY                 | AT_MOST                 | UNSPECIFIC         |
| -------------------------------- | ----------------------- | ----------------------- | ------------------ |
| dp/px                            | EXACTLY(childSize)      | EXACTLY(childSize)      | EXACTLY(childSize) |
| match_parent                     | EXACTLY(parentLeftSize) | AT_MOST(parentLeftSize) | UNSPECIFIC         |
| wrap_content                     | AT_MOST(parentLeftSize) | AT_MOST(parentLeftSize) | UNSPECIFIC         |

onMeasure中处理wrap_content：需要完成对内容的测量

View中onMeasure会获取默认值getDefaultSize，如果是AT_MOST或者EXACTLY，则直接使用父View传进来的大小

```java
    /**
     * 获取View的宽或者高的大小
     * 
     * 注意方法:getDefaultSize(int size, int measureSpec)第一个参数
     * size是调用getSuggestedMinimumWidth()方法获得的View的宽或高的最小值
     * 
     * 该方法的返回值有两种情况:
     * (1)measureSpec的specMode为MeasureSpec.UNSPECIFIED
     *    在此情况下该方法的返回值就是View的宽或者高最小值.
     *    该情况很少见,基本上可以忽略
     * (2)measureSpec的specMode为MeasureSpec.AT_MOST或MeasureSpec.EXACTLY:
     *    在此情况下该方法的返回值就是measureSpec中的specSize.
     *    这个值就是系统测量View得到的值.
     *    
     *  View的宽和高由measureSpec中的specSize决定!!!!!!!!
     *  
     *  所以在自定义View重写onMeasure()方法时必须设置wrap_content时自身的大小.
     *  
     *  因为如果子View在XML的布局文件中对于大小的设置采用wrap_content,
     *  不管父View的specMode是 MeasureSpec.AT_MOST还是MeasureSpec.EXACTLY
     *  对于子View而言:它的specMode都是MeasureSpec.AT_MOST,并且其大小都是
     *  parentLeftSize即父View目前剩余的可用空间.
     *  这时wrap_content就失去了原本的意义,变成了match_parent一样了.
     */
    public static int getDefaultSize(int size, int measureSpec) {
        int result = size;
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);

        switch (specMode) {
        case MeasureSpec.UNSPECIFIED:
            result = size;
            break;
        case MeasureSpec.AT_MOST:
        case MeasureSpec.EXACTLY:
            result = specSize;
            break;
        }
        return result;
    }
```

# 控制绘制顺序

图片来源于HenCoder大佬的教程，自定义View讲的很详细很清楚：[HenCoder Android 开发进阶：自定义 View 1-5 绘制顺序](https://hencoder.com/ui-1-5/)

![](自定义View/绘制顺序.jpg)

# 自定义属性

