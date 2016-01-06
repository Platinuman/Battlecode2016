package team061;

import battlecode.common.*;

public class Util extends Bot {

    public static RobotInfo closest(RobotInfo[] robots, MapLocation toHere) {
        RobotInfo closest = null;
        int bestDistSq = 999999;
        for (int i = robots.length; i-- > 0;) {
            int distSq = toHere.distanceSquaredTo(robots[i].location);
            if (distSq < bestDistSq) {
                bestDistSq = distSq;
                closest = robots[i];
            }
        }
        return closest;
    }
}
