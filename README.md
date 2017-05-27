# etagate
一个HTTP API网关实现

# 主要功能

+ 通过xml的方式定义微服务
+ 支持服务集群的配置
+ 支持权限的控制和自定义
+ 支持服务contextPath的修剪
+ 支持设置不控制权限的链接配置

# 使用说明

+ 配置一个xml文件，文件中定义各个微服务的地址和说明。示例如下：

```xml

<?xml version="1.0"?>
<gate>
    <!--网关监听的端口，不配置的话默认是80-->
   <property name="port">8088</property>
    <!--静态文件目录，如果所有app都没有route到，最后会选择到静态文件目录--> 
   <property name="static.file.dir">D:/source/phoenix/etagate/html</property> 
    <!--上传文件后，保存的目录，目前暂时只能保存，微服务应用还不能获取到文件-->
   <property name="upload.dir">d:/temp</property>
   
   <!--
    权限和访问控制的内容，gate网关会将所有的权限校验和访问控制转发给对应的app进行处理
    app: 完成auth相关功能的app名称，需要在下面的app定义中进行定义
    authentication：用户身份校验的url，如果校验成功，返回一个json对象，json对象中存在successfield字段，就会认为校验通过，返回空或者返回的json中没有successfield字段，会认为不通过
    authorisation：访问控制的url，返回字符串true，认为是有权访问，其他都认为是无权访问
    exclude end：不需要进行访问控制的资源后缀，
    exclude start：不需要进行访问控制的url开头，（不支持通配符）
   -->
   <auth app="auth" authentication="/auth/login" authorisation="/auth/checkPermission" successfield="userid">
      <exclude end="*.bmp,*.gif,*.jpg,*.png,*.woff,*.css,*.js" />
      <exclude start="/flume/,/flume2/" />
   </auth>

   <!--
   一个app标签，表示一个微服务，name是访问该微服务的contextPath，网关识别url中第一级，将其作为微服务的名称，相应的转发给该服务进行处理。通过配置
`cutContextPath`，可以设置转发的时候，是否要剪除第一级，默认是不剪除。 `timeout`表示转发请求时候的超时时间，默认值为5000。
   每个app都会接收到url第一级是该app名称的url，此外，可以通过include来匹配其他路径，支持通配符和正则表达式
-->
   <app name="base">
      <node host="127.0.0.1" port="8082"/>
   </app>
   <app name="auth" cutContextPath="false" timeout="3000">
      <node host="127.0.0.1" port="8080"/>
   </app>
   <app name="eventpersist" cutContextPath="true">
      <node host="172.21.9.8" port="7777"/>
   </app>      
   <app name="waweb">
      <node host="172.21.9.15" port="8080"/>
      <node host="172.21.9.16" port="8080"/>
      <include path="/static*"/>
      <include path="/a?login"/>
      <include path="/risk/*"/>
      <include path="/event/*"/>
      <include path="/widget/*"/>
      <include path="/api/*"/>
      <include path="/weakNess/*"/>
   </app>

</gate>

```

+  启动API网关的命令
  
  通过gradle jar打包出一个fatjar，然后运行一下命令启动API网关
  
  `java -jar etagate-fat.jar  -gfile conf/gate.xml`
  或者
  `java -jar etagate-fat.jar  -gfile http://you.config.server/gate.xml`




