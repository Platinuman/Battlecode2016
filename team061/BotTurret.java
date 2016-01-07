package team061;

import battlecode.common.*;

public class BotTurret extends Bot {
	public static void loop(RobotController theRC) throws GameActionException {
		Bot.init(theRC);

		// Debug.init("micro");

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
		here = rc.getLocation();
		Signal[] signals = rc.emptySignalQueue();
		attackIfApplicable(signals);
	}

	private static void attackIfApplicable(Signal[] signals) {
		if (rc.isWeaponReady()) {
			Combat.shootAtNearbyEnemies();
			if (rc.isWeaponReady()) {
				int[] indicesOfTargetSignals = getIndicesOfTargetSignals(signals);
				int numTargetSignals = indicesOfTargetSignals[indicesOfTargetSignals.length-1];
				MapLocation[] hostileLocations = new MapLocation[numTargetSignals];
				int[] healths = new int[numTargetSignals];
				RobotType[] hostileTypes = new RobotType[numTargetSignals];
				for (int i = 0; i < numTargetSignals; i++) {
					int[] message = signals[indicesOfTargetSignals[i]].getMessage();
					int[] decodedMessage = MessageEncode.TURRET_TARGET.decode(message);
					healths[i] = decodedMessage[0];
					hostileTypes[i] = RobotType.values()[decodedMessage[1]];
					hostileLocations[i] = new MapLocation(decodedMessage[2], decodedMessage[3]);
				}
				Combat.shootBestEnemyTakingIntoAccountScoutInfo(hostileLocations, healths, hostileTypes);
			}
		}
	}

	private static int[] getIndicesOfTargetSignals(Signal[] signals) {
		int[] indicesOfTargetSignals = new int[signals.length+1];
		int count = 0;
		for (int i = 0; i < signals.length; i++) {
			int[] message = signals[i].getMessage();
			MessageEncode msgType = MessageEncode.whichStruct(message[0]);
			if(msgType == MessageEncode.TURRET_TARGET){
				indicesOfTargetSignals[count] = i;
				count++;
			}
		}
		indicesOfTargetSignals[signals.length] = count;
		return indicesOfTargetSignals;
	}
	
}