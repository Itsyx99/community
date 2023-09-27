package community;

import community.util.MailClient;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@RunWith(SpringRunner.class)
@SpringBootTest
public class MailTests {
    @Autowired
    private MailClient mailClient;
    @Autowired
    private TemplateEngine templateEngine; //thymeleaf模板引擎

    @Test
    void testMail(){
        mailClient.sendMail("862624436@qq.com","Test邮箱发送","Welcome...");
    }

    @Test
    void testHtmlMail(){
        Context context = new Context();
        context.setVariable("username","sunyuxiang");
        String content = templateEngine.process("/mail/demo", context);
        mailClient.sendMail("862624436@qq.com","Test html发送",content);
    }
}
