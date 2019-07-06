# WEBSQL

#### 介绍
<<<<<<< HEAD
WEBSQL由JAVA语言纯编,H2内嵌数据库。完全开源,体积小,傻瓜式,开箱即用。 ———简约而不简单  

动态配置MYSQL,ORACLE等关系型数据库源,保存SQL文本实时获取,可控日志记录等功能网页版SQL执行工具。  



#### 软件架构
前端框架：Layui,cy-ui  

后端框架：springboot2.1.5  

数据层：JPA,H2内嵌数据库,druid  

权限框架：Shrio  

JSON处理：fastjson  

验证码：EasyCaptcha   

导出：easyexcel  

编辑器：CodeMirror   

缓存：ehcache




#### 功能介绍
![Image text](https://gitee.com/boy_0214/webplsql/readmeFile/index1.png)   

  


1. 数据源管理    

![Image text](https://gitee.com/boy_0214/webplsql/readmeFile/index2.png)  




数据源动态配置MYSQL,ORACLE数据库连接进行入池。  

系统管理-系统设置中数据源选项控制是否项目启动时进行加载数据源。  

项目启动时加载连接数据源确保网络通畅,手动加载需每次项目启动后手动进行加载。手动加载可避免项目启动时数据源未加载成功无通知等问题

        
2. SQL管理

![Image text](https://gitee.com/boy_0214/webplsql/readmeFile/index3.png)  

![Image text](https://gitee.com/boy_0214/webplsql/readmeFile/index4.png)


SQL窗口我们每天都会用的功能,它强大无比;ctrl键智能提示,多行SQL查询 ","分割或换行,多行查询结果集导出,动态获取已保存的SQL文本。使用三步: 选择数据源 > 输入脚本 > 执行  
SQL列表由SQL窗口内F9保存SQL文本,并在SQL列表展示、删除。



3. 日志管理    

![Image text](https://gitee.com/boy_0214/webplsql/readmeFile/index6.png)   

![Image text](https://gitee.com/boy_0214/webplsql/readmeFile/index7.png)    
   




执行脚本记录每次在SQL窗口F8执行后,会产生详细可查询    

登录系统记录每次账号登录后会产生详细可查询    

系统设置中日志记录可控执行操作是否记录日志    

系统设置中可全部清空SQL执行脚本记录及登录系统日志

4. 系统管理       

![Image text](https://gitee.com/boy_0214/webplsql/readmeFile/index8.png)  

    
  

数据库管理可登录内嵌H2操作台可直接操作

连接池管理可查看数据源是否加载配置信息等

5. 账号信息  
默认登录账号：admin/admin  

H2操作台默认地址：jdbc:h2:~/dbfile 账号：sa 密码：123456    

6. 常见错误  
未获取到有效数据源 >查看项目启动时主动初始化数据源,如果没有开启请手动点击加载。如果开启说明项目启动加载数据源时连接超时报错。可手动加载是否能连接,如果连接不上查看数据库是否正常。  


修改个人信息后必须重新登录,缓存中数据并没有更新  

锁屏解锁密码是websql,可自行修改 





#### 使用部署
运行环境：JDK8  
默认端口：80
访问路径：http://localhost/index

jar部署启动命令： nohup java -jar jar名称  >>web.log &  
指定端口号启动：   --server.port=8080 



=======
网页版sql管理工具，执行SQL，管理数据源，常用sql记录，体积小，傻瓜式便捷。

#### 软件架构
软件架构说明


#### 安装教程

1. xxxx
2. xxxx
3. xxxx

#### 使用说明

1. xxxx
2. xxxx
3. xxxx

#### 参与贡献

1. Fork 本仓库
2. 新建 Feat_xxx 分支
3. 提交代码
4. 新建 Pull Request


#### 码云特技

1. 使用 Readme\_XXX.md 来支持不同的语言，例如 Readme\_en.md, Readme\_zh.md
2. 码云官方博客 [blog.gitee.com](https://blog.gitee.com)
3. 你可以 [https://gitee.com/explore](https://gitee.com/explore) 这个地址来了解码云上的优秀开源项目
4. [GVP](https://gitee.com/gvp) 全称是码云最有价值开源项目，是码云综合评定出的优秀开源项目
5. 码云官方提供的使用手册 [https://gitee.com/help](https://gitee.com/help)
6. 码云封面人物是一档用来展示码云会员风采的栏目 [https://gitee.com/gitee-stars/](https://gitee.com/gitee-stars/)
>>>>>>> 9a789672541438a580a07ca3139ca1e0790c5bb2
