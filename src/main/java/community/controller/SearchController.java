package community.controller;

import community.entity.DiscussPost;
import community.entity.Page;
import community.service.ElasticsearchService;
import community.service.LikeService;
import community.service.UserService;
import community.util.CommunityConstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class SearchController implements CommunityConstance {
    @Autowired
    private ElasticsearchService elasticsearchService;
    @Autowired
    private UserService userService;
    @Autowired
    private LikeService likeService;

    @RequestMapping(path = "/search",method = RequestMethod.GET)
    public String search(String keyWord, Page page, Model model){
        //搜索帖子
        org.springframework.data.domain.Page<DiscussPost> searchResult = elasticsearchService.searchDiscussPost(keyWord, page.getCurrent() - 1, page.getLimit());
        // 聚合数据 查询帖子用户 点赞数量
        List<Map<String,Object>> discussPosts = new ArrayList<>();
        if(searchResult != null){
            for (DiscussPost post : searchResult) {
                Map<String,Object> map = new HashMap<>();
                // 帖子
                map.put("post",post);
                // 作者
                map.put("user",userService.findUserById(post.getUserId()));
                // 点赞数量
                map.put("likeCount",likeService.findEntityLikeCount(ENTITY_TYPE_POST,post.getId()));
                discussPosts.add(map);
            }
        }
        model.addAttribute("discussPosts",discussPosts);
        model.addAttribute("keyWord",keyWord);
        // 分页信息
        page.setPath("/search?keyWord="+keyWord);
        page.setRows(searchResult == null?0: (int) searchResult.getTotalElements());
        return "/site/search";
    }
}
