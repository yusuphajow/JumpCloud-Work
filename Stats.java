/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package yusupha.jumpcloud;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 *
 * @author yusupha
 */
public class Stats {
    
    @JsonProperty("TotalRequests")
    private String totalRequests;
    
    @JsonProperty("AverageTime")
    private String averageTime;

    public String getTotalRequests() {
        return totalRequests;
    }

    public void setTotalRequests(String totalRequests) {
        this.totalRequests = totalRequests;
    }

    public String getAverageTime() {
        return averageTime;
    }

    public void setAverageTime(String averageTime) {
        this.averageTime = averageTime;
    }
    
}
