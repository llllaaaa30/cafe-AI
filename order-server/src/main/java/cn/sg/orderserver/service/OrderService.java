package cn.sg.orderserver.service;

import cn.sg.orderserver.dto.CreateOrderCmd;
import cn.sg.orderserver.dto.CreateOrderResponse;
import cn.sg.orderserver.entity.Order;

import java.util.List;

/**
 * 订单服务接口
 * 定义订单相关的业务操作
 */
public interface OrderService {
    
    /**
     * 创建订单
     * @param request 创建订单请求
     * @return 创建订单响应，包含订单号
     */
    String createOrder(CreateOrderCmd request);

    /**
     * 订单详情
     * @param orderNumber 订单号
     * @return 订单详情
     */
    String orderDetail(String orderNumber);

    /**
     * 订单取消
     * @param orderNumber 订单号
     * @return 退款结果
     */
    String cancel(String orderNumber);

    /**
     * 获取所有订单
     * @return 所有订单列表
     */
    List<Order> orders();

    /**
     * 完成订单
     * @param orderNumber 订单号
     */
    void complete(String orderNumber);

    /**
     * 获取用户订单
     * @param userId 用户ID
     * @return 用户订单列表
     */
    List<Order> userOrder(String userId);
}