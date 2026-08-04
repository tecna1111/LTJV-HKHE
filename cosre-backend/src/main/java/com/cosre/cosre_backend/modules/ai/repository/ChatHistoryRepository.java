package com.cosre.cosre_backend.modules.ai.repository;


import com.cosre.cosre_backend.modules.ai.entity.ChatHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
@Repository
public interface ChatHistoryRepository 
   extends JpaRepository<ChatHistory,Long> {

}
