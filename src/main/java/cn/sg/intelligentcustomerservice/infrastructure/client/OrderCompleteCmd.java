package cn.sg.intelligentcustomerservice.infrastructure.client;


/**
 * 订单完成命令
 * 封装完成订单所需的参数
 *
 * Created on 2025/11/7.
 */
public record OrderCompleteCmd(
        /**
         * 要完成的订单号
         */
        String orderNumber) {
}