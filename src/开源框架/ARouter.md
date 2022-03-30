navigation如何导航?

通过`LogisticsCenter.complete`，从`Warehouse`中获取路由表，根据路径找到对应的路由Class，调用startActivity

Warehouse中保存了路由表和IProvider

```java
class Warehouse {
    // Cache route and metas
    static Map<String, Class<? extends IRouteGroup>> groupsIndex = new HashMap<>();
    static Map<String, RouteMeta> routes = new HashMap<>();

    // Cache provider
    static Map<Class, IProvider> providers = new HashMap<>();
    static Map<String, RouteMeta> providersIndex = new HashMap<>();

    // Cache interceptor
    static Map<Integer, Class<? extends IInterceptor>> interceptorsIndex = new UniqueKeyTreeMap<>("More than one interceptors use same priority [%s]");
    static List<IInterceptor> interceptors = new ArrayList<>();

    static void clear() {
        routes.clear();
        groupsIndex.clear();
        providers.clear();
        providersIndex.clear();
        interceptors.clear();
        interceptorsIndex.clear();
    }
}
```

路由表如何注册，注解生成`ARouter$$Group$$模块名`类，调用`loadInto`方法，生成RouteMeta注册到路由表中，并根据继承类型保存到不同map中，例如Provider、Activity、Fragment、Service等

数据如何注入？

1. 注解生成`类名$$ARouter$$Autowired`类，根据key从Arguments或Bundle中获取数据，设置到target中
2. 调用ARouter的`inject`方法，会反射创建`类名$$ARouter$$Autowired`的对象，调用inject方法



使用withObject传递对象，ARouter需要设置Json解析服务，否则传递参数会报空指针：`SerializationService.object2Json(java.lang.Object)' on a null object reference`
