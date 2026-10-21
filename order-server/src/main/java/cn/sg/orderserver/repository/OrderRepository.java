package cn.sg.orderserver.repository;

import cn.sg.orderserver.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, String> {

    List<Order> findAllByOrderByCreateTimeDesc();

    List<Order> findByUserId(String userId);
}