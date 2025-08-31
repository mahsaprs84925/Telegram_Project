package com.telegram.models;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents analytics data for a channel
 */
public class ChannelAnalytics {
    private String analyticsId;
    private String channelId;
    private LocalDateTime periodStart;
    private LocalDateTime periodEnd;
    private LocalDateTime createdAt; // When analytics record was created
    private LocalDateTime updatedAt; // When analytics record was last updated
    
    // Subscriber metrics
    private long totalSubscribers;
    private long newSubscribers;
    private long unsubscribers;
    private long netGrowth;
    
    // Content metrics
    private long totalPosts;
    private long totalViews;
    private long totalShares;
    private long totalReactions;
    private double averageViewsPerPost;
    private double averageEngagementRate;
    
    // Engagement metrics
    private long totalComments;
    private long totalLikes;
    private long activeSubscribers; // Subscribers who interacted
    private double subscriberEngagementRate;
    
    // Time-based metrics
    private Map<String, Long> viewsByHour; // Hour -> view count
    private Map<String, Long> viewsByDay; // Day -> view count
    private Map<String, Long> postsByCategory; // Category -> post count
    
    // Top performing content
    private String mostViewedPostId;
    private long mostViewedPostViews;
    private String mostEngagedPostId;
    private double mostEngagedPostRate;
    
    // Geographic data (simplified)
    private Map<String, Long> viewsByCountry;
    private Map<String, Long> subscribersByCountry;
    
    // Growth metrics
    private double growthRate; // Percentage growth
    private long previousPeriodSubscribers;
    private double viewsGrowthRate;
    private double engagementGrowthRate;
    
    // Retention metrics
    private double subscriberRetentionRate;
    private long returningSubscribers;
    private long newActiveSubscribers;
    
    // Constructors
    public ChannelAnalytics() {
        this.analyticsId = java.util.UUID.randomUUID().toString();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.viewsByHour = new HashMap<>();
        this.viewsByDay = new HashMap<>();
        this.postsByCategory = new HashMap<>();
        this.viewsByCountry = new HashMap<>();
        this.subscribersByCountry = new HashMap<>();
    }
    
    public ChannelAnalytics(String channelId, LocalDateTime periodStart, LocalDateTime periodEnd) {
        this();
        this.channelId = channelId;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
    }
    
    // Constructor for DAO usage
    public ChannelAnalytics(String analyticsId, String channelId, LocalDateTime periodStart, LocalDateTime periodEnd) {
        this();
        this.analyticsId = analyticsId;
        this.channelId = channelId;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
    }
    
    // Calculation methods
    public void calculateMetrics() {
        // Calculate net growth
        this.netGrowth = newSubscribers - unsubscribers;
        
        // Calculate average views per post
        if (totalPosts > 0) {
            this.averageViewsPerPost = (double) totalViews / totalPosts;
        }
        
        // Calculate average engagement rate
        if (totalViews > 0) {
            this.averageEngagementRate = (double) (totalReactions + totalShares + totalComments) / totalViews;
        }
        
        // Calculate subscriber engagement rate
        if (totalSubscribers > 0) {
            this.subscriberEngagementRate = (double) activeSubscribers / totalSubscribers;
        }
        
        // Calculate growth rate
        if (previousPeriodSubscribers > 0) {
            this.growthRate = ((double) netGrowth / previousPeriodSubscribers) * 100;
        }
        
        // Calculate retention rate
        if (totalSubscribers > 0) {
            this.subscriberRetentionRate = ((double) returningSubscribers / totalSubscribers) * 100;
        }
    }
    
    public void addViewByHour(int hour) {
        String hourKey = String.valueOf(hour);
        viewsByHour.merge(hourKey, 1L, Long::sum);
    }
    
    public void addViewByDay(String day) {
        viewsByDay.merge(day, 1L, Long::sum);
    }
    
    public void addPostByCategory(String category) {
        postsByCategory.merge(category, 1L, Long::sum);
    }
    
    public void addViewByCountry(String country) {
        viewsByCountry.merge(country, 1L, Long::sum);
    }
    
    public void addSubscriberByCountry(String country) {
        subscribersByCountry.merge(country, 1L, Long::sum);
    }
    
    // Peak activity analysis
    public String getPeakHour() {
        return viewsByHour.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("N/A");
    }
    
    public String getPeakDay() {
        return viewsByDay.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("N/A");
    }
    
    public String getMostPopularCategory() {
        return postsByCategory.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("N/A");
    }
    
    public String getTopCountry() {
        return viewsByCountry.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("N/A");
    }
    
    // Performance indicators
    public String getPerformanceGrade() {
        double score = 0;
        
        // Engagement rate scoring (0-40 points)
        if (averageEngagementRate >= 0.1) score += 40;
        else if (averageEngagementRate >= 0.05) score += 30;
        else if (averageEngagementRate >= 0.02) score += 20;
        else score += 10;
        
        // Growth rate scoring (0-30 points)
        if (growthRate >= 10) score += 30;
        else if (growthRate >= 5) score += 25;
        else if (growthRate >= 2) score += 20;
        else if (growthRate >= 0) score += 15;
        else score += 5;
        
        // Subscriber engagement scoring (0-30 points)
        if (subscriberEngagementRate >= 0.5) score += 30;
        else if (subscriberEngagementRate >= 0.3) score += 25;
        else if (subscriberEngagementRate >= 0.2) score += 20;
        else if (subscriberEngagementRate >= 0.1) score += 15;
        else score += 5;
        
        if (score >= 85) return "A+";
        else if (score >= 75) return "A";
        else if (score >= 65) return "B+";
        else if (score >= 55) return "B";
        else if (score >= 45) return "C+";
        else if (score >= 35) return "C";
        else return "D";
    }
    
    // Getters and Setters
    public String getAnalyticsId() {
        return analyticsId;
    }
    
    public void setAnalyticsId(String analyticsId) {
        this.analyticsId = analyticsId;
    }
    
    public String getChannelId() {
        return channelId;
    }
    
    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }
    
    public LocalDateTime getPeriodStart() {
        return periodStart;
    }
    
    public void setPeriodStart(LocalDateTime periodStart) {
        this.periodStart = periodStart;
    }
    
    public LocalDateTime getPeriodEnd() {
        return periodEnd;
    }
    
    public void setPeriodEnd(LocalDateTime periodEnd) {
        this.periodEnd = periodEnd;
    }
    
    public long getTotalSubscribers() {
        return totalSubscribers;
    }
    
    public void setTotalSubscribers(long totalSubscribers) {
        this.totalSubscribers = totalSubscribers;
    }
    
    public long getNewSubscribers() {
        return newSubscribers;
    }
    
    public void setNewSubscribers(long newSubscribers) {
        this.newSubscribers = newSubscribers;
    }
    
    public long getUnsubscribers() {
        return unsubscribers;
    }
    
    public void setUnsubscribers(long unsubscribers) {
        this.unsubscribers = unsubscribers;
    }
    
    public long getNetGrowth() {
        return netGrowth;
    }
    
    public void setNetGrowth(long netGrowth) {
        this.netGrowth = netGrowth;
    }
    
    public long getTotalPosts() {
        return totalPosts;
    }
    
    public void setTotalPosts(long totalPosts) {
        this.totalPosts = totalPosts;
    }
    
    public long getTotalViews() {
        return totalViews;
    }
    
    public void setTotalViews(long totalViews) {
        this.totalViews = totalViews;
    }
    
    public long getTotalShares() {
        return totalShares;
    }
    
    public void setTotalShares(long totalShares) {
        this.totalShares = totalShares;
    }
    
    public long getTotalReactions() {
        return totalReactions;
    }
    
    public void setTotalReactions(long totalReactions) {
        this.totalReactions = totalReactions;
    }
    
    public double getAverageViewsPerPost() {
        return averageViewsPerPost;
    }
    
    public void setAverageViewsPerPost(double averageViewsPerPost) {
        this.averageViewsPerPost = averageViewsPerPost;
    }
    
    public double getAverageEngagementRate() {
        return averageEngagementRate;
    }
    
    public void setAverageEngagementRate(double averageEngagementRate) {
        this.averageEngagementRate = averageEngagementRate;
    }
    
    public long getTotalComments() {
        return totalComments;
    }
    
    public void setTotalComments(long totalComments) {
        this.totalComments = totalComments;
    }
    
    public long getTotalLikes() {
        return totalLikes;
    }
    
    public void setTotalLikes(long totalLikes) {
        this.totalLikes = totalLikes;
    }
    
    public long getActiveSubscribers() {
        return activeSubscribers;
    }
    
    public void setActiveSubscribers(long activeSubscribers) {
        this.activeSubscribers = activeSubscribers;
    }
    
    public double getSubscriberEngagementRate() {
        return subscriberEngagementRate;
    }
    
    public void setSubscriberEngagementRate(double subscriberEngagementRate) {
        this.subscriberEngagementRate = subscriberEngagementRate;
    }
    
    public Map<String, Long> getViewsByHour() {
        return new HashMap<>(viewsByHour);
    }
    
    public void setViewsByHour(Map<String, Long> viewsByHour) {
        this.viewsByHour = new HashMap<>(viewsByHour);
    }
    
    public Map<String, Long> getViewsByDay() {
        return new HashMap<>(viewsByDay);
    }
    
    public void setViewsByDay(Map<String, Long> viewsByDay) {
        this.viewsByDay = new HashMap<>(viewsByDay);
    }
    
    public Map<String, Long> getPostsByCategory() {
        return new HashMap<>(postsByCategory);
    }
    
    public void setPostsByCategory(Map<String, Long> postsByCategory) {
        this.postsByCategory = new HashMap<>(postsByCategory);
    }
    
    public String getMostViewedPostId() {
        return mostViewedPostId;
    }
    
    public void setMostViewedPostId(String mostViewedPostId) {
        this.mostViewedPostId = mostViewedPostId;
    }
    
    public long getMostViewedPostViews() {
        return mostViewedPostViews;
    }
    
    public void setMostViewedPostViews(long mostViewedPostViews) {
        this.mostViewedPostViews = mostViewedPostViews;
    }
    
    public String getMostEngagedPostId() {
        return mostEngagedPostId;
    }
    
    public void setMostEngagedPostId(String mostEngagedPostId) {
        this.mostEngagedPostId = mostEngagedPostId;
    }
    
    public double getMostEngagedPostRate() {
        return mostEngagedPostRate;
    }
    
    public void setMostEngagedPostRate(double mostEngagedPostRate) {
        this.mostEngagedPostRate = mostEngagedPostRate;
    }
    
    public Map<String, Long> getViewsByCountry() {
        return new HashMap<>(viewsByCountry);
    }
    
    public void setViewsByCountry(Map<String, Long> viewsByCountry) {
        this.viewsByCountry = new HashMap<>(viewsByCountry);
    }
    
    public Map<String, Long> getSubscribersByCountry() {
        return new HashMap<>(subscribersByCountry);
    }
    
    public void setSubscribersByCountry(Map<String, Long> subscribersByCountry) {
        this.subscribersByCountry = new HashMap<>(subscribersByCountry);
    }
    
    public double getGrowthRate() {
        return growthRate;
    }
    
    public void setGrowthRate(double growthRate) {
        this.growthRate = growthRate;
    }
    
    public long getPreviousPeriodSubscribers() {
        return previousPeriodSubscribers;
    }
    
    public void setPreviousPeriodSubscribers(long previousPeriodSubscribers) {
        this.previousPeriodSubscribers = previousPeriodSubscribers;
    }
    
    public double getViewsGrowthRate() {
        return viewsGrowthRate;
    }
    
    public void setViewsGrowthRate(double viewsGrowthRate) {
        this.viewsGrowthRate = viewsGrowthRate;
    }
    
    public double getEngagementGrowthRate() {
        return engagementGrowthRate;
    }
    
    public void setEngagementGrowthRate(double engagementGrowthRate) {
        this.engagementGrowthRate = engagementGrowthRate;
    }
    
    public double getSubscriberRetentionRate() {
        return subscriberRetentionRate;
    }
    
    public void setSubscriberRetentionRate(double subscriberRetentionRate) {
        this.subscriberRetentionRate = subscriberRetentionRate;
    }
    
    public long getReturningSubscribers() {
        return returningSubscribers;
    }
    
    public void setReturningSubscribers(long returningSubscribers) {
        this.returningSubscribers = returningSubscribers;
    }
    
    public long getNewActiveSubscribers() {
        return newActiveSubscribers;
    }
    
    public void setNewActiveSubscribers(long newActiveSubscribers) {
        this.newActiveSubscribers = newActiveSubscribers;
    }
    
    // Additional methods for DAO compatibility
    public int getSubscriberCount() {
        return (int) totalSubscribers;
    }
    
    public void setSubscriberCount(int subscriberCount) {
        this.totalSubscribers = subscriberCount;
    }
    
    public int getUnsubscribes() {
        return (int) unsubscribers;
    }
    
    public void setUnsubscribes(int unsubscribes) {
        this.unsubscribers = unsubscribes;
    }
    
    public double getEngagementRate() {
        return averageEngagementRate;
    }
    
    public void setEngagementRate(double engagementRate) {
        this.averageEngagementRate = engagementRate;
    }
    
    public String getTopPerformingPostId() {
        return mostViewedPostId;
    }
    
    public void setTopPerformingPostId(String topPerformingPostId) {
        this.mostViewedPostId = topPerformingPostId;
    }
    
    public double getAvgViewsPerPost() {
        return averageViewsPerPost;
    }
    
    public void setAvgViewsPerPost(double avgViewsPerPost) {
        this.averageViewsPerPost = avgViewsPerPost;
    }
    
    public void setPeakHour(int peakHour) {
        // No-op - peak hour is calculated from data
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    @Override
    public String toString() {
        return "ChannelAnalytics{" +
                "channelId='" + channelId + '\'' +
                ", periodStart=" + periodStart +
                ", periodEnd=" + periodEnd +
                ", totalSubscribers=" + totalSubscribers +
                ", totalPosts=" + totalPosts +
                ", totalViews=" + totalViews +
                ", growthRate=" + growthRate + "%" +
                ", engagementRate=" + String.format("%.2f", averageEngagementRate * 100) + "%" +
                ", grade=" + getPerformanceGrade() +
                '}';
    }
}
