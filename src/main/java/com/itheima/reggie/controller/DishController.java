package com.itheima.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.reggie.common.Result;
import com.itheima.reggie.dto.DishDto;
import com.itheima.reggie.entity.Category;
import com.itheima.reggie.entity.Dish;
import com.itheima.reggie.entity.DishFlavor;
import com.itheima.reggie.entity.Setmeal;
import com.itheima.reggie.service.CategoryService;
import com.itheima.reggie.service.DishFlavorService;
import com.itheima.reggie.service.DishService;
import com.itheima.reggie.service.SetmealService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 菜品管理
 */
@Slf4j
@RestController
@RequestMapping("/dish")
public class DishController {

    @Autowired
    private DishService dishService;
    @Autowired
    private DishFlavorService dishFlavorService;
    @Autowired
    private CategoryService categoryService;
    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 新增菜品
     * @param dishDto
     * @return
     */
    @PostMapping
    public Result<String> save(@RequestBody DishDto dishDto){
        log.info("添加菜品内容：{}",dishDto.toString());
        //清理redis中的缓存数据
        String key="dish_"+dishDto.getCategoryId()+"_1";
        redisTemplate.delete(key);
        dishService.saveWithFlavor(dishDto);
        return Result.success("添加成功");
    }

    /**
     * 菜品信息分页查询
     * @param page
     * @param pageSize
     * @param name
     * @return
     */
    @GetMapping("/page")
    public Result<Page> page(int page,int pageSize,String name){
        log.info("菜品信息分页查询");
        //构造分页构造器
        Page<Dish> pageInfo = new Page<>(page, pageSize);
        Page<DishDto> dishDtoPage=new Page<>();

        //构造条件构造器
        LambdaQueryWrapper<Dish> queryWrapper=new LambdaQueryWrapper<>();
        //添加过滤条件
        queryWrapper.like(name!=null,Dish::getName,name);
        //添加排序条件
        queryWrapper.orderByAsc(Dish::getSort).orderByDesc(Dish::getUpdateTime);
        //执行分页查询
        dishService.page(pageInfo,queryWrapper);

        //对象拷贝pageInfo->ishDtoPage,忽略records属性
        BeanUtils.copyProperties(pageInfo,dishDtoPage,"records");
        //获取pageInfo的Records属性的内容
        List<Dish> records = pageInfo.getRecords();

        //通过流操作，遍历records,去获取DishDto类型的对象并返回
        List<DishDto> list = records.stream().map((item) -> {
            //获取DishDto对象
            DishDto dishDto = new DishDto();
            //获取分类id
            Long categoryId = item.getCategoryId();//分类id
            //根据分类id获得category对象
            Category category = categoryService.getById(categoryId);
            if (category!=null){
                //为dishDto对象设置分类名
                dishDto.setCategoryName(category.getName());
                //将每一个item的值拷贝到disDto中，补充disDto的属性
                BeanUtils.copyProperties(item, dishDto);
            }

            return dishDto;
        }).collect(Collectors.toList());
        //完善dishDtoPage的record的属性内容
        dishDtoPage.setRecords(list);

        return Result.success(dishDtoPage);
    }

    /**
     * 根据id查询菜品信息和口味信息
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    public Result<DishDto> getById(@PathVariable Long id){
        log.info("页面回显");
        DishDto dishDto= dishService.getByIdWithFlavor(id);
        return Result.success(dishDto);
    }

    /**
     * 修改菜品信息和口味信息
     * @param dishDto
     * @return
     */
    @PutMapping
    public Result<String> update(@RequestBody DishDto dishDto){
        log.info("修改菜品内容：{}",dishDto.toString());
        //清理redis中的缓存数据
        String key="dish_"+dishDto.getCategoryId()+"_1";
        redisTemplate.delete(key);
        dishService.updateWithFlavor(dishDto);

        return Result.success("修改成功");
    }

    /**
     * 根据ID批量删除菜品和口味信息
     * @param ids
     * @return
     */
    @DeleteMapping
    public Result<String> delete(Long[] ids){

        dishService.deleteWithFlavor(ids);
        return Result.success("删除成功");
    }


    /**
     * 根据ID批量修改菜品状态
     * @param ids
     * @return
     */
    @PostMapping("/status/{status}")
    public Result<String> statusUpdate(@PathVariable Integer status,Long[] ids){
        List<Long> list = Arrays.asList(ids);
        //构造条件构造器
        LambdaUpdateWrapper<Dish> updateWrapper = new LambdaUpdateWrapper<>();
        //添加过滤条件
        updateWrapper.set(Dish::getStatus,status).in(Dish::getId,list);
        dishService.update(updateWrapper);

        return Result.success("状态修改成功");
    }
/*
    *//**
     * 查询菜品回显
     * @param categoryId
     * @return
     *//*
    @GetMapping("/list")
    public Result<List<Dish>> list(Long categoryId,String name){
        //构造条件构造器
        LambdaQueryWrapper<Dish> queryWrapper=new LambdaQueryWrapper<>();
        //添加条件
        queryWrapper.like(name!=null,Dish::getName,name);
        queryWrapper.eq(Dish::getCategoryId,categoryId)
                .eq(Dish::getStatus,1);
        queryWrapper.orderByAsc(Dish::getSort).orderByDesc(Dish::getUpdateTime);
        //查询
        List<Dish> list = dishService.list(queryWrapper);
        return Result.success(list);

    }
    */

    /**
     * 查询菜品回显
     * @param dish
     * @return
     */
    @GetMapping("/list")
    public Result<List<DishDto>> list(Dish dish){
        List<DishDto> dishDtoList=null;

        String key="dish_"+dish.getCategoryId()+"_"+dish.getStatus();
        //从Redis中获取菜品缓存数据
        dishDtoList = (List<DishDto>) redisTemplate.opsForValue().get(key);
        if (dishDtoList !=null){
            //如果Redis中存在对应缓存数据，直接将数据返回
            return Result.success(dishDtoList);
        }
        //如果缓存不存在，则在数据库中查找数据，并将数据返回前端及加入Redis缓存管理
        //构造条件构造器
        LambdaQueryWrapper<Dish> queryWrapper=new LambdaQueryWrapper<>();
        //添加条件
        queryWrapper.eq(dish.getCategoryId() !=null,Dish::getCategoryId,dish.getCategoryId())
                .eq(Dish::getStatus,1);
        queryWrapper.orderByAsc(Dish::getSort).orderByDesc(Dish::getUpdateTime);
        //查询到符合条件的Dish对象集合
        List<Dish> list = dishService.list(queryWrapper);
        //获取菜品类别名和菜品口味集合，赋值并获取DishDto对象集合
        dishDtoList = list.stream().map(item -> {
            DishDto dishDto = new DishDto();
            BeanUtils.copyProperties(item, dishDto);
            //根据菜品类别id获取菜品类别名，为DishDto赋值
            Long categoryId = item.getCategoryId();
            Category category = categoryService.getById(categoryId);
            if (category != null) {
                String name = category.getName();
                dishDto.setCategoryName(name);
            }
            //根据菜品id，从口味表中获取对应菜品口味集合，为DishDto赋值
            Long dishId = item.getId();
            LambdaQueryWrapper<DishFlavor> flavorLambdaQueryWrapper=new LambdaQueryWrapper<>();
            flavorLambdaQueryWrapper.eq(DishFlavor::getDishId,dishId);
            List<DishFlavor> flavorList = dishFlavorService.list(flavorLambdaQueryWrapper);
            dishDto.setFlavors(flavorList);
            return dishDto;
        }).collect(Collectors.toList());
        //将数据加入Redis缓存管理并返回前端
        redisTemplate.opsForValue().set(key,dishDtoList,60, TimeUnit.MINUTES);//设置保存缓存有效时间1小时
        return Result.success(dishDtoList);

    }

}
