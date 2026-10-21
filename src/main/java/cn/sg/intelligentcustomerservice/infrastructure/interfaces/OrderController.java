package cn.sg.intelligentcustomerservice.infrastructure.interfaces;


import cn.sg.intelligentcustomerservice.infrastructure.client.OrderCancelCmd;
import cn.sg.intelligentcustomerservice.infrastructure.client.OrderClient;
import cn.sg.intelligentcustomerservice.infrastructure.client.OrderCompleteCmd;
import cn.sg.intelligentcustomerservice.infrastructure.client.OrderDTO;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 订单接口控制器
 * 作为智能客服服务的代理层，将对前端的订单操作请求通过OpenFeign转发到order-server（端口8083）
 * 路由前缀：/order
 *
 * Created on 2025/11/7.
 */
@Slf4j
@RestController
@RequestMapping("order")
@AllArgsConstructor
public class OrderController {

    private final OrderClient orderService;

    /**
     * 查询订单列表
     * 转发至order-server查询所有订单
     *
     * @return 订单DTO列表
     */
    @PostMapping("list")
    public List<OrderDTO> list() {
        return orderService.orderList();
    }

    /**
     * 完成订单
     * 转发至order-server将指定订单状态改为已完成
     *
     * @param cmd 包含订单号的完成命令
     * @return SUCCESS
     */
    @PostMapping("complete")
    public String complete(@RequestBody OrderCompleteCmd cmd) {
        orderService.complete(cmd);
        return "SUCCESS";
    }

    /**
     * 取消订单
     * 转发至order-server将指定订单状态改为已退款
     *
     * @param cmd 包含订单号的取消命令
     * @return 操作结果
     */
    @PostMapping("cancel")
    public String cancel(@RequestBody OrderCancelCmd cmd) {
        return orderService.cancel(cmd);
    }

}