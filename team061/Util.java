package team061;

import battlecode.common.*;

public class Util extends Bot {//NEW generic methods for use by many classes, optimization is key once again.

	public static boolean isSurrounded(MapLocation loc) throws GameActionException {// NEW move to Util
		Direction dir = Direction.NORTH;
		Boolean surrounded = true;
		for (int i = 0; i < 8; i++) {
			MapLocation newLoc = loc.add(dir);
			if (rc.canSenseLocation(newLoc) && rc.onTheMap(newLoc) && !rc.isLocationOccupied(newLoc) && rc.senseRubble(newLoc) < GameConstants.RUBBLE_OBSTRUCTION_THRESH) {
				surrounded = false;
				break;
			}
			dir = dir.rotateLeft();
		}
		return surrounded;
	}
	
	public static boolean checkRubbleAndClear(Direction dir,boolean clearToughRubble) throws GameActionException { // NEW Now checks all directions
		int toughRubble = (int) (GameConstants.RUBBLE_OBSTRUCTION_THRESH*2);
		double rubble = rc.senseRubble(here.add(dir));
		if (rubble > 0 && (clearToughRubble || rubble <= toughRubble)){
			rc.clearRubble(dir);
			return true;
		}
		Direction dirLeft = dir.rotateLeft();
		Direction dirRight = dir.rotateRight();
		dirLeft = dir.rotateLeft();
		dirRight = dir.rotateRight();
		for (int i = 0; i <= 4; i++) {
			rubble = rc.senseRubble(here.add(dirLeft));
			if ( rubble > 0 && (clearToughRubble || rubble <= toughRubble)) {
				rc.clearRubble(dirLeft);
				return true;
			}
			rubble = rc.senseRubble(here.add(dirRight));
			if (rubble > 0 && (clearToughRubble || rubble <= toughRubble)) {
				rc.clearRubble(dirRight);
				return true;
			}
			dirLeft = dirLeft.rotateLeft();
			dirRight = dirRight.rotateRight();
		}
		return false;
	}
	
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
       //MapLocation closest = null;
       int bestDistSq = 999999;
       int bestIndex = -1;
       for (int i = 0; i < size; i++) {
    	   if(locs[i] == null){
    		   continue;
    	   }
           int distSq = toHere.distanceSquaredTo(locs[i]);
           if (distSq < bestDistSq) {
               bestDistSq = distSq;
               //closest = locs[i];
               bestIndex = i;
           }
       }
       return bestIndex;
   }
      
   public static RobotInfo leastHealth(RobotInfo[] robots, int excludeArchons) {
		RobotInfo ret = null;
		double minHealth = 1e99;
		for (int i = 0; i < robots.length; i++) {
			if ((int)robots[i].health != (int)robots[i].maxHealth &&  robots[i].health < minHealth && (excludeArchons == 0 || robots[i].type != RobotType.ARCHON)) {
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
    
    public static RobotInfo[] combineTwoRIArrays( RobotInfo[] array1, int a1size, RobotInfo[] array2){
    	RobotInfo[] combo = new RobotInfo[a1size + array2.length];
    	for (int i = 0; i < a1size; i++){
			combo[i] = array1[i];
		}
    	for (int i = 0; i < array2.length; i++){
			combo[i + a1size] = array2[i];
		}
    	return combo;
    }
    
    public static RobotInfo[] combineTwoRIArrays( RobotInfo[] array1, RobotInfo[] array2, int a2size){
    	RobotInfo[] combo = new RobotInfo[array1.length + a2size];
    	for (int i = 0; i < array1.length; i++){
			combo[i] = array1[i];
		}
    	for (int i = 0; i < a2size; i++){
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
		int xavg = 0, yavg = 0;
		MapLocation loc;
		for(int i = 0; i < robots.length; i++){
			loc = robots[i].location;
			xavg += loc.x;
			yavg += loc.y;
		}
		return new MapLocation(Math.round(xavg/robots.length), Math.round(yavg/robots.length));
    }
    
    public static boolean isDangerous(RobotType theType){
    	switch(theType){
    	case ARCHON:
    		return false;
    	case TTM:
    		return false;
    	case ZOMBIEDEN:
    		if(getRoundsUntilNextZombieSpawn() < 20) return true;
    		return false;
    	case SCOUT:
    		return false;
    	default:
    	}
    	return true;
    }

    private static int getRoundsUntilNextZombieSpawn() {
    	// TODO Auto-generated method stub
    	int[] schedule = MapAnalysis.zombieRounds;
    	int nextRound = 9999;
    	int currentRound = rc.getRoundNum();
    	for(int i = 0; i < schedule.length; i++)
    		if(currentRound < schedule[i]){
    			nextRound = schedule[i];
    			break;
    		}
    	return nextRound - currentRound;
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

	public static RobotInfo farthestSpecificType(RobotInfo[] robots, MapLocation toHere, RobotType type) {
        RobotInfo farthest = null;
        int bestDistSq = -1;
        int distSq;
        for (int i = robots.length; i-- > 0;) {
            distSq = toHere.distanceSquaredTo(robots[i].location);
            if (distSq > bestDistSq && robots[i].type == type) {
                bestDistSq = distSq;
                farthest = robots[i];
            }
        }
        return farthest;
	}
	
	public static RobotInfo closestSpecificType(RobotInfo[] robots, MapLocation toHere, RobotType type) {
        RobotInfo farthest = null;
        int bestDistSq = 99999;
        int distSq;
        for (int i = robots.length; i-- > 0;) {
            distSq = toHere.distanceSquaredTo(robots[i].location);
            if (distSq < bestDistSq && robots[i].type == type) {
                bestDistSq = distSq;
                farthest = robots[i];
            }
        }
        return farthest;
	}
	
	public static void removeIndexFromArray(Object[] array, int index, int size){
		for(int i = index; i < size - 1; i++){
			array[i] = array[i+1];
		}
	}

	public static RobotInfo[] getUnitsOfType(RobotInfo[] array, RobotType t) {
		int[] inds = new int[array.length];
		int s = 0;
		for(int i = array.length; i --> 0; ){
			if(array[i].type == t){
				inds[s++] = i;
			}
		}
		RobotInfo[] units = new RobotInfo[s];
		for(int i = s ; i --> 0 ; ){
			units[i] = array[inds[i]]; 
		}
		return units;
	}

    public static RobotInfo[] combineThreeRIArrays( RobotInfo[] array1, int a1size, RobotInfo[] array2, RobotInfo[] array3){
    	RobotInfo[] combo = new RobotInfo[a1size + array2.length + array3.length];
    	for (int i = 0; i < a1size; i++){
			combo[i] = array1[i];
		}
    	for (int i = 0; i < array2.length; i++){
			combo[i + a1size] = array2[i];
		}
    	for (int i = 0; i < array3.length; i++){
			combo[i + a1size + array2.length] = array3[i];
		}
    	return combo;
    }

	public static double rubbleBetweenHereAndThere(MapLocation start, MapLocation end) {
		MapLocation current = start;
		double totalRubble = 0;
		while(!current.equals(end)){
			current = current.add(current.directionTo(end));
			if(rc.canSenseLocation(current))
				totalRubble += rc.senseRubble(current);
			else
				break;
		}
		return totalRubble;
	}

	public static RobotInfo[] removeHarmlessUnits(RobotInfo[] hostiles) {
		int newlength = 0;
		int[] inds = new int[hostiles.length];
		for (int i = 0; i < hostiles.length; i++){
			if(isDangerous(hostiles[i].type)){
				inds[newlength++] = i;
			}
		}
		RobotInfo[] harmfulUnits = new RobotInfo[newlength];
		for (int j = 0; j < newlength; j++){
			harmfulUnits[j] = hostiles[inds[j]];
		}
		return harmfulUnits;
	}
	
	public static MapLocation getLocationOfType(RobotInfo[] array, RobotType t){
		for(RobotInfo ri : array)
			if(ri.type == t)
				return ri.location;
		return null;
	}
	
	public static int indexOfLocation(MapLocation[] array, int arraySize, MapLocation loc){
		for(int i = arraySize; i --> 0; )
			if(array[i] != null && array[i].equals(loc))
				return i;
		return -1;
	}
}
