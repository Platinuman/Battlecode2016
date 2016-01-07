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
    
    public static RobotInfo leastHealth(RobotInfo[] robots, int excludeArchons) {
        RobotInfo ret = null;
        double minHealth = 1e99;
        for(int i = robots.length; i --> 0; ) {
            if(robots[i].health < minHealth && (excludeArchons == 0 || robots[i].type != RobotType.ARCHON)) {
                minHealth = robots[i].health;
                ret = robots[i];
            }
        }
        return ret;
    }
    
    public static Team getTeam(RobotType type){
    	switch(type){
    	case ARCHON:
    	case GUARD:
    	case SCOUT:
    	case SOLDIER:
    	case TTM:
    	case TURRET:
    	case VIPER:
    		return us.opponent();
    	default:
    		return Team.ZOMBIE;
    	}
    }
}
