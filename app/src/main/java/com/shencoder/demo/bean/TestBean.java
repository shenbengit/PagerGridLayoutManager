package com.shencoder.demo.bean;

/**
 * @author ShenBen
 * @date 2021/01/10 17:31
 * @email 714081644@qq.com
 */
public class TestBean {
    private int id;
    private String name;
    private boolean isChecked;

    public TestBean() {
    }

    public TestBean(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isChecked() {
        return isChecked;
    }

    public void setChecked(boolean checked) {
        isChecked = checked;
    }
}
