package community.controller;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.PutObjectRequest;
import community.annotation.LoginRequired;
import community.entity.LoginTicket;
import community.entity.User;
import community.mapper.LoginTicketMapper;
import community.service.FollowService;
import community.service.LikeService;
import community.service.UserService;
import community.util.CommunityConstance;
import community.util.CommunityUtil;
import community.util.HostHolder;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.spatial.prefix.tree.S2PrefixTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;

@Controller
@RequestMapping("/user")
public class UserController implements CommunityConstance {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Value("${community.path.upload}")
    private String uploadPath;
    @Value("${community.path.domain}")
    private String domain;
    @Value("${server.servlet.context-path}")
    private String contextPath;
    @Autowired
    private UserService userService;
    @Autowired
    private HostHolder hostHolder;
    @Autowired
    private LikeService likeService;
    @Autowired
    private FollowService followService;
    // 阿里云配置参数
    @Value("${aliyun.key.access}")
    private String accessKey;
    @Value("${aliyun.key.secret}")
    private String secretKey;
    @Value("${aliyun.bucket.header.name}")
    private String headerBucketName;
    @Value("${aliyun.bucket.header.url}")
    private String headerBucketUrl;


    @RequestMapping(path = "/setting",method = RequestMethod.GET)
    @LoginRequired
    public String getSettingPage(){
        return "/site/setting";
    }
    //传到阿里云
    @RequestMapping(path = "/upload/url",method = RequestMethod.POST)
    @LoginRequired
    @ResponseBody
    public String uploadHeaderUrl(MultipartFile headImage, Model model){

        if(headImage == null){
            model.addAttribute("error","您还没有选择图片!");
            return CommunityUtil.getJsonString(500,"你还没有选择图片!");
        }
        String originalFilename = headImage.getOriginalFilename();
        String suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
        //判断后缀是否合理
        if(StringUtils.isBlank(suffix)){
            model.addAttribute("error","文件格式不正确!");
            return CommunityUtil.getJsonString(500,"文件格式不正确!");
        }
        //生成随机文件名
        String filename =  CommunityUtil.generateUUID() + suffix;

        // 创建OSSClient实例。
        OSS ossClient = new OSSClientBuilder().build(headerBucketUrl, accessKey, secretKey);
        try {
            PutObjectRequest putObjectRequest = new PutObjectRequest(headerBucketName, filename,headImage.getInputStream());
            // 上传文件
            ossClient.putObject(putObjectRequest);

        } catch (OSSException | IOException e) {
            logger.error("上传文件失败,服务器发生异常!" + e.getMessage());
            return CommunityUtil.getJsonString(500,"上传失败，服务器繁忙!");
        }finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
        //更新当前用户头像的访问路径
        User user = hostHolder.getUser();
        String headUrl = "https://"+headerBucketName + "." + headerBucketUrl + "/" +filename;
        userService.updateHeader(user.getId(),headUrl);
        return CommunityUtil.getJsonString(0,"上传成功");
    }

    // 废弃 我们传到阿里云
    @RequestMapping(path = "/upload",method = RequestMethod.POST)
    @LoginRequired
    public String uploadHeader(MultipartFile headImage, Model model){

        if(headImage == null){
            model.addAttribute("error","您还没有选择图片!");
            return "/site/setting";
        }
        String originalFilename = headImage.getOriginalFilename();
        String suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
        //判断后缀是否合理
        if(StringUtils.isBlank(suffix)){
            model.addAttribute("error","文件格式不正确!");
            return "/site/setting";
        }
        //生成随机文件名
        String filename =  CommunityUtil.generateUUID() + suffix;
        //确定文件存放路径
        File dest = new File(uploadPath + "/" + filename);
        try {
            headImage.transferTo(dest);
        } catch (IOException e) {
            logger.error("上传文件失败,服务器发生异常!" + e.getMessage());
            throw  new RuntimeException("上传文件失败,服务器发生异常!");
        }
        //更新当前用户头像的访问路径(Web访问路径)
        //http://localhost:8080/community/user/header/xxx.png
        User user = hostHolder.getUser();
        String headUrl = domain + contextPath + "/user/header/" + filename;
        userService.updateHeader(user.getId(),headUrl);
        return "redirect:/index";
    }

    // 废弃 直接访问阿里云
    @RequestMapping(path = "/header/{fileName}",method = RequestMethod.GET)
    public void getHeader(@PathVariable String fileName, HttpServletResponse response){
        // 服务器存放路径
        fileName =  uploadPath + "/" + fileName;
        // 文件后缀
        String suffix = fileName.substring(fileName.lastIndexOf("."));
        // 响应图片
        response.setContentType("image/" + suffix);

        try (   ServletOutputStream outputStream = response.getOutputStream();
                FileInputStream fis = new FileInputStream(fileName);
        ) {
            byte[] buffer = new byte[1024];
            int b = 0;
            while ((b = fis.read(buffer)) != -1){
                outputStream.write(buffer, 0, b);
            }
        } catch (IOException e) {
            logger.error("读取图像失败！"+e.getMessage());
        }

    }

    //修改密码
    @LoginRequired
    @RequestMapping(path = "/updatePassword",method = RequestMethod.POST)
    public String updatePassword(String oldPassword, String newPassword, Model model, @CookieValue("ticket") String ticket){
        Map<String, Object> map = userService.updatePassword(oldPassword, newPassword);
        if(map.containsKey("passwordMsg")){
            model.addAttribute("passwordMsg",map.get("passwordMsg"));
            return "/site/setting";
        }
        // 退出用户
        userService.logout(ticket);
        return "redirect:/login";
    }
    //个人主页
    @RequestMapping(path = "/profile/{userId}",method = RequestMethod.GET)
    public String getProfilePage(@PathVariable("userId")int userId,Model model){
        User user = userService.findUserById(userId);
        if(user == null){
            throw new RuntimeException("该用户不存在!");
        }else {
            //用户
            model.addAttribute("user",user);
            //点赞数量
            int count = likeService.findUserLikeCount(user.getId());
            model.addAttribute("likeCount",count);

            // 查询关注数量
            long followeeCount = followService.findFolloweeCount(userId, ENTITY_TYPE_USER);
            model.addAttribute("followeeCount",followeeCount);
            // 查询粉丝数量
            long followerCount = followService.findFollowerCount(ENTITY_TYPE_USER, userId);
            model.addAttribute("followerCount",followerCount);
            // 查询是否已关注
            boolean hasFollowed = false;
            if (hostHolder.getUser() != null){
               hasFollowed =  followService.hasFollowed(hostHolder.getUser().getId(),ENTITY_TYPE_USER,userId);
            }
            model.addAttribute("hasFollowed",hasFollowed);
            return "/site/profile";
        }
    }

}
