package com.debankar.featureflags.domain;

public class Variant {
    private String name;
    private int weight;

    public Variant() {}

    public Variant(String name, int weight) {
        this.name = name;
        this.weight = weight;
    }

    public String getName() { return name; }
    public void setName(String name) {
        this.name = name;
    }

    public int getWeight() { return weight; }
    public void setWeight(int weight) {
        this.weight = weight;
    }

    @Override
    public String toString() {
        return "Variant{" +
                "name='" + name + '\'' +
                ", weight=" + weight +
                '}';
    }
}
