package com.dengxq.lnglat2Geo.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BusinessAreaGroup {
    public int cityAdCode;
    public List<BusinessAreaData> areas;
}
