package cn.sg.orderserver.controller;


import cn.sg.orderserver.entity.Order;
import cn.sg.orderserver.service.OrderService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 订单REST控制器
 * 提供标准的HTTP REST接口供智能客服服务通过OpenFeign调用
 * 路由前缀：/order
 *
 * 接口列表：
 *   POST /order/list      - 查询所有订单
 *   POST /order/create    - 创建新订单
 *   POST /order/userOrder - 查询指定用户的订单
 *   POST /order/detail    - 查询单个订单详情
 *   POST /order/complete  - 完成指定订单
 *   POST /order/cancel    - 取消指定订单
 *
 * Created on 2025/11/7.
 */
@Slf4j
@RestController
@RequestMapping("order")
@AllArgsConstructor
public class OrderController {
    private final OrderService orderService;

    /**
     * 查询所有订单列表（按创建时间倒序）
     * @return 订单列表
     */
    @PostMapping("list")
    public List<Order> list() {
        log.info("list");
        return orderService.orders();
    }

    /**
     * 创建新订单
     * @param request 包含用户ID、商品名称、价格等信息的创建请求
     * @return 创建成功的订单详情文本
     */
    @PostMapping("create")
    public String create(@RequestBody CreateOrderRequest request) {
        log.info("create order: userId={}, itemName={}", request.userId(), request.itemName());
        cn.sg.orderserver.dto.CreateOrderCmd cmd = new cn.sg.orderserver.dto.CreateOrderCmd(
                request.userId(), request.userName(), request.userPhone(),
                request.price(), request.itemName());
        return orderService.createOrder(cmd);
    }

    /**
     * 查询指定用户的所有订单
     * @param request 包含用户ID
     * @return 该用户的订单列表
     */
    @PostMapping("userOrder")
    public List<Order> userOrder(@RequestBody UserOrderRequest request) {
        log.info("userOrder: userId={}", request.userId());
        return orderService.userOrder(request.userId());
    }

    /**
     * 查询单个订单详情
     * @param request 包含订单号
     * @return 订单详情文本
     */
    @PostMapping("detail")
    public String detail(@RequestBody OrderDetailRequest request) {
        log.info("orderDetail: orderNumber={}", request.orderNumber());
        return orderService.orderDetail(request.orderNumber());
    }

    /**
     * 完成指定订单
     * @param cmd 包含订单号的完成命令
     * @return SUCCESS
     */
    @PostMapping("complete")
    public String complete(@RequestBody CompleteOrderCmd cmd) {
        log.info("complete");
        orderService.complete(cmd.orderNumber());
        return "SUCCESS";
    }

    /**
     * 取消指定订单
     * @param cmd 包含订单号的取消命令
     * @return 操作结果
     */
    @PostMapping("cancel")
    public String cancel(@RequestBody CancelOrderCmd cmd) {
        log.info("cancel");
        return orderService.cancel(cmd.orderNumber());
    }

    record CreateOrderRequest(String userId, String userName, String userPhone, String price, String itemName) {}
    record UserOrderRequest(String userId) {}
    record OrderDetailRequest(String orderNumber) {}
    record CompleteOrderCmd(String orderNumber) {}
    record CancelOrderCmd(String orderNumber) {}

}