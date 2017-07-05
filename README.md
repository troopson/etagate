# etagate
一个HTTP API网关实现，用于HTTP应用的微服务化。




## 主要功能

+ 通过xml的方式定义微服务

+ 支持服务集群的配置

+ 支持权限的控制和自定义

+ 支持服务contextPath的修剪

+ 支持设置不控制权限的链接配置

+ 支持断路器模式设置

+ 支持开发状态下动态指定服务的ip和端口

+ 支持外部访问请求的SSL设置

  ​


## 使用说明

+ 配置一个xml文件，文件中定义各个微服务的地址和说明。示例如下：

```xml

<?xml version="1.0"?>
<gate>
    <!--网关监听的端口，不配置的话默认是80-->
   <property name="port">8088</property>
    <!--静态文件目录，如果所有app都没有route到，最后会选择到静态文件目录--> 
   <property name="static.file.dir">data/html</property> 
    <!--上传文件后，保存的目录，目前暂时只能保存，微服务应用还不能获取到文件-->
   <property name="upload.dir">data/upload</property>
   <!--启动的实例个数，默认为1个，建议与处理器个数相同-->
   <property name="server.instance">2</property>
   <!--如果作为网站反向代理，可以配置默认的页面地址-->
   <property name="index.page">/auth/login</property>
  
   <!--配置是否需要增加session的支持，默认是true，如果配置为false，权限代理将不可用-->
   <!--property name="session">false</property-->
   <!--配置session超时时间，默认是30分钟，单位ms-->  
   <!-- property name="session.timeout"></property-->
  
   <!--如果需要启用SSL访问，可以配置如下两个属性，不配置不启用，etagate采用java keytool工具管理证书-->
   <!--property name="ssl.keystore">/keys/etagate.keystore</property-->
   <!--property name="ssl.keystore.pass">123456</property-->
   <!--如果是双向验证模式，需要配置如下两个属性，一般网站是单向，配置上面两个就可以了-->
   <!-- property name="ssl.client.keystore">/keys/client.keystore</property-->
   <!-- property name="ssl.client.keystore.pass">123456</property-->
  
   <!--
    权限和访问控制的内容，gate网关会将所有的权限校验和访问控制转发给对应的app进行处理
    app: 完成auth相关功能的app名称，需要在下面的app定义中进行定义
    authentication：用户身份校验的url，如果校验成功，返回一个json对象，json对象中存在successfield字段，就会认为校验通过，返回空或者返回的json中没有successfield字段，会认为不通过
    authorisation：访问控制的url，返回字符串true，认为是有权访问，其他都认为是无权访问
    mainpage：在用户身份校验成功后，自动跳转的页面，如果不配置（比如通过ajax方式发起的请求），直接返回ok字符串
    exclude end：不需要进行访问控制的资源后缀，自动排除了 *.bmp,*.ico,*.gif,*.jpg,*.png,*.woff,*.css,*.js 等文件
    exclude start：不需要进行访问控制的url开头，（不支持通配符）
   -->
 
   <auth app="auth" authentication="/auth/login" authorisation="/auth/checkPermission" mainpage= "/auth/mainpage" successfield="userid">
      <exclude end="**.woff2,*.css2" />
      <exclude start="/login/,/logout/" />
   </auth>

   <!--
   一个app标签，表示一个微服务，name是访问该微服务的contextPath，网关识别url中第一级，将其作为微服务的名称，相应的转发给该服务进行处理。通过配置
`cutContextPath`，可以设置转发的时候，是否要剪除第一级，默认是不剪除。 `timeout`表示转发请求时候的超时时间，默认值为5000。
   每个app都会接收到url第一级是该app名称的url，此外，可以通过include来匹配其他路径，支持通配符和正则表达式
  balanceStrategy: 当配置有多个node的时候，默认是采用轮流处理请求的方式，也可以通过配置一个NodeStragegy实现，来指定对请求处理的逻辑，目前系统还提供一个WeightNodeStrategy实现，可以通过node节点指定的weight来分发请求。
  timeout: 请求的超时时间，也是断路器的判断超时时间
  maxfail：如果配置了resetsecond，是指断路器的最大失败次数，超过将进入OPEN状态，如果没有配置，是某个节点允许的最大失败次数，超过将被移除。
  resetsecond：断路器进入OPEN状态后，等待多少秒以后，将进入HALF_OPEN状态，如果成功进入CLOSE，否则维持OPEN
-->
   <app name="base">
      <node host="127.0.0.1" port="8082"/>
   </app>
   <app name="auth" cutContextPath="false" timeout="3000">
      <node host="127.0.0.1" port="8080"/>
   </app>
   <app name="eventpersist" cutContextPath="true" timeout="3000" maxfail="5" resetsecond="10000">
      <node host="172.21.9.8" port="7777"/>
      <node host="172.21.9.9" port="7777"/>
   </app>      
  
   <app name="waweb" balanceStrategy="org.etagate.app.WeightNodeStrategy">
      <node host="172.21.9.15" port="8080" weight="1"/>
      <node host="172.21.9.16" port="8080" weight="3"/>
      <include path="/static*"/>
      <include path="/risk/*"/>
      <include path="/event/*"/>
   </app>

</gate>
```

+  启动API网关的命令

  通过gradle jar打包出一个fatjar，然后运行一下命令启动API网关

  `java -jar etagate-fat.jar  -gfile conf/gate.xml`

  或者

  `java -jar etagate-fat.jar  -gfile http://you.config.server/gate.xml`



## 关于权限控制

api网关本身不处理权限，而是将权限委托给鉴权的app节点进行处理。如果没有定义`<auth>`标签，那么api网关将忽略权限的处理。

### 身份认证
网关对所有请求进行分析，如果url和 authentication 的值一样，那么网关将认为是登录操作，并对请求结果进行拦截处理。处理的逻辑如下：

+ 请求必须返回一个json对象，我们叫这个json对象为用户principal，该对象中，必须包含了successfield所指定的key值（可以为false以外的任何值）

+ 如果不满足上一条，认为是身份认证失败，请求直接返回401状态码。




身份认证成功后，会自动跳转到mainpage所指定的页面上去，可以在json对象中设置mainpage属性，这样可以覆盖gate.xml中配置的mainpage，如果网关没有找到mainpage信息，那么直接返回一个ok字符串。

#### 访问的权限控制
每次收到url请求的时候，会先通过非授权列表的过滤，url不需要授权，那么直接转发给对应的服务器节点，如果需要授权，那么先把该url地址发送到鉴权app上进行权限校验。

权限校验的方式是通过rest api请求，请求地址为authorisation配置的地址，参数包括：身份认证时，返回的用户principal，还包括一个名称为permission的参数，参数的值即为要访问的url。

如果权限校验的rest请求返回true，那么认为有权访问，然后api网关将把请求转发给对应的app进行处理。其他认为是无权访问，request直接返回403状态码。

#### 请求中的用户身份信息
每次api网关转发一个请求的时候，都会把用户身份信息放到HTTP Request的Header中，名称为 `gate_principal`，值是用户principal对象序列化后的字符串。在app中，如果需要获取用户信息，可以从Header中得到值，并反序列化为json对象。此外，如果json中带有中文，请先将字符从`ISO8859-1`转为`UTF-8`。如下所示：

```java
String principal = request.getHeader("gate_principal");
if(principal==null)
	throw new Exception("no principal found, not login");
String s = new String(principal.getBytes("ISO8859-1"),"UTF-8");
JSONObject json = JSON.parseObject(s);
```




## 关于断路器设置
在app标签上，配置 了`maxfail` 和`resetsecond`属性后，针对该app的访问，将启用断路器保护模式。
如果请求失败次数，超过`maxfail`设置的次数，该服务器将进入保护模式，后续请求将不在转发到该节点。在等待`resetsecond`秒后，网关会将该服务器设置为HAL_OPEN状态，并尝试发送请求到该节点，如果失败，节点继续处于保护模式，成功则切换到正常的模式。

仅仅配置`maxfail`, 不会启用断路器模式，在访问目标节点失败次数超过maxfail后，该目标节点将从服务器列表中移除。

设置处于dev模式的app，不会启用断路器。


## 对开发过程的支持
在有多人开发同一个模块的时候，可以每个人机器上部署一个api网关，然后各自配置自己的api网关，另外还有一种模式是，在服务器上部署一个api网关，然后针对正在开发的模块，配置 属性 dev="true"。

```xml

    <app name="waweb" dev="true" timeout="90000">
        <node host="172.18.4.44" port="8080"/>
    </app>   
 
```
访问时，在请求中加上参数动态指定服务器地址：
`http://apigate.com:8888/waweb/index.html?waweb=192.168.112.12:8000`
对开发人员来说，通过这种机制，可以将api网关服务器上面的请求，转发到自己开发机器上进行调试。




## 开发路线图

1. 目前对上传的文件，app尚不能获取，下一步将提供接口，让app可以获取到上传的文件；
2. 提供节点的动态添加和移除的API， 提供获取各个app节点IP和端口的API；
3. 增加插件系统，使用者可以定义插件完成自定义的功能。


## 技术支持

QQ：122259023   email：troopson@163.com



