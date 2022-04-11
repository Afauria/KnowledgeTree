

1. 在HTTP和TCP之间加入了SSL/TLS安全协议，使得HTTP报文能够加密传输
2. HTTPS除了TCP三次握手之外，还需要进行SSL/TLS的握手过程
3. HTTP端口号是80，HTTPS端口号是443
4. 需要向CA申请数字证书，验证服务器身份是可信的

# HTTPS连接过程

SSL/TLS是四次握手

1. 客户端发出请求：
   1. 客户端支持的SSL/TLS协议版本
   2. 客户端支持的加密算法（例如RSA）
   3. 生成随机数Client-Random
2. 服务端响应：
   1. 确认SSL/TLS版本是否支持，如果不支持，则关闭加密通信
   2. 确认使用的加密算法
   3. 生成随机数Server-Random
   4. 服务端发送数字证书
3. 客户端解析数字证书，通过浏览器或系统中的CA公钥，检查证书机构、日期等。
   1. 客户端从数字证书中取出公钥
   2. 客户端生成随机数（pre-master key），使用双方协商的加密算法，将pre-masters和随机值Client-Random、Server-Random加密生成会话密钥
   3. 客户端将pre-master key使用公钥加密，发给服务端
   4. 客户端通过会话密钥加密消息，验证服务端是否能正常接收
4. 服务端使用私钥解密获得pre-master key密钥，和随机值1、2生成会话密钥
   1. 服务端同样返回加密消息，如果客户端能够接收，则表示SSL连接建立

<img src="HTTPS/HTTPS连接过程.png" style="zoom:67%;" />

HTTPS采用对称加密和非对称加密混合方式：

1. 非对称加密：握手过程中交换会话密钥。
2. 对称加密：使用会话密钥加密明文数据。

# 结语

待整理：

https://mp.weixin.qq.com/s/fvvnQ3xI1nz-Ri3vBfG2lw

https://mbd.baidu.com/newspage/data/landingsuper?rs=3469628913&ruk=-0z0OzrK880oNILHs9dUEQ&isBdboxFrom=1&pageType=1&urlext=%7B%22cuid%22%3A%22luv1ug8hHt_jividg8HltY84H8_SO-8jg8vyi0u72u0na28F0iHA80iX1R0OfSOSrI8mA%22%7D&context=%7B%22nid%22%3A%22news_9874847962039145753%22%7D

https://www.cnblogs.com/hencins/p/9564011.html

https://mbd.baidu.com/newspage/data/landingsuper?rs=744666260&ruk=-0z0OzrK880oNILHs9dUEQ&isBdboxFrom=1&pageType=1&urlext=%7B%22cuid%22%3A%22luv1ug8hHt_jividg8HltY84H8_SO-8jg8vyi0u72u0na28F0iHA80iX1R0OfSOSrI8mA%22%7D&context=%7B%22nid%22%3A%22news_9510593537971324837%22%7D

