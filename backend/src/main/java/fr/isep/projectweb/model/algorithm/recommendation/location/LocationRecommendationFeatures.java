package fr.isep.projectweb.model.algorithm.recommendation.location;

/*
 * Location 推荐算法输入特征说明：
 *
 * 这个类只负责保存“算法需要用到的数据”，不负责查询数据库，也不负责排序。
 *
 * 目前它已经被 GET /api/locations/search?keyword=xxx 使用：
 * 1. LocationService 先查询候选 location。
 * 2. LocationService 再从相关表中收集每个 location 的推荐特征。
 * 3. 这些特征会被放进 LocationRecommendationFeatures。
 * 4. LocationRecommendationScorer 根据这些特征计算 score。
 * 5. LocationService 根据 score 排序并返回 LocationResponse。
 *
 * 这样拆分的目的：
 * - Features 只表示算法输入。
 * - Scorer 只负责打分。
 * - Service 负责数据库查询、对象组装和 API 返回。
 * - Controller 保持简单，不直接接触算法细节。
 *
 * 当前 location 推荐算法会使用以下几类特征：
 * 1. 关键词相关性：keyword 与 name、description、address、city、country 的匹配情况。
 * 2. 活跃度：该地点关联的 event 数、即将发生的 event 数、post 数。
 * 3. 图片信息：该地点是否有图片，以及图片数量。
 * 4. 无障碍信息：wheelchairAccessible、hasElevator、accessibleToilet、
 *    quietEnvironment、stepFreeAccess。
 * 5. 信息完整度：是否有描述、地址、城市、国家、经纬度。
 *
 * 这个类保持为纯数据对象，是为了让算法层和数据库/JPA/Controller 解耦。
 */

/**
 * Input data used by the location recommendation/ranking algorithm.
 *
 * This class intentionally contains only plain values. It does not know how to
 * query the database, map DTOs, or handle HTTP requests. LocationService is
 * responsible for collecting these values from locations, events, posts,
 * images, and accessibility records, then passing them to the scorer.
 *
 * Keeping the features separate from the scorer makes the algorithm easier to
 * test and easier to evolve. For example, we can later add user-specific
 * features without changing the controller or repository contracts.
 */
public class LocationRecommendationFeatures {

    /**
     * The keyword used by the current search request.
     *
     * It is optional because the same scorer can later be reused for non-search
     * recommendations. When this value is blank, keyword relevance contributes
     * zero points and the score is based only on popularity, accessibility, media,
     * and profile completeness.
     */
    private String keyword;

    /**
     * Basic searchable text fields from the locations table.
     *
     * These values are used for keyword relevance. They are duplicated here
     * instead of passing the Location entity directly so the scorer stays
     * independent from JPA entities.
     */
    private String name;
    private String description;
    private String address;
    private String city;
    private String country;

    /**
     * Activity signals.
     *
     * eventCount measures general historical/activity density.
     * upcomingEventCount is weighted more strongly because current/future
     * activities are usually more useful to users than old activity.
     * postCount approximates community discussion or user interest around the
     * location.
     */
    private int eventCount;
    private int upcomingEventCount;
    private int postCount;

    /**
     * Media signal.
     *
     * imageCount rewards locations that have at least one image because they are
     * usually easier for users to evaluate in the frontend.
     */
    private int imageCount;

    /**
     * Accessibility signals from location_accessibility.
     *
     * Boolean is used instead of boolean so "unknown" can be represented as
     * null. The scorer currently treats null as false, but preserving null here
     * leaves room for future logic that distinguishes "false" from "not filled".
     */
    private Boolean wheelchairAccessible;
    private Boolean hasElevator;
    private Boolean accessibleToilet;
    private Boolean quietEnvironment;
    private Boolean stepFreeAccess;

    /**
     * True when both latitude and longitude are present.
     *
     * Coordinates are a completeness signal because they allow map display and
     * distance-based features later.
     */
    private boolean hasCoordinates;

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public int getEventCount() {
        return eventCount;
    }

    public void setEventCount(int eventCount) {
        this.eventCount = eventCount;
    }

    public int getUpcomingEventCount() {
        return upcomingEventCount;
    }

    public void setUpcomingEventCount(int upcomingEventCount) {
        this.upcomingEventCount = upcomingEventCount;
    }

    public int getPostCount() {
        return postCount;
    }

    public void setPostCount(int postCount) {
        this.postCount = postCount;
    }

    public int getImageCount() {
        return imageCount;
    }

    public void setImageCount(int imageCount) {
        this.imageCount = imageCount;
    }

    public Boolean getWheelchairAccessible() {
        return wheelchairAccessible;
    }

    public void setWheelchairAccessible(Boolean wheelchairAccessible) {
        this.wheelchairAccessible = wheelchairAccessible;
    }

    public Boolean getHasElevator() {
        return hasElevator;
    }

    public void setHasElevator(Boolean hasElevator) {
        this.hasElevator = hasElevator;
    }

    public Boolean getAccessibleToilet() {
        return accessibleToilet;
    }

    public void setAccessibleToilet(Boolean accessibleToilet) {
        this.accessibleToilet = accessibleToilet;
    }

    public Boolean getQuietEnvironment() {
        return quietEnvironment;
    }

    public void setQuietEnvironment(Boolean quietEnvironment) {
        this.quietEnvironment = quietEnvironment;
    }

    public Boolean getStepFreeAccess() {
        return stepFreeAccess;
    }

    public void setStepFreeAccess(Boolean stepFreeAccess) {
        this.stepFreeAccess = stepFreeAccess;
    }

    public boolean isHasCoordinates() {
        return hasCoordinates;
    }

    public void setHasCoordinates(boolean hasCoordinates) {
        this.hasCoordinates = hasCoordinates;
    }
}
