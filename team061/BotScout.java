package team061;

import java.util.Random;

import battlecode.common.*;

public class BotScout extends Bot {
    public static void loop(RobotController theRC) throws GameActionException {
        Bot.init(theRC);
//        Debug.init("micro");
        while (true) {
            try {
                turn();
            } catch (Exception e) {
                e.printStackTrace();
            }
            Clock.yield();
        }
    }

    private static void turn() throws GameActionException {
    	RobotInfo[] enemyLocations = rc.senseHostileRobots(rc.getLocation(),RobotType.SCOUT.sensorRadiusSquared);
    	
    }
}