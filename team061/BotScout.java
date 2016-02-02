package team061;

import battlecode.common.*;

public class BotScout extends Bot {
	protected static int scoutType; // NEW 0 = turret helper;
									// 1 = mobile helper;
									// 2 = explorer
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
	static MapLocation circlingLoc, farthestLoc;
	static int circlingTime, lastRoundNotifiedOfArmy, lastRoundNotifiedOfPN;
	static int lastCrunchRound;
	static int patience, PATIENCESTART;

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
		// MessageEncode.determineScoutType(); // NEW, based on strategy from archon in messages or something else

		/* // atScoutLocation = false; NEW move this so MESSAGE ENCODE size = 0;
		 * partAndNeutralLocs = new MapLocation[10000]; partsOrNeutrals = new
		 * int[10000]; range = 3;
		 */
		circlingTime = 0;
		scoutType = 0;
		lastRoundNotifiedOfArmy = 0;
		lastRoundNotifiedOfPN = 0;
		lastCrunchRound = 0;
		PATIENCESTART = 20;
		patience = PATIENCESTART; 
		farthestLoc = here;
		directionIAmMoving = center.directionTo(here);
	}

	private static void turn() throws GameActionException {
		here = rc.getLocation();
//		rc.setIndicatorDot(here, 255, 255, 255);
//		rc.setIndicatorString(0, "");
//		rc.setIndicatorString(1, "");
//		String s = "";
//		for(int i = 0; i < targetDenSize; i++){
//			if(targetDens[i] != null)
//			s += "[" + targetDens[i].x + ", " + targetDens[i].y +"], "; 
//		}
//		rc.setIndicatorString(1, s + " " + targetDenSize);
//		rc.setIndicatorString(2, "patience " + patience);
//		String s = "";
//		for (int i = 0; i < turretSize; i++) {
//			s += "[" + enemyTurrets[i].location.x + ", " + enemyTurrets[i].location.y + "], ";
//		}
//		rc.setIndicatorString(0, s + " " + turretSize);
		switch (scoutType) {
		case 0:// exploring
			RobotInfo[] zombies = rc.senseNearbyRobots(type.sensorRadiusSquared, Team.ZOMBIE);
			RobotInfo[] enemies = rc.senseNearbyRobots(here, type.sensorRadiusSquared, them);
			RobotInfo[] allies = rc.senseNearbyRobots(here, type.sensorRadiusSquared, us);
			RobotInfo[] hostiles = Util.removeHarmlessUnits(Util.combineTwoRIArrays(zombies, enemies));
			RobotInfo[] neutrals = rc.senseNearbyRobots(type.sensorRadiusSquared, Team.NEUTRAL);
			patience--;
			boolean turretsUpdated = updateTurretListAndDens(rc.emptySignalQueue(), enemies, Util.getUnitsOfType(allies, RobotType.SCOUT) != null);
			updateProgress();
			if(circlingLoc != null){
				rc.setIndicatorString(0, circlingLoc.toString());
				patience = PATIENCESTART;
			}
			MapLocation enemyArchonLocation = Util.getLocationOfType(enemies, RobotType.ARCHON);
			MapLocation neutralArchonLoc = Util.getLocationOfType(neutrals, RobotType.ARCHON);
			boolean seeEnemyArchon = enemyArchonLocation != null;
			boolean seeNeutralArchon = neutralArchonLoc != null;
			if(seeEnemyArchon){
				directionIAmMoving = here.directionTo(enemyArchonLocation);
				patience = PATIENCESTART;
				farthestLoc = here;
			} else if(seeNeutralArchon){
				directionIAmMoving = here.directionTo(neutralArchonLoc);
				patience = PATIENCESTART;
				farthestLoc = here;
			}
			if (rc.isCoreReady()) {
				if (circlingLoc != null) {
					Nav.goTo(circlingLoc, new SafetyPolicyAvoidAllUnits(Util.combineTwoRIArrays(enemyTurrets, turretSize, hostiles)));
					if(rc.isCoreReady() && hostiles.length > 0)
						Nav.flee(hostiles,allies);
				} else{
					Nav.explore(hostiles, allies);
				}
			}
			// notifySoldiersOfTurtle(hostileRobots);
			// rc.setIndicatorString(2, "found T");
			notifySoldiersOfZombieDen(zombies);
			// if (rc.getRoundNum() % 30 == 0) {
			updateCrunchTime(enemies,allies);
			// }
			int round = rc.getRoundNum();
			if (round - lastRoundNotifiedOfArmy > 25 && (seeEnemyArchon || enemies.length > 2)) {
				notifySoldiersOfEnemyArmy(enemies, seeEnemyArchon);
				lastRoundNotifiedOfArmy = round;
			}
			if (round - lastRoundNotifiedOfPN > 30 && (enemies.length == 0 || Util.closest(enemies, here).location.distanceSquaredTo(here) > 20)) {
				notifyArchonOfPartOrNeutral(neutrals, seeNeutralArchon);
				lastRoundNotifiedOfPN = round;
			}
			break;
		case 1:
			break;
		case 2:
			break;
		default:
			break;
		}
		rc.setIndicatorLine(here, here.add(directionIAmMoving), 255, 255, 255);
		return;
	}

	private static void updateProgress() {
		//see if we are making headway
		Direction dirFromBest = farthestLoc.directionTo(here);
		if(dirFromBest == directionIAmMoving || dirFromBest == directionIAmMoving.rotateLeft() || dirFromBest == directionIAmMoving.rotateRight()){
			patience = PATIENCESTART; 
			farthestLoc = here;
		}
	}

	/*
	 * Overrides updateTurretList in Bot because scouts also have to send
	 * signals. In addition to functions in Bot's version, scouts: -check if
	 * they can see any turrets that haven't been seen before -notify units of
	 * turrets that are no longer there
	 */
	public static boolean updateTurretListAndDens(Signal[] signals, RobotInfo[] enemies, boolean seeScout) throws GameActionException {
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
						break;
					case DEN_NOTIF:
						senderloc = signal.getLocation();
						data = purpose.decode(senderloc, message);
						MapLocation denLoc = new MapLocation(data[0], data[1]);
						if(data[2] == 1){
							if (!Util.containsMapLocation(targetDens, denLoc, targetDenSize)
									&& !Util.containsMapLocation(killedDens, denLoc, killedDenSize)) {
								targetDens[targetDenSize] = denLoc;
								targetDenSize++;
								numDensToHunt++;
							}
						} else if (!Util.containsMapLocation(killedDens, denLoc, killedDenSize)) {
							//rc.setIndicatorString(0, "not going for den at loc " + targetDens[closestIndex] + " on round " + rc.getRoundNum());
							killedDens[killedDenSize] = denLoc;
							killedDenSize++;
							numDensToHunt--;
						}
						break;
					default:
					}
				}
			}
		}

		for (int i = 0; i < turretSize; i++) {
			MapLocation t = enemyTurrets[i].location;
			if (rc.canSenseLocation(t)) {
				RobotInfo bot = rc.senseRobotAtLocation(t);
				if (bot == null || bot.type != RobotType.TURRET) {
					removeLocFromTurretArray(t);
					int[] myMsg = MessageEncode.ENEMY_TURRET_DEATH.encode(
							new int[] { t.x, t.y });
					rc.broadcastMessageSignal(myMsg[0], myMsg[1], 10000);
					i--;
					updated = true;
				} else if (!seeScout && bot.location.distanceSquaredTo(here) < 82)
					circlingLoc = bot.location;
			}
		}
		for (RobotInfo e : enemies)
			if (e.type == RobotType.TURRET) {
				if (circlingLoc == null) {
					circlingLoc = e.location;
				}
				if (!isLocationInTurretArray(e.location)) {
					enemyTurrets[turretSize] = e;
					turretSize++;
					int[] myMsg = MessageEncode.WARN_ABOUT_TURRETS.encode(new int[] { e.location.x, e.location.y});
					rc.broadcastMessageSignal(myMsg[0], myMsg[1], 10000);
					updated = true;
				}
			}
		if (turretSize == 0)
			circlingLoc = null;
		return updated;
	}

	private static void updateCrunchTime(RobotInfo[] enemiesInSight,RobotInfo[] allies) throws GameActionException {
		if(circlingLoc!=null)
			circlingTime+=1;
		else circlingTime = 0;
		if (circlingTime > 100 && circlingLoc != null
		        && areEnoughAlliesEngagedToBeatTheTurrets(enemiesInSight,allies)
		        && rc.getRoundNum() - lastCrunchRound > 25){
			int[] myMsg = MessageEncode.CRUNCH_TIME.encode(new int[] {circlingLoc.x,circlingLoc.y, numTurretsInRangeSquared(circlingLoc, 100) });
			rc.broadcastMessageSignal(myMsg[0], myMsg[1], 10000);
			lastCrunchRound = rc.getRoundNum();
		}
		// rc.setIndicatorString(2, "...");

	}
	private static boolean areEnoughAlliesEngagedToBeatTheTurrets(RobotInfo[] enemiesInSight, RobotInfo[] allies) {
		if(rc.getRoundNum()>2900)
			return true;
		int numEnemiesInTurtle = enemiesInSight.length;
		int numAlliesAttackingCrunch = allies.length;
		boolean alliesEngaged =  numAlliesAttackingCrunch >= numEnemiesInTurtle;
		int numVipers = 0;
		int numSoldiers =0;
		for(RobotInfo bot: allies){
			if(bot.type == RobotType.SOLDIER)
				numSoldiers+=1;
			else if(bot.type == RobotType.VIPER)
				numVipers+=1;
		}
		int viperPower = numVipers*(((int)(rc.getRoundNum() * 1.5) + 750) / 1500);
		boolean canWeBeat = numTurretsInRangeSquared(circlingLoc, 200) < numSoldiers/3 + viperPower;
		return canWeBeat && alliesEngaged;
		
	}
	
	private static void notifySoldiersOfEnemyArmy(RobotInfo[] enemies, boolean seeEnemyArchon) throws GameActionException {
		int[] myMsg = MessageEncode.ENEMY_ARMY_NOTIF
				.encode(new int[] { enemies[0].location.x, enemies[0].location.y, seeEnemyArchon ? 1 : 0 });
		if(!seeEnemyArchon)
			rc.broadcastMessageSignal(myMsg[0], myMsg[1], 5000);
		else
			rc.broadcastMessageSignal(myMsg[0], myMsg[1], 12800);
	}

	private static void notifyArchonOfPartOrNeutral(RobotInfo[] neutrals, boolean seeNeutralArchon) throws GameActionException {
		MapLocation partOrNeutralLoc = null;
		if (neutrals.length > 0) {
			partOrNeutralLoc = neutrals[0].location;
		} else {
			MapLocation[] parts = rc.sensePartLocations(-1);
			if (parts.length > 0)
				partOrNeutralLoc = parts[0];
		}
		if (partOrNeutralLoc != null) {
			int[] myMsg = MessageEncode.PART_OR_NEUTRAL_NOTIF
					.encode(new int[] { partOrNeutralLoc.x, partOrNeutralLoc.y , seeNeutralArchon ? 1 : 0});
			rc.broadcastMessageSignal(myMsg[0], myMsg[1], 4000);
		}
	}

	private static boolean notifySoldiersOfZombieDen(RobotInfo[] hostileRobots) throws GameActionException { // first
		MapLocation denLoc;
		for(int i = targetDenSize; i --> 0;){
			if(targetDens[i] == null) continue;
			denLoc = targetDens[i];
			if (rc.canSenseLocation(denLoc) && (rc.senseRobotAtLocation(denLoc) == null || rc.senseRobotAtLocation(denLoc).type != RobotType.ZOMBIEDEN)) {
				// tell people a den has been killed
				int[] myMsg = MessageEncode.DEN_NOTIF.encode(new int[] { denLoc.x, denLoc.y, 0});
				//System.out.println("dead den");
				rc.broadcastMessageSignal(myMsg[0], myMsg[1], 12800);
				killedDens[killedDenSize] = denLoc;
				targetDens[i] = null;
				killedDenSize++;
				numDensToHunt--;
			}
		}
		for (RobotInfo hostileUnit : hostileRobots) {
			if (hostileUnit.type == RobotType.ZOMBIEDEN) {
				if (!Util.containsMapLocation(targetDens, hostileUnit.location, targetDenSize)) {
					targetDens[targetDenSize] = hostileUnit.location;
					targetDenSize++;
					MapLocation hostileLoc = hostileUnit.location;
					int[] myMsg = MessageEncode.DEN_NOTIF.encode(new int[] { hostileLoc.x, hostileLoc.y, 1});
					rc.broadcastMessageSignal(myMsg[0], myMsg[1], 10000);
				}
				return true;
			}
		}
		return false;
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
	 */
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
	 * MessageEncode.DEN_NOTIF.encode(new
	 * int[]{closestPartOrNeutral.x, closestPartOrNeutral.y});
	 * rc.broadcastMessageSignal(msg[0],msg[1],here.distanceSquaredTo(mobileLoc)
	 * ); lastBroadcasted = closestPartOrNeutral; lastBroadcastedType = type; }
	 * }
	 */
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
