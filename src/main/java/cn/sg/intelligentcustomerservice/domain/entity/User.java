package cn.sg.intelligentcustomerservice.domain.entity;


import cn.sg.intelligentcustomerservice.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.util.UUID;

/**
 * 用户实体
 * 映射到数据库 t_user 表，通过UUID生成唯一用户ID
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "t_user")
public class User extends BaseEntity {

    /**
     * 用户唯一标识，由UUID生成（非自增，手动赋值）
     */
    @Id
    @Column(name = "id", length = 64)
    private String id;

    /**
     * 用户姓名
     */
    @Column(name = "name", length = 100)
    private String name;

    /**
     * 用户手机号
     */
    @Column(name = "phone", length = 20)
    private String phone;

    /**
     * 注册一个新用户
     * @param name  用户姓名
     * @param phone 手机号
     * @return 注册成功的用户实体（包含自动生成的UUID作为ID）
     */
    public static User register(String name, String phone) {
        User user = new User();
        user.setId(uid());
        user.setName(name);
        user.setPhone(phone);
        return user;
    }

    /**
     * 生成唯一用户ID
     * @return UUID字符串
     */
    public static String uid() {
        return UUID.randomUUID().toString();
    }

    /**
     * 将用户信息格式化为可读字符串，用于注入到LLM的Prompt中
     * @return 格式化后的用户信息文本
     */
    public String toStr() {
        return "用户ID: " + id + "\n" +
                "用户名：" + name + "\n" +
                "手机号：" + phone;
    }
}