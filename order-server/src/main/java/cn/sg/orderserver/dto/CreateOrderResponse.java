package cn.sg.orderserver.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * 创建订单响应对象
 * 返回生成的订单号
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateOrderResponse {
    
    /**
     * 订单号，格式为ORDER+时间戳
     */
    private String orderNumber;
    
    /**
     * 创建成功响应的便捷方法
     * @param orderNumber 订单号
     * @return 成功响应对象
     */
    public static CreateOrderResponse success(String orderNumber) {
        return new CreateOrderResponse(orderNumber);
    }
}