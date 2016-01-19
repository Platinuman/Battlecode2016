package OrganizedBot;

import battlecode.common.*;

public class BotScout extends Bot {
	protected static int scoutType; // NEW 0 = turret helper; 1 = mobile helper,
									// 2 = explorer etc
	/*
	 * NEW re add these if they are absolutely necessary (many will be) static
	 * MapLocation alpha; static MapLocation mobileLoc; static int mobileID;
	 * static boolean isMobile; static Direction directionIAmMoving; static int
	 * lastSignaled; static MapLocation[] partAndNeutralLocs; static int[]
	 * partsOrNeutrals; static int size; static MapLocation[] dens; static int
	 * denSize; static boolean withinRange; // static MapLocation[]
	 * preferredScoutLocations; static MapLocation dest; static int range; //
	 * static boolean atScoutLocation; static MapLocation lastBroadcasted;
	 * static int lastBroadcastedType;
	 */
	static MapLocation[] dens;
	static int denSize;
	static boolean foundTurtle;

	public static void loop(RobotController theRC) throws GameActionException {
		Bot.init(theRC);
		init();
		while (true) {
			try {
				turn();
			} catch (Exception e) {
				e.printStackTrace();
			}
			Clock.yield();
		}
	}
	
	private static void init() throws GameActionException {
		foundTurtle = false;
		// MessageEncode.determineScoutType(); // NEW, based on strategy from
		// archon in messages or something else

		/*
		 * // atScoutLocation = false; NEW move this so MESSAGE ENCODE size = 0;
		 * partAndNeutralLocs = new MapLocation[10000]; partsOrNeutrals = new
		 * int[10000]; range = 3;
		 */
		/*
		 * THIS SHOULD BE IN MESSAGE ENCODE Signal[] signals =
		 * rc.emptySignalQueue(); for (int i = 0; i < signals.length; i++) {
		 * int[] message = signals[i].getMessage(); MessageEncode msgType =
		 * MessageEncode.whichStruct(message[0]); if (signals[i].getTeam() == us
		 * && msgType == MessageEncode.ALPHA_ARCHON_LOCATION) { int[]
		 * decodedMessage =
		 * MessageEncode.ALPHA_ARCHON_LOCATION.decode(signals[i].getLocation(),
		 * message); alpha = new MapLocation(decodedMessage[0],
		 * decodedMessage[1]); isMobile = false; //rc.setIndicatorString(0 ,
		 * "i have an alpha"); break; } else if (signals[i].getTeam() == us &&
		 * msgType == MessageEncode.MOBILE_ARCHON_LOCATION){ int[]
		 * decodedMessage =
		 * MessageEncode.MOBILE_ARCHON_LOCATION.decode(signals[i].getLocation(),
		 * message); mobileLoc = new MapLocation(decodedMessage[0],
		 * decodedMessage[1]); isMobile = true; dens = new MapLocation[10000];
		 * mobileID = signals[i].getID(); rc.setIndicatorString(0 ,"i am mobile"
		 * ); break; } }
		 */
		/*
		 * preferredScoutLocations = new MapLocation[] { alpha.add(2, 2),
		 * alpha.add(2, -2), alpha.add(-2, 2), alpha.add(-2, -2), alpha.add(4,
		 * 2), alpha.add(4, -2), alpha.add(-4, 2), alpha.add(-4, -2),
		 * alpha.add(2, 4), alpha.add(2, -4), alpha.add(-2, 4), alpha.add(-2,
		 * -4) };
		 */
		scoutType = 0;
		denSize = 0;
		dens = new MapLocation[10000];
	}

	/*
	 * NEW MOVE THIS TO UTIL private static boolean
	 * checkRubbleAndClear(Direction dir) {
	 * 
	 * // if (rc.senseRubble(rc.getLocation().add(dir)) >=
	 * GameConstants.RUBBLE_OBSTRUCTION_THRESH) { if
	 * (rc.senseRubble(rc.getLocation().add(dir)) > 0) { try {
	 * rc.clearRubble(dir); } catch (Exception e) {
	 * System.out.println(e.getMessage()); e.printStackTrace(); } return true; }
	 * return false; }
	 */
	private static void turn() throws GameActionException {
		here = rc.getLocation();
		switch (scoutType) { // NEW should call methods in Harass why the hell should they be in harass they're literally only for scouts
		case 0://exploring
			RobotInfo[] hostileRobots = rc.senseHostileRobots(here, RobotType.SCOUT.sensorRadiusSquared);
			RobotInfo[] enemies = rc.senseNearbyRobots(here, RobotType.SCOUT.sensorRadiusSquared, them);
			Nav.explore();
			if(!foundTurtle)
			notifySoldiersOfTurtle(hostileRobots);
			notifySoldiersOfZombieDen(hostileRobots);
			if(rc.getRoundNum() % 5 == 0){
				notifySoldiersOfEnemyArmy();
			}
			notifySoldiersOfEnemyArmy();
			if(rc.getRoundNum() % 10 == 0){
				notifyArchonOfPartOrNeutral();
			}
			break;
		case 1:
			break;
		case 2:
			break;
		default:
			break;
		}
		return;
		/*
		 * This should all be moved to Harass if(!isMobile){ if
		 * (rc.isCoreReady()) { moveToLocFartherThanAlphaIfPossible(here); } if
		 * (rc.isCoreReady()) { Direction dirToClear = Direction.NORTH; for (int
		 * i = 0; i < 8; i++) { if (checkRubbleAndClear(dirToClear)) { break; }
		 * dirToClear = dirToClear.rotateRight(); } }
		 * 
		 * 
		 * RobotInfo[] enemyRobots = rc.senseHostileRobots(rc.getLocation(),
		 * RobotType.SCOUT.sensorRadiusSquared); for (int i = 0; i <
		 * enemyRobots.length; i++) { if (i == 20) { break; } MapLocation loc =
		 * enemyRobots[i].location; double health = enemyRobots[i].health;
		 * RobotType type = enemyRobots[i].type; int[] message =
		 * MessageEncode.TURRET_TARGET .encode(new int[] { (int) (health),
		 * type.ordinal(), loc.x, loc.y });
		 * rc.broadcastMessageSignal(message[0], message[1], (int)
		 * (RobotType.SCOUT.sensorRadiusSquared *
		 * GameConstants.BROADCAST_RANGE_MULTIPLIER)); rc.setIndicatorString(3 ,
		 * "i recommend" + loc.x + ", " + loc.y); } Signal[] signals =
		 * rc.emptySignalQueue(); updateMaxRange(signals); } else{
		 * if(rc.getRoundNum() % 5 == 0 && rc.senseHostileRobots(here,
		 * RobotType.SCOUT.sensorRadiusSquared).length == 0){
		 * addPartsAndNeutrals(); } updateMobileLocation(); if(rc.getRoundNum()
		 * < roundToStopHuntingDens) explore(); else followArchon(); }
		 * 
		 */
		/*
		 * if (!atScoutLocation) { for (int i = 0; i <
		 * preferredScoutLocations.length; i++) { if
		 * (preferredScoutLocations[i].equals(here)) { atScoutLocation = true; }
		 * } }
		 * 
		 * if (!atScoutLocation && dest == null) { if (rc.isCoreReady()) { for
		 * (int i = 0; i < preferredScoutLocations.length; i++) { MapLocation
		 * scoutLocation = preferredScoutLocations[i]; if
		 * (rc.canSense(scoutLocation)) { if
		 * (!rc.isLocationOccupied(scoutLocation) && rc.onTheMap(scoutLocation))
		 * { NavSafetyPolicy theSafety = new
		 * SafetyPolicyAvoidAllUnits(enemyRobots);
		 * if(theSafety.isSafeToMoveTo(scoutLocation)){ dest = scoutLocation;
		 * Nav.goTo(scoutLocation, theSafety); } } } } } } else if
		 * (!atScoutLocation && rc.isCoreReady()){ NavSafetyPolicy theSafety =
		 * new SafetyPolicyAvoidAllUnits(enemyRobots);
		 * if(theSafety.isSafeToMoveTo(dest)){ Nav.goTo(dest, theSafety); }
		 * else{ dest = null; } }
		 */
	}
	private static void notifySoldiersOfEnemyArmy() throws GameActionException{
		RobotInfo[] enemies = rc.senseNearbyRobots(here, RobotType.SCOUT.sensorRadiusSquared, them);
		if(enemies.length > 2){
			int[] myMsg = MessageEncode.ENEMY_ARMY_NOTIF.encode(new int[] { enemies[0].location.x, enemies[0].location.y });
			rc.broadcastMessageSignal(myMsg[0], myMsg[1], 5000);
		}
	}

	private static void notifyArchonOfPartOrNeutral() throws GameActionException {
		MapLocation[] possibleLocs = here.getAllMapLocationsWithinRadiusSq(here, RobotType.SCOUT.sensorRadiusSquared);
		MapLocation partOrNeutralLoc = null;
		for (MapLocation loc : possibleLocs) {
			if (!rc.canSense(loc)) {
				continue;
			}
			if (rc.senseParts(loc) > 0) {
				partOrNeutralLoc = loc;
				break;
			} else {
				RobotInfo ri = rc.senseRobotAtLocation(loc);
				if (ri != null && ri.team == Team.NEUTRAL) {
					partOrNeutralLoc = loc;
					break;
				}
			}
		}
		if(partOrNeutralLoc != null){
			int[] myMsg = MessageEncode.PART_OR_NEUTRAL_NOTIF.encode(new int[] { partOrNeutralLoc.x, partOrNeutralLoc.y });
			rc.broadcastMessageSignal(myMsg[0], myMsg[1], 4000);
		}
	}

	/*
	 * private static void followArchon() throws GameActionException{
	 * if(rc.isCoreReady()){ RobotInfo[] hostileRobots =
	 * rc.senseHostileRobots(here, RobotType.SCOUT.sensorRadiusSquared);
	 * NavSafetyPolicy theSafety = new SafetyPolicyAvoidAllUnits(hostileRobots);
	 * Nav.goTo(mobileLoc, theSafety); } //the following part should tell the
	 * archon the next location only if it has finished doing it's job. it
	 * doesn't work now. if(withinRange){ if(lastBroadcasted != null &&
	 * rc.canSense(lastBroadcasted)){ RobotInfo ri =
	 * rc.senseRobotAtLocation(lastBroadcasted); int type = lastBroadcastedType;
	 * //0 if part, 1 if neutral if(ri != null && (ri.ID == mobileID || (type ==
	 * 1 && ri.team == us))) notifyArchonAboutClosestPartOrNeutral(); } else{
	 * notifyArchonAboutClosestPartOrNeutral(); } }
	 * 
	 * }
	 * 
	 * private static void updateMobileLocation() { Signal[] signals =
	 * rc.emptySignalQueue(); if(rc.getRoundNum() == roundToStopHuntingDens){
	 * for (int i = 0; i < signals.length; i++) { int[] message =
	 * signals[i].getMessage(); MessageEncode msgType =
	 * MessageEncode.whichStruct(message[0]); if (signals[i].getTeam() == us &&
	 * msgType == MessageEncode.MOBILE_ARCHON_LOCATION){ int[] decodedMessage =
	 * MessageEncode.MOBILE_ARCHON_LOCATION.decode(signals[i].getLocation(),
	 * message); mobileLoc = new MapLocation(decodedMessage[0],
	 * decodedMessage[1]); } } } if(rc.getRoundNum() > roundToStopHuntingDens){
	 * RobotInfo[] allies =
	 * rc.senseNearbyRobots(RobotType.SCOUT.sensorRadiusSquared, us);
	 * withinRange = false; for(RobotInfo ally : allies){ if(ally.ID ==
	 * mobileID){ mobileLoc = ally.location; withinRange = true; break; } } } }
	 * */
	  /*
	 * private static void addPartsAndNeutrals() throws GameActionException{
	 * //add all seen parts and neutrals to arrays, in a corresponding array add
	 * a 1 if it's a neutral (stay 0 if part) MapLocation[] possibleLocs =
	 * here.getAllMapLocationsWithinRadiusSq(here,
	 * RobotType.SCOUT.sensorRadiusSquared); for(MapLocation loc: possibleLocs){
	 * if(!rc.canSense(loc)){ continue; } if(rc.senseParts(loc) > 0 ){
	 * partAndNeutralLocs[size] = loc; size++; } else{ RobotInfo ri =
	 * rc.senseRobotAtLocation(loc); if(ri != null && ri.team == Team.NEUTRAL){
	 * partAndNeutralLocs[size] = loc; partsOrNeutrals[size] = 1; size++; } } }
	 * }
	 * 
	 * private static void notifyArchonAboutClosestPartOrNeutral() throws
	 * GameActionException{ int bestIndex =
	 * Util.closestLocation(partAndNeutralLocs, mobileLoc, size); MapLocation
	 * closestPartOrNeutral = here; if(bestIndex != -1){ closestPartOrNeutral =
	 * partAndNeutralLocs[bestIndex]; while(true){ partAndNeutralLocs[bestIndex]
	 * = null; if(rc.canSense(closestPartOrNeutral) &&
	 * !Combat.isSafe(closestPartOrNeutral)){ bestIndex =
	 * Util.closestLocation(partAndNeutralLocs, mobileLoc, size); if(bestIndex
	 * == -1) break; } else{ break; } } } if(bestIndex == -1){ // int[] msg =
	 * MessageEncode.STOP_BEING_MOBILE.encode(new int[]{mobileLoc.x,
	 * mobileLoc.y}); //
	 * rc.broadcastMessageSignal(msg[0],msg[1],here.distanceSquaredTo(mobileLoc)
	 * ); } else{ int type = partsOrNeutrals[bestIndex]; int[] msg =
	 * MessageEncode.DIRECT_MOBILE_ARCHON.encode(new
	 * int[]{closestPartOrNeutral.x, closestPartOrNeutral.y});
	 * rc.broadcastMessageSignal(msg[0],msg[1],here.distanceSquaredTo(mobileLoc)
	 * ); lastBroadcasted = closestPartOrNeutral; lastBroadcastedType = type; }
	 * }
	 */
	private static void notifySoldiersOfTurtle(RobotInfo[] hostileRobots) throws GameActionException { 																								// first
		//RobotInfo[] turtleLocs;
		for (RobotInfo hostileUnit : hostileRobots) {
			if (hostileUnit.type == RobotType.TURRET) {
				MapLocation turtleLoc = hostileUnit.location;
				int[] myMsg = MessageEncode.WARN_ABOUT_TURRETS.encode(new int[] {turtleLoc.x, turtleLoc.y,-1,-1,-1,-1,-1,-1,-1,-1 });
				rc.broadcastMessageSignal(myMsg[0], myMsg[1], 10000);
				rc.setIndicatorString(1, "see a turtle and am notifiying");
			}
		}
	foundTurtle = true;
	}

	private static boolean notifySoldiersOfZombieDen(RobotInfo[] hostileRobots) throws GameActionException { 																								// first
		for (RobotInfo hostileUnit : hostileRobots) {
			if (hostileUnit.type == RobotType.ZOMBIEDEN) {
				if (!Util.containsMapLocation(dens, hostileUnit.location, denSize)) {
					dens[denSize] = hostileUnit.location;
					denSize++;
					MapLocation hostileLoc = hostileUnit.location;
					int[] myMsg = MessageEncode.DIRECT_MOBILE_ARCHON.encode(new int[] { hostileLoc.x, hostileLoc.y });
					rc.broadcastMessageSignal(myMsg[0], myMsg[1], 10000);
				}
				return true;
			}
		}
		return false;
	}
	 /* 
	 * private static void moveToLocFartherThanAlphaIfPossible(MapLocation here)
	 * throws GameActionException { Direction dir = Direction.NORTH; boolean
	 * shouldMove = false; Direction bestDir = dir; int bestScore = 0; int
	 * nearestScout = distToNearestScout(here); int distanceToAlpha =
	 * here.distanceSquaredTo(alpha); RobotInfo[] enemyRobots =
	 * rc.senseHostileRobots(rc.getLocation(),
	 * RobotType.SCOUT.sensorRadiusSquared); NavSafetyPolicy theSafety = new
	 * SafetyPolicyAvoidAllUnits(enemyRobots); for (int i = 0; i < 8; i++) {
	 * MapLocation newLoc = here.add(dir); if (rc.onTheMap(newLoc) &&
	 * !rc.isLocationOccupied(newLoc) && rc.senseRubble(newLoc) <
	 * GameConstants.RUBBLE_OBSTRUCTION_THRESH) { int newDistanceToAlpha =
	 * newLoc.distanceSquaredTo(alpha); int newNearestScout =
	 * distToNearestScout(newLoc); if (newDistanceToAlpha < range &&
	 * theSafety.isSafeToMoveTo(newLoc)) { int score = 1*(newDistanceToAlpha -
	 * distanceToAlpha) + (nearestScout - newNearestScout)*1; if (score >
	 * bestScore) { bestScore = score; bestDir = dir; shouldMove = true; } } }
	 * dir = dir.rotateLeft(); } if (rc.canMove(bestDir) && shouldMove) {
	 * rc.move(bestDir); } }
	 * 
	 * private static int distToNearestScout(MapLocation loc) throws
	 * GameActionException { RobotInfo[] nearbyAllies =
	 * rc.senseNearbyRobots(loc, 15, us); int nearestScout = 0; for (int i = 0;
	 * i < nearbyAllies.length; i++) { if
	 * (nearbyAllies[i].location.distanceSquaredTo(loc) > nearestScout) {
	 * nearestScout += 100/nearbyAllies[i].location.distanceSquaredTo(loc); } }
	 * return nearestScout; }
	 * 
	 * private static void updateMaxRange(Signal[] signals) { // NEW TO UTIL //
	 * boolean rangeUpdated = false; for (int i = 0; i < signals.length; i++) {
	 * if (signals[i].getTeam() == them) { continue; } int[] message =
	 * signals[i].getMessage(); MessageEncode msgType =
	 * MessageEncode.whichStruct(message[0]); if (signals[i].getTeam() == us &&
	 * msgType == MessageEncode.PROXIMITY_NOTIFICATION) { int[] decodedMessage =
	 * MessageEncode.PROXIMITY_NOTIFICATION.decode(signals[i].getLocation(),
	 * message); range = decodedMessage[0] - 1; // System.out.println(range); //
	 * rangeUpdated = true; break; } } return; // return rangeUpdated; }
	 */
}