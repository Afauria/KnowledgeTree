# Introduction

1. 安装GitBook环境：`npm install -g gitbook-cli`
2. 使用`gitbook --version`检查是否安装成功
3. clone项目
4. 安装GitBook插件：`gitbook install`
5. `gitbook serve`本地运行Gitbook，会打包到`_book`夹中，将`_book`添加到`.gitignore`
6. `gitbook build . docs`打包到`docs`文件夹中，用于部署到`GitHub Pages`中
7. GitHub上`Settings-Pages`里面配置，选择docs目录，即可访问`https://afauria.github.io/<仓库名称>/`，由于绑定了域名，因此也可以访问`https://blog.afauria.xyz/GitBook-Learner/`



安装后提示`if (cb) cb.apply(this, arguments)`：

> node版本的问题。可以找到报错文件`\node_modules\graceful-fs\polyfills.js:287`
>
> 注释掉下面三行代码：62、63、64行
>
>   // fs.stat = statFix(fs.stat)
>   // fs.fstat = statFix(fs.fstat)
>   // fs.lstat = statFix(fs.lstat)
