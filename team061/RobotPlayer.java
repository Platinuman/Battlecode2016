package team061;

import battlecode.common.*;

public class RobotPlayer {

	public static void run(RobotController rc) throws Exception {

		switch (rc.getType()) {
		case ARCHON:
			BotArchon.loop(rc);
			break;

		case GUARD:
			BotGuard.loop(rc);
			break;

		case SCOUT:
			BotScout.loop(rc);
			break;

		case SOLDIER:
			BotSoldier.loop(rc);
			break;

		case TURRET:
			BotTurret.loop(rc);
			break;

		case VIPER:
			BotViper.loop(rc);
			break;
		default:
			throw new Exception("Unknown robot type!");
		}
	}
}
