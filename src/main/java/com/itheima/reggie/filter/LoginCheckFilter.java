package com.itheima.reggie.filter;

import com.alibaba.fastjson.JSON;
import com.itheima.reggie.common.BaseContext;
import com.itheima.reggie.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.AntPathMatcher;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 过滤器
 * 检查用户是否已经完成登录
 */
@Slf4j
@WebFilter(filterName = "LoginCheckFilter",urlPatterns = "/*")
public class LoginCheckFilter implements Filter {
    //路径匹配器，支持通配符
    public static final AntPathMatcher PATH_MATCHER=new AntPathMatcher();
    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest request= (HttpServletRequest) servletRequest;
        HttpServletResponse response= (HttpServletResponse) servletResponse;


        //获取本次请求的URL
        String requestURI = request.getRequestURI();
        log.info("拦截到请求:{}",requestURI);
        //定义不需要处理的请求路径
        String[] urls=new String[]{
                "/employee/login",
                "/employee/logout",
                "/backend/**",
                "/front/**",
                "/user/sendMsg",
                "/user/login"

        };
        //判断本次请求是否需要处理
        boolean check = check(urls, requestURI);
        //如果不需要处理，直接放行
        if (check){
            log.info("本次请求不需要处理，{}",requestURI);
            filterChain.doFilter(request,response);
            return;
        }

        //判断管理端用户登录状态，如果已登录，则直接放行---1
        if (request.getSession().getAttribute("employee") != null) {
            log.info("用户已登录，用户id为：{}",request.getSession().getAttribute("employee"));

            //将当前登录用户id存入线程保存
            Long empId = (Long) request.getSession().getAttribute("employee");
            BaseContext.setCurrentId(empId);

            //放行
            filterChain.doFilter(request,response);
            return;
        }

        //判断客户端用户登录状态，如果已登录，则直接放行---2
        if (request.getSession().getAttribute("user") != null) {
            log.info("用户已登录，用户id为：{}",request.getSession().getAttribute("user"));

            //将当前登录用户id存入线程保存
            Long userId = (Long) request.getSession().getAttribute("user");
            BaseContext.setCurrentId(userId);

            //放行
            filterChain.doFilter(request,response);
            return;
        }

        //如果未登录，返回未登录结果,通过输出流向客户端响应数据
        log.info("用户未登录");
        response.getWriter().write(JSON.toJSONString(Result.error("NOTLOGIN")));
        return;


    }

    /**
     * 路径匹配，检查本次请求是否需要放行
     * @param urls
     * @param requestURI
     * @return
     */
    public boolean check(String[] urls,String requestURI){
        for (String url : urls) {
            boolean match = PATH_MATCHER.match(url, requestURI);
            if (match){
                return true;
            }
        }
        return false;
    }
}
