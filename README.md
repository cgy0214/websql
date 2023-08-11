# websql

#### 常见问题交流

后台私信，邮件平时很少登录查看，特此创建一个群，方便大家交流。

![输入图片说明](https://foruda.gitee.com/images/1674874677305767312/2d168845_1509614.png)

#### 介绍

websql v3 JAVA语言开发,H2内嵌管理数据库;执行效率更快、完全开源、体积小傻瓜式,开箱即用。 ———简约而不简单

支持动态配置`多数据源`,`权限控制`,在线`执行sql`，常用`sql文本实时获取`,`导出、打印`结果集、可控的`日志记录`，`危险`SQL限制运行，生产环境`数据`同步,`openapi`
ETL等功能;众多功能集一身的`SQL在线执行工具`。

#### 支持的数据库

    oracle
    mysql
    H2
    postgresql
    sqlite
    sqlserver
    dm 达梦
    kingbase8 人大金仓
    oscar     神州通用

#### 软件架构

前端框架：layui

后端框架：springboot

数据层：jpa,druid

权限框架：sa-token

JSON处理：fastjson

验证码：easyCaptcha

编辑器：codeMirror

工具包： huTool

#### 功能介绍

![登录页](https://foruda.gitee.com/images/1691402713565081017/1660e955_1509614.png "登录页")
![首页](https://foruda.gitee.com/images/1691402776952802962/a239856a_1509614.png "首页")

1. 数据源管理

![数据源管理](https://foruda.gitee.com/images/1691402817433211812/1adf327c_1509614.png "数据源管理")

数据源动态配置多种数据库连接进行入池。

系统管理-系统设置中数据源选项控制是否项目启动时进行加载数据源。

项目启动时加载连接数据源确保网络通畅,手动加载需每次项目启动后手动进行加载。手动加载可避免项目启动时数据源未加载成功无通知等问题

2. SQL管理

![SQL执行](https://foruda.gitee.com/images/1691402910993783640/880384b4_1509614.png "SQL执行")

![SQL文本](https://foruda.gitee.com/images/1691402970076842620/2a3bfc13_1509614.png "SQL文本")

SQL窗口我们每天都会用的功能,它强大无比;ctrl键智能提示,多行SQL查询 ","
分割或换行,多行查询结果集导出,动态获取已保存的SQL文本。使用三步: 选择数据源 > 输入脚本 > 执行  
SQL列表由SQL窗口内F9保存SQL文本,并在SQL列表展示、删除。

3.ETL管理
![ETL](https://foruda.gitee.com/images/1691403029285250547/70a86db3_1509614.png "ETL")
![作业任务](https://foruda.gitee.com/images/1691403222695321116/656f4def_1509614.png "作业任务")

定时任务执行脚本,可跨库同步数据结果并展示；可选同步数据源,根据执行SQL结果插入库表中。真正的便捷执行计划ETL抽取，数据同步等

3. 日志管理

![SQL日志](https://foruda.gitee.com/images/1691403288265388238/47751afe_1509614.png "SQL日志")
![登录日志](https://foruda.gitee.com/images/1691403327768784930/1cc8c82d_1509614.png "登录日志")

执行脚本记录每次在SQL窗口F8执行后,会产生详细可查询

登录系统记录每次账号登录后会产生详细可查询



4. 用户管理

![用户管理](https://foruda.gitee.com/images/1691403417331976051/68b538d6_1509614.png "用户管理")

可新增不同用户，赋予不同操作权限登录系统使用


5. 参数设置

![参数设置](https://foruda.gitee.com/images/1691403486151708385/822eef90_1509614.png "参数设置")

系统设置中日志记录可控执行操作是否记录日志

系统设置中可全部清空SQL执行脚本记录及登录系统日志

数据库管理可登录内嵌H2操作台可直接操作

连接池管理可查看数据源是否加载配置信息等

6. 驱动管理

![驱动设置](https://foruda.gitee.com/images/1691403564521878212/433f7e0b_1509614.png "驱动设置")

针对不同数据库类型支持添加不同的驱动信息

7. openapi

支持http调用系统接口形式执行SQL动作 <a href='https://gitee.com/boy_0214/websql/wikis/openapi'>查看示例</a>



#### 应用部署

运行环境：jdk8 / jdk17  
默认端口：80  
访问路径：http://localhost/index  
默认登录账号：admin/admin  
指定端口号： --server.port=8080  
使用内置H2不需要独立安装数据库及创建表结构，系统会自动创建

    nohup java -jar websql.jar  >>web.log &

#### Docker部署

    docker pull cgycms/websql:latest
    
    docker run -di --name websql -p 80:80 cgycms/websql:latest
    
    docker logs websql
