package cn.sg.orderserver.entity;

import cn.sg.orderserver.dto.CreateOrderCmd;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "t_order")
public class Order {

    @Id
    @Column(name = "order_number", length = 64)
    private String orderNumber;

    @Column(name = "user_id", length = 64)
    private String userId;

    @Column(name = "user_name", length = 100)
    private String userName;

    @Column(name = "user_phone", length = 20)
    private String userPhone;

    @Column(name = "price", length = 20)
    private String price;

    @Column(name = "item_name", length = 100)
    private String itemName;

    @Column(name = "status")
    private Integer status;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    public static Order create(CreateOrderCmd cmd) {
        Order order = new Order();
        order.setOrderNumber(generateOrderNumber());
        order.setUserId(cmd.getUserId());
        order.setUserName(cmd.getUserName());
        order.setUserPhone(cmd.getUserPhone());
        order.setPrice(cmd.getPrice());
        order.setItemName(cmd.getItemName());
        order.setStatus(0);
        order.setCreateTime(LocalDateTime.now());
        return order;
    }

    private static String generateOrderNumber() {
        return "ORDER" + System.currentTimeMillis();
    }

    public String toStr() {
        String statusStr = status == 0 ? "制作中" : status == 1 ? "已完成" : "已退款";
        return "订单号：" + orderNumber + "\n" +
                "用户姓名：" + userName + "\n" +
                "用户手机号：" + userPhone + "\n" +
                "商品名称：" + itemName + "\n" +
                "价格：" + price + "\n" +
                "订单状态：" + statusStr + "\n" +
                "创建时间：" + createTime;
    }

    public boolean isCompleted() {
        return status == 1;
    }

    public boolean isRefund() {
        return status == 2;
    }

    public void cancel() {
        status = 2;
    }

    public void complete() {
        status = 1;
    }
}