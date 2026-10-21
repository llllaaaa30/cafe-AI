package cn.sg.intelligentcustomerservice.domain.service;


import cn.sg.intelligentcustomerservice.domain.entity.User;
import cn.sg.intelligentcustomerservice.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 用户领域服务
 * 使用MySQL管理用户注册和查询，服务器重启后用户数据不丢失
 */
@Service
@RequiredArgsConstructor
public class UserDomainService {

    private final UserRepository userRepository;

    /**
     * 注册新用户并持久化到数据库
     * @param name  用户姓名
     * @param phone 手机号
     * @return 注册成功的用户实体
     */
    public User register(String name, String phone) {
        User user = User.register(name, phone);
        return userRepository.saveAndFlush(user);
    }

    /**
     * 根据用户ID从数据库获取用户信息
     * @param id 用户ID
     * @return 用户实体，如果不存在则返回null
     */
    public User get(String id) {
        return userRepository.findById(id).orElse(null);
    }

}