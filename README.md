# community
**社区交流系统**：

**技术栈**：Spring Boot + RabbitMQ + Elasticsearch+ MySQL + Redis + Spring Security

该项目是一个以科研、就业信息为基点的交流平台。目的是便于人员交流前沿研究热点、发布招聘信息，提高信息交流的效率。项目包括用户的注册、登录、帖子发布评论、系统通知、用户私信、数据统计、全局搜索等功能。

**主要内容：**

1、使用了RabbitMQ消息队列，将用户的评论和点赞等通知操作存入消息队列进行消费，对系统进行解耦、削峰。

2、使用了前缀树算法实现对帖子和评论敏感词汇过滤，保证系统信息安全。

3、对热门帖子的内容数据使用Caffeine和Redis构建多级缓存，解决系统可能遇到的缓存雪崩问题并提高系统性能。

4、使用 Quartz 实现分布式场景下任务定时调度功能，定时计算帖子的得分。

5、使用Elasticsearch实现了全文搜索功能，可准确匹配搜索结果，并高亮显示关键词。

6、利用Spring Security实现了权限控制，实现了多重角色、URL级别的权限管理;

7、使用Redis的HyperLogLog、Bitmap数据结构实现网站UV、DAU 的统计功能，节省系统内存空间使用。

**架构**：

![image-20230927164448961](https://blog-itsyx.oss-cn-hangzhou.aliyuncs.com/image-20230927164448961.png)
