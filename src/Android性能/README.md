还未迁移，见[博客](https://blog.afauria.xyz/categories/Android/)

https://blog.csdn.net/weixin_39792393/article/details/112784765

# 大图片

1. Android 3.0 之前：Bitmap像素数据存储在 native 堆中，数据的释放依赖 Java对象的 finalize() 方法回调，该方法的调用不太可靠，而且现在已经被 Java 标记为废弃。
2. Android 3.0 ~ 7.0：Bitmap 像素数据存储在 Java 堆中，确实解决了可靠释放的问题；也带来一个新问题：Android 应用程序对 Java 堆的限制是很严格的，创建Bitmap这种大对象容易引起 GC。
3. Android 8.0 及以后：Bitmap 像素数据存储在 native 堆中，同时引入了 NativeAllocationRegistry 机制保证了 native 内存释放的可靠性，同时可以用的空间大大增加。

从资源文件中获取

```java
Bitmap rawBitmap = BitmapFactory.decodeResource(getResources(),R.drawable.img1);  

//将Bitmap转换为Drawable   Drawable转Bitmap
Drawable newBitmapDrawable = new BitmapDrawable(Bitmap);
//如果要获取BitMapDrawable中所包装的BitMap对象，可以用getBitMap()方法；
BitmapDrawable bitmapDrawable=(BitmapDrawable)drawable;
Bitmap  bitmap = bitmapDrawable.getBitmap();
```

从SD卡中得到图片

```java
//方法1
String SDCarePath=Environment.getExternalStorageDirectory().toString(); 
String filePath=SDCarePath+"/"+"haha.jpg"; 
Bitmap rawBitmap1 = BitmapFactory.decodeFile(filePath, null); 
//方法2
InputStream inputStream=getBitmapInputStreamFromSDCard("haha.jpg"); 
Bitmap rawBitmap2 = BitmapFactory.decodeStream(inputStream);
```

```java
//设置图片的圆角，返回设置后的BitMap
public Bitmap toRoundCorner(Bitmap bitmap, int pixels) {
        Bitmap roundCornerBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Config.ARGB_8888);
        Canvas canvas = new Canvas(roundCornerBitmap);
        int color = 0xff424242;// int color = 0xff424242;
        Paint paint = new Paint();
        paint.setColor(color);
        // 防止锯齿
        paint.setAntiAlias(true);
        Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        RectF rectF = new RectF(rect);
        float roundPx = pixels;
        // 相当于清屏
        canvas.drawARGB(0, 0, 0, 0);
        // 先画了一个带圆角的矩形
        canvas.drawRoundRect(rectF, roundPx, roundPx, paint);
        paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
        // 再把原来的bitmap画到现在的bitmap！！！注意这个理解
        canvas.drawBitmap(bitmap, rect, rect, paint);
        return roundCornerBitmap;
    }
```

```java
	//将图片高宽和的大小kB压缩,得到图片原始的高宽
        int rawHeight = rawBitmap.getHeight();
        int rawWidth = rawBitmap.getWidth();
        // 设定图片新的高宽
        int newHeight = 500;
        int newWidth = 500;
        // 计算缩放因子
        float heightScale = ((float) newHeight) / rawHeight;
        float widthScale = ((float) newWidth) / rawWidth;
        // 新建立矩阵
        Matrix matrix = new Matrix();
	// 将图片大小压缩
        matrix.postScale(heightScale, widthScale);
        // 设置图片的旋转角度
        // matrix.postRotate(-30);
        // 设置图片的倾斜
        // matrix.postSkew(0.1f, 0.1f);

// 压缩后图片的宽和高以及kB大小均会变化
Bitmap newBitmap = Bitmap.createBitmap(rawBitmap, 0, 0, rawWidth, rawWidth, matrix, true);
```


由于前面创建的Bitmap所占用的内存还没有回收，而导致引发OutOfMemory错误，所以用下面方法判断是否回收。

```java
if(!bitmap.isRecycled())
{
   bitmap.recycle()
}      
```



`public static Bitmap createBitmap (Bitmap source, int x, int y, int width, int height) `
简单的剪切图像的方法


压缩图片：Bitmap所占用的内存 = 图片长度 x 图片宽度 x 一个像素点占用的字节数。3个参数，任意减少一个的值，就达到了压缩的效果。

参数分别为：原始bitmap，修改后宽高
Bitmap.createScaledBitmap(bit, 150, 150, true);



缩放法
`public static Bitmap createBitmap (Bitmap source, int x, int y, int width, int height, Matrix m, boolean filter)`
从原始位图剪切图像，这是一种高级的方式。可以用Matrix(矩阵)来实现旋转等高级方式截图
参数说明：
　　Bitmap source：要从中截图的原始位图
　　int x:起始x坐标
　　int y：起始y坐标
int width：要截的图的宽度
int height：要截的图的高度
Bitmap.Config  config：一个枚举类型的配置，可以定义截到的新位图的质量
返回值：返回一个剪切好的Bitmap

```java
Matrix matrix = new Matrix();
matrix.setScale(0.5f, 0.5f);
bm = Bitmap.createBitmap(bit, 0, 0, bit.getWidth(), bit.getHeight(), matrix, true);
ByteArrayOutputStream baos = new ByteArrayOutputStream();
//质量压缩方法，宽高不变，quality:0-100，100表示不压缩，把压缩后的数据存放到baos中  
bit.compress(CompressFormat.JPEG, quality, baos);
byte[] bytes = baos.toByteArray();
bm = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
```



```java
private Bitmap compressImage(Bitmap image) {  
    ByteArrayOutputStream baos = new ByteArrayOutputStream();  
    image.compress(Bitmap.CompressFormat.JPEG, 100, baos);//质量压缩方法，这里100表示不压缩，把压缩后的数据存放到baos中  
    int options = 100;  
    while ( baos.toByteArray().length / 1024>100) {  //循环判断如果压缩后图片是否大于100kb,大于继续压缩         
        baos.reset();//重置baos即清空baos  
        image.compress(Bitmap.CompressFormat.JPEG, options, baos);//这里压缩options%，把压缩后的数据存放到baos中  
        options -= 10;//每次都减少10  
    }  
    ByteArrayInputStream isBm = new ByteArrayInputStream(baos.toByteArray());//把压缩后的数据baos存放到ByteArrayInputStream中  
    Bitmap bitmap = BitmapFactory.decodeStream(isBm, null, null);//把ByteArrayInputStream数据生成图片  
    return bitmap;  
}
		
```

```java
//-----------------------Android大图的处理方式---------------------------
private void setPicToImageView(ImageView imageView, File imageFile){
	int imageViewWidth = imageView.getWidth();
	int imageViewHeight = imageView.getHeight();
	BitmapFactory.Options opts = new Options();
	//设置这个，只得到Bitmap的属性信息放入opts，而不把Bitmap加载到内存中
	opts.inJustDecodeBounds = true;	
	BitmapFactory.decodeFile(imageFile.getPath(), opts);
	
	int bitmapWidth = opts.outWidth;
	int bitmapHeight = opts.outHeight;
	//取最大的比例，保证整个图片的长或者宽必定在该屏幕中可以显示得下
	int scale = Math.max(imageViewWidth / bitmapWidth, imageViewHeight / bitmapHeight);
	
	//缩放的比例
	opts.inSampleSize = scale;
	//内存不足时可被回收
	opts.inPurgeable = true;
	//设置为false,表示不仅Bitmap的属性，也要加载bitmap
	opts.inJustDecodeBounds = false;
	
	Bitmap bitmap = BitmapFactory.decodeFile(imageFile.getPath(), opts);
	imageView.setImageBitmap(bitmap);
}
```
