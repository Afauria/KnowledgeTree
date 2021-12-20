# GitBook说明文档

源项目放到GitHub私有仓库，build之后的静态网页文件放到公共仓库，部署到GitHub Pages。

* 公共仓库才能使用GitHub Pages，私有的要付费
* GitHub Pages要求打包到docs目录

> //todo 目前先放到一起，完善之后再分仓库。放一起的时候需要打包到docs目录。分仓库之后可以配置为GitHub Pages根目录

## 使用步骤

1. 安装GitBook环境：`npm install -g gitbook-cli`
2. 使用`gitbook --version`检查是否安装成功
3. clone源码项目
4. 安装GitBook插件：`gitbook install`
5. `gitbook serve`本地运行Gitbook，会打包到`_book`文件夹中，将`_book`添加到`.gitignore`
6. `gitbook build . docs`打包到`docs`文件夹中，用于部署到`GitHub Pages`中。（不指定路径会打包到`_book`文件夹中）
7. GitHub上`Settings-Pages`里面配置，选择`docs`目录，即可访问`https://afauria.github.io/<仓库名称>/`，由于绑定了域名，因此也可以访问`https://blog.afauria.xyz/KnowledgeTree/`

## 配置

### 配置文档编译路径

```json
{
  "root": "src"
}
```
### 配置友情链接，如博客

```json
{
  "links": {
    "sidebar": {
      "我的博客": "https://afauria.github.io"
    }
  }
}
```

### 词汇表文件

将一些专业名词、术语放到列表中，文章中引用了词汇，鼠标悬浮会有提示，并且可以链接跳转查看。新建`Glossary.md`文件，配置如下

```
## Git
分布式版本控制软件

## Markdown
排版语言
```

### 插件禁用

`book.json`中`plugins`中是插件列表，前面加一个减号表示禁用。`pluginsConfig`是某一个插件的配置

## 配置自定义样式

`book.json`中配置样式，新建`styles/website.css`文件，可以通过浏览器检查html元素来修改样式。如修改侧边栏宽度，修改代码块高度等

```json
{
  "styles": {
    "website": "./styles/website.css"
  }
}
```

### 配置`package.json`运行命令

使用`gitbook`命令打包发布。也可以配置npm自定义打包脚本。新建`package.json`文件，配置如下，去掉注释。

使用`npm run serve/build/pdf/epub/mobi`运行。

```json
{
  "scripts": {
 		"serve": "gitbook serve", //本地运行
  	"build": "gitbook build src ./build/dist", //打包静态网页到build/dist目录
  	"pdf": "gitbook pdf src ./build/pdf", //打包pdf
  	"epub": "gitbook epub src ./build/epub", //打包epub
  	"mobi": "gitbook mobi src ./build/mobi", //打包mobi
    "deploy": "gitbook build src docs && git push" //打包并发布
	}
}
```

## 错误处理

安装后提示`if (cb) cb.apply(this, arguments)`：

> node版本的问题。可以找到报错文件`\node_modules\graceful-fs\polyfills.js:287`
>
> 注释掉下面三行代码：62、63、64行
>
>   // fs.stat = statFix(fs.stat)
>   // fs.fstat = statFix(fs.fstat)
>   // fs.lstat = statFix(fs.lstat)