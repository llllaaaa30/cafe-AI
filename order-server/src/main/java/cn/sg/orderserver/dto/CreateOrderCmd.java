package cn.sg.orderserver.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;

/**
 * 创建订单请求对象
 * 包含下单所需的基本信息
 */
@Data
@RequiredArgsConstructor
public class CreateOrderCmd {
    
    /**
     * 会话ID，用于标识用户会话
     */
    @NotBlank(message = "用户ID")
    private final String userId;

    /**
     * 用户名称
     */
    private final String userName;

    /**
     * 用户手机号
     */
    private final String userPhone;

    @NotBlank(message = "商品价格")
    private final String price;

    @NotBlank(message = "商品名称")
    private final String itemName;
}