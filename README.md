# websql

#### 常见问题交流

后台私信，邮件平时很少登录查看，特此创建一个群，方便大家交流。

![输入图片说明](https://foruda.gitee.com/images/1674874677305767312/2d168845_1509614.png)

#### 介绍

websql v2JAVA语言开发,H2内嵌管理数据库。重构底层，执行效率更快、完全开源、体积小傻瓜式,开箱即用。 ———简约而不简单

支持动态配置`多数据源`,`权限控制`,在线`执行sql`，常用`sql文本实时获取`,`导出`结果集、可控的`日志记录`，生产环境`数据`,`openapi`，
同步测试环境等功能；众多功能集一身的`SQL在线执行工具`。

#### 支持的数据库

    Oracle
    Mysql
    H2
    postgresql
    sqlite
    sqlserver

#### 软件架构

前端框架：layui,cy-ui

后端框架：springboot2.1.5

数据层：jpa,H2内嵌数据库,druid

权限框架：sa-token

JSON处理：fastjson

验证码：easyCaptcha

编辑器：codeMirror

工具包： hutool

#### 功能介绍

![输入图片说明](https://images.gitee.com/uploads/images/2019/0706/114810_62a5b9c9_1509614.png "1.png")
![输入图片说明](https://images.gitee.com/uploads/images/2019/0706/115207_935b9c0c_1509614.png "00.png")

1. 数据源管理

![输入图片说明](https://images.gitee.com/uploads/images/2019/0706/114920_6b8b4578_1509614.png "3.png")

数据源动态配置多种数据库连接进行入池。

系统管理-系统设置中数据源选项控制是否项目启动时进行加载数据源。

项目启动时加载连接数据源确保网络通畅,手动加载需每次项目启动后手动进行加载。手动加载可避免项目启动时数据源未加载成功无通知等问题

2. SQL管理

![输入图片说明](https://images.gitee.com/uploads/images/2019/0706/114943_ac844114_1509614.png "4.png")

![输入图片说明](https://images.gitee.com/uploads/images/2019/0706/115005_79aec273_1509614.png "6.png")

SQL窗口我们每天都会用的功能,它强大无比;ctrl键智能提示,多行SQL查询 ","
分割或换行,多行查询结果集导出,动态获取已保存的SQL文本。使用三步: 选择数据源 > 输入脚本 > 执行  
SQL列表由SQL窗口内F9保存SQL文本,并在SQL列表展示、删除。

3.作业管理
![输入图片说明](https://images.gitee.com/uploads/images/2019/0919/181025_5e6288d1_1509614.png "10.png")

定时任务执行脚本,可跨库同步数据结果并展示；可选同步数据源,根据执行SQL结果插入库表中。真正的便捷执行计划作业，数据同步作业等

3. 日志管理

![输入图片说明](https://images.gitee.com/uploads/images/2019/0706/115108_4ea05dc4_1509614.png "7.png")

执行脚本记录每次在SQL窗口F8执行后,会产生详细可查询

登录系统记录每次账号登录后会产生详细可查询

系统设置中日志记录可控执行操作是否记录日志

系统设置中可全部清空SQL执行脚本记录及登录系统日志

4. 系统管理

![输入图片说明](https://images.gitee.com/uploads/images/2019/0706/115131_4472e186_1509614.png "8.png")

数据库管理可登录内嵌H2操作台可直接操作

连接池管理可查看数据源是否加载配置信息等

5. openapi

支持http调用系统接口形式执行SQL动作 <a href='https://gitee.com/boy_0214/websql/wikis/openapi'>查看示例</a>



#### 应用部署

运行环境：jdk8  
默认端口：80  
访问路径：http://localhost/index  
默认登录账号：admin/admin  
指定端口号： --server.port=8080

    nohup java -jar websql.jar  >>web.log &

##### Docker部署

    docker pull cgycms/websql:2.1
    
    docker run -di --name websql -p 80:80 cgycms/websql:2.1
    
    docker logs websql
