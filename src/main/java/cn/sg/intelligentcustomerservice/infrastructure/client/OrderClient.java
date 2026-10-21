package cn.sg.intelligentcustomerservice.infrastructure.client;


import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * 订单服务Feign客户端
 * 通过OpenFeign声明式调用 order-server（端口8083）的REST接口
 * 用于在智能客服服务中代理转发订单相关操作
 *
 * 接口列表：
 *   orderList()   - 查询所有订单
 *   createOrder() - 创建新订单
 *   userOrder()   - 查询指定用户的订单
 *   complete()    - 完成订单
 *   cancel()      - 取消订单
 *
 * Created on 2025/11/7.
 */
@FeignClient(name = "order-service", url = "http://localhost:8083")
public interface OrderClient {

    /**
     * 查询所有订单列表
     * @return 订单DTO列表
     */
    @PostMapping("/order/list")
    List<OrderDTO> orderList();

    /**
     * 创建新订单
     * @param request 包含用户ID、商品名称、价格的创建请求
     * @return 创建成功的订单详情文本
     */
    @PostMapping("/order/create")
    String createOrder(@RequestBody CreateOrderRequest request);

    /**
     * 查询指定用户的所有订单
     * @param request 包含用户ID的查询请求
     * @return 该用户的订单列表
     */
    @PostMapping("/order/userOrder")
    List<OrderDTO> userOrder(@RequestBody UserOrderRequest request);

    /**
     * 完成指定订单
     * @param cmd 包含订单号的完成命令
     */
    @PostMapping("/order/complete")
    void complete(@RequestBody OrderCompleteCmd cmd);

    /**
     * 取消指定订单
     * @param cmd 包含订单号的取消命令
     * @return 操作结果
     */
    @PostMapping("/order/cancel")
    String cancel(@RequestBody OrderCancelCmd cmd);

    record CreateOrderRequest(String userId, String userName, String userPhone, String price, String itemName) {}
    record UserOrderRequest(String userId) {}
}