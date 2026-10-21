package cn.sg.intelligentcustomerservice.infrastructure.interfaces;


import cn.sg.intelligentcustomerservice.application.service.UserApplicationService;
import cn.sg.intelligentcustomerservice.domain.entity.User;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户接口控制器
 * 提供用户注册等操作的REST API
 * 路由前缀：/user
 */
@Slf4j
@RestController
@RequestMapping("user")
@AllArgsConstructor
public class UserController {

    private final UserApplicationService userApplicationService;

    /**
     * 用户注册
     * @param cmd 包含用户名和手机号的注册命令
     * @return 注册成功的用户实体
     */
    @PostMapping("register")
    public User register(@RequestBody RegisterCMD  cmd) {
        return userApplicationService.register(cmd.name(), cmd.phone());
    }

    /**
     * 注册命令（Java 14 Record）
     */
    public record RegisterCMD(String name, String phone) {
    }

}