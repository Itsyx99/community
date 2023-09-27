package community;

import community.entity.DiscussPost;
import community.entity.LoginTicket;
import community.entity.User;
import community.mapper.DisscussPostMapper;
import community.mapper.LoginTicketMapper;
import community.mapper.UserMapper;
import org.apache.ibatis.annotations.Mapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ansi.AnsiOutput;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Date;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest
class CommunityApplicationTests {

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private DisscussPostMapper disscussPostMapper;
    @Autowired
    LoginTicketMapper loginTicketMapper;

    @Test
    void contextLoads() {
        System.out.println(("----- selectAll method test ------"));
        List<User> userList = userMapper.selectList(null);
        userList.forEach(System.out::println);
    }
    @Test
    void selectById(){
        User user = userMapper.selectById(1);
        System.out.println(user);
    }

    @Test
    void selectByname(){
        User user = userMapper.selectByName("aaa");
        System.out.println(user);
    }
    @Test
    void InsertUser(){
        User user = new User();
        userMapper.insertUser(user);
        System.out.println(user);
    }
    @Test
    void testSelectPosts(){
        List<DiscussPost> discussPosts = disscussPostMapper.selectDiscussPosts(149, 0, 10,0);
        for (DiscussPost discussPost : discussPosts) {
            System.out.println(discussPost);
        }

        int count = disscussPostMapper.selectDiscussPostRows(149);
        System.out.println(count);
    }
    @Test
    void testInsertLoginTicket(){
        LoginTicket loginTicket = new LoginTicket();
        loginTicket.setUserId(101);
        loginTicket.setTicket("abc");
        loginTicket.setStatus(0);
        loginTicket.setExpired(new Date(System.currentTimeMillis() + 1000*60*10));
        loginTicketMapper.insertLoginTicket(loginTicket);
    }

    @Test
    void testSelectLoginTicket(){
        LoginTicket loginTicket = loginTicketMapper.selectByTicket("abc");
        System.out.println(loginTicket);

        loginTicketMapper.updateStatus(loginTicket.getTicket(),1);
        loginTicket = loginTicketMapper.selectByTicket("abc");
        System.out.println(loginTicket);
    }
}
