package com.order.springboottesttransaction.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.order.springboottesttransaction.dto.OrderAddDto;
import com.order.springboottesttransaction.entity.OrderDetail;
import com.order.springboottesttransaction.entity.OrderMaster;
import com.order.springboottesttransaction.entity.ProductInfo;
import com.order.springboottesttransaction.enums.AppCode;
import com.order.springboottesttransaction.enums.OrderStatusEnums;
import com.order.springboottesttransaction.enums.ProductStatusEnums;
import com.order.springboottesttransaction.exception.APIException;
import com.order.springboottesttransaction.mapper.ProductInfoMapper;
import com.order.springboottesttransaction.service.OrderDetailService;
import com.order.springboottesttransaction.service.OrderMasterService;
import com.order.springboottesttransaction.mapper.OrderMasterMapper;
import com.order.springboottesttransaction.utils.BeanConvertUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * https://blog.csdn.net/u013919153/article/details/116045229
* @author Administrator
* @description 针对表【order_master】的数据库操作Service实现
* @createDate 2022-09-25 18:06:32
*/
@Transactional(rollbackFor = {RuntimeException.class, Exception.class})
@Service
public class OrderMasterServiceImpl extends ServiceImpl<OrderMasterMapper, OrderMaster>
    implements OrderMasterService{

    @Autowired
    OrderMasterMapper orderMasterMapper;

    @Autowired
    ProductInfoMapper productInfoMapper;

    @Autowired
    OrderDetailService orderDetailService;

    @Override
    public Integer createOrder(OrderAddDto orderAddDto) {

        Integer  orderid=0;
        try {
            // 订单总金额
            BigDecimal amount = BigDecimal.ZERO;
            // 订单详情PO
            List<OrderDetail> orderDetails = new ArrayList<OrderDetail>();

            // 从ids中查找所有商品信息
            for (OrderDetail orderDetail : orderAddDto.getOrderDetails()) {
                ProductInfo productInfo = productInfoMapper.selectById(orderDetail.getProductId());
                if (null == productInfo || ProductStatusEnums.DOWN.getCode() == productInfo.getProductStatus()) {
                    throw new APIException(AppCode.PRODUCT_NOT_EXIST, "上架商品中无法查询到：" + orderDetail.getProductId());
                }
                // 计算订单总金额
                amount = amount.add(productInfo.getProductPrice()
                        .multiply(new BigDecimal(orderDetail.getProductNumber())));

                // 设置订单详情Po
                BeanUtils.copyProperties(productInfo, orderDetail);
                orderDetails.add(orderDetail);
            }

            // 设置主订单，状态是未支付
            OrderMaster orderMaster = BeanConvertUtils.convertTo(orderAddDto, OrderMaster::new);
            orderMaster.setOrderAmount(amount);
            orderMaster.setStatus(OrderStatusEnums.NO_PAY.getCode());
            orderMaster.setCreateUser("admin");
            orderMaster.setUpdateUser("admin");
            save(orderMaster);
            Integer a=0;
            Integer b=0;
            Integer c=0;
            c=a/b;//制造一个异常，测试mybatisplus 的回滚

            // 设置detail的order主键
            orderDetails.stream().forEach(p -> p.setOrderId(orderMaster.getOrderId()));
            orderDetailService.saveBatch(orderDetails);
            orderid= orderMaster.getOrderId();

        } catch (Exception e) {
            e.printStackTrace();
            //设置手动回滚
            //这里注释掉了，则不会回滚数据了
            TransactionAspectSupport.currentTransactionStatus()
                    .setRollbackOnly();

        }
        finally {

            return orderid;
        }


    }
}




