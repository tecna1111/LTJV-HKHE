package com.cosre.cosre_backend.modules.notification.repository;
import com.cosre.cosre_backend.modules.notification.entity.EmailDelivery;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;
public interface EmailDeliveryRepository extends JpaRepository<EmailDelivery, Long> {
    boolean existsByUserIdAndEventKey(Long userId, String eventKey);
    List<EmailDelivery> findTop100ByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(String status, LocalDateTime time);
}
