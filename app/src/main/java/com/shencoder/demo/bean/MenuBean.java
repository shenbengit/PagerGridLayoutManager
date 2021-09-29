package com.shencoder.demo.bean;

/**
 * @author ShenBen
 * @date 2021/09/27 20:25
 * @email 714081644@qq.com
 */
public class MenuBean {
    private String title;
    private boolean isEmpty;

    public MenuBean() {
    }

    public MenuBean(String title, boolean isEmpty) {
        this.title = title;
        this.isEmpty = isEmpty;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public boolean isEmpty() {
        return isEmpty;
    }

    public void setEmpty(boolean empty) {
        isEmpty = empty;
    }
}
