package cn.sg.intelligentcustomerservice.infrastructure.client;


/**
 * 订单取消命令
 * 封装取消订单所需的参数
 *
 * Created on 2025/11/7.
 *
 * @author chenjn
 */
public record OrderCancelCmd(
        /**
         * 要取消的订单号
         */
        String orderNumber) {
}