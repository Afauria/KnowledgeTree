js构造函数和普通函数区别
在命名规则上，构造函数一般是首字母大写，普通函数则是遵照小驼峰式命名法。
在方法调用上，

* 构造函数使用new关键字调用，普通函数直接调用。
* 构造函数内部会创建一个实例，调用普通函数时则不会创建新的对象。
* 构造函数内部的this指向是新创建的person实例，而普通函数内部的this指向调用函数的对象（如果没有对象调用，默认为window）
* 构造函数返回值是创建的对象，普通函数不return的话没有返回值（undefined）

构造函数的执行流程

	(1)立刻在堆内存中创建一个新的对象
	(2)将新建的对象设置为函数中的this
	(3)逐个执行函数中的代码
	(4)将新建的对象作为返回值
相当于

```
function Person(){
	this = new Object();
	
	this.name = object;
	……
	
	return this;
}
```

原型
js中每个函数就是一个对象（Function），这个对象有一个prototype属性，表示该函数的原型，也表示一个类的成员的集合。
prototype属性指向一个对象，称为原型对象，包含函数实例的方法和属性
将函数用作构造函数调用（使用new操作符调用）的时候，新创建的对象会从原型对象上继承属性和方法

__proto__属性，用来读取或设置当前对象的prototype对象。
大部分浏览器支持，但es6本身并不提倡用这种方式。
从语义上：加了双下划线表示内部属性，不对外开放。
从兼容性上：只有浏览器环境支持这个属性，不支持其他环境，如node

__proto__实际上调用的是Object.prototype.__proto__，

```
function Student(){
  //
}
var stu=new Student();
stu.__proto__==Student  //false
stu.__proto__==Student.prototype  //true
```

js中类的创建

1. 工厂方式，经典方式

js对象可以先创建后动态定义

```
var person = new Object();
person.name = "Afauria";
person.age = 20;
person.say = function(str){
	console.log(str);
}
```

可以用工厂方法封装，返回对象：

```
//缺点：函数被创建多次
function createPerson(name, age){
	var person = new Object();
	person.name = name;
	person.age = age;
	person.say = function(str){
		console.log(str);
	}
	return person;
}
var person = createPerson("Afauria", 20);
```
缺点：函数被创建多次
其他用法：在外部定义函数，通过属性指向函数，可以避免函数多次创建

2. 构造函数方式

```
function Person(name, age){
	this.name = name;
	this.age = age;
	this.say = function(str){
		console.log(str);
	}
}
var person = new Person("Afauria", 20);
```
缺点：函数被创建多次
其他用法：函数在外部定义

3. 混合工厂方式

```
//伪构造函数
function Person(name, age){
	var temp = new Object();
	temp.name = name;
	temp.age = age;
	temp.say = function(str){
		console.log(str);
	}
	return temp;
}

var person = new Person();
```
与工厂方法类似，只不过使用了new运算符，使得它看起来像构造函数

4. 原型方式

```
//定义空构造函数
function Person(){
}
//在类的原型上添加属性和方法
Person.prototype.name = "Afauria";
Person.prototype.age = 20;
Person.prototype.say = function(str){
	console.log(str);
}
//创建对象
var person = new Person();
```
优点：函数只创建一次
缺点：多个实例共享一个对象：这种方式创建的对象所有的引用指向同一个地方，只有一个实例，改变一个对象的属性另一个对象也会改变

5. 构造/原型混合方式

```
//构造函数定义非函数属性
function Person(name, age){
	this.name = name;
	this.age = age;
}
//函数属性使用原型方式定义
Person.prototype.say = function(str){
	console.log(str);
}
```
优点：函数只创建一次，每个对象属性有自己的值
缺点：与其他面向对象语言不同，大多数面向对象语言从视觉上对属性和方法进行了封装，即类

6. 动态原型方式

```
function Person(name, age){
	this.name = name;
	this.age = age;
	//判断是否定义了该方法
	if (typeof this.say != "function") {
        Person.prototype.say = function (str) {
            console.log(str);
        }
	}
	//或者通过一个变量来判断
	if (typeof Person._initialized == "undefined") {
		Person.prototype.say = function(str) {
            console.log(str);
		};
   		Person._initialized = true;
    }
}
```

7. es6引入类关键字，可以看作是一种语法糖，不需要加function关键字。

```
class Person{
	constructor(name, age){
		this.name = name;
		this.age = age;
	}
	say(str){
		console.log(str);
	}
}

let person = new Person("Afauria", 20);
```


```
//本质还是构造函数，如下：
console.log(typeof Person);//function  
console.log(Person === Person.prototype.constructor);//true</span>  

//构造函数的prototype属性在es6的类里面也存在，在类的实例上调用方法就是调用原型上的方法
console.log(person.constructor === Person.prototype.constructor);//true</span>  
```

原型动态添加方法

```
Object.assign(Person.prototype,{
    getWidth(){
        console.log('12');
    },
    getHeight(){
        console.log('24');
    }
});
```
construtor函数默认返回this，也可以自己指定返回的对象



js创建对象效率

{}是字面量，可以立即求值，而new Object()本质上是方法（只不过这个方法是内置的）调用，既然是方法调用，就涉及到在proto链中遍历该方法，当找到该方法后，又会生产方法调用必须的堆栈信息，方法调用结束后，还要释放该堆栈.

new Object()构造器可以根据参数创建不同的对象，效率较低：
	
	传入String 返回String，类似new String()
	传入Number 返回Number，类似new Number()
	传入Object 返回Object，其实没啥用


```
var Obj = function() {};

var a = {};
var b = new Object();
var c = new Obj();
//c > b > a
```

js浅拷贝和深拷贝

```
var obj = {
	‘data‘: [11, 2, 3],
	‘name‘: ‘mfg‘,
	fn: function() {}
};
var objNew = { ...obj };
var objNew2 = Object.assign({}, obj);
console.log(objNew === obj) //false
console.log(objNew2 === obj) //false
console.log(objNew.fn === obj.fn) //true
console.log(objNew2.fn === obj.fn) //true

```
[关于js中的浅拷贝和深拷贝](http://www.360doc.com/content/18/0202/07/40810192_727093135.shtml)








