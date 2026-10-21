package cn.sg.orderserver.service.impl;

import cn.sg.orderserver.dto.CreateOrderCmd;
import cn.sg.orderserver.entity.Order;
import cn.sg.orderserver.repository.OrderRepository;
import cn.sg.orderserver.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;

    @Override
    @Transactional
    public String createOrder(CreateOrderCmd cmd) {
        log.info("开始创建订单，用户ID: {}", cmd.getUserId());
        Order order = Order.create(cmd);
        orderRepository.saveAndFlush(order);
        log.info("订单创建成功，订单号: {}", order.getOrderNumber());
        return order.toStr();
    }

    @Override
    public String orderDetail(String orderNumber) {
        return orderRepository.findById(orderNumber)
                .map(Order::toStr)
                .orElse("未查询到订单信息");
    }

    @Override
    @Transactional
    public String cancel(String orderNumber) {
        Order order = orderRepository.findById(orderNumber).orElse(null);
        if (Objects.isNull(order)) {
            return "未查询到订单信息";
        }
        if (order.isRefund()) {
            return "订单已退款";
        }
        if (order.isCompleted()) {
            return "订单已完成，不允许退款";
        }
        order.cancel();
        orderRepository.saveAndFlush(order);
        return "SUCCESS";
    }

    @Override
    public List<Order> orders() {
        return orderRepository.findAllByOrderByCreateTimeDesc();
    }

    @Override
    @Transactional
    public void complete(String orderNumber) {
        orderRepository.findById(orderNumber).ifPresent(order -> {
            order.complete();
            orderRepository.saveAndFlush(order);
        });
    }

    @Override
    public List<Order> userOrder(String userId) {
        return orderRepository.findByUserId(userId);
    }
}