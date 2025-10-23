package roboifsc;
import robocode.*;
import java.awt.Color;

public class RobobatalhaIFSC extends AdvancedRobot {
	double energiaInicial = 100;
	public void run() {
		setColors(Color.red,Color.blue,Color.blue); 
		setAdjustGunForRobotTurn(true);
		setAdjustRadarForGunTurn(true);
		while(true) {
			turnRadarRight(360);
		}
	}

	public void onScannedRobot(ScannedRobotEvent e) {
		double quedaDeEnergia = energiaInicial - e.getEnergy();

		if (quedaDeEnergia>0&&quedaDeEnergia<=3) {
			setTurnRight(e.getBearing()+90+(Math.random()*90-45));
			setAhead(150*(Math.random()>0.5?1 : -1));
		}

		double radarLock = getHeading() - getRadarHeading() + e.getBearing();
		setTurnRadarRight(radarLock);
		execute();
	}
}
	