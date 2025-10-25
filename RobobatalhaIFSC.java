package roboifsc;
import robocode.*;
//import java.awt.Color;

// API help : https://robocode.sourceforge.io/docs/robocode/robocode/Robot.html

/**
 * RobobatalhaIFSC - a robot by (your name here)
 */
public class RobobatalhaIFSC extends Robot
{
	/**
	 * run: RobobatalhaIFSC's default behavior
	 */
	public void run() {
		// Initialization of the robot should be put here

		// After trying out your robot, try uncommenting the import at the top,
		// and the next line:

		// setColors(Color.red,Color.blue,Color.green); // body,gun,radar

		// Robot main loop
		while(true) {
			// Replace the next 4 lines with any behavior you wod like
			ahead(100);
			turnGunRight(360);
			back(100);
			turnGunRight(360);
		}
	}

	/**
	 * onScannedRobot: What to do when you see another robot
	 */
	public void onScannedRobot(ScannedRobotEvent e) {
		// Trava o radar no inimigo
		double radarTurn = getHeading() + e.getBearing() - getRadarHeading();
		turnRadarRight(normalizeBearing(radarTurn));

		// Mira e atira (temporário)
		double absoluteBearing = getHeading() + e.getBearing();
		double gunTurn = normalizeBearing(absoluteBearing - getGunHeading());
		turnGunRight(gunTurn);

		if (Math.abs(gunTurn) < 10) {
			fire(2);
		}
	}

	public double normalizeBearing(double angle) {
		while (angle > 180) angle -= 360;
		while (angle < -180) angle += 360;
		return angle;
	}

	/**
	 * onHitByBullet: What to do when you're hit by a bullet
	 */
	public void onHitByBullet(HitByBulletEvent e) {
		// Replace the next line with any behavior you would like
		back(10);
	}
	
	/**
	 * onHitWall: What to do when you hit a wall
	 */
	public void onHitWall(HitWallEvent e) {
		// Replace the next line with any behavior you would like
		back(20);
	}	
}