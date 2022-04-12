<div class="poem-side">
  <div id="hitokoto">loading...</div>
  <div id="hitokotofrom"></div>
  <script defer>
  fetch('https://v1.hitokoto.cn')
    .then(response => response.json())
    .then(data => {
       	hitokoto.innerHTML = data.hitokoto
    	if(data.from_who != null) {
      	  hitokotofrom.innerHTML ='——' + data.from_who + ' 《' + data.from + '》'
      	} else {
	  hitokotofrom.innerHTML ='——《' + data.from + '》' 
      }
    })
    .catch(console.error) 
</script>
</div>

# 知识体系

整理中...（粗体表示暂未整理）

精心整理，原创发布。包含大量容易忽略的细节，尽可能通俗、简短，不吹水。

数据结构和算法见[牛客博客](https://blog.nowcoder.net/afauria)

> 之前都在LeetCode刷题，最近才发现牛客有博客功能，原来都是创建工程做笔记，有点蠢。

设计模式篇源码[仓库位置](https://github.com/Afauria/DesignPattern)

Android篇、源码篇、设计模式零零散散存了很多草稿，需要慢慢整理。

有问题或者错误欢迎交流指正，可以提issue或者在[博客](https://blog.afauria.xyz/2021/04/04/%E7%9F%A5%E8%AF%86%E4%BD%93%E7%B3%BB/)中评论回复

# TODO

* 维护文章内锚点链接比较麻烦，而且写好的文章还会修改，花精力在这上面太蠢了，而且强迫症、格式控受不了。还是直接去掉好了。。。后面可以尝试修改JS脚本，自动提取文章内部一级标题锚点作为子目录
* 目前先放到一起，完善之后再分仓库。放一起的时候需要打包到docs目录。分仓库之后可以配置为GitHub Pages根目录

# 参考资料

具体的参考资料在章节中说明。主要的参考资料和引用在此统一致谢，不在具体章节中单独说明。

* [Java工程师成神之路](https://hollischuang.github.io/toBeTopJavaer/#/)
* [C语言中文网-软件设计模式](http://c.biancheng.net/view/1317.html)

引用一句话：技术是繁荣且浩瀚的，人的脑力和体力都是有限的，不管3年5年还是8年，所关注的点多少都会有些变化，理解源码是提升自己的一种方式，在年轻力盛的时期最好能看多少看多少，过几年你可能都会忘记你所看过的一切，也许你只记得一部分，也许你再无精力去关注具体实现了，但，当有一个棘手的问题摆在你的面前，你会本能的从你的记忆海洋中嗅出一丝光亮，那是指引你攻破难关，常年累月的本能，将你沉淀的化成本能吧，将不在乎多年后有个少年郎指着鼻子问你知道什么是Lru吗？我只会笑道：忘记了。看到他我想起了当年的自己，好怀念啊。
