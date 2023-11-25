package com.itheima.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.reggie.common.Result;
import com.itheima.reggie.dto.SetmealDto;
import com.itheima.reggie.entity.Category;
import com.itheima.reggie.entity.Setmeal;
import com.itheima.reggie.service.CategoryService;
import com.itheima.reggie.service.SetmealDishService;
import com.itheima.reggie.service.SetmealService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 套餐管理
 */
@Slf4j
@RestController
@RequestMapping("/setmeal")
public class SetMealController {

    @Autowired
    private SetmealService setmealService;

    @Autowired
    private SetmealDishService setmealDishService;

    @Autowired
    public CategoryService categoryService;

    /**
     * 分页显示
     * @param page
     * @param pageSize
     * @param name
     * @return
     */
    @GetMapping("/page")
    public Result<Page> page(int page,int pageSize,String name){
        log.info("页面展示");
        //构造分页构造器
        Page<Setmeal> PageInfo = new Page<>(page, pageSize);
        Page<SetmealDto> setmealDtoPage=new Page<>();

        //构造条件构造器
        LambdaQueryWrapper<Setmeal> queryWrapper=new LambdaQueryWrapper<>();
        //添加条件
        queryWrapper.like(name!=null,Setmeal::getName,name);
        queryWrapper.orderByAsc(Setmeal::getCode).orderByDesc(Setmeal::getUpdateTime);
        //分页查询
        setmealService.page(PageInfo, queryWrapper);
        //拷贝除records的其他全部内容
        BeanUtils.copyProperties(PageInfo,setmealDtoPage,"records");
        List<Setmeal> records = PageInfo.getRecords();

        List<SetmealDto> setmealDtoList = records.stream().map(item -> {
            //获取SetmealDto对象
            SetmealDto setmealDto = new SetmealDto();
            //获取套餐id
            Category category = categoryService.getById(item.getCategoryId());

            if (category!=null){
                //设置套餐名字,并将Setmeal对象内容拷贝到setmealDto对象
                setmealDto.setCategoryName(category.getName());
                BeanUtils.copyProperties(item, setmealDto);
            }
            return setmealDto;
        }).collect(Collectors.toList());

        setmealDtoPage.setRecords(setmealDtoList);

        return Result.success(setmealDtoPage);
    }

    /**
     * 新增套餐
     * @param setmealDto
     * @return
     */
    @PostMapping
    public Result<String> save(@RequestBody SetmealDto setmealDto){
        log.info("新增套餐");
        setmealService.saveWithDish(setmealDto);
        return Result.success("添加套餐成功");
    }

    /**
     * 批量删除套餐和套餐菜品关系
     * @param ids
     * @return
     */
    @DeleteMapping
    public Result<String> deleteWithDish(Long[] ids){
        log.info("删除套餐");
        setmealService.deleteWithDish(ids);

        return Result.success("删除成功");
    }

    /**
     * 根据id获得套餐和套餐菜品
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    public Result<SetmealDto> getById(@PathVariable Long id){
        SetmealDto setmealDto=setmealService.getByIdWithDish(id);

        return Result.success(setmealDto);
    }

    /**
     * 修改套餐
     * @param setmealDto
     * @return
     */
    @PutMapping
    public Result<String> update(@RequestBody SetmealDto setmealDto){
        setmealService.updateWithDish(setmealDto);
        return Result.success("修改成功");
    }

    /**
     * 修改套餐状态
     * @param status
     * @param ids
     * @return
     */
    @PostMapping("/status/{status}")
    public Result<String> statusUpdate(@PathVariable Integer status,Long[] ids){
        LambdaUpdateWrapper<Setmeal> updateWrapper=new LambdaUpdateWrapper<>();
        updateWrapper.in(Setmeal::getId,ids).set(Setmeal::getStatus,status);
        setmealService.update(updateWrapper);
        return Result.success("套餐状态修改成功");
    }

    /**
     * 根据条件查询套餐数据
     * @param setmeal
     * @return
     */
    @GetMapping("/list")
    public Result<List<Setmeal>> list(Setmeal setmeal){
        log.info(setmeal.toString());
        LambdaQueryWrapper<Setmeal> queryWrapper=new LambdaQueryWrapper<>();
        queryWrapper.eq(setmeal.getCategoryId()!=null,Setmeal::getCategoryId,setmeal.getCategoryId())
                .eq(setmeal.getStatus()!=null,Setmeal::getStatus,setmeal.getStatus())
                .orderByDesc(Setmeal::getUpdateTime);

        List<Setmeal> list = setmealService.list(queryWrapper);

        return Result.success(list);
    }








}
