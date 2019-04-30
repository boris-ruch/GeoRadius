package com.boo.georadius;


import com.boo.georadius.util.RequiresRedisServer;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands.DistanceUnit;
import org.springframework.data.redis.connection.RedisGeoCommands.GeoLocation;
import org.springframework.data.redis.core.GeoOperations;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


@RunWith(SpringRunner.class)
@SpringBootTest
public class GeoOperationsTests {

    // we only want to run this tests when redis is up an running
    @ClassRule
    public static RequiresRedisServer requiresServer = RequiresRedisServer.onLocalhost().atLeast("3.2");

    @Autowired
    private RedisOperations<String, String> operations;

    private GeoOperations<String, String> geoOperations;

    private Point thalwil = new Point(47.296143, 8.565117);

    @Before
    public void before() {

        geoOperations = operations.opsForGeo();
        geoOperations.add("Switzerland", thalwil, "Thalwil");
        geoOperations.add("Switzerland", new Point(47.307241, 8.554210), "Rüschlikon");
        geoOperations.add("Switzerland", new Point(47.229341, 8.67204), "Wädenswil");
        geoOperations.add("Switzerland", new Point(46.946877, 7.444299), "Bern");
        geoOperations.add("Switzerland", new Point(47.560757, 7.582703), "Basel");
    }

    @Test
    public void geoRadiusByMember() {

        GeoResults<GeoLocation<String>> byDistance = geoOperations.radius("Switzerland", "Thalwil",
                new Distance(20, DistanceUnit.KILOMETERS));

        assertThat(byDistance).hasSize(3).extracting("content.name").contains("Rüschlikon", "Thalwil", "Wädenswil");

    }


    @Test
    public void geoRadius() {

        Circle circle = new Circle(thalwil, new Distance(20, DistanceUnit.KILOMETERS));
        GeoResults<GeoLocation<String>> result = geoOperations.radius("Switzerland", circle);

        assertThat(result).hasSize(3).extracting("content.name").contains("Rüschlikon", "Thalwil", "Wädenswil");
    }


    @Test
    public void geoDistance() {

        Distance distance = geoOperations.distance("Switzerland", "Thalwil", "Rüschlikon", DistanceUnit.KILOMETERS);

        assertThat(distance.getValue()).isBetween(1d, 2d);
    }


    @Test
    public void geoHash() {

        List<String> geohashes = geoOperations.hash("Switzerland", "Thalwil", "Rüschlikon");

        assertThat(geohashes).hasSize(2).contains("t198sr0p2j0", "t198st0pfs0");
    }
    
}