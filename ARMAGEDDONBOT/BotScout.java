package ARMAGEDDONBOT;

import battlecode.common.*;

public class BotScout extends Bot {
	protected static int scoutType;
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
			RobotInfo[] zombies = rc.senseNearbyRobots(type.sensorRadiusSquared, Team.ZOMBIE);
			RobotInfo[] allies = rc.senseNearbyRobots(here, type.sensorRadiusSquared, us);
			RobotInfo[] neutrals = rc.senseNearbyRobots(type.sensorRadiusSquared, Team.NEUTRAL);
			patience--;
			updateTurretListAndDens(rc.emptySignalQueue(), zombies);
			updateProgress();
				MapLocation neutralArchonLoc = Util.getLocationOfType(neutrals, RobotType.ARCHON);
			//boolean seeEnemyArchon = enemyArchonLocation != null;
			boolean seeNeutralArchon = neutralArchonLoc != null;

			if(seeNeutralArchon){
				directionIAmMoving = here.directionTo(neutralArchonLoc);
				patience = PATIENCESTART;
				farthestLoc = here;
			}
			if (rc.isCoreReady()) {

					Nav.explore(zombies, allies);
			}
			notifySoldiersOfZombieDen(zombies);
			String dens = "";
			if(targetDenSize > 0){
				for(int i = 0; i < targetDenSize; i++){
					MapLocation den = targetDens[i];
					if(den != null)
						dens += den.toString() + ", ";
				}
			}
			rc.setIndicatorString(0, dens);
	}

	private static void updateProgress() {
		Direction dirFromBest = farthestLoc.directionTo(here);
		if(dirFromBest == directionIAmMoving || dirFromBest == directionIAmMoving.rotateLeft() || dirFromBest == directionIAmMoving.rotateRight()){
			patience = PATIENCESTART; 
			farthestLoc = here;
		}
	}

	public static boolean updateTurretListAndDens(Signal[] signals, RobotInfo[] enemies) throws GameActionException {
		boolean updated = false;
		for (Signal signal : signals) {
			if (signal.getTeam() == us) {
				int[] message = signal.getMessage();
				if (message != null) {
					MessageEncode purpose = MessageEncode.whichStruct(message[0]);
					int[] data;
					MapLocation senderloc, loc;
					switch(purpose){

					case DEN_NOTIF:
						senderloc = signal.getLocation();
						data = purpose.decode(senderloc, message);
						MapLocation denLoc = new MapLocation(data[0], data[1]);
						if(data[2] == 1){
							if (!Util.containsMapLocation(targetDens, denLoc, targetDenSize)){
								targetDens[targetDenSize] = denLoc;
								targetDenSize++;
								numDensToHunt++;
								updated = true;
							}
						} else {
							int deadDenIndex = Util.indexOfLocation(targetDens, targetDenSize, denLoc);
							if(deadDenIndex != -1){
								targetDens[deadDenIndex] = null;
								numDensToHunt--;
								updated = true;
							}
						}
						break;
					default:
					}
				} else {
					MapLocation signalLoc = signal.getLocation();
					int closestIndex = Util.closestLocation(targetDens, signalLoc, targetDenSize);
					if (closestIndex != -1) {
						MapLocation killedDen = targetDens[closestIndex];
						targetDens[closestIndex] = null;
						numDensToHunt--;
						updated = true;
					}
				}
			}
		}
		return updated;
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
				//killedDens[killedDenSize] = denLoc;
				targetDens[i] = null;
				//killedDenSize++;
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
			}
		}
		return false;
	}
}
