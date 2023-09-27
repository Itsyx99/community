package community.config;

import community.util.CommunityConstance;
import community.util.CommunityUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

@Configuration
public class SecurityConfig  extends WebSecurityConfigurerAdapter implements CommunityConstance {

    @Override
    public void configure(WebSecurity web) throws Exception {
        web.ignoring().antMatchers("/resources/**");
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {

    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        // 进行授权
        http.authorizeRequests()
                .antMatchers(
                        "/user/setting",
                        "/user/upload",
                        "/discuss/add",
                        "/comment/add/**",
                        "/letter/**",
                        "/notice/**",
                        "/like",
                        "/follow",
                        "/unfollow"
                )
                .hasAnyAuthority(
                        AUTHORITY_USER,
                        AUTHORITY_ADMIN,
                        AUTHORITY_MODERATOR
                ).antMatchers(
            "/discuss/top",
                        "/discuss/wonderful"
                ).hasAnyAuthority(AUTHORITY_MODERATOR)
                .antMatchers("/discuss/delete","/data/**")
                .hasAnyAuthority(AUTHORITY_ADMIN)
                .anyRequest().permitAll()
                .and().cors().disable(); // 禁用cros授权
        // 权限不够时的处理
        http.exceptionHandling().authenticationEntryPoint(new AuthenticationEntryPoint() { // 未登录时处理
            @Override
            public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException e) throws IOException, ServletException {
                String xRequestedWith = request.getHeader("x-requested-with");
                if("XMLHttpRequest".equals(xRequestedWith)){  // 异步请求
                    response.setContentType("application/plain;charset=utf-8");
                    PrintWriter writer = response.getWriter();
                    writer.write(CommunityUtil.getJsonString(403,"你还没有登录!"));
                }else { // 同步请求
                    response.sendRedirect(request.getContextPath() +"/login");
                }

            }
        }).accessDeniedHandler(new AccessDeniedHandler() {  // 权限不足时处理
            @Override
            public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException e) throws IOException, ServletException {
                String xRequestedWith = request.getHeader("x-requested-with");
                if("XMLHttpRequest".equals(xRequestedWith)){  // 异步请求
                    response.setContentType("application/plain;charset=utf-8");
                    PrintWriter writer = response.getWriter();
                    writer.write(CommunityUtil.getJsonString(403,"你没有访问此功能权限!"));
                }else { // 同步请求
                    response.sendRedirect(request.getContextPath() +"/denied");
                }
            }
        });
        // Security 底层默认会拦截/logout请求，执行推出处理
        // 覆盖它默认的逻辑，才能执行我们自己退出的代码
        http.logout().logoutUrl("/securitylogout");

    }
}
