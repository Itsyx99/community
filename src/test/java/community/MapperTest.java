package community;

import community.entity.Message;
import community.mapper.MessageMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest
public class MapperTest {

    @Autowired
    private MessageMapper messageMapper;
    @Test
    public void testSelectLetters(){
        List<Message> messageList = messageMapper.selectConversations(111, 0, 20);
        for (Message message : messageList) {
            System.out.println(message);
        }

        int count = messageMapper.selectConversationCount(111);
        System.out.println(count);

        List<Message> messages = messageMapper.selectLetters("111_112", 0, 10);
        for (Message message : messages) {
            System.out.println(message);
        }

        int letterCount = messageMapper.selectLetterCount("111_112");
        System.out.println(letterCount);

        int unReadCount = messageMapper.selectLetterUnReadCount(131, "111_131");
        System.out.println(unReadCount);
    }
}
