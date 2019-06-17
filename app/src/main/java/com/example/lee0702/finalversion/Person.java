package com.example.lee0702.finalversion;

public class Person {
    private String name;
    private String number;
    private String ip;
    private String edswitch;

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getNumber() {
        return number;
    }
    public void setNumber(String number) {
        this.number = number;
    }
    public String getIP() {
        return ip;
    }
    public void setIP(String ip) {
        this.ip = ip;
    }
    public String getEdswitch() {
        return edswitch;
    }
    public void setEdswitch(String edswitch) {
        this.edswitch = edswitch;
    }

    public Person(String name, String number) {
        super();
        this.name = name;
        this.number = number;
    }
}
