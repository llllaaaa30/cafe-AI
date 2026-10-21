package cn.sg.intelligentcustomerservice.domain.repository;

import cn.sg.intelligentcustomerservice.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 用户仓储接口
 * 基于Spring Data JPA，提供用户实体的数据库访问能力
 */
public interface UserRepository extends JpaRepository<User, String> {
}