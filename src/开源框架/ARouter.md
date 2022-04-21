# 实例

寄信。

目的地、明信片、物流中心。



路由注册和查找

1. 注解生成IRouteRoot和IRouteGroup，包含loadInto方法
   1. IRouteRoot：loadInto保存IRouteGroup的Class对象到WareHouse中
   2. IRouteGroup：loadInto构造RouteMeta对象（包括Class对象、Group、Path等），添加到WareHouse中

2. init中，遍历DexFile元素对象，根据class前缀名称，找到每个模块的`IRouteRoot`类，调用`loadInto`方法，注册路由组，保存到WareHouse的`Map<String, Class>`中
3. 首次调用navigation路由的时候，`LogisticsCenter.complete`方法根据Postcard的Group信息从WareHouse中获取IRouteGroup的类对象，反射创建实例，调用loadInto方法，注册该组所有每个页面的路由，构造RouteMeta对象，存到WareHouse的`Map<String, RouteMeta>`中。（实现了懒加载，因为有一些组件可能不会打开）
4. 如果已经注册，则根据PostCard的path信息，找到RouteMeta，取出Class对象
   1. 调用startActivity
   2. 反射实例化创建Provider，缓存到WareHouse中。每个Provider接口只有一个实例

路由参数注入：

1. apt解析`@AutoWired`注解，生成特定名称的XXAutoWire的类，包含一个inject方法，从Intent或者Arguments中获取到参数，给目标类的成员变量赋值
2. 调用inject方法时，反射实例化XXAutoWire类，调用inject方法

# 路由类型

```java
public enum RouteType {
    ACTIVITY(0, "android.app.Activity"),
    SERVICE(1, "android.app.Service"),
    PROVIDER(2, "com.alibaba.android.arouter.facade.template.IProvider"),
    CONTENT_PROVIDER(-1, "android.app.ContentProvider"),
    BOARDCAST(-1, ""),
    METHOD(-1, ""),
    FRAGMENT(-1, "android.app.Fragment"),
    UNKNOWN(-1, "Unknown route type");
    ...
}
```

# 注解

注解生成`ARouter$$Group$$模块名`类，调用`loadInto`方法，生成RouteMeta注册到路由表中，并根据继承类型保存到不同map中，例如Provider、Activity、Fragment、Service等

# 路由表注册

`ARouter.init(application);`中调用`LogisticsCenter.init`（物流中心）方法初始化路由表，保存到`WareHouse`（仓库）中。

1. 读取Dex文件，遍历所有类，根据包名前缀找到生成的类

   ```java
   dexfile = new DexFile(path);
   
   Enumeration<String> dexEntries = dexfile.entries();
   while (dexEntries.hasMoreElements()) {
       String className = dexEntries.nextElement();
       if (className.startsWith(packageName)) {
           classNames.add(className);
       }
   }
   ```

2. 初始化`IRouteRoot`子类：每个模块独立，类名为`ARouter$$ROOT$$模块名`

   1. 反射创建实例，并调用`loadInto`方法，保存到`Warehouse.groupIndex`中，key为Group名称，value为`IRouteGroup`类对象。
   2. 这个时候还没注册我们定义的路由，只是注册了路由组。等到路由导航的时候路由是否已注册，如果没有则找到对应的`IRouteGroup`组，调用`loadInto`方法，注册路由

3. 初始化`IProviderGroup`子类：类名为`ARouter$$Group$$组名`，反射创建实例

   1. 保存到`Warehouse.providersIndex`中，key为类名，value为Provider路由元数据。
   2. 这个时候还注册Provider实例对象，只是注册了Provider路由元数据，等到需要的时候获取类对象，反射实例化

4. 初始化`IInterceptorGroup`子类，主要用于navigation跳转时进行拦截，例如检查登录失败则不跳转。类名为`ARouter$$Interceptors$$拦截器名`

```java
//LogisticsCenter.java
public synchronized static void init(Context context, ThreadPoolExecutor tpe) throws HandlerException {
    ...
    //读取Dex文件，遍历所有类，根据包名前缀找到生成的类
    routerMap = ClassUtils.getFileNameByPackageName(mContext, ROUTE_ROOT_PAKCAGE);
    ...
    for (String className : routerMap) {
        //初始化`IRouteRoot`类，每个模块独立，类名为`ARouter$$ROOT$$模块名`
        if (className.startsWith(ROUTE_ROOT_PAKCAGE + DOT + SDK_NAME + SEPARATOR + SUFFIX_ROOT)) {
            ((IRouteRoot) (Class.forName(className).getConstructor().newInstance())).loadInto(Warehouse.groupsIndex);
        } else if (className.startsWith(ROUTE_ROOT_PAKCAGE + DOT + SDK_NAME + SEPARATOR + SUFFIX_INTERCEPTORS)) {
            // Load interceptorMeta
            ((IInterceptorGroup) (Class.forName(className).getConstructor().newInstance())).loadInto(Warehouse.interceptorsIndex);
        } else if (className.startsWith(ROUTE_ROOT_PAKCAGE + DOT + SDK_NAME + SEPARATOR + SUFFIX_PROVIDERS)) {
            // Load providerIndex
            ((IProviderGroup) (Class.forName(className).getConstructor().newInstance())).loadInto(Warehouse.providersIndex);
        }
    }
}
```

```java
//IRouteRoot
public class ARouter$$Root$$afauria implements IRouteRoot {
  @Override
  public void loadInto(Map<String, Class<? extends IRouteGroup>> routes) {
    routes.put("common", ARouter$$Group$$common.class);
  }
}
//IProviderGroup
public class ARouter$$Providers$$afauria implements IProviderGroup {
  @Override
  public void loadInto(Map<String, RouteMeta> providers) {
    providers.put("com.alibaba.android.arouter.facade.service.SerializationService", RouteMeta.build(RouteType.PROVIDER, JsonServiceImpl.class, "/common/json", "common", null, -1, -2147483648));
  }
}
//路由组，在LogisticsCenter.complete方法中懒加载注册路由
public class ARouter$$Group$$common implements IRouteGroup {
  @Override
  public void loadInto(Map<String, RouteMeta> atlas) {
    atlas.put("/common/json", RouteMeta.build(RouteType.PROVIDER, JsonServiceImpl.class, "/common/json", "common", null, -1, -2147483648));
  }
}
//懒加载，Provider
public class ARouter$$Providers$$afauria implements IProviderGroup {
  @Override
  public void loadInto(Map<String, RouteMeta> providers) {
    providers.put("com.alibaba.android.arouter.facade.service.SerializationService", RouteMeta.build(RouteType.PROVIDER, JsonServiceImpl.class, "/common/json", "common", null, -1, -2147483648));
  }
}
```

完善Postcard目标信息

```java
//LogisticsCenter.complete
public synchronized static void completion(Postcard postcard) {
    //检查是否注册了路由信息
    RouteMeta routeMeta = Warehouse.routes.get(postcard.getPath());
    if (null == routeMeta) {    // Maybe its does't exist, or didn't load.
        //如果没有注册，则先获取路由组信息
        Class<? extends IRouteGroup> groupMeta = Warehouse.groupsIndex.get(postcard.getGroup());  // Load route meta.
        if (null == groupMeta) {
            throw new NoRouteFoundException(TAG + "There is no route match the path [" + postcard.getPath() + "], in group [" + postcard.getGroup() + "]");
        } else {
            // Load route and cache it into memory, then delete from metas.
            try {
                //实例化路由组对象，调用loadInto方法，注册路由组中所有的路由
                IRouteGroup iGroupInstance = groupMeta.getConstructor().newInstance();
                iGroupInstance.loadInto(Warehouse.routes);
                Warehouse.groupsIndex.remove(postcard.getGroup());
            } catch (Exception e) {
                throw new HandlerException(TAG + "Fatal exception when loading group meta. [" + e.getMessage() + "]");
            }
            //再次执行completion
            completion(postcard);   // Reload
        }
    } else {
        //设置PostCard Class对象，如果路由是Activity、Service等
        postcard.setDestination(routeMeta.getDestination());
        postcard.setType(routeMeta.getType());
        postcard.setPriority(routeMeta.getPriority());
        postcard.setExtra(routeMeta.getExtra());

        Uri rawUri = postcard.getUri();
        if (null != rawUri) {   // Try to set params into bundle.
            Map<String, String> resultMap = TextUtils.splitQueryParameters(rawUri);
            Map<String, Integer> paramsType = routeMeta.getParamsType();

            if (MapUtils.isNotEmpty(paramsType)) {
                // Set value by its type, just for params which annotation by @Param
                for (Map.Entry<String, Integer> params : paramsType.entrySet()) {
                    setValue(postcard, params.getValue(), params.getKey(), resultMap.get(params.getKey()));
                }

                // Save params name which need auto inject.
                postcard.getExtras().putStringArray(ARouter.AUTO_INJECT, paramsType.keySet().toArray(new String[]{}));
            }

            // Save raw uri
            postcard.withString(ARouter.RAW_URI, rawUri.toString());
        }
        switch (routeMeta.getType()) {
            case PROVIDER:  // if the route is provider, should find its instance
                // Its provider, so it must implement IProvider
                Class<? extends IProvider> providerMeta = (Class<? extends IProvider>) routeMeta.getDestination();
                IProvider instance = Warehouse.providers.get(providerMeta);
                if (null == instance) { // There's no instance of this provider
                    IProvider provider;
                    try {
                        //实例化provider对象
                        provider = providerMeta.getConstructor().newInstance();
                        provider.init(mContext);
                        //缓存Provider
                        Warehouse.providers.put(providerMeta, provider);
                        instance = provider;
                    } catch (Exception e) {
                        throw new HandlerException("Init provider failed! " + e.getMessage());
                    }
                }
                //设置明信片，如果navigation是Provider，返回缓存或者新建的实例
                postcard.setProvider(instance);
                postcard.greenChannel();    // Provider should skip all of interceptors
                break;
            case FRAGMENT:
                postcard.greenChannel();    // Fragment needn't interceptors
            default:
                break;
        }
    }
}
```

navigation如何导航?

1. 从路由表中获取路由信息，设置到Postcard中
2. 根据路由类型执行不同的逻辑

```java
private Object _navigation(final Context context, final Postcard postcard, final int requestCode, final NavigationCallback callback) {
    final Context currentContext = null == context ? mContext : context;

    switch (postcard.getType()) {
        //如果是Activity，则从Postcard取出Intent信息，在主线程调用startActivity
        case ACTIVITY:
            // Build intent
            final Intent intent = new Intent(currentContext, postcard.getDestination());
            intent.putExtras(postcard.getExtras());
            // Set flags.
            int flags = postcard.getFlags();
            if (-1 != flags) {
                intent.setFlags(flags);
            } else if (!(currentContext instanceof Activity)) {    // Non activity, need less one flag.
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            }
            // Set Actions
            String action = postcard.getAction();
            if (!TextUtils.isEmpty(action)) {
                intent.setAction(action);
            }
            // Navigation in main looper.
            runInMainThread(new Runnable() {
                @Override
                public void run() {
                    startActivity(requestCode, currentContext, intent, postcard, callback);
                }
            });
            break;
        //如果是Provider，返回缓存或反射创建的实例
        case PROVIDER:
            return postcard.getProvider();
        //如果是Fragment、Broadcast、ContentProvider则每次都实例化。其中Fragment还需要取出参数，放到Arguments中
        case BOARDCAST:
        case CONTENT_PROVIDER:
        case FRAGMENT:
            Class fragmentMeta = postcard.getDestination();
            try {
                Object instance = fragmentMeta.getConstructor().newInstance();
                if (instance instanceof Fragment) {
                    ((Fragment) instance).setArguments(postcard.getExtras());
                } else if (instance instanceof android.support.v4.app.Fragment) {
                    ((android.support.v4.app.Fragment) instance).setArguments(postcard.getExtras());
                }

                return instance;
            } catch (Exception ex) {
                logger.error(Consts.TAG, "Fetch fragment instance error, " + TextUtils.formatStackTrace(ex.getStackTrace()));
            }
        case METHOD:
        case SERVICE:
        default:
            return null;
    }

    return null;
}
```



Warehouse中保存了路由表和IProvider。以`/`分割Group

```java
class Warehouse {
    //存储IRouteGroup，反射创建IRouteGroup对象，调用loadInto方法，注册路由
    static Map<String, Class<? extends IRouteGroup>> groupsIndex = new HashMap<>();
    //存储路由元数据：例如类对象、Group、path等
    static Map<String, RouteMeta> routes = new HashMap<>();

    //保存Provider实例
    static Map<Class, IProvider> providers = new HashMap<>();
    //保存Provider路由元数据：例如类对象、Group、path等
    static Map<String, RouteMeta> providersIndex = new HashMap<>();

    //保存Interceptor的类对象
    static Map<Integer, Class<? extends IInterceptor>> interceptorsIndex = new UniqueKeyTreeMap<>("More than one interceptors use same priority [%s]");
    //保存Interceptor实例
    static List<IInterceptor> interceptors = new ArrayList<>();
}
```



`Postcard`（明信片）

数据如何注入？

1. 注解生成`类名$$ARouter$$Autowired`类，根据key从Arguments或Bundle中获取数据，设置到target中
2. 调用ARouter的`inject`方法：
   1. 从路由表中找到`AutowiredService`服务，调用`autowire`方法
   2. `autowire`方法中根据传入的对象，反射创建`类名$$ARouter$$Autowired`类的对象，调用inject方法


```java
@Route(path = "/arouter/service/autowired")
public class AutowiredServiceImpl implements AutowiredService {
    private LruCache<String, ISyringe> classCache;
    private List<String> blackList;

    @Override
    public void init(Context context) {
        classCache = new LruCache<>(66);
        blackList = new ArrayList<>();
    }

    @Override
    public void autowire(Object instance) {
        String className = instance.getClass().getName();
        try {
            if (!blackList.contains(className)) {
                ISyringe autowiredHelper = classCache.get(className);
                if (null == autowiredHelper) {  // No cache.
                    autowiredHelper = (ISyringe) Class.forName(instance.getClass().getName() + SUFFIX_AUTOWIRED).getConstructor().newInstance();
                }
                autowiredHelper.inject(instance);
                classCache.put(className, autowiredHelper);
            }
        } catch (Exception ex) {
            blackList.add(className);    // This instance need not autowired.
        }
    }
}
```



使用withObject传递对象，ARouter需要设置Json解析服务，否则传递参数会报空指针：`SerializationService.object2Json(java.lang.Object)' on a null object reference`
