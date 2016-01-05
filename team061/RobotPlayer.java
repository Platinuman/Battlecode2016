package team061;

import battlecode.common.*;

public class RobotPlayer {

	public static void run(RobotController rc) throws Exception {

		switch (rc.getType()){
		case ARCHON:
			botArchon.loop(rc);
			break;

		case GUARD:
			botGuard.loop(rc);
			break;

		case SCOUT:
			botScout.loop(rc);
			break;

		case SOLDIER:
			botSoldier.loop(rc);
			break;

		case TURRET:
			botTurret.loop(rc);
			break;

		case VIPER:
			botViper.loop(rc);
			break;
		default:
			throw new Exception("Unknown robot type!");
		}
	}
}
