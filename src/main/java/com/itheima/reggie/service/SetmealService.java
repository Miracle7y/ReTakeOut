package com.itheima.reggie.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.itheima.reggie.dto.SetmealDto;
import com.itheima.reggie.entity.Setmeal;

public interface SetmealService extends IService<Setmeal> {
    //添加套餐和套餐菜品关系
    void saveWithDish(SetmealDto setmealDto);
    //批量删除套餐和套餐菜品关系
    void deleteWithDish(Long[] ids);
    //查询套餐和套餐菜品关系
    SetmealDto getByIdWithDish(Long id);
    //修改套餐
    void updateWithDish(SetmealDto setmealDto);
}
