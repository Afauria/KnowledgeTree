# XML解析

## XML简介

XML（eXtensible Markup Language，可扩展标记语言）：类似于HTML（超文本标记语言）。用于结构化传输和存储数据。格式如下：

```xml
<?xml version="1.0" encoding="UTF-8"?> <!--xml声明-->
<Document> <!--根元素，文档开始-->
    text <!--文本节点-->
	<Tag property1="value" property2="value"> <!--子元素，标签开始、属性和值-->
        text <!--文本节点-->
    </Tag> <!--标签结束-->
</Document> <!--文档结束-->
```

XML本身不会做任何事情，需要被程序解析，主要有三种解析方式：

1. DOM（Document Object Model）解析：先把xml文档解析到内存中，通过Dom API来访问树形结构，提取数据。消耗内存大，不适用于大文件解析。
2. SAX（Simple API XML）解析：基于事件驱动，顺序扫描文档，扫描到Document、Element开始和结束的地方通知事件处理函数。解析速度快，占用内存小
3. PULL解析：按顺序遍历读取所有元素，根据事件类型进行解析处理。类似SAX解析，只不过没有事件回调，需要自行判断事件进行处理。只需要处理元素开始事件和结束事件，通过`nextText()`提取标签内文本，通过`next()`解析下一个元素。Android系统默认使用pull解析

## 解析示例

如下内容，解析到对应的`List<Student>`对象中

```xml
<students>
    <student>
        <name sex="man">小明</name>
        <nickName>明明</nickName>
    </student>
    <student>
        <name sex="woman">小红</name>
        <nickName>红红</nickName>
    </student>
    <student>
        <name sex="man">小亮</name>
        <nickName>亮亮</nickName>
    </student>
</students>
```

首先创建XMl对应的Bean对象

```java
public class Student {
    private String name;
    private String sex;
    private String nickName;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSex() {
        return sex;
    }

    public void setSex(String sex) {
        this.sex = sex;
    }

    public String getNickName() {
        return nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    @Override
    public String toString() {
        return "Student{" +
                "name='" + name + '\'' +
                ", sex='" + sex + '\'' +
                ", nickName='" + nickName + '\'' +
                '}';
    }
}
```

### DOM解析

将XMl以文档树的形式存放到内存中，通过Dom API访问XML树。消耗内存大。

1. 创建`DocumentBuilder`：`DocumentBuilderFactory.newInstance().newDocumentBuilder();`
2. 调用`builder.parse()`方法，传入xml数据流解析生成Document对象
3. Document由多个NodeList节点列表和Node节点组成，通过对应的API遍历和获取Document内的信息。

```java
public class DOMXml {
    public List<Student> dom2xml(InputStream is) throws Exception {
        //一系列的初始化
        List<Student> list = new ArrayList<>();
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        //获得Document对象
        Document document = builder.parse(is);
        //获得student的List
        NodeList studentList = document.getElementsByTagName("student");
        //遍历student标签
        for (int i = 0; i < studentList.getLength(); i++) {
            //获得student标签
            Node node_student = studentList.item(i);
            //获得student标签里面的标签
            NodeList childNodes = node_student.getChildNodes();
            //新建student对象
            Student student = new Student();
            //遍历student标签里面的标签
            for (int j = 0; j < childNodes.getLength(); j++) {
                //获得name和nickName标签
                Node childNode = childNodes.item(j);
                //判断是name还是nickName
                if ("name".equals(childNode.getNodeName())) {
                    String name = childNode.getTextContent();
                    student.setName(name);
                    //获取name的属性
                    NamedNodeMap nnm = childNode.getAttributes();
                    //获取sex属性，由于只有一个属性，所以取0
                    Node n = nnm.item(0);
                    student.setSex(n.getTextContent());
                } else if ("nickName".equals(childNode.getNodeName())) {
                    String nickName = childNode.getTextContent();
                    student.setNickName(nickName);
                }
            }
            //加到List中
            list.add(student);
        }
        return list;
    }
}
```

### SAX解析

采用事件驱动，不需要解析整个文档，而是根据读到的字符（文档开头和结束、标签开头和结束、文本内容等）触发相应的事件回调。解析速度快，内存占用少。

> 这些事件定义在`ContentHandler`接口中，直接使用接口需要实现所有方法，因此iAndroid提供了一个`DefaultHandler`类，提供了默认的空实现，因此只需要继承`DefaultHandler`重写感兴趣的方法即可。

1. 创建`SAXParser`：`SAXParserFactory.newInstance().newSAXParser();`
2. 定义一个`Handler`继承`DefaultHandler`，重写感兴趣的事件
   1. `startDocument`：文档开始
   2. `endDocument`：文档结束
   3. `startElement`：元素开始
   4. `endElement`：元素结束
   5. `characters`：标签内文本字符数据
3. 调用`parse`方法传入对应的`handler`进行解析

> Android提供了工具类，可以直接调用`Xml.parse()`方法，传入xml文档和对应的handler进行解析

```java
public class SAXXml {
    public static List<Student> sax2xml(InputStream is) throws Exception {
        SAXParserFactory spf = SAXParserFactory.newInstance();
        //初始化Sax解析器
        SAXParser sp = spf.newSAXParser();
        //新建解析处理器
        MyHandler handler = new MyHandler();
        //将解析交给处理器
        sp.parse(is, handler);
        //返回List
        return handler.getList();
    }

    public static class MyHandler extends DefaultHandler {
        private List<Student> list;
        private Student student;
        //用于存储读取的临时变量
        private String tempString;

        //解析到文档开始调用，一般做初始化操作
        @Override
        public void startDocument() throws SAXException {
            list = new ArrayList<>();
            super.startDocument();
        }

        // 解析到文档末尾调用，一般做回收操作
        @Override
        public void endDocument() throws SAXException {
            super.endDocument();
        }

        //每读到一个元素就调用该方法
        //uri表示命名空间
        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            if ("student".equals(qName)) {
                //读到student标签
                student = new Student();
            } else if ("name".equals(qName)) {
                //获取name里面的属性
                String sex = attributes.getValue("sex");
                student.setSex(sex);
            }
            super.startElement(uri, localName, qName, attributes);
        }

        //读到元素的结尾调用
        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            if ("student".equals(qName)) {
                list.add(student);
            }
            if ("name".equals(qName)) {
                student.setName(tempString);
            } else if ("nickName".equals(qName)) {
                student.setNickName(tempString);
            }
            super.endElement(uri, localName, qName);
        }

        //读到标签内文本调用
        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            tempString = new String(ch, start, length);
            super.characters(ch, start, length);
        }

        //获取该List
        public List<Student> getList() {
            return list;
        }
    }
}
```

### PULL解析

类似SAX解析，采用事件驱动。不同的是没有相应的事件回调，需要自行判断事件进行处理

1. 创建`XmlPullParser`实例
2. 通过工厂创建：`XmlPullParserFactory.newInstance().newPullParser();`
3. 通过Android工具类创建：`Xml.newPullParser();`
4. 设置xml文件流和编码格式：`parser.setInput(is, "utf-8")`
5. 获取事件类型：`parse.getEventType`。事件类型分为五种：
   1. `START_DOCUMENT`：文档开始
   2. `START_TAG`：标签开始
   3. `TEXT`：标签内文本
   4. `END_TAG`：标签结束
   5. `END_DOCUMENT`：文档结束
6. 判断事件类型，解析元素：获取标签名称、属性和值、标签内文本等
7. 调用`nextText()`提取标签内文本，调用`next()`解析下一个元素。

```java
public class PULLXml {
    public static List<Student> pull2xml(InputStream is) throws Exception {
        List<Student> list = null;
        Student student = null;
        //创建xmlPull解析器
        //XmlPullParserFactory.newInstance().newPullParser();
        XmlPullParser parser = Xml.newPullParser();
        ///初始化xmlPull解析器
        parser.setInput(is, "utf-8");
        //读取文件的类型
        int type = parser.getEventType();
        //循环判断事件类型，直到document结束
        while (type != XmlPullParser.END_DOCUMENT) {
            switch (type) {
                //开始标签
                case XmlPullParser.START_TAG:
                    if ("students".equals(parser.getName())) {
                        list = new ArrayList<>();
                    } else if ("student".equals(parser.getName())) {
                        student = new Student();
                    } else if ("name".equals(parser.getName())) {
                        //获取sex属性
                        String sex = parser.getAttributeValue(null,"sex");
                        student.setSex(sex);
                        //获取name值
                        String name = parser.nextText();
                        student.setName(name);
                    } else if ("nickName".equals(parser.getName())) {
                        //获取nickName值
                        String nickName = parser.nextText();
                        student.setNickName(nickName);
                    }
                    break;
                //结束标签
                case XmlPullParser.END_TAG:
                    if ("student".equals(parser.getName())) {
                        list.add(student);
                    }
                    break;
            }
            //继续往下读取标签类型
            type = parser.next();
        }
        return list;
    }
}

```

# XML生成

生成xml文件内容如下

```xml
<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<students>
	<student>
		<name sex="man">小明</name>
	</student>
</students>
```

## Dom生成

主要使用`org.w3c.dom`、`javax.xml.transform`、`javax.xml.parsers`包

```java
public class DomGenerate {
    public void createXml() throws Exception {
        // 创建解析器工厂
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = factory.newDocumentBuilder();
        //创建Document对象
        Document document = db.newDocument();
        //创建根元素students并添加到document中
        Element rootElement = document.createElement("students");
        document.appendChild(rootElement);
        //创建student元素并添加到students中
        Element student = document.createElement("student");
        rootElement.appendChild(student);
        //创建name元素并添加到student中
        Element name = document.createElement("name");
        student.appendChild(name);
        //添加文本内容
        name.setTextContent("小明");
        //添加属性
        name.setAttribute("sex", "man");

        // 创建TransformerFactory对象
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        // 创建 Transformer对象
        Transformer transformer = transformerFactory.newTransformer();

        // 输出内容是否使用换行
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        // 创建xml文件并写入内容
        transformer.transform(new DOMSource(document), new StreamResult(new File("students.xml")));
    }
}
```

## Dom4j生成

主要使用dom4j库生成，包名为`org.dom4j`

```java
public class Dom4jGenerate {
    public void createXml() throws Exception {
        //创建dom4j的document对象
        Document document = DocumentHelper.createDocument();
        //创建根节点students
        Element rootElement = document.addElement("students");
        //生成子元素
        Element student = rootElement.addElement("student");
        Element name = student.addElement("name");
        //添加文本内容
        name.setText("小明");
        //添加属性
        name.addAttribute("sex", "man");

        //设置生成xml的格式
        OutputFormat format = OutputFormat.createPrettyPrint();
        //设置编码格式
        format.setEncoding("UTF-8");
        //生成xml文件
        File file = new File("students.xml");
        XMLWriter writer = new XMLWriter(new FileOutputStream(file), format);
        writer.write(document);
        writer.close();
    }
}
```

## jDom生成

主要使用jDom库生成，包名为`org.jdom`

```java
public class JDomGenerate {
    public void createXml() throws Exception {
        //创建根元素students
        Element root = new Element("students");
        //创建Document对象，并设置根元素
        Document document = new Document(root);
        //创建student子元素并添加到根元素中
        Element student = new Element("student");
        root.addContent(student);
        //创建name元素并添加到student中
        Element name = new Element("name");
        student.addContent(name);
        //设置文本内容
        name.setText("小明");
        //添加属性
        name.setAttribute("sex", "man");

        Format format = Format.getCompactFormat();
        format.setIndent("	");
        format.setEncoding("UTF-8");
        //创建XMLOutputter的对象
        XMLOutputter out = new XMLOutputter(format);
        //将document转换成xml文档
        File file = new File("students.xml");
        out.output(document, new FileOutputStream(file));
    }
}
```

## 使用Sax生成

主要使用`javax.xml.transform`和`org.xml.sax`包

```java
public class SaxGenerate {
    public void createXml() throws Exception {
        //创建SAXTransformerFactory对象
        SAXTransformerFactory tff = (SAXTransformerFactory) SAXTransformerFactory.newInstance();
        //通过SAXTransformerFactory创建一个TransformerHandler的对象
        TransformerHandler handler = tff.newTransformerHandler();
        //通过handler创建一个Transformer对象
        Transformer tr = handler.getTransformer();
        //通过Transformer对象对生成的xml文件进行设置
        //设置编码方式
        tr.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        //设置是否换行
        tr.setOutputProperty(OutputKeys.INDENT, "yes");
        //创建一个Result对象
        File f = new File("students.xml");
        Result result = new StreamResult(new FileOutputStream(f));
        //handler输出结果到result中
        handler.setResult(result);

        // 打开document
        handler.startDocument();
        //添加根元素students开始标签
        AttributesImpl attr = new AttributesImpl();
        handler.startElement("", "", "students", attr);
        //添加子元素student开始标签
        handler.startElement("", "", "student", attr);
        //添加属性
        attr.addAttribute("", "", "sex", "", "man");
        //添加name开始标签
        handler.startElement("", "", "name", attr);
        //清除属性
        attr.clear();
        //添加文本内容
        String name = "小明";
        handler.characters(name.toCharArray(), 0, name.length());
        //对称闭合标签
        handler.endElement("", "", "name");
        handler.endElement("", "", "student");
        handler.endElement("", "", "students");
        // 关闭document
        handler.endDocument();
    }
}
```

## 总结

总的来说就是对各种库的API的使用。

上面是生成固定的内容，无法复用，实际使用中可以定义Java Bean对象，将Bean转换为xml文档

Dom生成有多个三方库可以使用，存在很多同名的类，使用的时候需要注意包名。

Dom基于树，生成的内容会保存到内存中，最后解析Document对象生成，过程中可以随时操作和修改元素。

Sax基于事件，使用的时候需要对称闭合标签，且不能修改已经生成的标签。
