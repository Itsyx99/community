package community;

import community.util.SensitiveFilter;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class SensitiveTest {
    @Autowired
    private SensitiveFilter sensitiveFilter;
    @Test
    public void sensitiveFilter(){
        String s = "这里面可以赌博，可以吸毒，可以开票，哈哈哈哈测试";
        String filter = sensitiveFilter.filter(s);
        System.out.println(filter);

        String s2 = "这★★里面可以★★赌★博★，可以★★吸★★毒★★，可以★开★票★★，哈哈哈哈测试";
        String filter2 = sensitiveFilter.filter(s2);
        System.out.println(filter2);
    }
}
