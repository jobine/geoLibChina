package com.dengxq.lnglat2Geo.entity;

public class Bound {
    public Location mix;
    public Location max;

    public Bound(Location mix, Location max) {
        this.mix = mix;
        this.max = max;
    }

    public Location getMix() {
        return mix;
    }

    public Location getMax() {
        return max;
    }

    public void setMix(Location mix) {
        this.mix = mix;
    }

    public void setMax(Location max) {
        this.max = max;
    }
}
