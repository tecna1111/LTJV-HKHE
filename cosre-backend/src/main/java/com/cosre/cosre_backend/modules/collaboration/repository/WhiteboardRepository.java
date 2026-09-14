package com.cosre.cosre_backend.modules.collaboration.repository;

import com.cosre.cosre_backend.modules.collaboration.entity.Whiteboard;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface WhiteboardRepository extends JpaRepository<Whiteboard, Long> {
    Optional<Whiteboard> findByTeamId(Long teamId);
}