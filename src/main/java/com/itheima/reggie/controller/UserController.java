package com.itheima.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.itheima.reggie.common.Result;
import com.itheima.reggie.entity.User;
import com.itheima.reggie.service.UserService;
import com.itheima.reggie.utils.SMSUtils;
import com.itheima.reggie.utils.ValidateCodeUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 用户管理
 */
@Slf4j
@RestController
@RequestMapping("/user")
public class UserController {
    @Autowired
    private UserService userService;
    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 发送手机验证码
     * @param user
     * @return
     */
    @PostMapping("/sendMsg")
    public Result<String> sendMsg(@RequestBody User user,HttpSession httpSession){
        //获取手机号
        String phone = user.getPhone();
        if (StringUtils.isNotEmpty(phone)){
            //生成随机的4位验证码
            String code = ValidateCodeUtils.generateValidateCode(4).toString();
            log.info("code={}",code);

            //调用阿里云的短信服务API完成发送短信
            //SMSUtils.sendMessage("TakeOut","验证码短信",phone,code);

            //保存验证码到Session
            //httpSession.setAttribute(phone,code);

            //将生成的验证码缓存到Redis中，并设置有效期为5分钟
            redisTemplate.opsForValue().set(phone,code,5, TimeUnit.MINUTES);
            return Result.success("验证码发送成功");
        }

        return Result.error("短信发送失败");

    }

    /**
     * 移动端用户登录
     * @param map
     * @return
     */
    @PostMapping("/login")
    public Result<User> login(@RequestBody Map map, HttpSession session){
        log.info("登录，{}",map);
        //获取手机号和验证码
        String phone = map.get("phone").toString();
        String code = map.get("code").toString();
        /*从Session中获取验证码---1
        //从Session中获取保存的验证码
        Object sessionCode = session.getAttribute(phone);
        */

        //从Redis中获取验证码---2
        String codeInRedis = (String) redisTemplate.opsForValue().get(phone);
        //验证码比对
        if (!code.equals(codeInRedis)){
            //比对不成功，退出
            return Result.error("验证码已过期或错误");
        }

        //判断当前手机号是否是新用户，新用户自动注册
        LambdaQueryWrapper<User> queryWrapper=new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getPhone,phone);
        User user = userService.getOne(queryWrapper);
        if (user==null){
            user=new User();
            user.setPhone(phone);
            user.setStatus(1);
            userService.save(user);
        }
        session.setAttribute("user",user.getId());
        //比对成功，删除Redis中的验证码。
        redisTemplate.delete(phone);

        return Result.success(user);
    }



}
