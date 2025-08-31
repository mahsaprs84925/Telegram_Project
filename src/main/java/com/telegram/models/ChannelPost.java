package com.telegram.models;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents a scheduled or published post in a channel
 */
public class ChannelPost {
    
    public enum PostType {
        TEXT("Text", "Regular text message"),
        MEDIA("Media", "Image, video, or audio content"),
        POLL("Poll", "Interactive poll"),
        ANNOUNCEMENT("Announcement", "Important announcement with special formatting"),
        FORWARDED("Forwarded", "Forwarded from another channel or chat");
        
        private final String displayName;
        private final String description;
        
        PostType(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }
        
        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
    }
    
    public enum PostStatus {
        DRAFT("Draft", "Post is being edited"),
        SCHEDULED("Scheduled", "Post is scheduled for future publication"),
        PUBLISHED("Published", "Post is live and visible to subscribers"),
        ARCHIVED("Archived", "Post is archived and hidden"),
        DELETED("Deleted", "Post has been deleted");
        
        private final String displayName;
        private final String description;
        
        PostStatus(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }
        
        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
    }
    
    private String postId;
    private String channelId;
    private String authorId; // Admin who created the post
    private String title;
    private String content;
    private PostType postType;
    private PostStatus status;
    private List<String> mediaUrls;
    private List<String> tags; // Hashtags for organization
    private LocalDateTime createdAt;
    private LocalDateTime scheduledAt; // When to publish (null for immediate)
    private LocalDateTime publishedAt; // When it was actually published
    private LocalDateTime lastEditedAt;
    private LocalDateTime modifiedAt; // When the post was last modified
    private String lastEditedBy;
    
    // Analytics data
    private long viewCount;
    private long shareCount;
    private long reactionCount;
    private String thumbnailUrl; // For media posts
    private boolean isPinned;
    private boolean allowComments;
    private boolean sendNotification; // Whether to notify subscribers
    
    // Advanced features
    private String templateId; // Reference to post template
    private String originalPostId; // For forwarded posts
    private String originalChannelId; // For forwarded posts
    private List<String> mentions; // User IDs mentioned in the post
    
    // Constructors
    public ChannelPost() {
        this.postId = UUID.randomUUID().toString();
        this.postType = PostType.TEXT;
        this.status = PostStatus.DRAFT;
        this.mediaUrls = new ArrayList<>();
        this.tags = new ArrayList<>();
        this.mentions = new ArrayList<>();
        this.createdAt = LocalDateTime.now();
        this.viewCount = 0;
        this.shareCount = 0;
        this.reactionCount = 0;
        this.isPinned = false;
        this.allowComments = true;
        this.sendNotification = true;
    }
    
    public ChannelPost(String channelId, String authorId, String title, String content) {
        this();
        this.channelId = channelId;
        this.authorId = authorId;
        this.title = title;
        this.content = content;
    }
    
    public ChannelPost(String channelId, String authorId, String title, String content, PostType postType) {
        this(channelId, authorId, title, content);
        this.postType = postType;
    }
    
    // Utility methods
    public boolean isScheduled() {
        return status == PostStatus.SCHEDULED && scheduledAt != null && scheduledAt.isAfter(LocalDateTime.now());
    }
    
    public boolean isPublished() {
        return status == PostStatus.PUBLISHED;
    }
    
    public boolean isDraft() {
        return status == PostStatus.DRAFT;
    }
    
    public boolean canEdit() {
        return status == PostStatus.DRAFT || status == PostStatus.SCHEDULED;
    }
    
    public void publish() {
        this.status = PostStatus.PUBLISHED;
        this.publishedAt = LocalDateTime.now();
    }
    
    public void schedule(LocalDateTime scheduleTime) {
        this.status = PostStatus.SCHEDULED;
        this.scheduledAt = scheduleTime;
    }
    
    public void archive() {
        this.status = PostStatus.ARCHIVED;
    }
    
    public void delete() {
        this.status = PostStatus.DELETED;
    }
    
    public void addView() {
        this.viewCount++;
    }
    
    public void addShare() {
        this.shareCount++;
    }
    
    public void addReaction() {
        this.reactionCount++;
    }
    
    public void addTag(String tag) {
        if (!tags.contains(tag)) {
            tags.add(tag);
            updateLastEdited();
        }
    }
    
    public void removeTag(String tag) {
        if (tags.remove(tag)) {
            updateLastEdited();
        }
    }
    
    public void addMediaUrl(String mediaUrl) {
        mediaUrls.add(mediaUrl);
        if (postType == PostType.TEXT && !mediaUrls.isEmpty()) {
            postType = PostType.MEDIA;
        }
        updateLastEdited();
    }
    
    public void addMention(String userId) {
        if (!mentions.contains(userId)) {
            mentions.add(userId);
            updateLastEdited();
        }
    }
    
    private void updateLastEdited() {
        this.lastEditedAt = LocalDateTime.now();
    }
    
    public void setLastEditedBy(String userId) {
        this.lastEditedBy = userId;
        updateLastEdited();
    }
    
    /**
     * Get post preview text (truncated content)
     */
    public String getPreview(int maxLength) {
        if (content == null) return "";
        if (content.length() <= maxLength) return content;
        return content.substring(0, maxLength - 3) + "...";
    }
    
    /**
     * Check if post is ready to publish
     */
    public boolean isReadyToPublish() {
        return (content != null && !content.trim().isEmpty()) || !mediaUrls.isEmpty();
    }
    
    /**
     * Get engagement rate (reactions + shares / views)
     */
    public double getEngagementRate() {
        if (viewCount == 0) return 0.0;
        return (double) (reactionCount + shareCount) / viewCount;
    }
    
    // Getters and Setters
    public String getPostId() {
        return postId;
    }
    
    public void setPostId(String postId) {
        this.postId = postId;
    }
    
    public String getChannelId() {
        return channelId;
    }
    
    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }
    
    public String getAuthorId() {
        return authorId;
    }
    
    public void setAuthorId(String authorId) {
        this.authorId = authorId;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
        updateLastEdited();
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
        updateLastEdited();
    }
    
    public PostType getPostType() {
        return postType;
    }
    
    public void setPostType(PostType postType) {
        this.postType = postType;
    }
    
    public PostStatus getStatus() {
        return status;
    }
    
    public void setStatus(PostStatus status) {
        this.status = status;
    }
    
    public List<String> getMediaUrls() {
        return new ArrayList<>(mediaUrls);
    }
    
    public void setMediaUrls(List<String> mediaUrls) {
        this.mediaUrls = new ArrayList<>(mediaUrls);
        updateLastEdited();
    }
    
    public List<String> getTags() {
        return new ArrayList<>(tags);
    }
    
    public void setTags(List<String> tags) {
        this.tags = new ArrayList<>(tags);
        updateLastEdited();
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getScheduledAt() {
        return scheduledAt;
    }
    
    public void setScheduledAt(LocalDateTime scheduledAt) {
        this.scheduledAt = scheduledAt;
    }
    
    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }
    
    public void setPublishedAt(LocalDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }
    
    public LocalDateTime getLastEditedAt() {
        return lastEditedAt;
    }
    
    public void setLastEditedAt(LocalDateTime lastEditedAt) {
        this.lastEditedAt = lastEditedAt;
    }
    
    public String getLastEditedBy() {
        return lastEditedBy;
    }
    
    public long getViewCount() {
        return viewCount;
    }
    
    public void setViewCount(long viewCount) {
        this.viewCount = viewCount;
    }
    
    public long getShareCount() {
        return shareCount;
    }
    
    public void setShareCount(long shareCount) {
        this.shareCount = shareCount;
    }
    
    public long getReactionCount() {
        return reactionCount;
    }
    
    public void setReactionCount(long reactionCount) {
        this.reactionCount = reactionCount;
    }
    
    public String getThumbnailUrl() {
        return thumbnailUrl;
    }
    
    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }
    
    public boolean isPinned() {
        return isPinned;
    }
    
    public void setPinned(boolean pinned) {
        isPinned = pinned;
    }
    
    public boolean isAllowComments() {
        return allowComments;
    }
    
    public void setAllowComments(boolean allowComments) {
        this.allowComments = allowComments;
    }
    
    public boolean isSendNotification() {
        return sendNotification;
    }
    
    public void setSendNotification(boolean sendNotification) {
        this.sendNotification = sendNotification;
    }
    
    public String getTemplateId() {
        return templateId;
    }
    
    public void setTemplateId(String templateId) {
        this.templateId = templateId;
    }
    
    public String getOriginalPostId() {
        return originalPostId;
    }
    
    public void setOriginalPostId(String originalPostId) {
        this.originalPostId = originalPostId;
    }
    
    public String getOriginalChannelId() {
        return originalChannelId;
    }
    
    public void setOriginalChannelId(String originalChannelId) {
        this.originalChannelId = originalChannelId;
    }
    
    public List<String> getMentions() {
        return new ArrayList<>(mentions);
    }
    
    public void setMentions(List<String> mentions) {
        this.mentions = new ArrayList<>(mentions);
    }
    
    // Additional methods needed for DAO compatibility
    public long getCommentCount() {
        // For now, return 0 - would need to implement comment system
        return 0L;
    }
    
    public void setCommentCount(long commentCount) {
        // No-op for now - would need to implement comment system
    }
    
    public LocalDateTime getUpdatedAt() {
        return modifiedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.modifiedAt = updatedAt;
    }
    
    @Override
    public String toString() {
        return "ChannelPost{" +
                "postId='" + postId + '\'' +
                ", channelId='" + channelId + '\'' +
                ", title='" + title + '\'' +
                ", postType=" + postType +
                ", status=" + status +
                ", viewCount=" + viewCount +
                ", publishedAt=" + publishedAt +
                '}';
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ChannelPost that = (ChannelPost) obj;
        return postId.equals(that.postId);
    }
    
    @Override
    public int hashCode() {
        return postId.hashCode();
    }
}
