-- Advanced Groups and Channels Database Schema Updates
-- This file contains all the new tables and modifications needed for the enhanced features

-- ===================================
-- GROUP ROLES AND PERMISSIONS SYSTEM
-- ===================================

-- Table for group roles (must be created first)
CREATE TABLE IF NOT EXISTS group_roles (
    role_id VARCHAR(255) PRIMARY KEY,
    group_id VARCHAR(255) NOT NULL,
    role_name VARCHAR(100) NOT NULL,
    role_type VARCHAR(50) NOT NULL, -- OWNER, ADMIN, MODERATOR, MEMBER, RESTRICTED
    description TEXT,
    color VARCHAR(7) DEFAULT '#999999', -- Hex color code
    is_default BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    modified_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    modified_by VARCHAR(255),
    FOREIGN KEY (group_id) REFERENCES group_chats(group_id) ON DELETE CASCADE,
    FOREIGN KEY (created_by) REFERENCES users(user_id) ON DELETE SET NULL,
    FOREIGN KEY (modified_by) REFERENCES users(user_id) ON DELETE SET NULL
);

-- Table for role permissions (many-to-many)
CREATE TABLE IF NOT EXISTS group_role_permissions (
    role_id VARCHAR(255),
    permission VARCHAR(100), -- Enum values from GroupPermission
    PRIMARY KEY (role_id, permission),
    FOREIGN KEY (role_id) REFERENCES group_roles(role_id) ON DELETE CASCADE
);

-- Add role_id column to existing group_members table if it doesn't exist
ALTER TABLE group_members ADD COLUMN IF NOT EXISTS role_id VARCHAR(255);
ALTER TABLE group_members ADD COLUMN IF NOT EXISTS joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE group_members ADD COLUMN IF NOT EXISTS last_activity TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE group_members ADD COLUMN IF NOT EXISTS invited_by VARCHAR(255);
ALTER TABLE group_members ADD COLUMN IF NOT EXISTS nickname VARCHAR(100);
ALTER TABLE group_members ADD COLUMN IF NOT EXISTS is_muted BOOLEAN DEFAULT FALSE;
ALTER TABLE group_members ADD COLUMN IF NOT EXISTS muted_until TIMESTAMP;
ALTER TABLE group_members ADD COLUMN IF NOT EXISTS is_banned BOOLEAN DEFAULT FALSE;
ALTER TABLE group_members ADD COLUMN IF NOT EXISTS banned_until TIMESTAMP;
ALTER TABLE group_members ADD COLUMN IF NOT EXISTS ban_reason TEXT;
ALTER TABLE group_members ADD COLUMN IF NOT EXISTS promoted_at TIMESTAMP;
ALTER TABLE group_members ADD COLUMN IF NOT EXISTS promoted_by VARCHAR(255);
ALTER TABLE group_members ADD COLUMN IF NOT EXISTS message_count INTEGER DEFAULT 0;
ALTER TABLE group_members ADD COLUMN IF NOT EXISTS last_message_at TIMESTAMP;
ALTER TABLE group_members ADD COLUMN IF NOT EXISTS custom_title VARCHAR(100);
ALTER TABLE group_members ADD COLUMN IF NOT EXISTS total_messages INTEGER DEFAULT 0;
ALTER TABLE group_members ADD COLUMN IF NOT EXISTS total_reactions INTEGER DEFAULT 0;
ALTER TABLE group_members ADD COLUMN IF NOT EXISTS warnings_count INTEGER DEFAULT 0;

-- ===================================
-- CHANNEL POSTS AND CONTENT MANAGEMENT
-- ===================================

-- Table for channel posts
CREATE TABLE IF NOT EXISTS channel_posts (
    post_id VARCHAR(255) PRIMARY KEY,
    channel_id VARCHAR(255) NOT NULL,
    author_id VARCHAR(255) NOT NULL,
    title VARCHAR(500),
    content TEXT,
    post_type VARCHAR(50) DEFAULT 'TEXT', -- TEXT, MEDIA, POLL, ANNOUNCEMENT, FORWARDED
    status VARCHAR(50) DEFAULT 'DRAFT', -- DRAFT, SCHEDULED, PUBLISHED, ARCHIVED, DELETED
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    scheduled_at TIMESTAMP,
    published_at TIMESTAMP,
    last_edited_at TIMESTAMP,
    last_edited_by VARCHAR(255),
    view_count BIGINT DEFAULT 0,
    share_count BIGINT DEFAULT 0,
    reaction_count BIGINT DEFAULT 0,
    thumbnail_url VARCHAR(500),
    is_pinned BOOLEAN DEFAULT FALSE,
    allow_comments BOOLEAN DEFAULT TRUE,
    send_notification BOOLEAN DEFAULT TRUE,
    template_id VARCHAR(255),
    original_post_id VARCHAR(255), -- For forwarded posts
    original_channel_id VARCHAR(255), -- For forwarded posts
    FOREIGN KEY (channel_id) REFERENCES channels(channel_id) ON DELETE CASCADE,
    FOREIGN KEY (author_id) REFERENCES users(user_id) ON DELETE SET NULL,
    FOREIGN KEY (last_edited_by) REFERENCES users(user_id) ON DELETE SET NULL,
    FOREIGN KEY (original_post_id) REFERENCES channel_posts(post_id) ON DELETE SET NULL,
    FOREIGN KEY (original_channel_id) REFERENCES channels(channel_id) ON DELETE SET NULL
);

-- Table for post media URLs
CREATE TABLE IF NOT EXISTS channel_post_media (
    post_id VARCHAR(255),
    media_url VARCHAR(500),
    media_type VARCHAR(50), -- IMAGE, VIDEO, AUDIO, DOCUMENT
    display_order INTEGER DEFAULT 0,
    PRIMARY KEY (post_id, media_url),
    FOREIGN KEY (post_id) REFERENCES channel_posts(post_id) ON DELETE CASCADE
);

-- Table for post tags/hashtags
CREATE TABLE IF NOT EXISTS channel_post_tags (
    post_id VARCHAR(255),
    tag VARCHAR(100),
    PRIMARY KEY (post_id, tag),
    FOREIGN KEY (post_id) REFERENCES channel_posts(post_id) ON DELETE CASCADE
);

-- Table for post mentions
CREATE TABLE IF NOT EXISTS channel_post_mentions (
    post_id VARCHAR(255),
    user_id VARCHAR(255),
    PRIMARY KEY (post_id, user_id),
    FOREIGN KEY (post_id) REFERENCES channel_posts(post_id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

-- ===================================
-- CHANNEL ANALYTICS
-- ===================================

-- Table for channel analytics
CREATE TABLE IF NOT EXISTS channel_analytics (
    analytics_id VARCHAR(255) PRIMARY KEY,
    channel_id VARCHAR(255) NOT NULL,
    period_start TIMESTAMP NOT NULL,
    period_end TIMESTAMP NOT NULL,
    total_subscribers BIGINT DEFAULT 0,
    new_subscribers BIGINT DEFAULT 0,
    unsubscribers BIGINT DEFAULT 0,
    net_growth BIGINT DEFAULT 0,
    total_posts BIGINT DEFAULT 0,
    total_views BIGINT DEFAULT 0,
    total_shares BIGINT DEFAULT 0,
    total_reactions BIGINT DEFAULT 0,
    average_views_per_post DECIMAL(10,2) DEFAULT 0,
    average_engagement_rate DECIMAL(5,4) DEFAULT 0,
    total_comments BIGINT DEFAULT 0,
    total_likes BIGINT DEFAULT 0,
    active_subscribers BIGINT DEFAULT 0,
    subscriber_engagement_rate DECIMAL(5,4) DEFAULT 0,
    most_viewed_post_id VARCHAR(255),
    most_viewed_post_views BIGINT DEFAULT 0,
    most_engaged_post_id VARCHAR(255),
    most_engaged_post_rate DECIMAL(5,4) DEFAULT 0,
    growth_rate DECIMAL(5,2) DEFAULT 0,
    previous_period_subscribers BIGINT DEFAULT 0,
    views_growth_rate DECIMAL(5,2) DEFAULT 0,
    engagement_growth_rate DECIMAL(5,2) DEFAULT 0,
    subscriber_retention_rate DECIMAL(5,2) DEFAULT 0,
    returning_subscribers BIGINT DEFAULT 0,
    new_active_subscribers BIGINT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (channel_id) REFERENCES channels(channel_id) ON DELETE CASCADE,
    FOREIGN KEY (most_viewed_post_id) REFERENCES channel_posts(post_id) ON DELETE SET NULL,
    FOREIGN KEY (most_engaged_post_id) REFERENCES channel_posts(post_id) ON DELETE SET NULL
);

-- Table for time-based analytics (views by hour, day, etc.)
CREATE TABLE IF NOT EXISTS channel_analytics_time (
    analytics_id VARCHAR(255),
    time_type VARCHAR(20), -- HOUR, DAY, WEEK, MONTH
    time_value VARCHAR(50), -- Hour (0-23), Day name, Week number, Month name
    count_value BIGINT DEFAULT 0,
    PRIMARY KEY (analytics_id, time_type, time_value),
    FOREIGN KEY (analytics_id) REFERENCES channel_analytics(analytics_id) ON DELETE CASCADE
);

-- Table for geographic analytics
CREATE TABLE IF NOT EXISTS channel_analytics_geo (
    analytics_id VARCHAR(255),
    country_code VARCHAR(3),
    country_name VARCHAR(100),
    views BIGINT DEFAULT 0,
    subscribers BIGINT DEFAULT 0,
    PRIMARY KEY (analytics_id, country_code),
    FOREIGN KEY (analytics_id) REFERENCES channel_analytics(analytics_id) ON DELETE CASCADE
);

-- ===================================
-- MODERATION AND MANAGEMENT
-- ===================================

-- Table for moderation logs
CREATE TABLE IF NOT EXISTS moderation_logs (
    log_id VARCHAR(255) PRIMARY KEY,
    group_id VARCHAR(255),
    channel_id VARCHAR(255),
    moderator_id VARCHAR(255) NOT NULL,
    target_user_id VARCHAR(255),
    target_message_id VARCHAR(255),
    action_type VARCHAR(50) NOT NULL, -- MUTE, BAN, KICK, DELETE_MESSAGE, WARN, PROMOTE, DEMOTE
    reason TEXT,
    duration_minutes INTEGER, -- For temporary actions
    action_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    additional_data JSON, -- For storing additional action-specific data
    FOREIGN KEY (group_id) REFERENCES group_chats(group_id) ON DELETE CASCADE,
    FOREIGN KEY (channel_id) REFERENCES channels(channel_id) ON DELETE CASCADE,
    FOREIGN KEY (moderator_id) REFERENCES users(user_id) ON DELETE SET NULL,
    FOREIGN KEY (target_user_id) REFERENCES users(user_id) ON DELETE SET NULL,
    FOREIGN KEY (target_message_id) REFERENCES messages(message_id) ON DELETE SET NULL
);

-- Table for scheduled messages/posts
CREATE TABLE IF NOT EXISTS scheduled_messages (
    scheduled_id VARCHAR(255) PRIMARY KEY,
    group_id VARCHAR(255),
    channel_id VARCHAR(255),
    sender_id VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    media_path VARCHAR(500),
    message_type VARCHAR(50) DEFAULT 'TEXT',
    scheduled_for TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(50) DEFAULT 'PENDING', -- PENDING, SENT, CANCELLED, FAILED
    sent_at TIMESTAMP,
    failure_reason TEXT,
    FOREIGN KEY (group_id) REFERENCES group_chats(group_id) ON DELETE CASCADE,
    FOREIGN KEY (channel_id) REFERENCES channels(channel_id) ON DELETE CASCADE,
    FOREIGN KEY (sender_id) REFERENCES users(user_id) ON DELETE CASCADE
);

-- ===================================
-- ENHANCED FEATURES TABLES
-- ===================================

-- Table for message reactions
CREATE TABLE IF NOT EXISTS message_reactions (
    reaction_id VARCHAR(255) PRIMARY KEY,
    message_id VARCHAR(255) NOT NULL,
    user_id VARCHAR(255) NOT NULL,
    reaction_type VARCHAR(50) NOT NULL, -- LIKE, LOVE, LAUGH, ANGRY, etc.
    emoji VARCHAR(10), -- Actual emoji character
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (message_id) REFERENCES messages(message_id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    UNIQUE(message_id, user_id, reaction_type)
);

-- Table for message threads (replies)
CREATE TABLE IF NOT EXISTS message_threads (
    thread_id VARCHAR(255) PRIMARY KEY,
    parent_message_id VARCHAR(255) NOT NULL,
    reply_message_id VARCHAR(255) NOT NULL,
    thread_level INTEGER DEFAULT 1, -- Nesting level
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (parent_message_id) REFERENCES messages(message_id) ON DELETE CASCADE,
    FOREIGN KEY (reply_message_id) REFERENCES messages(message_id) ON DELETE CASCADE
);

-- Table for group/channel categories
CREATE TABLE IF NOT EXISTS chat_categories (
    category_id VARCHAR(255) PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL, -- Categories are user-specific
    category_name VARCHAR(100) NOT NULL,
    category_color VARCHAR(7) DEFAULT '#2196F3',
    display_order INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

-- Table for assigning chats to categories
CREATE TABLE IF NOT EXISTS chat_category_assignments (
    category_id VARCHAR(255),
    group_id VARCHAR(255),
    channel_id VARCHAR(255),
    private_chat_id VARCHAR(255),
    assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (category_id) REFERENCES chat_categories(category_id) ON DELETE CASCADE,
    FOREIGN KEY (group_id) REFERENCES group_chats(group_id) ON DELETE CASCADE,
    FOREIGN KEY (channel_id) REFERENCES channels(channel_id) ON DELETE CASCADE,
    FOREIGN KEY (private_chat_id) REFERENCES private_chats(chat_id) ON DELETE CASCADE,
    CHECK (
        (group_id IS NOT NULL AND channel_id IS NULL AND private_chat_id IS NULL) OR
        (group_id IS NULL AND channel_id IS NOT NULL AND private_chat_id IS NULL) OR
        (group_id IS NULL AND channel_id IS NULL AND private_chat_id IS NOT NULL)
    )
);

-- Table for post templates
CREATE TABLE IF NOT EXISTS post_templates (
    template_id VARCHAR(255) PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    template_name VARCHAR(100) NOT NULL,
    template_content TEXT NOT NULL,
    template_type VARCHAR(50) DEFAULT 'GENERAL',
    category VARCHAR(50),
    is_public BOOLEAN DEFAULT FALSE,
    usage_count INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

-- ===================================
-- INDEXES FOR PERFORMANCE
-- ===================================

-- Indexes for group roles and permissions
CREATE INDEX IF NOT EXISTS idx_group_roles_group_id ON group_roles(group_id);
CREATE INDEX IF NOT EXISTS idx_group_roles_type ON group_roles(role_type);
CREATE INDEX IF NOT EXISTS idx_group_role_permissions_role ON group_role_permissions(role_id);

-- Indexes for enhanced group members
CREATE INDEX IF NOT EXISTS idx_group_members_group_id ON group_members(group_id);
CREATE INDEX IF NOT EXISTS idx_group_members_user_id ON group_members(user_id);
CREATE INDEX IF NOT EXISTS idx_group_members_role_id ON group_members(role_id);
CREATE INDEX IF NOT EXISTS idx_group_members_activity ON group_members(last_activity);
CREATE INDEX IF NOT EXISTS idx_group_members_joined ON group_members(joined_at);

-- Indexes for channel posts
CREATE INDEX IF NOT EXISTS idx_channel_posts_channel ON channel_posts(channel_id);
CREATE INDEX IF NOT EXISTS idx_channel_posts_author ON channel_posts(author_id);
CREATE INDEX IF NOT EXISTS idx_channel_posts_status ON channel_posts(status);
CREATE INDEX IF NOT EXISTS idx_channel_posts_published ON channel_posts(published_at);
CREATE INDEX IF NOT EXISTS idx_channel_posts_scheduled ON channel_posts(scheduled_at);
CREATE INDEX IF NOT EXISTS idx_channel_posts_views ON channel_posts(view_count DESC);

-- Indexes for analytics
CREATE INDEX IF NOT EXISTS idx_channel_analytics_channel ON channel_analytics(channel_id);
CREATE INDEX IF NOT EXISTS idx_channel_analytics_period ON channel_analytics(period_start, period_end);

-- Indexes for moderation
CREATE INDEX IF NOT EXISTS idx_moderation_logs_group ON moderation_logs(group_id);
CREATE INDEX IF NOT EXISTS idx_moderation_logs_channel ON moderation_logs(channel_id);
CREATE INDEX IF NOT EXISTS idx_moderation_logs_moderator ON moderation_logs(moderator_id);
CREATE INDEX IF NOT EXISTS idx_moderation_logs_timestamp ON moderation_logs(action_timestamp);

-- Indexes for scheduled messages
CREATE INDEX IF NOT EXISTS idx_scheduled_messages_scheduled_for ON scheduled_messages(scheduled_for);
CREATE INDEX IF NOT EXISTS idx_scheduled_messages_status ON scheduled_messages(status);

-- Indexes for reactions and threads
CREATE INDEX IF NOT EXISTS idx_message_reactions_message ON message_reactions(message_id);
CREATE INDEX IF NOT EXISTS idx_message_reactions_user ON message_reactions(user_id);
CREATE INDEX IF NOT EXISTS idx_message_threads_parent ON message_threads(parent_message_id);
CREATE INDEX IF NOT EXISTS idx_message_threads_reply ON message_threads(reply_message_id);

-- ===================================
-- DATA MIGRATION NOTES
-- ===================================

/*
Migration Steps for Existing Data:

1. Create default roles for existing groups:
   - For each group in group_chats, create default roles (Owner, Admin, Member)
   - Migrate existing is_admin flag to appropriate roles

2. Migrate existing group_members:
   - Copy data from old group_members table
   - Assign appropriate role_id based on is_admin flag
   - Set default values for new columns

3. Initialize analytics:
   - Create initial analytics records for existing channels
   - Set baseline metrics to 0

4. Set up moderation logs:
   - No historical data migration needed
   - Start fresh logging from implementation date
*/
