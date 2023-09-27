package community;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class QuartTest {

    @Autowired
    private Scheduler scheduler;

    @Test
    public void testDeleteJob(){
        boolean b = false;
        try {
            b = scheduler.deleteJob(new JobKey("alphaJob", "alphaJobGroup"));
        } catch (SchedulerException e) {
            e.printStackTrace();
        }
        System.out.println(b);
    }
}
