package community.event;

import com.alibaba.fastjson.JSONObject;
import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.PutObjectRequest;
import community.entity.DiscussPost;
import community.entity.Event;
import community.entity.Message;
import community.service.DiscussPostService;
import community.service.ElasticsearchService;
import community.service.MessageService;
import community.util.CommunityConstance;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;

// 消费者
@Component
public class EventConsumer implements CommunityConstance {

    private static final Logger logger = LoggerFactory.getLogger(EventConsumer.class);

    @Autowired
    private MessageService messageService;
    @Autowired
    private DiscussPostService discussPostService;
    @Autowired
    private ElasticsearchService elasticsearchService;

    @Value("${wk.image.storage}")
    private String wkImageStorage;
    @Value("${wk.image.command}")
    private String wkImageCommand;

    // 阿里云配置参数
    @Value("${aliyun.key.access}")
    private String accessKey;
    @Value("${aliyun.key.secret}")
    private String secretKey;
    @Value("${aliyun.bucket.share.name}")
    private String headerBucketName;
    @Value("${aliyun.bucket.share.url}")
    private String headerBucketUrl;
    @Autowired
    private ThreadPoolTaskScheduler taskScheduler;

   // 消费提醒事件
    @KafkaListener(topics ={TOPIC_COMMENT,TOPIC_LIKE,TOPIC_FOLLOW})
    public void handlerCommentMessage(ConsumerRecord record){
        if(record == null || record.value() == null){
            logger.error("消息的内容为空!");
            return;
        }
        Event event = JSONObject.parseObject(record.value().toString(),Event.class);
        if(event == null){
            logger.error("消息格式有误!");
            return;
        }
        // 发送站内通知
        Message message = new Message();
        message.setFromId(SYSTEM_ID);
        message.setToId(event.getEntityUserId());
        message.setConversationId(event.getTopic());
        message.setCreateTime(new Date());
        Map<String,Object> content = new HashMap<>();
        content.put("userId",event.getUserId());
        content.put("entityType",event.getEntityType());
        content.put("entityId",event.getEntityId());

        if(!event.getData().isEmpty()){
            for (Map.Entry<String,Object> entry:event.getData().entrySet()){
                content.put(entry.getKey(),entry.getValue());
            }
        }
        message.setContent(JSONObject.toJSONString(content));
        messageService.addMessage(message);
    }

    // 消费发帖事件
    @KafkaListener(topics = {TOPIC_PUBLISH})
    public void handlerPublishMessage(ConsumerRecord record){
        if(record == null || record.value() == null){
            logger.error("消息的内容为空!");
            return;
        }
        Event event = JSONObject.parseObject(record.value().toString(),Event.class);
        if(event == null){
            logger.error("消息格式有误!");
            return;
        }
        DiscussPost post = discussPostService.findDiscussPostById(event.getEntityId());
        elasticsearchService.saveDisscussPost(post);
    }

    // 消费删帖事件
    @KafkaListener(topics = {TOPIC_DELETE})
    public void handlerDeleteMessage(ConsumerRecord record){
        if(record == null || record.value() == null){
            logger.error("消息的内容为空!");
            return;
        }
        Event event = JSONObject.parseObject(record.value().toString(),Event.class);
        if(event == null){
            logger.error("消息格式有误!");
            return;
        }
        elasticsearchService.deleteDiscussPost(event.getEntityId());
    }

    // 消费分享事件
    @KafkaListener(topics = TOPIC_SHARE)
    public void handlerShareMessage(ConsumerRecord record){
        if(record == null || record.value() == null){
            logger.error("消息的内容为空!");
            return;
        }
        Event event = JSONObject.parseObject(record.value().toString(),Event.class);
        if(event == null){
            logger.error("消息格式有误!");
            return;
        }
        String htmlUrl = event.getData().get("htmlUrl").toString();
        String fileName = event.getData().get("fileName").toString();
        String suffix = event.getData().get("suffix").toString();

        String cmd = wkImageCommand + " --quality 75 " + htmlUrl + " " + wkImageStorage + "/" + fileName + suffix;
        try {
            Runtime.getRuntime().exec(cmd);
            logger.info("生成长图成功:"+cmd);
        } catch (IOException e) {
            logger.info("生成长图失败！"+e.getMessage());
        }

        // 启用定时器 ，监视该图片 一旦生成了，则上传至阿里云
        UploadTask task = new UploadTask(fileName,suffix);
       Future future =  taskScheduler.scheduleAtFixedRate(task,1000);
       task.setFuture(future);

    }
    class UploadTask implements Runnable{
        // 文件名称
        private String fileName;
        // 文件后缀
        private String suffix;
        // 启动任务返回值
        private Future future;
        // 开始时间
        private long startTime;
        // 上传次数
        private int uploadTimes;


        public UploadTask(String fileName,String suffix) {
            this.fileName = fileName;
            this.suffix = suffix;
            this.startTime = System.currentTimeMillis();
        }
        public void setFuture(Future future){
            this.future = future;
        }
        @Override
        public void run() {
            // 生成失败
            if (System.currentTimeMillis() - startTime > 30000){
                logger.error("执行时间过长，终止任务:"+fileName);
                future.cancel(true);
                return;
            }
            // 上传失败
            if( uploadTimes > 3){
                logger.error("上传次数过多，终止任务:"+fileName);
                future.cancel(true);
                return;
            }
            String path = wkImageStorage + "/" + fileName + suffix;
            File file = new File(path);
            if(file.exists()){
                logger.info(String.format("开始第%d次上传[%s]",++uploadTimes,fileName));
                // 创建OSSClient实例。
                OSS ossClient = new OSSClientBuilder().build(headerBucketUrl, accessKey, secretKey);
                try {
                    PutObjectRequest putObjectRequest = new PutObjectRequest(headerBucketName, fileName,file);
                    // 上传文件
                    ossClient.putObject(putObjectRequest);
                } catch (OSSException e ) {
                    logger.error("上传文件失败,服务器发生异常!" + e.getMessage());
                } catch (ClientException ce) {
                    logger.error("上传文件失败,服务器发生异常!" + ce.getMessage());
                }finally {
                    if (ossClient != null) {
                        ossClient.shutdown();
                        // 上传成功 取消定时
                        future.cancel(true);
                    }
                }
            }else {
                logger.info("等待图片生成["+fileName+"].");
            }
        }
    }
}
