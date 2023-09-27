package community.service;

import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import community.entity.DiscussPost;
import community.mapper.DisscussPostMapper;
import community.util.SensitiveFilter;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class DiscussPostService {
    @Autowired
    private DisscussPostMapper disscussPostMapper;
    @Autowired
    private SensitiveFilter sensitiveFilter;
    @Autowired
    private RedisTemplate redisTemplate;
    @Value("${caffenie.posts.max-size}")
    private int maxSize;
    @Value("${caffenie.posts.expire-seconds}")
    private int expireSeconds;

    private static final Logger logger = LoggerFactory.getLogger(DiscussPostService.class);

    // Caffenie核心接口:Cache,   LoadingCache,  AsyncLoadingCache

    // 帖子列表的缓存
    private LoadingCache<String,List<DiscussPost>> postListCache;
    // 帖子总数的缓存
    private LoadingCache<Integer,Integer> postRowsCache;

    @PostConstruct
    public void init(){
        //初始化帖子列表缓存
        postListCache = Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterWrite(expireSeconds, TimeUnit.SECONDS)
                .build(new CacheLoader<String, List<DiscussPost>>() {
                    @Override
                    public @Nullable List<DiscussPost> load(@NonNull String key) throws Exception {
                        // 实现访问数据库查数据
                        if(key == null || key.length() == 0){
                            throw new IllegalArgumentException("参数错误!");
                        }
                        String[] params = key.split(":");
                        if(params == null || params.length!=2){
                            throw new IllegalArgumentException("参数错误!");
                        }
                        int offset = Integer.valueOf(params[0]);
                        int limit = Integer.valueOf(params[1]);

                        // 二级缓存 Redis->Mysql
                        // 从redis中查
                        List<DiscussPost> discussPosts = (List<DiscussPost>) redisTemplate.opsForValue().get(key);
                        if(discussPosts != null){
                            logger.debug("load post list from Redis.");
                            return discussPosts;
                        }
                        logger.debug("load post list from DB.");
                        discussPosts = disscussPostMapper.selectDiscussPosts(0, offset, limit, 1);
                        redisTemplate.opsForValue().set(key,discussPosts,300,TimeUnit.SECONDS);
                        return discussPosts;
                    }
                });
        //初始化帖子总数缓存
        postRowsCache = Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterWrite(expireSeconds,TimeUnit.SECONDS)
                .build(new CacheLoader<Integer, Integer>() {
                    @Override
                    public @Nullable Integer load(@NonNull Integer key) throws Exception {
                        // 二级缓存 Redis->Mysql
                        // 从redis中查
                        Integer  rows = (Integer) redisTemplate.opsForValue().get(key.toString());
                        if(rows != null){
                            logger.debug("load post list from Redis.");
                            return rows;
                        }
                        logger.debug("load post list from DB.");
                        rows =  disscussPostMapper.selectDiscussPostRows(key);
                        redisTemplate.opsForValue().set(key.toString(),rows,300,TimeUnit.SECONDS);
                        return rows;
                    }
                });

    }

    public List<DiscussPost> findDisscussPosts(int userId, int offSet, int limit ,int orderMode){
        if(userId == 0 && orderMode == 1){ // 首页热帖才缓存
            return postListCache.get(offSet + ":" + limit);
        }
        logger.debug("load post list from DB.");
        return disscussPostMapper.selectDiscussPosts(userId,offSet,limit,orderMode);
    }

    public int findDisscussPostRows(int userId){
        if(userId == 0){ // 首页查询
            return postRowsCache.get(userId);
        }
        logger.debug("load post rows from DB.");
        return disscussPostMapper.selectDiscussPostRows(userId);
    }

    public int addDisscussPost(DiscussPost discussPost){
        if(discussPost == null){
            throw  new IllegalArgumentException("参数不能为空!");
        }
        //转义HTML标记
        discussPost.setTitle(HtmlUtils.htmlEscape(discussPost.getTitle()));
        discussPost.setContent(HtmlUtils.htmlEscape(discussPost.getContent()));
        // 过滤敏感词
        discussPost.setTitle(sensitiveFilter.filter(discussPost.getTitle()));
        discussPost.setContent(sensitiveFilter.filter(discussPost.getContent()));

        return disscussPostMapper.insertDiscussPost(discussPost);
    }

    //根据id查询帖子
    public DiscussPost findDiscussPostById(int id){
        return disscussPostMapper.slelectDiscussPostById(id);
    }

    //更新评论数量
    public int updateCommentCount(int id,int commentCount){
        return disscussPostMapper.updateCommentCount(id,commentCount);
    }

    // 更新帖子类型
    public int updateType(int id,int type){
        return disscussPostMapper.updateType(id,type);
    }

    // 更新帖子状态
    public int updateStatus(int id,int status){
        return disscussPostMapper.updateStatus(id,status);
    }

    // 更新帖子分数
    public int updateScore(int id,double score){
        return disscussPostMapper.updateScore(id,score);
    }

}
