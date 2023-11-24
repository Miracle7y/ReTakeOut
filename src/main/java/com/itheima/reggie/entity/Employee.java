package com.itheima.reggie.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 员工实体类
 */
@Data
public class Employee implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;                //员工编号

    private String username;        //账号名

    private String name;            //员工名

    private String password;        //密码

    private String phone;           //电话号码

    private String sex;             //员工性别

    private String idNumber;        //身份证

    private Integer status;         //账号状态

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;       //创建时间

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;       //更新时间

    @TableField(fill = FieldFill.INSERT)
    private Long createUser;                //创建人ID

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateUser;                //更新人ID

}