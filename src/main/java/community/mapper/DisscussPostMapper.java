package community.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import community.entity.DiscussPost;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DisscussPostMapper extends BaseMapper<DiscussPost> {

    //userId默认为0，表示查询所有，当userId不为0，为用户个人主页查询我的帖子
    List<DiscussPost> selectDiscussPosts(int userId, int offSet, int limit,int orderMode);

    //查询帖子总数 如果在sql中需要用到动态条件并且方法只有一个参数，一定需要@Param
    int selectDiscussPostRows(@Param("userId") int userId);

    //增加帖子方法
    int insertDiscussPost(DiscussPost discussPost);

    //查询帖子详情
    DiscussPost slelectDiscussPostById(int id);

    //更新帖子评论数量
    int updateCommentCount(int id,int commentCount);

    // 修改帖子类型
    int updateType(int id,int type);

    //修改帖子状态
    int updateStatus(int id,int status);

    // 修改帖子分数
    int updateScore(int id , double score);

}
