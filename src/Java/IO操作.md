# IO类型

<img src="IO操作/IO类型.png" style="zoom:50%;" />

# RandomAccessFile

随机访问文件

1. 允许自由定义文件记录指针
2. 允许向文件追加内容
3. 只能读写文件，不能读写其他IO

使用场景：

1. 断点续传
2. 多线程从不同的位置写入
