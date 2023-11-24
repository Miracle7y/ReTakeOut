package com.itheima.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.reggie.common.Result;
import com.itheima.reggie.entity.Employee;
import com.itheima.reggie.service.EmployeeService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;

@Slf4j
@RestController
@RequestMapping("/employee")
public class EmployeeController {

    @Autowired
    private EmployeeService employeeService;

    /**
     * 员工登录
     * @param request
     * @param employee
     * @return
     */
    @PostMapping("/login")
    public Result<Employee> login(HttpServletRequest request, @RequestBody Employee employee){
        //密码做MD5加密处理
        String password = employee.getPassword();
        password= DigestUtils.md5DigestAsHex(password.getBytes());

        //根据用户名查数据库
        LambdaQueryWrapper<Employee> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Employee::getUsername,employee.getUsername());
        Employee emp = employeeService.getOne(queryWrapper);

        //判断是否查到数据
        if (emp == null){
            return Result.error("登录失败");
        }

        //查到数据，密码比对不一致输出结果
        if (!emp.getPassword().equals(password)){
            return Result.error("密码错误");
        }

        //密码比对成功，查看员工状态，如果是禁用，返回员工已禁用结果
        if (emp.getStatus() == 0){
            return Result.error("员工已禁用");
        }

        //账号正常，登录成功，将员工id存入Session并返回成功结果
        request.getSession().setAttribute("employee",emp.getId());
        return Result.success(emp);

    }

    /**
     * 员工退出
     * @param request
     * @return
     */
    @PostMapping("/logout")
    public Result<String> logout(HttpServletRequest request){
        //清理Session中保存当前登录员工的id
        request.getSession().removeAttribute("employee");

        return Result.success("退出成功");
    }

    /**
     * 新增员工
     * @param employee
     * @return
     */
    @PostMapping
    public Result<String> save(@RequestBody Employee employee){
        log.info("新增员工信息：{}",employee);
        //补全员工信息
        employee.setPassword(DigestUtils.md5DigestAsHex("123456".getBytes()));
/*
        employee.setCreateTime(LocalDateTime.now());
        employee.setUpdateTime(LocalDateTime.now());
        Long empId = (Long) request.getSession().getAttribute("employee");
        employee.setCreateUser(empId);
        employee.setUpdateUser(empId);
*/

        //新增员工
        employeeService.save(employee);


        return Result.success("新增员工成功");
    }

    /**
     * 员工信息查询
     * @param page
     * @param pageSize
     * @param name
     * @return
     */
    @GetMapping("/page")
    public Result<Page> page(int page,int pageSize,String name){
        log.info("page={},pageSize={},name={}",page,pageSize,name);

        //构造分页构造器
        Page pageInfo=new Page(page,pageSize);
        //构造条件构造器
        LambdaQueryWrapper<Employee> queryWrapper=new LambdaQueryWrapper<>();
        //添加过滤条件
        queryWrapper.like(StringUtils.isNotEmpty(name),Employee::getName,name);
        //添加排序条件
        queryWrapper.orderByDesc(Employee::getCreateTime);
        //执行查询
        employeeService.page(pageInfo,queryWrapper);

        return Result.success(pageInfo);
    }

    /**
     * 根据ID修改员工信息
     * @return
     */
    @PutMapping
    public Result<String> update(HttpServletRequest request,@RequestBody Employee employee){
        log.info(employee.toString());
/*
        //从Session中获取当前登录用户ID
        Long empId = (Long) request.getSession().getAttribute("employee");
        //更新员工数据的更改时间和更改人id
        employee.setUpdateTime(LocalDateTime.now());
        employee.setUpdateUser(empId);
*/
        //根据Id更新
        employeeService.updateById(employee);

        return Result.success("账号更新成功");
    }

    /**
     * 根据id查询员工
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    public Result<Employee> getById(@PathVariable Long id){
        log.info("根据id查询员工信息...");
        Employee emp = employeeService.getById(id);

        if (emp != null){
            return Result.success(emp);
        }
        return Result.error("没有查询到对应的员工信息");
    }
}


