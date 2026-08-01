package com.chatboxai.chat_service.chat.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.chatboxai.chat_service.chat.entity.Conversation;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    /** Danh sách hội thoại của một user, mới hoạt động nhất lên đầu. */
    List<Conversation> findByUserIdOrderByUpdatedAtDesc(String userId);

    /**
     * Tìm hội thoại theo id NHƯNG chỉ trong phạm vi của chủ sở hữu.
     *
     * Đây là điểm quan trọng nhất của repository này. Cách viết ngây thơ là
     * findById(id) rồi mới so sánh userId trong service — chỉ cần một nhánh code
     * quên so sánh là lộ hội thoại của người khác (lỗ hổng IDOR). Nhét userId
     * thẳng vào mệnh đề WHERE thì không có đường nào quên được.
     */
    Optional<Conversation> findByIdAndUserId(Long id, String userId);
}
