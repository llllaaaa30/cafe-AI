package cn.sg.intelligentcustomerservice.infrastructure.client;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 订单实体类
 * 表示系统中的订单信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDTO {
    
    /**
     * 订单号，唯一标识
     */
    private String orderNumber;

    /**
     * 用户ID
     */
    private String userId;

    private String userName;

    private String userPhone;

    /**
     * 订单价格
     */
    private String price;

    /**
     * 商品名称
     */
    private String itemName;

    /**
     * 订单状态
     * 0 制作中
     * 1 已完成
     * 2 已退款
     */
    private Integer status;

    /**
     * 订单创建时间
     */
    private LocalDateTime createTime;

}