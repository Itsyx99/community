package community.controller;

import community.entity.DiscussPost;
import community.entity.Page;
import community.entity.User;
import community.service.DiscussPostService;
import community.service.LikeService;
import community.service.UserService;
import community.util.CommunityConstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class HomeController implements CommunityConstance {
    @Autowired
    private DiscussPostService discussPostService;

    @Autowired
    private UserService userService;

    @Autowired
    private LikeService likeService;

    @RequestMapping(path = "/index",method = RequestMethod.GET)
    public String getIndexPage(Model model, Page page,@RequestParam(name = "orderMode",defaultValue = "0" ) int orderMode){
        //方法调用之前，springmvc会自动实例化model和Page,并将page注入model
        //所以在thyemleaf中可以直接访问page对象的数据
        page.setRows(discussPostService.findDisscussPostRows(0));
        page.setPath("/index?orderMode="+orderMode);
        List<DiscussPost> list = discussPostService.findDisscussPosts(0, page.getOffset(), page.getLimit(),orderMode);
        List<Map<String,Object>> disscussPosts = new ArrayList<>();
        if(list.size()!=0){
            for (DiscussPost disscussPost : list) {
                Map<String,Object> map = new HashMap<>();
                map.put("post",disscussPost);
                User user = userService.findUserById(disscussPost.getUserId());
                map.put("user",user);
                // 查询点赞数量
                long likeCount = likeService.findEntityLikeCount(ENTITY_TYPE_POST,disscussPost.getId());
                map.put("likeCount",likeCount);
                disscussPosts.add(map);
            }
        }
        model.addAttribute("discussPosts",disscussPosts);
        model.addAttribute("orderMode",orderMode);
        return "/index";
    }

    @RequestMapping(path = "/error",method = RequestMethod.GET)
    public String getErroePage(){
        return "/error/500";
    }
    // 拒绝访问时的提示页面
    @RequestMapping(path = "/denied",method = RequestMethod.GET)
    public String getDeniedPage(){
        return "/error/404";
    }
}
