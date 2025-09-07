package com.telegram.services;

import com.telegram.dao.ChannelDAO;
import com.telegram.models.ChannelPost;
import com.telegram.models.ChannelAnalytics;
import com.telegram.models.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing channel analytics and insights
 */
public class ChannelAnalyticsService {
    private static final Logger logger = LoggerFactory.getLogger(ChannelAnalyticsService.class);
    private static ChannelAnalyticsService instance;
    
    private final ChannelDAO channelDAO;
    private final Map<String, List<ChannelPost>> channelPostsCache;
    private final Map<String, ChannelAnalytics> analyticsCache;
    
    private ChannelAnalyticsService() {
        this.channelDAO = new ChannelDAO();
        this.channelPostsCache = new HashMap<>();
        this.analyticsCache = new HashMap<>();
    }
    
    public static synchronized ChannelAnalyticsService getInstance() {
        if (instance == null) {
            instance = new ChannelAnalyticsService();
        }
        return instance;
    }
    
    /**
     * Generate analytics for a channel for a specific time period
     */
    public ChannelAnalytics generateAnalytics(String channelId, LocalDateTime periodStart, LocalDateTime periodEnd) {
        try {
            ChannelAnalytics analytics = new ChannelAnalytics(channelId, periodStart, periodEnd);
            
            // Get channel info
            // TODO: Load from database
            Channel channel = null; // channelDAO.findById(channelId);
            if (channel == null) {
                logger.warn("Channel {} not found for analytics", channelId);
                return null;
            }
            
            // Get posts for the period
            List<ChannelPost> posts = getChannelPostsInPeriod(channelId, periodStart, periodEnd);
            
            // Calculate basic metrics
            calculateBasicMetrics(analytics, posts);
            
            // Calculate engagement metrics
            calculateEngagementMetrics(analytics, posts);
            
            // Calculate growth metrics
            calculateGrowthMetrics(analytics, channelId, periodStart);
            
            // Calculate time-based metrics
            calculateTimeBasedMetrics(analytics, posts);
            
            // Find top performing content
            findTopContent(analytics, posts);
            
            // Calculate all derived metrics
            analytics.calculateMetrics();
            
            // Cache the analytics
            analyticsCache.put(channelId + ":" + periodStart + ":" + periodEnd, analytics);
            
            logger.info("Generated analytics for channel {} for period {} to {}", 
                channelId, periodStart, periodEnd);
            
            return analytics;
        } catch (Exception e) {
            logger.error("Error generating analytics for channel {}", channelId, e);
            return null;
        }
    }
    
    /**
     * Get analytics for the last 30 days
     */
    public ChannelAnalytics getMonthlyAnalytics(String channelId) {
        LocalDateTime end = LocalDateTime.now();
        LocalDateTime start = end.minus(30, ChronoUnit.DAYS);
        return generateAnalytics(channelId, start, end);
    }
    
    /**
     * Get analytics for the last 7 days
     */
    public ChannelAnalytics getWeeklyAnalytics(String channelId) {
        LocalDateTime end = LocalDateTime.now();
        LocalDateTime start = end.minus(7, ChronoUnit.DAYS);
        return generateAnalytics(channelId, start, end);
    }
    
    /**
     * Get analytics for today
     */
    public ChannelAnalytics getDailyAnalytics(String channelId) {
        LocalDateTime end = LocalDateTime.now();
        LocalDateTime start = end.truncatedTo(ChronoUnit.DAYS);
        return generateAnalytics(channelId, start, end);
    }
    
    /**
     * Calculate basic metrics like posts, views, shares
     */
    private void calculateBasicMetrics(ChannelAnalytics analytics, List<ChannelPost> posts) {
        analytics.setTotalPosts(posts.size());
        
        long totalViews = posts.stream()
            .mapToLong(ChannelPost::getViewCount)
            .sum();
        analytics.setTotalViews(totalViews);
        
        long totalShares = posts.stream()
            .mapToLong(ChannelPost::getShareCount)
            .sum();
        analytics.setTotalShares(totalShares);
        
        long totalReactions = posts.stream()
            .mapToLong(ChannelPost::getReactionCount)
            .sum();
        analytics.setTotalReactions(totalReactions);
    }
    
    /**
     * Calculate engagement metrics
     */
    private void calculateEngagementMetrics(ChannelAnalytics analytics, List<ChannelPost> posts) {
        // TODO: Calculate engagement metrics based on subscriber interactions
        // This would involve loading subscriber data and interaction history
        
        // For now, we'll use simplified calculations
        analytics.setActiveSubscribers(analytics.getTotalSubscribers() / 10); // Estimated
        analytics.setTotalComments(analytics.getTotalReactions() / 2); // Estimated
        analytics.setTotalLikes(analytics.getTotalReactions()); // All reactions as likes
    }
    
    /**
     * Calculate growth metrics compared to previous period
     */
    private void calculateGrowthMetrics(ChannelAnalytics analytics, String channelId, LocalDateTime periodStart) {
        try {
            // Get previous period analytics for comparison
            LocalDateTime previousEnd = periodStart;
            long daysBetween = ChronoUnit.DAYS.between(analytics.getPeriodStart(), analytics.getPeriodEnd());
            LocalDateTime previousStart = previousEnd.minus(daysBetween, ChronoUnit.DAYS);
            
            // TODO: Load previous period data from database
            // For now, we'll simulate some growth data
            analytics.setPreviousPeriodSubscribers(analytics.getTotalSubscribers() - 10);
            analytics.setNewSubscribers(15);
            analytics.setUnsubscribers(5);
            
            // Calculate growth rates
            if (analytics.getPreviousPeriodSubscribers() > 0) {
                double growthRate = ((double) analytics.getNetGrowth() / analytics.getPreviousPeriodSubscribers()) * 100;
                analytics.setGrowthRate(growthRate);
            }
            
            // Set retention metrics (simplified)
            analytics.setReturningSubscribers(analytics.getTotalSubscribers() - analytics.getNewSubscribers());
            analytics.setNewActiveSubscribers(analytics.getActiveSubscribers() / 5); // Estimated
            
        } catch (Exception e) {
            logger.error("Error calculating growth metrics for channel {}", channelId, e);
        }
    }
    
    /**
     * Calculate time-based metrics (views by hour, day, etc.)
     */
    private void calculateTimeBasedMetrics(ChannelAnalytics analytics, List<ChannelPost> posts) {
        // Simulate time-based distribution of views
        Random random = new Random();
        
        // Views by hour (simulate peak hours)
        for (int hour = 0; hour < 24; hour++) {
            long views = random.nextInt(100) + (hour >= 9 && hour <= 21 ? 50 : 10); // Higher during day
            analytics.addViewByHour(hour);
            for (int i = 0; i < views; i++) {
                analytics.addViewByHour(hour);
            }
        }
        
        // Views by day of week
        String[] days = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};
        for (String day : days) {
            long views = random.nextInt(500) + 100;
            for (int i = 0; i < views; i++) {
                analytics.addViewByDay(day);
            }
        }
        
        // Posts by category
        Map<String, Long> categoryCounts = posts.stream()
            .collect(Collectors.groupingBy(
                post -> post.getPostType().getDisplayName(),
                Collectors.counting()
            ));
        
        for (Map.Entry<String, Long> entry : categoryCounts.entrySet()) {
            for (int i = 0; i < entry.getValue(); i++) {
                analytics.addPostByCategory(entry.getKey());
            }
        }
    }
    
    /**
     * Find top performing content
     */
    private void findTopContent(ChannelAnalytics analytics, List<ChannelPost> posts) {
        if (posts.isEmpty()) return;
        
        // Most viewed post
        ChannelPost mostViewed = posts.stream()
            .max(Comparator.comparingLong(ChannelPost::getViewCount))
            .orElse(null);
        
        if (mostViewed != null) {
            analytics.setMostViewedPostId(mostViewed.getPostId());
            analytics.setMostViewedPostViews(mostViewed.getViewCount());
        }
        
        // Most engaged post (highest engagement rate)
        ChannelPost mostEngaged = posts.stream()
            .max(Comparator.comparingDouble(ChannelPost::getEngagementRate))
            .orElse(null);
        
        if (mostEngaged != null) {
            analytics.setMostEngagedPostId(mostEngaged.getPostId());
            analytics.setMostEngagedPostRate(mostEngaged.getEngagementRate());
        }
    }
    
    /**
     * Get posts for a specific time period
     */
    private List<ChannelPost> getChannelPostsInPeriod(String channelId, LocalDateTime start, LocalDateTime end) {
        try {
            // Try cache first
            List<ChannelPost> cachedPosts = channelPostsCache.get(channelId);
            if (cachedPosts != null) {
                return cachedPosts.stream()
                    .filter(post -> post.getPublishedAt() != null)
                    .filter(post -> !post.getPublishedAt().isBefore(start))
                    .filter(post -> !post.getPublishedAt().isAfter(end))
                    .collect(Collectors.toList());
            }
            
            // TODO: Load from database with date filtering
            return new ArrayList<>();
            
        } catch (Exception e) {
            logger.error("Error getting posts for channel {} in period {} to {}", 
                channelId, start, end, e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Track a post view (called when a post is viewed)
     */
    public void trackPostView(String channelId, String postId, String viewerId) {
        try {
            // TODO: Record view in database
            
            // Update post view count
            List<ChannelPost> posts = channelPostsCache.get(channelId);
            if (posts != null) {
                posts.stream()
                    .filter(post -> post.getPostId().equals(postId))
                    .findFirst()
                    .ifPresent(ChannelPost::addView);
            }
            
            // Update analytics
            LocalDateTime now = LocalDateTime.now();
            String analyticsKey = channelId + ":" + now.toLocalDate();
            ChannelAnalytics analytics = analyticsCache.get(analyticsKey);
            if (analytics != null) {
                analytics.addViewByHour(now.getHour());
                analytics.addViewByDay(now.getDayOfWeek().toString());
            }
            
        } catch (Exception e) {
            logger.error("Error tracking view for post {} in channel {}", postId, channelId, e);
        }
    }
    
    /**
     * Track a post share
     */
    public void trackPostShare(String channelId, String postId, String sharerId) {
        try {
            // TODO: Record share in database
            
            // Update post share count
            List<ChannelPost> posts = channelPostsCache.get(channelId);
            if (posts != null) {
                posts.stream()
                    .filter(post -> post.getPostId().equals(postId))
                    .findFirst()
                    .ifPresent(ChannelPost::addShare);
            }
            
        } catch (Exception e) {
            logger.error("Error tracking share for post {} in channel {}", postId, channelId, e);
        }
    }
    
    /**
     * Track a post reaction
     */
    public void trackPostReaction(String channelId, String postId, String reactorId) {
        try {
            // TODO: Record reaction in database
            
            // Update post reaction count
            List<ChannelPost> posts = channelPostsCache.get(channelId);
            if (posts != null) {
                posts.stream()
                    .filter(post -> post.getPostId().equals(postId))
                    .findFirst()
                    .ifPresent(ChannelPost::addReaction);
            }
            
        } catch (Exception e) {
            logger.error("Error tracking reaction for post {} in channel {}", postId, channelId, e);
        }
    }
    
    /**
     * Get analytics comparison between two periods
     */
    public Map<String, Object> compareAnalytics(String channelId, 
                                               LocalDateTime period1Start, LocalDateTime period1End,
                                               LocalDateTime period2Start, LocalDateTime period2End) {
        try {
            ChannelAnalytics analytics1 = generateAnalytics(channelId, period1Start, period1End);
            ChannelAnalytics analytics2 = generateAnalytics(channelId, period2Start, period2End);
            
            Map<String, Object> comparison = new HashMap<>();
            
            if (analytics1 != null && analytics2 != null) {
                // Compare key metrics
                comparison.put("subscriberChange", analytics2.getTotalSubscribers() - analytics1.getTotalSubscribers());
                comparison.put("viewsChange", analytics2.getTotalViews() - analytics1.getTotalViews());
                comparison.put("postsChange", analytics2.getTotalPosts() - analytics1.getTotalPosts());
                comparison.put("engagementChange", analytics2.getAverageEngagementRate() - analytics1.getAverageEngagementRate());
                
                // Calculate percentage changes
                if (analytics1.getTotalSubscribers() > 0) {
                    double subscriberChangePercent = ((double) (analytics2.getTotalSubscribers() - analytics1.getTotalSubscribers()) / analytics1.getTotalSubscribers()) * 100;
                    comparison.put("subscriberChangePercent", subscriberChangePercent);
                }
                
                if (analytics1.getTotalViews() > 0) {
                    double viewsChangePercent = ((double) (analytics2.getTotalViews() - analytics1.getTotalViews()) / analytics1.getTotalViews()) * 100;
                    comparison.put("viewsChangePercent", viewsChangePercent);
                }
                
                comparison.put("period1", analytics1);
                comparison.put("period2", analytics2);
            }
            
            return comparison;
            
        } catch (Exception e) {
            logger.error("Error comparing analytics for channel {}", channelId, e);
            return new HashMap<>();
        }
    }
    
    /**
     * Generate analytics report summary
     */
    public String generateReportSummary(ChannelAnalytics analytics) {
        if (analytics == null) return "No analytics data available.";
        
        StringBuilder summary = new StringBuilder();
        summary.append("📊 Channel Analytics Report\n\n");
        
        summary.append("📈 Growth & Engagement\n");
        summary.append(String.format("• Total Subscribers: %,d", analytics.getTotalSubscribers()));
        if (analytics.getGrowthRate() > 0) {
            summary.append(String.format(" (+%.1f%%)", analytics.getGrowthRate()));
        }
        summary.append("\n");
        
        summary.append(String.format("• New Subscribers: %,d\n", analytics.getNewSubscribers()));
        summary.append(String.format("• Unsubscribers: %,d\n", analytics.getUnsubscribers()));
        summary.append(String.format("• Net Growth: %,d\n\n", analytics.getNetGrowth()));
        
        summary.append("📝 Content Performance\n");
        summary.append(String.format("• Total Posts: %,d\n", analytics.getTotalPosts()));
        summary.append(String.format("• Total Views: %,d\n", analytics.getTotalViews()));
        summary.append(String.format("• Avg Views/Post: %.1f\n", analytics.getAverageViewsPerPost()));
        summary.append(String.format("• Engagement Rate: %.2f%%\n\n", analytics.getAverageEngagementRate() * 100));
        
        summary.append("🎯 Key Insights\n");
        summary.append(String.format("• Peak Activity Hour: %s:00\n", analytics.getPeakHour()));
        summary.append(String.format("• Best Day: %s\n", analytics.getPeakDay()));
        summary.append(String.format("• Performance Grade: %s\n", analytics.getPerformanceGrade()));
        
        return summary.toString();
    }
    
    /**
     * Clear analytics cache for a channel
     */
    public void clearChannelCache(String channelId) {
        channelPostsCache.remove(channelId);
        analyticsCache.entrySet().removeIf(entry -> entry.getKey().startsWith(channelId + ":"));
    }
    
    /**
     * Clear all analytics caches
     */
    public void clearAllCaches() {
        channelPostsCache.clear();
        analyticsCache.clear();
    }
}
