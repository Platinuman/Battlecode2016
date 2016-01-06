package team061;

import battlecode.common.*;

public class Util extends Bot {

   public static RobotInfo closest(RobotInfo[] robots, MapLocation toHere) {
        RobotInfo ret = null;
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
    
    public static RobotInfo leastHealth(RobotInfo[] robots) {
        RobotInfo ret = null;
        double minHealth = 1e99;
        for(int i = robots.length; i --> 0; ) {
            if(robots[i].health < minHealth) {
                minHealth = robots[i].health;
                ret = robots[i];
            }
        }
        return ret;
    }
}
>>>>>>> 03fcacb5af2e42974fe1bb553e850e5892debc97
