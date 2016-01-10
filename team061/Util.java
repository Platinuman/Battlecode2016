package team061;

import battlecode.common.*;

public class Util extends Bot {

   public static RobotInfo closest(RobotInfo[] robots, MapLocation toHere) {
        RobotInfo closest = null;
        int bestDistSq = 999999;
        int distSq;
        for (int i = robots.length; i-- > 0;) {
            distSq = toHere.distanceSquaredTo(robots[i].location);
            if (distSq < bestDistSq) {
                bestDistSq = distSq;
                closest = robots[i];
            }
        }
        return closest;
    }
    
   public static int closestLocation(MapLocation locs[], MapLocation toHere, int size) {
       MapLocation closest = null;
       int bestDistSq = 999999;
       int bestIndex = -1;
       for (int i = 0; i < size; i++) {
    	   if(locs[i] == null){
    		   continue;
    	   }
           int distSq = toHere.distanceSquaredTo(locs[i]);
           if (distSq < bestDistSq) {
               bestDistSq = distSq;
               closest = locs[i];
               i = bestIndex;
           }
       }
       return bestIndex;
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
    
    public static RobotInfo[] combineTwoRIArrays( RobotInfo[] array1, RobotInfo[] array2){
    	RobotInfo[] combo = new RobotInfo[array1.length + array2.length];
    	for (int i = 0; i < array1.length; i++){
			combo[i] = array1[i];
		}
    	for (int i = 0; i < array2.length; i++){
			combo[i + array1.length] = array2[i];
		}
    	return combo;
    }
    
	/**
	 * This method finds the location of the "center of mass" of an array of robots
	 * 
	 * @param robots
	 */
	public static MapLocation centroidOfUnits(RobotInfo[] robots){
		// TODO: this method
		float xavg = 0, yavg = 0;
		MapLocation loc;
		for(int i = 0; i < robots.length; i++){
			loc = robots[i].location;
			xavg += loc.x;
			yavg += loc.y;
		}
		return new MapLocation(Math.round(xavg/robots.length), Math.round(yavg/robots.length));
	}

	public static boolean containsMapLocation(MapLocation[] locs, MapLocation location, int size) {
		for(int i = 0; i < size; i++){
			MapLocation loc = locs[i];
			if(locs[i] == null){
				continue;
			}
			if(loc.equals(location)){
				return true;
			}
		}
		return false;
	}
}