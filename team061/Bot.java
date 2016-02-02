package team061;

import java.util.*;
import battlecode.common.*;

public class Bot {
	public static RobotController rc;
	protected static Team us;
	protected static Team them;
	protected static RobotType type;
	public static MapLocation here; // bot classes are responsible for keeping this up to date
	public static MapLocation center;
	protected static Random rand;
	public static MapLocation[] targetDens;
	public static int targetDenSize;
	public static MapLocation[] killedDens;
	public static int killedDenSize;
	public static MapLocation targetLoc;
	public static int bestIndex;
	public static int numDensToHunt;
	public static int turnCreated;
	public static Direction directionIAmMoving;
	public static MapLocation[] initialEnemyArchonLocs;
	// TODO: get rid of this stupid directions thing and use direction order and dir.ordinal()
	protected static Direction[] directions = { Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST,
			Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST };
	protected static int[] directionOrder = {0,1,-1,2,-2,3,-3,4};
	public static RobotInfo[] enemyTurrets;
	public static int turretSize;
	public static int lastTurnFled;
	
	protected static void init(RobotController theRC) throws GameActionException {
		rc = theRC;
		turnCreated = rc.getRoundNum();
		us = rc.getTeam();
		them = us.opponent();
		//initialEnemyArchonLocs = rc.getInitialArchonLocations(them); Set in MapAnalysis
		here = rc.getLocation();
		rand = new Random(rc.getID());
		type = rc.getType();
		targetDens = new MapLocation[10000];
		killedDens = new MapLocation[10000];
		targetDenSize = bestIndex = numDensToHunt = killedDenSize = 0;
		MapAnalysis.analyze();
		enemyTurrets = new RobotInfo[64];
		turretSize = 0;
	}

//---- Enemy turret tracking methods below ----
	/*
	 * Keeps known turret location list up to date based only on message signals (from scouts).
	 */
	public static boolean updateTurretList(Signal[] signals) throws GameActionException{
		boolean updated = false;
		for (Signal signal : signals) {
			if (signal.getTeam() == us) {
				int[] message = signal.getMessage();
				if (message != null) {
					MessageEncode purpose = MessageEncode.whichStruct(message[0]);
					int[] data;
					MapLocation senderloc, loc;
					switch(purpose){
					case ENEMY_TURRET_DEATH:
						data = purpose.decode(signal.getLocation(), message);
						removeLocFromTurretArray(new MapLocation(data[0],data[1]));
						updated = true;
						break;
					case WARN_ABOUT_TURRETS:
						senderloc = signal.getLocation();
						data = purpose.decode(senderloc, message);
						loc = new MapLocation(data[0], data[1]);
						if(!isLocationInTurretArray(loc)){
							enemyTurrets[turretSize]= new RobotInfo(0, them, RobotType.TURRET, loc,0,0,0,0,0,0,0);
							turretSize++;
						}
						updated = true;
						break;
					case RELAY_TURRET_INFO:
						senderloc = signal.getLocation();
						data = purpose.decode(senderloc, message);
						for(int i = 0; i< data.length; i +=2){
							loc = new MapLocation(data[i], data[i+1]);
							if(loc.equals(senderloc)){
								break;
							}
							if(!isLocationInTurretArray(loc)){
								enemyTurrets[turretSize]= new RobotInfo(0, them, RobotType.TURRET, loc,0,0,0,0,0,0,0);
								turretSize++;
							}
						}
						updated = true;
					default:
					}
				}
			}
		}
		return updated;
	}
	
	public static boolean removeLocFromTurretArray(MapLocation loc) {
		for(int i = 0 ; i < turretSize; i++){
			if( enemyTurrets[i].location.equals(loc)){
				Util.removeIndexFromArray(enemyTurrets, i, turretSize--);
				return true;
			}
		}
		return false;
	}
	public static boolean isLocationInTurretArray(MapLocation loc){
		for(int i = 0 ; i < turretSize; i++){
			if( enemyTurrets[i].location.equals(loc)){
				return true;
			}
		}
		return false;
	}
	public static int numTurretsInRangeSquared(MapLocation loc, int range){
		int count = 0;
		for(int i = 0; i < turretSize ; i++){
			if(enemyTurrets[i].location.distanceSquaredTo(loc) <= range)
				count++;
		}
		return count;
	}
	public static boolean isInRangeOfTurrets(MapLocation loc){
		for(int i = 0 ; i < turretSize; i++){
			if(enemyTurrets[i].location.distanceSquaredTo(loc) <= RobotType.TURRET.attackRadiusSquared){
				return true;
			}
		}
		return false;
	}
}
