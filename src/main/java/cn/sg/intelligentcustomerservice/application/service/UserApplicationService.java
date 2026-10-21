package cn.sg.intelligentcustomerservice.application.service;


import cn.sg.intelligentcustomerservice.domain.entity.User;
import cn.sg.intelligentcustomerservice.domain.service.UserDomainService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;


/**
 * 用户应用服务
 * 负责用户注册等业务用例的编排，作为接口层与领域层之间的桥梁
 *
 * Created on 2025/11/8.
 */
@Service
@AllArgsConstructor
public class UserApplicationService {
    private final UserDomainService userDomainService;

    /**
     * 用户注册
     * @param name  用户名称
     * @param phone 用户手机号
     * @return 注册成功的用户实体
     */
    public User register(String name, String phone) {
        return userDomainService.register(name, phone);
    }
}