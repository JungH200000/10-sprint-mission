package com.sprint.mission.discodeit.entity;

import com.sprint.mission.discodeit.entity.base.BaseUpdatableEntity;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
public class Message extends BaseUpdatableEntity {
    private final Channel channel; // 메시지가 위치한 채널
    private final User author; // 메시지 작성자
    private String content; // 메시지 내용

    private final List<UUID> attachmentIds; // 메세지 첨부파일

    // 생성자
    public Message(Channel channel, User author, String content) {
        this.channel = channel;
        this.author = author;
        this.content = content;
        this.attachmentIds = new ArrayList<>();
    }

    @Override
    public String toString() {
        return "Message{" +
                "id = " + getId() + ", " +
                "channel = " + channel.getId() + ", " +
//                "createdAt = " + getCreatedAt() + ", " +
//                "updatedAt = " + getUpdatedAt() + ", " +
                "author = " + author.getId() + ", " +
                "bytes = " + content +
                "}";
    }

    // getter
    public List<UUID> getAttachmentIds() {
        return attachmentIds.stream().toList();
    }

    // update
    public void updateContent(String content) {
        this.content = content;
    }

    public void addAttachmentId(UUID attachmentId) {
        this.attachmentIds.add(attachmentId);
    }
    public void removeAttachmentId(UUID attachmentId) {
        this.attachmentIds.removeIf(id -> id.equals(attachmentId));
    }
}
