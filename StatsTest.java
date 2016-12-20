/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package yusupha.jumpcloud.test;

import java.io.IOException;
import com.fasterxml.jackson.databind.ObjectMapper;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.junit.Assert;
import org.junit.Test;
import static org.apache.commons.codec.binary.Base64.isBase64;
import yusupha.jumpcloud.Stats;

/**
 *
 * @author yusupha
 */
public class StatsTest {

    protected static final WebTarget TARGET;
    public String angryMonkeyPassword = "{\"password\":\"angrymonkey\"}";

    static {
        TARGET = ClientBuilder.newClient().target("https://radiant-gorge-83016.herokuapp.com/");
    }

    /*Submit POST request with password and return Job ID*/
    public String submitPostWithPassword(String password) {
        String jobId = TARGET.path("hash")
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.entity(password, MediaType.APPLICATION_JSON), String.class);
        return jobId;
    }

    /*Submit POST request to shutdown server*/
    public Response submitPostWithShutdown() {
        String inputShutdown = "shutdown";
        Response responseShutdown = TARGET.path("hash")
                .request(MediaType.TEXT_PLAIN_TYPE)
                .post(Entity.entity(inputShutdown, MediaType.APPLICATION_JSON), Response.class);
        return responseShutdown;
    }

    /*Submit GET request for Stats*/
    public Stats submitGetStats() throws IOException {
        String restRequestStats = TARGET.path("stats")
                .request(MediaType.TEXT_PLAIN_TYPE)
                .get(String.class);
        ObjectMapper mapper = new ObjectMapper();
        Stats stats = (Stats) mapper.readValue(restRequestStats, Stats.class);
        return stats;
    }

    /*Submit Get request for specified password string Hash path*/
    public String submitGetHash(String jobId) {
        String passHashCode = TARGET.path("hash/" + jobId)
                .request(MediaType.TEXT_PLAIN_TYPE)
                .get(String.class);
        return passHashCode;

    }

    /*Method to convert strings to doubles so we can do calculations on the stats*/
    public Double convertStrDbl(String stat) {
        stat = stat.replaceAll(",", "");
        Integer.parseInt(stat);
        return Double.parseDouble(stat);
    }


    /* 
    *Verify status code 200 returned
    *Verifies stats are reset which per spec confirms successful shutdown
     */
    @Test
    public void serverShutdown() throws IOException {
        if (submitGetStats().getAverageTime().equals("0") || submitGetStats().getTotalRequests().equals("0")) {
            submitPostWithPassword(angryMonkeyPassword);
        }
        //Verify status code 200 returned
        Assert.assertEquals(200, submitPostWithShutdown().getStatus());
        //Verifies stats are reset which per spec confirms successful shutdown
        Assert.assertEquals("0", submitGetStats().getAverageTime());
        Assert.assertEquals("0", submitGetStats().getTotalRequests());
    }


    /*
    *Verify password hash returned contain base 64 characters
    *Verify same encoding is returned for POST requests that use same password
    *Verify correct message for non-existent jobId
     */
    @Test
    public void statsAfterPost() throws IOException {

        //Verify base 64 characters in hash
        Assert.assertTrue(isBase64(submitGetHash(submitPostWithPassword(angryMonkeyPassword))));
        //Verify same hash encoding is returned for Post requests
        Assert.assertEquals(submitGetHash(submitPostWithPassword(angryMonkeyPassword)), submitGetHash(submitPostWithPassword(angryMonkeyPassword)));
    }

    /*
    *Using multiple gets:
    *Verify average time for Get hash
    *Verify average time to generate hash is greater than 5000 milliseconds
    *Verify number of hash requests is correct
     */
    @Test
    public void multiplePostsFollowedByMultipleGets() throws IOException {
        //Resets stats counts if necessary
        if (!submitGetStats().getAverageTime().equals("0") || !submitGetStats().getTotalRequests().equals("0")) {
            submitPostWithShutdown();
        }

        int numTests = 10;
        String[] passwords = new String[numTests];
        String[] ids = new String[numTests];
        String[] hashes = new String[numTests];

        for (int index = 0; index < numTests; index++) {
            passwords[index] = "{\"password\":\""
                    + Long.toHexString(Double.doubleToLongBits(Math.random()))
                    + "\"}";
        }

        long start = System.currentTimeMillis();

        for (int index = 0; index < numTests; index++) {
            ids[index] = submitPostWithPassword(passwords[index]);
        }

        long timePosts = System.currentTimeMillis() - start;

        start = System.currentTimeMillis();

        for (int index = 0; index < numTests; index++) {
            hashes[index] = submitGetHash(ids[index]);
        }

        long timeGets = System.currentTimeMillis() - start;
        double avgHashStats = convertStrDbl(submitGetStats().getAverageTime());
        int getReqStats = Integer.parseInt(submitGetStats().getTotalRequests());
        double averagePosts = timePosts / numTests;
        double averageHashGets = timeGets / numTests;
        System.out.println("Average Posts: " + averagePosts + "; Average Gets:" + averageHashGets);
        //Verify average time for Get hash
        Assert.assertEquals(averageHashGets, avgHashStats, 10);
        //Verify stats requests reported are correct
        Assert.assertEquals(numTests, getReqStats);
        //Verify average time to generate hash is greater than 5000 milliseconds
        Assert.assertTrue(averagePosts > 5000);
    }

    /*Verify that multiple concurrent connections can be processed
     */
    @Test
    public void concurrentPostsAndGets() throws IOException {
        int initReqs = Integer.parseInt(submitGetStats().getTotalRequests());
          new Thread( new Runnable() {
            public void run() {
               submitGetHash(submitPostWithPassword(angryMonkeyPassword));
            }
        }).start();

        new Thread( new Runnable() {
            public void run() {
               submitGetHash(submitPostWithPassword(angryMonkeyPassword));
            }
        }).start();
        int noConcReqs = Integer.parseInt(submitGetStats().getTotalRequests()) - initReqs;

        //Verify concurrent posts requests were succesful
        Assert.assertEquals(2, noConcReqs);
    }

    /*Verify shutdown is handled gracefully
    *This means shutdown waits for other processes
    *such as Post to complete before shutdown is finalized
     */
    @Test
    public void postsAfterShutdown() {
        String hashIdBefore = submitGetHash((submitPostWithPassword(angryMonkeyPassword)));
        submitPostWithShutdown();
        System.out.println(hashIdBefore);
        Assert.assertNotNull(hashIdBefore);
    }
}
