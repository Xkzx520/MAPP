package com.example.mapp.model;

public class Course {
    private int id;
    private String courseName;
    private String intro;
    private String coverUrl;
    private int courseType;
    private String createTime;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getCourseName() {
        return courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    public String getIntro() {
        return intro;
    }

    public void setIntro(String intro) {
        this.intro = intro;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
    }

    public int getCourseType() {
        return courseType;
    }

    public void setCourseType(int courseType) {
        this.courseType = courseType;
    }

    public String getCreateTime() {
        return createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }

    public String getCourseTypeName() {
        switch (courseType) {
            case 1:
                return "AI生物识别";
            case 2:
                return "交互式模拟";
            case 3:
                return "智能硬件DIY";
            default:
                return "未知类型";
        }
    }
}