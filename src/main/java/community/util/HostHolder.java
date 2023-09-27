package community.util;

import community.entity.User;
import org.springframework.stereotype.Component;

/**
 * 持有用户信息，用于替代session对象
 */
@Component
public class HostHolder {
    private ThreadLocal<User> threadLocal = new ThreadLocal<>();

    public void setUser(User user){
        threadLocal.set(user);
    }
    public User getUser(){
       return threadLocal.get();
    }
    public void clean(){
        threadLocal.remove();
    }
}
